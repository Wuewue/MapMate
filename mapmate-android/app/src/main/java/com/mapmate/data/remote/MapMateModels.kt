package com.mapmate.data.remote

data class HomeMapFeed(
    val serverTimeMillis: Long,
    val friends: List<MapFriend>,
    val nearbyFriendIds: List<String>,
)

data class MapFriend(
    val uid: String,
    val displayName: String,
    val username: String?,
    val avatarUrl: String?,
    val initials: String,
    val privacyMode: PrivacyMode,
    val streakDays: Int,
    val battery: BatterySnapshot,
    val lastActiveAtMillis: Long?,
    val location: FriendLocation?,
)

data class FriendLocation(
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Double?,
    val visibility: PrivacyMode,
    val isStale: Boolean,
    val updatedAtMillis: Long?,
    val distanceMeters: Double?,
    val speedMps: Double?,
    val headingDegrees: Double?,
    val transportMode: TransportMode,
)

data class BatterySnapshot(
    val percent: Int?,
    val status: BatteryStatus,
    val updatedAtMillis: Long?,
)

data class LocationUpdateRequest(
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Double,
    val speedMps: Double? = null,
    val headingDegrees: Double? = null,
    val transportMode: TransportMode = TransportMode.Unknown,
    val isBackground: Boolean = false,
)

data class VisitedPlace(
    val id: String,
    val label: String?,
    val placeType: String,
    val lat: Double,
    val lng: Double,
    val visitCount: Int,
    val firstVisitedAtMillis: Long?,
    val lastVisitedAtMillis: Long?,
)

data class UserSearchResult(
    val uid: String,
    val displayName: String,
    val username: String?,
    val avatarUrl: String?,
    val initials: String,
)

data class NotificationItem(
    val id: String,
    val type: String,
    val actorUid: String?,
    val title: String,
    val body: String,
    val read: Boolean,
    val createdAtMillis: Long?,
)

data class ActivityItem(
    val id: String,
    val type: String,
    val title: String,
    val createdAtMillis: Long?,
)

data class NotificationSettingsUpdate(
    val friendRequests: Boolean = true,
    val emojis: Boolean = true,
    val offGridAlerts: Boolean = true,
    val locationAccuracy: Boolean = true,
)

enum class PrivacyMode(val wireValue: String) {
    Precise("precise"),
    Blurred("blurred"),
    Frozen("frozen"),
    Hidden("hidden");

    companion object {
        fun fromWire(value: String?): PrivacyMode = values().firstOrNull { it.wireValue == value } ?: Hidden
    }
}

enum class TransportMode(val wireValue: String) {
    Walk("walk"),
    Bike("bike"),
    Car("car"),
    Ship("ship"),
    Still("still"),
    Unknown("unknown");

    companion object {
        fun fromWire(value: String?): TransportMode = values().firstOrNull { it.wireValue == value } ?: Unknown
    }
}

enum class BatteryStatus(val wireValue: String) {
    Charging("charging"),
    Low("low"),
    Full("full"),
    Normal("normal"),
    Unknown("unknown");

    companion object {
        fun fromWire(value: String?): BatteryStatus = values().firstOrNull { it.wireValue == value } ?: Unknown
    }
}

sealed class MapMateRemoteError(
    val userMessage: String,
) {
    object NoInternet : MapMateRemoteError("No internet connection. Please check your network and try again.")
    object Unauthorized : MapMateRemoteError("Please sign in again to continue.")
    object PermissionDenied : MapMateRemoteError("You do not have permission to access this data.")
    object NotFound : MapMateRemoteError("The requested data could not be found.")
    object Server : MapMateRemoteError("Server error. Please try again in a moment.")
    object InvalidRequest : MapMateRemoteError("Something about this request was invalid.")
    data class Unknown(val rawMessage: String?) : MapMateRemoteError(rawMessage ?: "Something went wrong.")
}

class MapMateRemoteException(
    val reason: MapMateRemoteError,
    cause: Throwable? = null,
) : Exception(reason.userMessage, cause)
