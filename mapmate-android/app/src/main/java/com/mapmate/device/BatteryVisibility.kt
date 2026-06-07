package com.mapmate.device

import java.time.Instant

// req: UR-B1, SRS-DI-001, SRS-DI-005, what a friend actually sees of my battery
// the battery twin of Visibility, null from the resolver = friend sees nothing
data class BatteryVisibility(
    val status: BatteryStatus,
    val asOfTime: Instant,  // when this reading was captured
    val isFresh: Boolean,   // false = cached fallback, ui shows "last known"
)
