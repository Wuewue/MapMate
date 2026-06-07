package com.mapmate.demo

import com.mapmate.device.BatteryStatus
import com.mapmate.device.Coordinates
import com.mapmate.device.DeviceSnapshot
import com.mapmate.distance.Distance
import com.mapmate.privacy.AlertDetector
import com.mapmate.privacy.ConnectionState
import com.mapmate.privacy.OffTheGridAlert
import com.mapmate.privacy.PrivacyMode
import com.mapmate.privacy.PrivacyResolver
import com.mapmate.privacy.Visibility
import com.mapmate.telemetry.KTransportMode
import com.mapmate.telemetry.SpeedSmoother
import com.mapmate.telemetry.TransportClassifier
import java.time.Duration
import java.time.Instant

// this is the actual merge -> every member's real code runs here together on seeded data
// so the app demonstrates the whole pipeline live on a bare emulator (no firebase needed)
//   khanh -> SpeedSmoother + TransportClassifier give each friend a live transport mode
//   mike  -> PrivacyResolver decides what coords the viewer sees (precise/blurred/frozen/hidden)
//   mike  -> AlertDetector fires off-the-grid alerts (battery dead / signal lost / stale)
//   tue   -> Distance.haversine runs on the PRIVACY-FILTERED coords, never the real spot
//   teo   -> profile/auth/friends layer drives login + the friends list (see profile package)
// pure logic, no android imports -> same "testable without android" pattern as mike's lib

// what one friend looks like to the viewer after the whole pipeline runs
data class FriendState(
    val id: String,
    val name: String,
    val initials: String,
    // khanh's classifier output
    val transport: KTransportMode,
    // mike's battery layer
    val batteryPercent: Int,
    val charging: Boolean,
    val batteryLevel: com.mapmate.device.BatteryLevel,
    // mike's privacy resolver output -> drives the map render
    val precision: com.mapmate.privacy.Precision,
    val renderLat: Double?,   // privacy filtered coords, null when hidden
    val renderLng: Double?,
    val blurRadiusMeters: Int?,
    // normalised 0..1 position on the stylized map canvas (from the FILTERED coords)
    val mapX: Float?,
    val mapY: Float?,
    // tue's distance, computed from the filtered coords
    val distanceMeters: Int?,
    val isNearby: Boolean,
    val online: Boolean,
)

// the self chip -> my own transport + battery, plus whether i'm sharing my location
data class SelfState(
    val transport: KTransportMode,
    val batteryPercent: Int,
    val batteryLevel: com.mapmate.device.BatteryLevel,
    val sharing: Boolean = true,
)

// one off-the-grid alert (mike's AlertDetector)
data class AlertState(
    val friendId: String,
    val friendName: String,
    val reason: com.mapmate.privacy.OffTheGridReason,
)

// full snapshot the ui renders each tick
data class DemoState(
    val self: SelfState,
    val friends: List<FriendState>,
    val alerts: List<AlertState>,
    val nearbyCount: Int,
)

