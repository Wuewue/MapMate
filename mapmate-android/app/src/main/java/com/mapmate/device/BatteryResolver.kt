package com.mapmate.device

import java.time.Duration
import java.time.Instant

// req: UR-B1, SRS-DI-001, SRS-DI-005, battery sharing gate & last-known fallback
// BatteryResolver: decides what battery info a friend sees, same toggle idea as AlertDetector
// Priority order:
// 1. sharing toggled off by user -> null
// 2. battery permission missing -> null
// 3. fresh snapshot -> live reading
// 4. stale snapshot -> same reading flagged isFresh = false

object BatteryResolver {
    fun resolve(
        snapshot: DeviceSnapshot,
        now: Instant,
        sharingEnabled: Boolean, // user toggle, same idea as alertsEnabled
        staleAfter: Duration = Duration.ofMinutes(5),
    ): BatteryVisibility? {

        // if the user turned battery sharing off, share nothing
        if (!sharingEnabled) return null

        // no permission to read battery -> nothing to share
        if (!snapshot.batteryPermissionGranted) return null

        // stale check, same 5 min default & same .abs() clock-skew guard the other two use
        val isStale = Duration.between(snapshot.lastUpdatedAt, now).abs() > staleAfter

        return BatteryVisibility(
            status   = snapshot.battery,
            asOfTime = snapshot.lastUpdatedAt,
            isFresh  = !isStale,
        )
    }
}
