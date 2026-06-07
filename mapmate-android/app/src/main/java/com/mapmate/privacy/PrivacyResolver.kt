package com.mapmate.privacy

import com.mapmate.device.Coordinates
import com.mapmate.device.DeviceSnapshot
import java.time.Duration
import java.time.Instant

// req: UR-C1, UR-C2, SRS-PRV-001, SRS-PRV-002, SRS-PRV-003, SRS-PRV-004, SRS-PRV-005, SRS-DI-005
// implements all three privacy modes & the last-known fallback when data is stale
//
// take the user's friend snapshot -> what the viewer should actually see

// decision tree (top to bottom, first match wins):
// 1. location permission revoked -> return Hidden (strictest signal, beats every mode)
// 2. mode is Frozen -> return the frozen coords
// 3. snapshot has no location -> return Hidden
// 4. data is stale -> return last know with mode-appropriate coords
// 5. mode is Precise & fresh -> return EXACT with real coords
// 6. mode is Blurred & fresh -> return APPROXIMATE with randomised centre

object PrivacyResolver {
    fun resolve(
        mode: PrivacyMode,
        snapshot: DeviceSnapshot, // user's friend current device state
        now: Instant, // current time
        staleAfter: Duration = Duration.ofMinutes(5),
    ): Visibility {
        // req: SRS-PRV-001, if friend revoked location permission dont leak cached coords
        // permission denial is the strictest signal -> HIDDEN no matter what mode is set
        if (!snapshot.locationPermissionGranted) {
            return Visibility(
                coordinates = null,
                precision   = Precision.HIDDEN,
                asOfTime    = null,
            )
        }
        // Frozen
        if (mode is PrivacyMode.Frozen) {
            return Visibility(
                coordinates = mode.anchorLocation,
                precision   = Precision.FROZEN,
                asOfTime    = mode.anchorTime,
            )
        }
        // No location -> hidden
        if (snapshot.location == null) {
            return Visibility(
                coordinates = null,
                precision   = Precision.HIDDEN,
                asOfTime    = null,
            )
        }
        // Stale -> give last known location
        // .abs() so a future timestamp from clock skew can't look fresh forever
        // > not >= so exactly at the threshold still counts as fresh
        val isStale = Duration.between(snapshot.lastUpdatedAt, now).abs() > staleAfter

        // Precise
        return when (mode) {
            is PrivacyMode.Precise -> Visibility(
                coordinates = snapshot.location,
                precision   = if (isStale) Precision.LAST_KNOWN else Precision.EXACT,
                asOfTime    = snapshot.lastUpdatedAt,
            )
            // Blur
            is PrivacyMode.Blurred -> Visibility(
                coordinates = randomNearbyPoint(snapshot.location, mode.radiusMeters), // offset coords
                precision   = if (isStale) Precision.LAST_KNOWN else Precision.APPROXIMATE,
                asOfTime    = snapshot.lastUpdatedAt,
                approximateRadiusMeters = mode.radiusMeters, // for circle render
            )
            is PrivacyMode.Frozen -> error("can't get here, frozen handled above")
        }
    }

    // req: SRS-PRV-004, backs the Blurred mode guarantee
    // helper: random centre for the Blurred circle
    // privacy guarantee: the real location always lands inside the displayed circle
    private fun randomNearbyPoint(realPoint: Coordinates, radiusMeters: Int): Coordinates {
        // pick a random direction
        // 2π radians = full circle. Math.random() returns 0..1
        val angleRadians = Math.random() * 2 * Math.PI

        // pick a random change in distance
        // because Math.random() returns 0..1 -> this is guaranteed ≤ radiusMeters
        // in theory random() can hit exactly 0 -> zero offset, odds ~1 in 2^53, fine for our scope
        val distanceMeters = Math.random() * radiusMeters

        // convert metres to degrees of lat/lon
        // 1 deg latitude approx 111 km anywhere on earth
        // 1 deg longitude approx 111 km * cos(lat) -> shorter as you move away from equator
        // no wrap at ±180 & cos(lat) hits 0 at the poles, real demo locations only
        val latRad = Math.toRadians(realPoint.latitude)
        val degreesPerMeterLat = 1.0 / 111_000.0
        val degreesPerMeterLon = 1.0 / (111_000.0 * kotlin.math.cos(latRad))

        // break into cos and sin components
        // distance * conversion rate = degree offset to walk
        val latOffset = distanceMeters * kotlin.math.cos(angleRadians) * degreesPerMeterLat
        val lonOffset = distanceMeters * kotlin.math.sin(angleRadians) * degreesPerMeterLon

        // offset applied, done
        return Coordinates(
            latitude  = realPoint.latitude  + latOffset,
            longitude = realPoint.longitude + lonOffset,
        )
    }
}
