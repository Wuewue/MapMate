package com.mapmate.device

// shared primitive used by both device & privacy packages, no single req id
// lives in device/ because raw GPS coords originate from the device
// no range check on purpose, gps gives sane values & blur offsets stay tiny
data class Coordinates(
    val latitude: Double,
    val longitude: Double,
)
