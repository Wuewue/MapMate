package com.mapmate.privacy

import com.mapmate.device.DeviceSnapshot
import java.time.Duration
import java.time.Instant

// req: UR-C4, caller hint about the friend's network state (OS / firebase / etc)
enum class ConnectionState { ONLINE, OFFLINE, UNKNOWN }

// req: UR-C4, UR-C5, alert detection with on/off toggle
// AlertDetector: looks at a friend's current snapshot, decides whether an alert should fire and what reason to report
// Priority order:
// 1. alerts disabled by user
// 2. battery == 0% -> BATTERY_DEAD
// 3. caller hints OFFLINE -> SIGNAL_LOST
// 4. snapshot is stale -> NO_RECENT_UPDATE
// 5. otherwise -> null

object AlertDetector {
    fun detect(
        friendId: String,
        snapshot: DeviceSnapshot,
        now: Instant,
        alertsEnabled: Boolean, // user toggle
        staleAfter: Duration = Duration.ofMinutes(30),
        knownConnectionState: ConnectionState = ConnectionState.UNKNOWN,
    ): OffTheGridAlert? {

        // if the user turned alerts off, never fire anything
        if (!alertsEnabled) return null

        // when is a chain of conditions, first true wins
        val reason: OffTheGridReason? = when {
            // 0% fires even if plugged in, phone is off until it gains some charge
            snapshot.battery.percentage == 0 -> OffTheGridReason.BATTERY_DEAD
            // only confirmed OFFLINE fires this, ONLINE & UNKNOWN both skip the arm
            knownConnectionState == ConnectionState.OFFLINE -> OffTheGridReason.SIGNAL_LOST
            // .abs() so clock skew can't hide a quiet device, ONLINE does not suppress this check
            Duration.between(snapshot.lastUpdatedAt, now).abs() > staleAfter -> OffTheGridReason.NO_RECENT_UPDATE
            else -> null
        }

        // if nothing triggered, no alert
        if (reason == null) return null

        // build and return the alert payload
        return OffTheGridAlert(
            friendId = friendId,
            reason = reason,
            detectedAt = now,
            lastKnownSnapshot = snapshot,
        )
    }
}
