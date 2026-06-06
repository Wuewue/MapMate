package com.mapmate.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mapmate.data.remote.BatteryStatus
import com.mapmate.data.remote.LocationUpdateRequest
import com.mapmate.data.remote.MapMateRepository
import com.mapmate.data.remote.TransportMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MapMateSyncCoordinator(
    private val context: Context,
    private val repository: MapMateRepository,
) {
    private val appContext = context.applicationContext
    private val locationClient = LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission")
    fun startActiveLocationSync(scope: CoroutineScope): Job {
        return scope.launch {
            while (isActive) {
                if (hasFineLocationPermission()) {
                    val cancellationTokenSource = CancellationTokenSource()
                    val location = locationClient
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                        .await()

                    location?.let {
                        repository.updateMyLocation(
                            LocationUpdateRequest(
                                lat = it.latitude,
                                lng = it.longitude,
                                accuracyMeters = it.accuracy.toDouble(),
                                speedMps = if (it.hasSpeed()) it.speed.toDouble() else null,
                                headingDegrees = if (it.hasBearing()) it.bearing.toDouble() else null,
                                transportMode = inferTransportMode(it),
                                isBackground = false,
                            ),
                        )
                    }
                }

                delay(ACTIVE_LOCATION_INTERVAL_MS)
            }
        }
    }

    fun startBatterySync(scope: CoroutineScope): Job {
        return scope.launch {
            while (isActive) {
                val percent = batteryPercent()
                val status = batteryStatus()

                if (percent != null) {
                    repository.updateBatteryStatus(percent, status)
                }

                delay(BATTERY_INTERVAL_MS)
            }
        }
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun batteryPercent(): Int? {
        val batteryManager = appContext.getSystemService(BatteryManager::class.java)
        val value = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return value.takeIf { it in 0..100 }
    }

    private fun batteryStatus(): BatteryStatus {
        val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawStatus = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val percent = batteryPercent()

        return when {
            rawStatus == BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.Charging
            rawStatus == BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.Full
            percent != null && percent <= 20 -> BatteryStatus.Low
            percent != null -> BatteryStatus.Normal
            else -> BatteryStatus.Unknown
        }
    }

    private fun inferTransportMode(location: Location): TransportMode {
        val speedMps = if (location.hasSpeed()) location.speed else return TransportMode.Unknown

        return when {
            speedMps < 0.7f -> TransportMode.Still
            speedMps < 2.3f -> TransportMode.Walk
            speedMps < 7.5f -> TransportMode.Bike
            speedMps < 36f -> TransportMode.Car
            else -> TransportMode.Unknown
        }
    }

    private companion object {
        const val ACTIVE_LOCATION_INTERVAL_MS = 3_000L
        const val BATTERY_INTERVAL_MS = 120_000L
    }
}
