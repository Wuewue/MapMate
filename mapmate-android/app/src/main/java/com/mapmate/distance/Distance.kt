package com.mapmate.distance

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// 1:1 kotlin port of tue's distance.js (getStraightLineDistance)
// tue wrote it in node, the logic is identical here, just kotlin syntax
// his getNerabyFriends + updateRealTimeRoute talk to firestore / google maps
// -> those stay server side, only the pure haversine math is ported
object Distance {

    // earth radius in km, same R = 6371 tue used
    private const val EARTH_RADIUS_KM = 6371.0

    // req: SRS-DI-005, straight line distance between two lat/lng points in km
    // IMPORTANT pass the privacy filtered point as lat2/lng2 (Visibility.coordinates)
    // never the real DeviceSnapshot.location -> see PrivacyResolver, blurred friends must stay blurred
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    // same as tue's distanceMeters = Math.round(km * 1000)
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        return (haversineKm(lat1, lon1, lat2, lon2) * 1000).roundToInt()
    }

    // tue's nearby rule -> within 1 km counts as nearby
    fun isNearby(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Boolean {
        return haversineKm(lat1, lon1, lat2, lon2) <= 1.0
    }
}
