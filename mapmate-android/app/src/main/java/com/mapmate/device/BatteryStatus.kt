package com.mapmate.device

// req: UR-B1, SRS-DI-001, battery % + charging status sharing
data class BatteryStatus(
    val percentage: Int,        // 0..100, validated below
    val isCharging: Boolean,    // true = plugged in
) {
    // init runs every time a BatteryStatus instance is constructed
    // require() throws IllegalArgumentException if the condition is false
    init {
        require(percentage in 0..100) { "battery % out of range: $percentage" }
    }

    val level: BatteryLevel
        get() = when { // computed property -> runs every time .level is read
            isCharging && percentage == 100 -> BatteryLevel.FULL
            isCharging -> BatteryLevel.CHARGING
            percentage <= 15 -> BatteryLevel.LOW
            else -> BatteryLevel.NORMAL
        }
}
