package com.mapmate.device

// req: UR-B1, SRS-DI-001, SRS-DI-006, derived view of battery status
// LOW = 15% or lower, not charging
// NORMAL = 16% or higher, not charging (100% unplugged lands here too)
// CHARGING = any %, plugged in (below 100)
// FULL = 100%, plugged in
enum class BatteryLevel { LOW, NORMAL, CHARGING, FULL }
