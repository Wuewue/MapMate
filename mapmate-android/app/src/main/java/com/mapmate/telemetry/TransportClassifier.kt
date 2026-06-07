package com.mapmate.telemetry

import android.util.Log

class TransportClassifier {

    companion object {
        private const val TAG             = "TransportClassifier"
        private const val WALK_MAX_SPEED  = 2.5f
        private const val BIKE_MAX_SPEED  = 8.0f
        private const val SHIP_MIN_SPEED  = 2.0f
        private const val SHIP_MAX_SPEED  = 25.0f
        private const val CONFIRM_FROM_UNKNOWN = 2
        private const val CONFIRM_NORMAL       = 3
        private const val CONFIRM_CAR_TO_WALK  = 5
    }

    var currentMode: KTransportMode = KTransportMode.UNKNOWN
        private set

    private var pendingMode: KTransportMode = KTransportMode.UNKNOWN
    private var confirmCount: Int = 0

    fun classify(smoothedSpeed: Float, isOverWater: Boolean = false): KTransportMode {
        val candidate = computeCandidate(smoothedSpeed, isOverWater)

        if (candidate == currentMode) {
            confirmCount = 0
            pendingMode = currentMode
            return currentMode
        }

        if (candidate == pendingMode) {
            confirmCount++
        } else {
            pendingMode = candidate
            confirmCount = 1
        }

        val required = requiredConfirmations(currentMode, pendingMode)
        if (confirmCount >= required) {
            Log.d(TAG, "🚗 Mode: $currentMode → $pendingMode")
            currentMode = pendingMode
            confirmCount = 0
        }

        return currentMode
    }

    private fun computeCandidate(speed: Float, isOverWater: Boolean): KTransportMode {
        if (isOverWater && speed in SHIP_MIN_SPEED..SHIP_MAX_SPEED) return KTransportMode.SHIP
        return when {
            speed < WALK_MAX_SPEED -> KTransportMode.WALK
            speed < BIKE_MAX_SPEED -> KTransportMode.BIKE
            else                   -> KTransportMode.CAR
        }
    }

    private fun requiredConfirmations(from: KTransportMode, to: KTransportMode): Int = when {
        from == KTransportMode.UNKNOWN                              -> CONFIRM_FROM_UNKNOWN
        from == KTransportMode.CAR && to == KTransportMode.WALK    -> CONFIRM_CAR_TO_WALK
        to == KTransportMode.SHIP || from == KTransportMode.SHIP   -> CONFIRM_NORMAL + 1
        else                                                        -> CONFIRM_NORMAL
    }

    fun reset() {
        currentMode = KTransportMode.UNKNOWN
        pendingMode = KTransportMode.UNKNOWN
        confirmCount = 0
    }
}