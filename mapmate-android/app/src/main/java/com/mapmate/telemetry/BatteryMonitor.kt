package com.mapmate.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

class BatteryMonitor(private val context: Context) {

    companion object {
        private const val TAG = "BatteryMonitor"
    }

    fun getCurrentStatus(): BatteryStatus? {
        return try {
            val intent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            ) ?: return null

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            if (level == -1 || scale == -1) return null

            val percentage = (level * 100 / scale.toFloat()).toInt()
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val result = BatteryStatus.from(percentage, isCharging)
            Log.d(TAG, "🔋 ${result.percentage}% | ${result.state}")
            result

        } catch (e: Exception) {
            Log.e(TAG, "Error reading battery", e)
            null
        }
    }
}