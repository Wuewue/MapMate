package com.mapmate.profile.data.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val basicInfo: String = "",
    val locationPrivacy: LocationPrivacyOption = LocationPrivacyOption.FRIENDS_ONLY,
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class Friend(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val createdAt: Long = 0L
)

data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: Long = 0L,
    val respondedAt: Long? = null
)

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

data class BlockedUser(
    val id: String = "",
    val blockerUid: String = "",
    val blockedUid: String = "",
    val createdAt: Long = 0L
)

data class NotificationItem(
    val id: String = "",
    val uid: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "profile",
    val isRead: Boolean = false,
    val createdAt: Long = 0L
)

data class RecentActivity(
    val id: String = "",
    val uid: String = "",
    val type: String = "",
    val description: String = "",
    val createdAt: Long = 0L
)

enum class LocationPrivacyOption(
    val displayName: String,
    val description: String
) {
    EVERYONE("Everyone", "All friends can see your live location."),
    FRIENDS_ONLY("Friends only", "Only accepted friends can see your location."),
    SELECTED_FRIENDS("Selected friends", "Only trusted friends can see your location."),
    GHOST_MODE("Ghost mode", "Friends see a blurred or frozen location."),
    NO_ONE("No one", "Your location is hidden from everyone.")
}

data class FriendStreak(
    val id: String = "",
    val userIds: List<String> = emptyList(),
    val days: Int = 0,
    val lastInteractionAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class AuthState(
    val isLoggedIn: Boolean = false,
    val userEmail: String = "",
    val message: String? = null
)

data class EmailVerificationState(
    val email: String = "",
    val isVerificationEmailSent: Boolean = false,
    val isVerified: Boolean = false,
    val message: String? = null
)
