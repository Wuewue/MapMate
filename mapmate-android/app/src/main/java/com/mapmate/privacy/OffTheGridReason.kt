package com.mapmate.privacy

// req: UR-C4, the three triggers for an Off-the-Grid alert
enum class OffTheGridReason {
    BATTERY_DEAD, // battery hit 0%
    SIGNAL_LOST, // confirmed via OS, firebase etc that the device is offline
    NO_RECENT_UPDATE, // haven't heard from them within the alert staleness window (i set as 30 min)
}
