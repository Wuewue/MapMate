package com.mapmate.demo

import com.mapmate.device.LocationSharingMode

// app-wide settings the settings screen edits, with sensible defaults
// offGridAlerts + shareLocation + locationMode gate real merged behavior, the rest hold preference
data class AppSettings(
    val offGridAlerts: Boolean = true,
    val shareLocation: Boolean = true,
    val locationMode: LocationSharingMode = LocationSharingMode.INCLUDE_BACKGROUND,
    val hasBackgroundPermission: Boolean = false,
    val notifications: Boolean = true,
    val voiceGuidance: Boolean = true,
    val darkMode: Boolean = true,
)

// static demo content for screens that don't run live merged logic (trips, search, add-friend)
// kept separate from MapMateEngine, which runs the real ported pipeline
object DemoData {
    data class Trip(val destination: String, val date: String, val duration: String, val distance: String)
    data class Place(val name: String, val address: String, val tag: String? = null)
    data class Suggestion(val name: String, val initials: String, val mutual: Int)
    data class Stat(val value: String, val label: String)

    val trips = listOf(
        Trip("Riverside Office", "Today · 8:24 AM", "12 min", "4.2 km"),
        Trip("Sunset Market", "Yesterday", "8 min", "2.1 km"),
        Trip("Mom's House", "Sat", "34 min", "21 km"),
        Trip("Airport T2", "Thu", "47 min", "38 km"),
    )
    val recentPlaces = listOf(
        Place("Riverside Office", "12 Bay Ave, District 1", "Work"),
        Place("Sunset Market", "88 Pham Ngu Lao"),
        Place("Central Library", "227 Nguyen Van Cu"),
    )
    val suggestedPlaces = listOf(
        Place("Highlands Coffee", "0.3 km away"),
        Place("Ben Thanh Market", "1.2 km away"),
        Place("September 23 Park", "0.6 km away"),
        Place("Bitexco Tower", "0.9 km away"),
    )
    val suggestions = listOf(
        Suggestion("Emma Wright", "EW", 4),
        Suggestion("Daniel Cruz", "DC", 2),
        Suggestion("Aisha Khan", "AK", 6),
    )
    val weeklyStats = listOf(
        Stat("142 km", "THIS WEEK"),
        Stat("8.4 h", "DRIVING"),
        Stat("23", "TRIPS"),
    )
}
