package com.mapmate.privacy

import com.mapmate.device.DeviceSnapshot
import java.time.Instant

// req: UR-C4, payload pushed to teammate 1's notification module
data class OffTheGridAlert(
    val friendId: String,
    val reason: OffTheGridReason,
    val detectedAt: Instant,
    val lastKnownSnapshot: DeviceSnapshot, // the snapshot that triggered the alert
)
