package com.mapmate.telemetry

data class BatteryStatus(
    val percentage: Int,
    val isCharging: Boolean,
    val state: KBatteryState
) {
    companion object {
        fun from(percentage: Int, isCharging: Boolean): BatteryStatus {
            return BatteryStatus(
                percentage = percentage,
                isCharging = isCharging,
                state = KBatteryState.fromPercentage(percentage, isCharging)
            )
        }
    }
}

enum class KBatteryState {
    CHARGING,
    FULL,
    LOW,
    NORMAL;

    companion object {
        fun fromPercentage(percentage: Int, isCharging: Boolean): KBatteryState = when {
            isCharging       -> CHARGING
            percentage >= 95 -> FULL
            percentage < 20  -> LOW
            else             -> NORMAL
        }
    }
}