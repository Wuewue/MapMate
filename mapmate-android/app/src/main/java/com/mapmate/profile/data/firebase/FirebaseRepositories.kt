package com.mapmate.profile.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.mapmate.profile.data.model.FriendRequestStatus
import com.mapmate.profile.data.model.FriendStreak
import com.mapmate.profile.data.model.LocationPrivacyOption
import com.mapmate.profile.data.model.NotificationItem
import com.mapmate.profile.data.model.RecentActivity
import com.mapmate.profile.data.model.UserProfile
import com.mapmate.profile.data.repository.ActivityRepository
import com.mapmate.profile.data.repository.AuthRepository
import com.mapmate.profile.data.repository.FriendRepository
import com.mapmate.profile.data.repository.NotificationRepository
import com.mapmate.profile.data.repository.PrivacyRepository
import com.mapmate.profile.data.repository.ProfileRepository
import com.mapmate.profile.data.repository.StreakRepository
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"
private const val FRIEND_REQUESTS_COLLECTION = "friendRequests"
private const val FRIENDSHIPS_COLLECTION = "friendships"
private const val BLOCKS_COLLECTION = "blocks"
private const val NOTIFICATIONS_COLLECTION = "notifications"
private const val ACTIVITIES_COLLECTION = "activities"
private const val FRIEND_STREAKS_COLLECTION = "friendStreaks"

private suspend fun <T> firebaseResult(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (exception: Exception) {
        Result.failure(exception)
    }

private fun requireCurrentUser(auth: FirebaseAuth): FirebaseUser =
    auth.currentUser ?: throw IllegalStateException("No authenticated Firebase user.")

private fun normalizeEmail(email: String): String = email.trim().lowercase()

private fun timestampMillis(value: Any?): Long =
    when (value) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> 0L
    }

private fun privacyOption(value: String?): LocationPrivacyOption =
    value
        ?.let { runCatching { LocationPrivacyOption.valueOf(it) }.getOrNull() }
        ?: LocationPrivacyOption.FRIENDS_ONLY

private fun DocumentSnapshot.stringList(field: String): List<String> =
    (get(field) as? List<*>).orEmpty().filterIsInstance<String>()

private fun DocumentSnapshot.toUserProfile(): UserProfile =
    UserProfile(
        uid = getString("uid") ?: id,
        email = getString("email").orEmpty(),
        name = getString("name").orEmpty(),
        avatarUrl = getString("avatarUrl").orEmpty(),
        basicInfo = getString("basicInfo").orEmpty(),
        locationPrivacy = privacyOption(getString("locationPrivacy")),
        notificationsEnabled = getBoolean("notificationsEnabled") ?: true,
        createdAt = timestampMillis(get("createdAt")),
        updatedAt = timestampMillis(get("updatedAt"))
    )

private fun DocumentSnapshot.toNotificationItem(): NotificationItem =
    NotificationItem(
        id = id,
        uid = getString("uid").orEmpty(),
        title = getString("title").orEmpty(),
        message = getString("message").orEmpty(),
        type = getString("type") ?: "profile",
        isRead = getBoolean("isRead") ?: false,
        createdAt = timestampMillis(get("createdAt"))
    )

private fun DocumentSnapshot.toRecentActivity(): RecentActivity =
    RecentActivity(
        id = id,
        uid = getString("uid").orEmpty(),
        type = getString("type").orEmpty(),
        description = getString("description").orEmpty(),
        createdAt = timestampMillis(get("createdAt"))
    )

private fun DocumentSnapshot.toFriendStreak(): FriendStreak =
    FriendStreak(
        id = id,
        userIds = stringList("userIds"),
        days = getLong("days")?.toInt() ?: 0,
        lastInteractionAt = timestampMillis(get("lastInteractionAt")),
        updatedAt = timestampMillis(get("updatedAt"))
    )

private fun pairDocumentId(uidA: String, uidB: String): String =
    listOf(uidA, uidB).sorted().joinToString("_")

private fun blockDocumentId(blockerUid: String, blockedUid: String): String =
    "${blockerUid}_$blockedUid"