class MapMateEngine(
    // "me" sits at the center of the demo map
    private val myLat: Double = 10.7769,
    private val myLng: Double = 106.7009,
) {
    // bounding box (~ +/- 1.5km) used to map real coords -> 0..1 canvas position
    private val spanDeg = 0.018

    // app-level toggles, driven by the settings screen
    private var alertsEnabled = true
    private var shareLocation = true
    fun setAlertsEnabled(enabled: Boolean) { alertsEnabled = enabled }
    fun setShareLocation(enabled: Boolean) { shareLocation = enabled }

    // per-friend mutable sim state (the moving parts)
    private inner class Sim(
        val id: String,
        val name: String,
        val initials: String,
        var lat: Double,
        var lng: Double,
        var heading: Double,           // radians, direction of travel
        var targetSpeed: Float,        // m/s we're nudging toward
        var privacy: PrivacyMode,
        var batteryPercent: Int,
        var charging: Boolean,
        var connection: ConnectionState,
        var alertsEnabled: Boolean,
        var lastUpdatedAt: Instant,
        // khanh's filter + classifier, one instance per friend so hysteresis is per person
        val smoother: SpeedSmoother = SpeedSmoother(),
        val classifier: TransportClassifier = TransportClassifier(),
        var phoneDead: Boolean = false,
    )

    private val sims = mutableListOf<Sim>()
    private var clock: Instant = Instant.parse("2026-06-07T09:00:00Z")

    init {
        // seed a small friend group, each with a different privacy mode so all paths show
        sims += Sim(
            id = "maya", name = "Maya Chen", initials = "MC",
            lat = myLat + 0.004, lng = myLng + 0.003, heading = 0.6, targetSpeed = 1.4f,
            privacy = PrivacyMode.Precise,
            batteryPercent = 76, charging = false, connection = ConnectionState.ONLINE,
            alertsEnabled = true, lastUpdatedAt = clock,
        )
        sims += Sim(
            id = "noah", name = "Noah Park", initials = "NP",
            lat = myLat - 0.0035, lng = myLng + 0.0052, heading = 2.1, targetSpeed = 6.5f,
            privacy = PrivacyMode.Blurred(500),
            batteryPercent = 44, charging = false, connection = ConnectionState.ONLINE,
            alertsEnabled = true, lastUpdatedAt = clock,
        )
        sims += Sim(
            id = "sofia", name = "Sofia Reyes", initials = "SR",
            lat = myLat + 0.0061, lng = myLng - 0.0044, heading = 4.0, targetSpeed = 13.0f,
            privacy = PrivacyMode.Frozen(Coordinates(myLat + 0.0061, myLng - 0.0044), clock),
            batteryPercent = 90, charging = true, connection = ConnectionState.ONLINE,
            alertsEnabled = true, lastUpdatedAt = clock,
        )
        sims += Sim(
            id = "leo", name = "Leo Tran", initials = "LT",
            lat = myLat - 0.007, lng = myLng - 0.006, heading = 1.0, targetSpeed = 0.2f,
            privacy = PrivacyMode.Precise,
            batteryPercent = 12, charging = false, connection = ConnectionState.ONLINE,
            alertsEnabled = true, lastUpdatedAt = clock,
        )
    }

    // change a friend's privacy mode (settings screen calls this) -> next tick re-renders
    fun setPrivacy(friendId: String, mode: PrivacyMode) {
        sims.firstOrNull { it.id == friendId }?.let { sim ->
            // frozen anchors at the friend's current spot + now
            sim.privacy = if (mode is PrivacyMode.Frozen) {
                PrivacyMode.Frozen(Coordinates(sim.lat, sim.lng), clock)
            } else {
                mode
            }
        }
    }

    // demo button -> make a friend's phone die so mike's AlertDetector fires BATTERY_DEAD
    fun simulatePhoneDying(friendId: String) {
        sims.firstOrNull { it.id == friendId }?.let {
            it.phoneDead = true
            it.batteryPercent = 0
            it.connection = ConnectionState.OFFLINE
        }
    }

    fun friendIds(): List<String> = sims.map { it.id }

    // accept a friend request -> add them to the live group, they appear on the map next tick
    fun addFriend(name: String, initials: String) {
        val n = sims.size
        val ang = n * 1.3
        sims += Sim(
            id = "added-$initials-$n".lowercase(),
            name = name,
            initials = initials,
            lat = myLat + 0.005 * kotlin.math.cos(ang),
            lng = myLng + 0.005 * kotlin.math.sin(ang),
            heading = ang,
            targetSpeed = 1.2f,
            privacy = PrivacyMode.Precise,
            batteryPercent = 80,
            charging = false,
            connection = ConnectionState.ONLINE,
            alertsEnabled = true,
            lastUpdatedAt = clock,
        )
    }

    // advance the simulation one step + run everyone's pipeline -> fresh DemoState
    fun tick(stepMillis: Long = 1500L): DemoState {
        clock = clock.plusMillis(stepMillis)
        val dtSeconds = stepMillis / 1000.0

        val friendStates = sims.map { sim ->
            advance(sim, dtSeconds)

            // ---- khanh: smooth the raw speed then classify transport mode ----
            val rawSpeed = if (sim.phoneDead) 0f else sim.targetSpeed + (Math.random().toFloat() - 0.5f)
            val smoothed = sim.smoother.add(rawSpeed.coerceAtLeast(0f))
            val transport = sim.classifier.classify(smoothed)

            // ---- mike: build the device snapshot, then resolve what the viewer sees ----
            val battery = BatteryStatus(percentage = sim.batteryPercent, isCharging = sim.charging)
            val snapshot = DeviceSnapshot(
                battery = battery,
                // advance() freezes position when phoneDead, so lat/lng are already the last-known spot
                location = Coordinates(sim.lat, sim.lng),
                lastUpdatedAt = if (sim.phoneDead) sim.lastUpdatedAt else clock.also { sim.lastUpdatedAt = it },
                locationPermissionGranted = true,
                batteryPermissionGranted = true,
            )
            val visibility: Visibility = PrivacyResolver.resolve(sim.privacy, snapshot, clock)

            // ---- tue: distance from me to the FILTERED point (never the real spot) ----
            val coords = visibility.coordinates
            val distM = coords?.let { Distance.haversineMeters(myLat, myLng, it.latitude, it.longitude) }
            val near = coords?.let { Distance.isNearby(myLat, myLng, it.latitude, it.longitude) } ?: false

            FriendState(
                id = sim.id,
                name = sim.name,
                initials = sim.initials,
                transport = transport,
                batteryPercent = sim.batteryPercent,
                charging = sim.charging,
                batteryLevel = battery.level,
                precision = visibility.precision,
                renderLat = coords?.latitude,
                renderLng = coords?.longitude,
                blurRadiusMeters = visibility.approximateRadiusMeters,
                mapX = coords?.let { normX(it.longitude) },
                mapY = coords?.let { normY(it.latitude) },
                distanceMeters = distM,
                isNearby = near,
                online = sim.connection != ConnectionState.OFFLINE && !sim.phoneDead,
            )
        }

        // ---- mike: off-the-grid alerts across all friends (gated by the user's alerts toggle) ----
        val alerts = if (!alertsEnabled) emptyList<AlertState>() else sims.mapNotNull { sim ->
            val battery = BatteryStatus(percentage = sim.batteryPercent, isCharging = sim.charging)
            val snapshot = DeviceSnapshot(
                battery = battery,
                // AlertDetector never reads location -> pass null so the real spot can't leak via lastKnownSnapshot
                location = null,
                lastUpdatedAt = sim.lastUpdatedAt,
                locationPermissionGranted = true,
                batteryPermissionGranted = true,
            )
            val alert: OffTheGridAlert? = AlertDetector.detect(
                friendId = sim.id,
                snapshot = snapshot,
                now = clock,
                alertsEnabled = sim.alertsEnabled,
                knownConnectionState = sim.connection,
            )
            alert?.let { AlertState(it.friendId, sim.name, it.reason) }
        }

        // self: classify my own (parked) state for the self chip
        val self = SelfState(
            transport = KTransportMode.WALK,
            batteryPercent = 82,
            batteryLevel = BatteryStatus(82, false).level,
            sharing = shareLocation,
        )

        return DemoState(
            self = self,
            friends = friendStates,
            alerts = alerts,
            nearbyCount = friendStates.count { it.isNearby },
        )
    }

    // nudge a friend along its heading, wander a little, keep it on the map
    private fun advance(sim: Sim, dtSeconds: Double) {
        if (sim.phoneDead) return
        val metersMoved = sim.targetSpeed * dtSeconds
        val dLat = (metersMoved * kotlin.math.cos(sim.heading)) / 111_000.0
        // coerceAtLeast guards against cos(lat)->0 near the poles (NaN); demo is nowhere near, but safe
        val dLng = (metersMoved * kotlin.math.sin(sim.heading)) /
            (111_000.0 * kotlin.math.cos(Math.toRadians(sim.lat)).coerceAtLeast(0.01))
        sim.lat += dLat
        sim.lng += dLng
        // gentle wander + bounce back toward center if drifting off the box
        sim.heading += (Math.random() - 0.5) * 0.4
        if (kotlin.math.abs(sim.lat - myLat) > spanDeg || kotlin.math.abs(sim.lng - myLng) > spanDeg) {
            sim.heading = kotlin.math.atan2(myLng - sim.lng, myLat - sim.lat)
        }
        // slowly drain battery so the demo feels alive
        if (!sim.charging && Math.random() < 0.15 && sim.batteryPercent > 0) sim.batteryPercent--
    }

    // map real coords -> 0..1 on the canvas, clamped
    private fun normX(lng: Double): Float =
        (((lng - (myLng - spanDeg)) / (2 * spanDeg)).toFloat()).coerceIn(0.05f, 0.95f)

    private fun normY(lat: Double): Float =
        ((1.0 - (lat - (myLat - spanDeg)) / (2 * spanDeg)).toFloat()).coerceIn(0.05f, 0.95f)
}
