package com.mapmate.telemetry

enum class KMotionStatus {
    STILL,
    WALKING,
    MOVING;

    companion object {
        fun fromSpeed(speedMps: Float): KMotionStatus = when {
            speedMps < 0.5f -> STILL
            speedMps < 2.5f -> WALKING
            else            -> MOVING
        }
    }
}