private fun defaultProfileData(user: FirebaseUser): Map<String, Any?> {
    val email = normalizeEmail(user.email.orEmpty())
    val defaultName = user.displayName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: email.substringBefore("@").ifBlank { "MapMate user" }

    return mapOf(
        "uid" to user.uid,
        "email" to email,
        "name" to defaultName,
        "avatarUrl" to "",
        "basicInfo" to "",
        "locationPrivacy" to LocationPrivacyOption.FRIENDS_ONLY.name,
        "notificationsEnabled" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

private fun notificationData(
    uid: String,
    title: String,
    message: String,
    type: String
): Map<String, Any?> =
    mapOf(
        "uid" to uid,
        "title" to title,
        "message" to message,
        "type" to type,
        "isRead" to false,
        "createdAt" to FieldValue.serverTimestamp()
    )

private fun activityData(uid: String, type: String, description: String): Map<String, Any?> =
    mapOf(
        "uid" to uid,
        "type" to type,
        "description" to description,
        "createdAt" to FieldValue.serverTimestamp()
    )

private suspend fun createDefaultProfileIfMissing(
    firestore: FirebaseFirestore,
    user: FirebaseUser
) {
    val profileRef = firestore.collection(USERS_COLLECTION).document(user.uid)
    val profileSnapshot = profileRef.get().await()

    if (!profileSnapshot.exists()) {
        profileRef.set(defaultProfileData(user)).await()
    }
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {
    override suspend fun signUp(email: String, password: String): Result<Unit> =
        firebaseResult {
            val authResult = auth.createUserWithEmailAndPassword(normalizeEmail(email), password).await()
            val user = authResult.user ?: requireCurrentUser(auth)
            user.sendEmailVerification().await()
            createDefaultProfileIfMissing(firestore, user)
        }

    override suspend fun sendEmailVerification(): Result<Unit> =
        firebaseResult {
            requireCurrentUser(auth).sendEmailVerification().await()
        }

    override suspend fun login(email: String, password: String): Result<Unit> =
        firebaseResult {
            auth.signInWithEmailAndPassword(normalizeEmail(email), password).await()
        }

    override suspend fun logout(): Result<Unit> =
        firebaseResult {
            auth.signOut()
        }
}

class FirebaseProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ProfileRepository {
    override suspend fun getCurrentProfile(): Result<UserProfile> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            val profileSnapshot = firestore.collection(USERS_COLLECTION).document(user.uid).get().await()

            if (!profileSnapshot.exists()) {
                throw IllegalStateException("Profile document users/${user.uid} does not exist.")
            }

            profileSnapshot.toUserProfile()
        }

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            val targetUid = profile.uid.ifBlank { user.uid }

            if (targetUid != user.uid) {
                throw IllegalArgumentException("Cannot update another user's profile.")
            }

            val updateData = mapOf(
                "uid" to user.uid,
                "email" to normalizeEmail(user.email ?: profile.email),
                "name" to profile.name.trim(),
                "avatarUrl" to profile.avatarUrl.trim(),
                "basicInfo" to profile.basicInfo.trim(),
                "locationPrivacy" to profile.locationPrivacy.name,
                "notificationsEnabled" to profile.notificationsEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(updateData, SetOptions.merge())
                .await()
        }
}

class FirebaseFriendRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FriendRepository {
    override suspend fun sendFriendRequest(targetEmail: String): Result<Unit> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            val targetProfile = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", normalizeEmail(targetEmail))
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?: throw IllegalArgumentException("No user profile found for the target email.")

            val targetUid = targetProfile.getString("uid") ?: targetProfile.id

            if (targetUid == user.uid) {
                throw IllegalArgumentException("Cannot send a friend request to yourself.")
            }

            if (hasBlockBetween(user.uid, targetUid)) {
                throw IllegalStateException("Cannot send a friend request when a block exists.")
            }

            if (hasFriendship(user.uid, targetUid)) {
                throw IllegalStateException("Users are already friends.")
            }

            if (hasPendingRequestBetween(user.uid, targetUid)) {
                throw IllegalStateException("A pending friend request already exists.")
            }

            // TODO: Move trusted request validation and notification/activity creation to Cloud Functions.
            val requestRef = firestore.collection(FRIEND_REQUESTS_COLLECTION).document()
            val batch = firestore.batch()

            batch.set(
                requestRef,
                mapOf(
                    "fromUid" to user.uid,
                    "toUid" to targetUid,
                    "status" to FriendRequestStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "respondedAt" to null
                )
            )
            batch.set(
                firestore.collection(NOTIFICATIONS_COLLECTION).document(),
                notificationData(
                    uid = targetUid,
                    title = "New friend request",
                    message = "You have a new friend request.",
                    type = "friend_request"
                )
            )
            batch.set(
                firestore.collection(ACTIVITIES_COLLECTION).document(),
                activityData(
                    uid = user.uid,
                    type = "friend_request_sent",
                    description = "Sent a friend request."
                )
            )
            batch.commit().await()
        }

    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            val requestRef = firestore.collection(FRIEND_REQUESTS_COLLECTION).document(requestId)

            // TODO: Prefer a Cloud Function for production so clients cannot create fake friendships.
            firestore.runTransaction { transaction ->
                val requestSnapshot = transaction.get(requestRef)
                if (!requestSnapshot.exists()) {
                    throw IllegalArgumentException("Friend request does not exist.")
                }

                val fromUid = requestSnapshot.getString("fromUid").orEmpty()
                val toUid = requestSnapshot.getString("toUid").orEmpty()
                val status = requestSnapshot.getString("status").orEmpty()

                if (toUid != user.uid) {
                    throw IllegalArgumentException("Only the request receiver can accept this request.")
                }

                if (status != FriendRequestStatus.PENDING.name) {
                    throw IllegalStateException("Only pending friend requests can be accepted.")
                }

                val outgoingBlockRef = firestore.collection(BLOCKS_COLLECTION).document(blockDocumentId(fromUid, toUid))
                val incomingBlockRef = firestore.collection(BLOCKS_COLLECTION).document(blockDocumentId(toUid, fromUid))
                if (transaction.get(outgoingBlockRef).exists() || transaction.get(incomingBlockRef).exists()) {
                    throw IllegalStateException("Cannot accept a friend request when a block exists.")
                }

                val friendshipRef = firestore.collection(FRIENDSHIPS_COLLECTION).document(pairDocumentId(fromUid, toUid))
                if (transaction.get(friendshipRef).exists()) {
                    throw IllegalStateException("Friendship already exists.")
                }

                val userIds = listOf(fromUid, toUid).sorted()
                transaction.set(
                    friendshipRef,
                    mapOf(
                        "userIds" to userIds,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    requestRef,
                    mapOf(
                        "status" to FriendRequestStatus.ACCEPTED.name,
                        "respondedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    firestore.collection(NOTIFICATIONS_COLLECTION).document(),
                    notificationData(
                        uid = fromUid,
                        title = "Friend request accepted",
                        message = "Your friend request was accepted.",
                        type = "friend_accept"
                    )
                )
                transaction.set(
                    firestore.collection(ACTIVITIES_COLLECTION).document(),
                    activityData(
                        uid = fromUid,
                        type = "friend_added",
                        description = "A friend request was accepted."
                    )
                )
                transaction.set(
                    firestore.collection(ACTIVITIES_COLLECTION).document(),
                    activityData(
                        uid = toUid,
                        type = "friend_added",
                        description = "Accepted a friend request."
                    )
                )
                null
            }.await()
        }

    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            val requestRef = firestore.collection(FRIEND_REQUESTS_COLLECTION).document(requestId)

            firestore.runTransaction { transaction ->
                val requestSnapshot = transaction.get(requestRef)
                if (!requestSnapshot.exists()) {
                    throw IllegalArgumentException("Friend request does not exist.")
                }

                val toUid = requestSnapshot.getString("toUid").orEmpty()
                val status = requestSnapshot.getString("status").orEmpty()

                if (toUid != user.uid) {
                    throw IllegalArgumentException("Only the request receiver can reject this request.")
                }

                if (status != FriendRequestStatus.PENDING.name) {
                    throw IllegalStateException("Only pending friend requests can be rejected.")
                }

                transaction.update(
                    requestRef,
                    mapOf(
                        "status" to FriendRequestStatus.REJECTED.name,
                        "respondedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    firestore.collection(ACTIVITIES_COLLECTION).document(),
                    activityData(
                        uid = user.uid,
                        type = "friend_request_rejected",
                        description = "Rejected a friend request."
                    )
                )
                null
            }.await()
        }

    override suspend fun unfriend(friendUid: String): Result<Unit> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            if (friendUid == user.uid) {
                throw IllegalArgumentException("Cannot unfriend yourself.")
            }

            // TODO: Enforce friendship deletion rules with Security Rules or a Cloud Function.
            val batch = firestore.batch()
            batch.delete(
                firestore.collection(FRIENDSHIPS_COLLECTION)
                    .document(pairDocumentId(user.uid, friendUid))
            )
            batch.set(
                firestore.collection(ACTIVITIES_COLLECTION).document(),
                activityData(
                    uid = user.uid,
                    type = "friend_removed",
                    description = "Removed a friend."
                )
            )
            batch.commit().await()
        }

    override suspend fun blockUser(targetUid: String): Result<Unit> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            if (targetUid == user.uid) {
                throw IllegalArgumentException("Cannot block yourself.")
            }

            // TODO: Move block side effects to Cloud Functions so pending requests are handled consistently.
            val batch = firestore.batch()
            batch.set(
                firestore.collection(BLOCKS_COLLECTION).document(blockDocumentId(user.uid, targetUid)),
                mapOf(
                    "blockerUid" to user.uid,
                    "blockedUid" to targetUid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.delete(
                firestore.collection(FRIENDSHIPS_COLLECTION)
                    .document(pairDocumentId(user.uid, targetUid))
            )
            batch.set(
                firestore.collection(ACTIVITIES_COLLECTION).document(),
                activityData(
                    uid = user.uid,
                    type = "user_blocked",
                    description = "Blocked a user."
                )
            )

            pendingRequestsBetween(user.uid, targetUid).forEach { request ->
                batch.update(
                    request.reference,
                    mapOf(
                        "status" to FriendRequestStatus.REJECTED.name,
                        "respondedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
        }

    override suspend fun unblockUser(targetUid: String): Result<Unit> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            val batch = firestore.batch()

            batch.delete(
                firestore.collection(BLOCKS_COLLECTION).document(blockDocumentId(user.uid, targetUid))
            )
            batch.set(
                firestore.collection(ACTIVITIES_COLLECTION).document(),
                activityData(
                    uid = user.uid,
                    type = "user_unblocked",
                    description = "Unblocked a user."
                )
            )
            batch.commit().await()
        }

    private suspend fun hasFriendship(uidA: String, uidB: String): Boolean =
        firestore.collection(FRIENDSHIPS_COLLECTION)
            .document(pairDocumentId(uidA, uidB))
            .get()
            .await()
            .exists()

    private suspend fun hasBlockBetween(uidA: String, uidB: String): Boolean {
        val directBlock = firestore.collection(BLOCKS_COLLECTION)
            .document(blockDocumentId(uidA, uidB))
            .get()
            .await()
            .exists()
        val reverseBlock = firestore.collection(BLOCKS_COLLECTION)
            .document(blockDocumentId(uidB, uidA))
            .get()
            .await()
            .exists()

        return directBlock || reverseBlock
    }

    private suspend fun hasPendingRequestBetween(uidA: String, uidB: String): Boolean =
        pendingRequestsBetween(uidA, uidB).isNotEmpty()

    private suspend fun pendingRequestsBetween(uidA: String, uidB: String): List<DocumentSnapshot> {
        val outgoing = firestore.collection(FRIEND_REQUESTS_COLLECTION)
            .whereEqualTo("fromUid", uidA)
            .whereEqualTo("toUid", uidB)
            .whereEqualTo("status", FriendRequestStatus.PENDING.name)
            .get()
            .await()
            .documents
        val incoming = firestore.collection(FRIEND_REQUESTS_COLLECTION)
            .whereEqualTo("fromUid", uidB)
            .whereEqualTo("toUid", uidA)
            .whereEqualTo("status", FriendRequestStatus.PENDING.name)
            .get()
            .await()
            .documents

        return outgoing + incoming
    }
}

class FirebasePrivacyRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PrivacyRepository {
    override suspend fun updateLocationPrivacy(option: LocationPrivacyOption): Result<Unit> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(
                    mapOf(
                        "locationPrivacy" to option.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()
        }
}

class FirebaseNotificationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationRepository {
    override suspend fun getNotifications(): Result<List<NotificationItem>> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            firestore.collection(NOTIFICATIONS_COLLECTION)
                .whereEqualTo("uid", user.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .map { it.toNotificationItem() }
        }

    override suspend fun markAsRead(notificationId: String): Result<Unit> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            val notificationRef = firestore.collection(NOTIFICATIONS_COLLECTION).document(notificationId)
            val notificationSnapshot = notificationRef.get().await()

            if (!notificationSnapshot.exists()) {
                throw IllegalArgumentException("Notification does not exist.")
            }

            if (notificationSnapshot.getString("uid") != user.uid) {
                throw IllegalArgumentException("Cannot update another user's notification.")
            }

            notificationRef.update("isRead", true).await()
        }

    override suspend fun updateNotificationPreference(enabled: Boolean): Result<Unit> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(
                    mapOf(
                        "notificationsEnabled" to enabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()
        }
}

class FirebaseActivityRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ActivityRepository {
    override suspend fun getRecentActivities(): Result<List<RecentActivity>> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            firestore.collection(ACTIVITIES_COLLECTION)
                .whereEqualTo("uid", user.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .map { it.toRecentActivity() }
        }
}

class FirebaseStreakRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : StreakRepository {
    override suspend fun getFriendStreaks(): Result<List<FriendStreak>> =
        firebaseResult {
            val user = requireCurrentUser(auth)
            firestore.collection(FRIEND_STREAKS_COLLECTION)
                .whereArrayContains("userIds", user.uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .map { it.toFriendStreak() }
        }
}
