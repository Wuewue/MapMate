package com.mapmate.privacy

import com.mapmate.device.Coordinates
import java.time.Instant

// req: UR-C1, UR-C2, SRS-PRV-001, SRS-PRV-002, per-friend privacy mode (one of three)
// the user's per-friend privacy choice
// sealed class = closed family -> PrivacyMode can ONLY be one of the three
sealed class PrivacyMode {
    // req: SRS-PRV-003, exact GPS visible
    // Precise
    object Precise : PrivacyMode()

    // req: SRS-PRV-004, randomised approximate area, not exact pin
    // Blurred
    data class Blurred(
        val radiusMeters: Int = 500, // set radius as 500 for now
    ) : PrivacyMode() {
        // same fail-fast pattern as BatteryStatus, a circle needs a real radius
        init {
            require(radiusMeters > 0) { "blur radius must be positive: $radiusMeters" }
        }
    }

    // req: SRS-PRV-005, last known location locked at anchor point
    // Frozen
    data class Frozen( // basically captures that moment user activate Frozen mode
        val anchorLocation: Coordinates,
        val anchorTime: Instant,
    ) : PrivacyMode()
}
