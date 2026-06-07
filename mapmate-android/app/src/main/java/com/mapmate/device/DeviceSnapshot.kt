package com.mapmate.device

import java.time.Instant

// req: SRS-DI-001, SRS-DI-005, UR-B3, bundles battery & location & last-update fallback & permission flags
data class DeviceSnapshot(
    val battery: BatteryStatus,
    val location: Coordinates?, // ? means can be null when no GPS fix
    val lastUpdatedAt: Instant, // java.time.Instant = a precise moment in time
    val locationPermissionGranted: Boolean,
    val batteryPermissionGranted: Boolean,
)
