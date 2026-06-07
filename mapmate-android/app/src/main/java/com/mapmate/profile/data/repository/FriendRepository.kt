package com.mapmate.profile.data.repository

interface FriendRepository {
    suspend fun sendFriendRequest(targetEmail: String): Result<Unit>
    suspend fun acceptFriendRequest(requestId: String): Result<Unit>
    suspend fun rejectFriendRequest(requestId: String): Result<Unit>
    suspend fun unfriend(friendUid: String): Result<Unit>
    suspend fun blockUser(targetUid: String): Result<Unit>
    suspend fun unblockUser(targetUid: String): Result<Unit>
}
