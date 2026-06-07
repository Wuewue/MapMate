package com.mapmate.profile.validation

import com.mapmate.profile.data.model.LocationPrivacyOption

object ValidationRules {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val validFriendActions = setOf(
        "send_request",
        "accept_request",
        "reject_request",
        "unfriend",
        "block",
        "unblock"
    )

    fun isValidEmail(email: String): Boolean = email.trim().matches(emailRegex)

    fun isPasswordPresent(password: String): Boolean = password.isNotBlank()

    fun isProfileNamePresent(name: String): Boolean = name.isNotBlank()

    fun isValidLocationPrivacyOption(option: String): Boolean =
        LocationPrivacyOption.entries.any { it.name == option.trim().uppercase() }

    fun isValidFriendAction(action: String): Boolean =
        validFriendActions.contains(action.trim().lowercase())

    fun canSendFriendRequest(isAlreadyFriend: Boolean, isBlockedRelationship: Boolean): Boolean =
        !isAlreadyFriend && !isBlockedRelationship

    fun canInteractWhenNotBlocked(isBlockedRelationship: Boolean): Boolean = !isBlockedRelationship

    fun isValidNotificationPreference(enabled: Boolean?): Boolean = enabled != null

    fun isValidDocumentId(id: String): Boolean = id.isNotBlank()
}
