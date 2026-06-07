package com.mapmate.profile.domain.usecase

import com.mapmate.profile.data.repository.FriendRepository
import com.mapmate.profile.validation.ValidationRules

class SendFriendRequestUseCase(private val friendRepository: FriendRepository) {
    suspend operator fun invoke(
        targetEmail: String,
        isAlreadyFriend: Boolean = false,
        isBlockedRelationship: Boolean = false
    ): Result<Unit> {
        val normalizedEmail = targetEmail.trim().lowercase()
        if (!ValidationRules.isValidEmail(normalizedEmail)) {
            return Result.failure(IllegalArgumentException("Enter a valid friend email."))
        }
        if (!ValidationRules.canSendFriendRequest(isAlreadyFriend, isBlockedRelationship)) {
            return Result.failure(IllegalStateException("Friend request is not allowed for this relationship."))
        }
        return friendRepository.sendFriendRequest(normalizedEmail)
    }
}

class FriendRequestDecisionUseCase(private val friendRepository: FriendRepository) {
    suspend fun accept(requestId: String): Result<Unit> {
        if (!ValidationRules.isValidDocumentId(requestId)) {
            return Result.failure(IllegalArgumentException("Friend request id is required."))
        }
        return friendRepository.acceptFriendRequest(requestId)
    }

    suspend fun reject(requestId: String): Result<Unit> {
        if (!ValidationRules.isValidDocumentId(requestId)) {
            return Result.failure(IllegalArgumentException("Friend request id is required."))
        }
        return friendRepository.rejectFriendRequest(requestId)
    }
}

class FriendRelationshipUseCase(private val friendRepository: FriendRepository) {
    suspend fun unfriend(friendUid: String): Result<Unit> {
        if (!ValidationRules.isValidDocumentId(friendUid)) {
            return Result.failure(IllegalArgumentException("Friend uid is required."))
        }
        return friendRepository.unfriend(friendUid)
    }

    suspend fun block(targetUid: String): Result<Unit> {
        if (!ValidationRules.isValidDocumentId(targetUid)) {
            return Result.failure(IllegalArgumentException("Target uid is required."))
        }
        return friendRepository.blockUser(targetUid)
    }

    suspend fun unblock(targetUid: String): Result<Unit> {
        if (!ValidationRules.isValidDocumentId(targetUid)) {
            return Result.failure(IllegalArgumentException("Target uid is required."))
        }
        return friendRepository.unblockUser(targetUid)
    }
}
