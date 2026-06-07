package com.mapmate.data.remote

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.mapmate.BuildConfig
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class MapMateRepository(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage,
    private val connectivityMonitor: ConnectivityMonitor,
) {
    constructor(context: Context) : this(
        auth = FirebaseAuth.getInstance(),
        functions = FirebaseFunctions.getInstance("asia-southeast1"),
        storage = FirebaseStorage.getInstance(),
        connectivityMonitor = ConnectivityMonitor(context),
    )

    suspend fun requestEmailOtp(email: String): Result<String?> = call(
        functionName = "requestEmailOtp",
        payload = mapOf("email" to email),
        authRequired = false,
    ) { data ->
        data.asStringMap().string("debugCode")
    }

    suspend fun verifyEmailOtp(
        email: String,
        code: String,
    ): Result<Unit> {
        val tokenResult = call(
            functionName = "verifyEmailOtp",
            payload = mapOf(
                "email" to email,
                "code" to code,
            ),
            authRequired = false,
        ) { data ->
            data.asStringMap().string("customToken").orEmpty()
        }

        return tokenResult.fold(
            onSuccess = { customToken ->
                try {
                    auth.signInWithCustomToken(customToken).await()
                    Result.success(Unit)
                } catch (exception: Exception) {
                    Result.failure(MapMateRemoteException(MapMateRemoteError.Unknown(exception.message), exception))
                }
            },
            onFailure = { throwable -> Result.failure(throwable) },
        )
    }

    suspend fun sendEmailSignInLink(
        email: String,
        continueUrl: String,
    ): Result<Unit> {
        return try {
            val settings = ActionCodeSettings.newBuilder()
                .setUrl(continueUrl)
                .setHandleCodeInApp(true)
                .setAndroidPackageName(BuildConfig.APPLICATION_ID, true, null)
                .build()

            auth.sendSignInLinkToEmail(email, settings).await()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(MapMateRemoteException(MapMateRemoteError.Unknown(exception.message), exception))
        }
    }

    suspend fun completeEmailSignInLink(
        email: String,
        emailLink: String,
    ): Result<Unit> {
        return try {
            if (!auth.isSignInWithEmailLink(emailLink)) {
                return Result.failure(MapMateRemoteException(MapMateRemoteError.InvalidRequest))
            }

            auth.signInWithEmailLink(email, emailLink).await()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(MapMateRemoteException(MapMateRemoteError.Unknown(exception.message), exception))
        }
    }

    suspend fun createProfile(
        displayName: String,
        username: String,
        avatarUrl: String? = null,
        bio: String? = null,
    ): Result<Unit> = callVoid(
        functionName = "createProfile",
        payload = mapOf(
            "displayName" to displayName,
            "username" to username,
            "avatarUrl" to avatarUrl,
            "bio" to bio,
        ),
    )

    suspend fun updateProfile(
        displayName: String? = null,
        avatarUrl: String? = null,
        bio: String? = null,
    ): Result<Unit> = callVoid(
        functionName = "updateProfile",
        payload = mapOf(
            "displayName" to displayName,
            "avatarUrl" to avatarUrl,
            "bio" to bio,
        ),
    )

    suspend fun uploadAvatar(file: File): Result<String> {
        if (!connectivityMonitor.hasInternet()) {
            return Result.failure(MapMateRemoteException(MapMateRemoteError.NoInternet))
        }

        val uid = auth.currentUser?.uid
            ?: return Result.failure(MapMateRemoteException(MapMateRemoteError.Unauthorized))

        return try {
            val extension = file.extension.ifBlank { "jpg" }
            val ref = storage.reference.child("avatars/$uid/${UUID.randomUUID()}.$extension")
            ref.putFile(Uri.fromFile(file)).await()
            val avatarUrl = ref.downloadUrl.await().toString()

            updateProfile(avatarUrl = avatarUrl)
                .fold(
                    onSuccess = { Result.success(avatarUrl) },
                    onFailure = { Result.failure(it) },
                )
        } catch (exception: Exception) {
            Result.failure(MapMateRemoteException(MapMateRemoteError.Unknown(exception.message), exception))
        }
    }

    suspend fun searchUsers(query: String): Result<List<UserSearchResult>> = call(
        functionName = "searchUsers",
        payload = mapOf("query" to query),
    ) { data ->
        data.asStringMap()
            .list("users")
            .map { parseUserSearchResult(it.asStringMap()) }
    }

    suspend fun sendFriendRequest(targetUid: String): Result<Unit> = callVoid(
        functionName = "sendFriendRequest",
        payload = mapOf("targetUid" to targetUid),
    )

    suspend fun acceptFriendRequest(requesterUid: String): Result<Unit> = callVoid(
        functionName = "acceptFriendRequest",
        payload = mapOf("requesterUid" to requesterUid),
    )

    suspend fun unfriend(friendUid: String): Result<Unit> = callVoid(
        functionName = "unfriendUser",
        payload = mapOf("friendUid" to friendUid),
    )

    suspend fun blockUser(blockedUid: String): Result<Unit> = callVoid(
        functionName = "blockUser",
        payload = mapOf("blockedUid" to blockedUid),
    )

    suspend fun updateFriendPrivacy(
        friendUid: String,
        privacyMode: PrivacyMode,
    ): Result<Unit> = callVoid(
        functionName = "updateFriendPrivacy",
        payload = mapOf(
            "friendUid" to friendUid,
            "privacyMode" to privacyMode.wireValue,
        ),
    )

    suspend fun updateMyLocation(request: LocationUpdateRequest): Result<Unit> = callVoid(
        functionName = "updateMyLocation",
        payload = mapOf(
            "lat" to request.lat,
            "lng" to request.lng,
            "accuracyMeters" to request.accuracyMeters,
            "speedMps" to request.speedMps,
            "headingDegrees" to request.headingDegrees,
            "transportMode" to request.transportMode.wireValue,
            "isBackground" to request.isBackground,
        ),
    )

    suspend fun updateBatteryStatus(
        batteryPercent: Int,
        status: BatteryStatus,
    ): Result<Unit> = callVoid(
        functionName = "updateBatteryStatus",
        payload = mapOf(
            "batteryPercent" to batteryPercent,
            "batteryStatus" to status.wireValue,
        ),
    )

    suspend fun registerFcmToken(token: String): Result<Unit> = callVoid(
        functionName = "registerFcmToken",
        payload = mapOf("token" to token),
    )

    suspend fun updateNotificationSettings(settings: NotificationSettingsUpdate): Result<Unit> = callVoid(
        functionName = "updateNotificationSettings",
        payload = mapOf(
            "friendRequests" to settings.friendRequests,
            "emojis" to settings.emojis,
            "offGridAlerts" to settings.offGridAlerts,
            "locationAccuracy" to settings.locationAccuracy,
        ),
    )

    suspend fun getNotifications(limit: Int = 30): Result<List<NotificationItem>> = call(
        functionName = "getNotifications",
        payload = mapOf("limit" to limit),
    ) { data ->
        data.asStringMap()
            .list("notifications")
            .map { parseNotificationItem(it.asStringMap()) }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> = callVoid(
        functionName = "markNotificationRead",
        payload = mapOf("notificationId" to notificationId),
    )

    suspend fun getRecentActivities(): Result<List<ActivityItem>> = call(
        functionName = "getRecentActivities",
    ) { data ->
        data.asStringMap()
            .list("activities")
            .map { parseActivityItem(it.asStringMap()) }
    }

    suspend fun getHomeMapFeed(
        currentLat: Double? = null,
        currentLng: Double? = null,
    ): Result<HomeMapFeed> = call(
        functionName = "getHomeMapFeed",
        payload = mapOf(
            "currentLat" to currentLat,
            "currentLng" to currentLng,
        ),
        parser = ::parseHomeMapFeed,
    )

    suspend fun sendEmoji(
        friendUid: String,
        emoji: String,
    ): Result<Unit> = callVoid(
        functionName = "sendEmoji",
        payload = mapOf(
            "friendUid" to friendUid,
            "emoji" to emoji,
        ),
    )

    suspend fun recordVisitedPlace(
        lat: Double,
        lng: Double,
        label: String? = null,
        placeType: String? = null,
    ): Result<String> = call(
        functionName = "recordVisitedPlace",
        payload = mapOf(
            "lat" to lat,
            "lng" to lng,
            "label" to label,
            "placeType" to placeType,
        ),
        parser = { data -> data.asStringMap().string("placeId").orEmpty() },
    )

    suspend fun getVisitedPlaces(limit: Int = 30): Result<List<VisitedPlace>> = call(
        functionName = "getVisitedPlaces",
        payload = mapOf("limit" to limit),
        parser = { data ->
            data.asStringMap()
                .list("places")
                .map { parseVisitedPlace(it.asStringMap()) }
        },
    )

    private suspend fun callVoid(
        functionName: String,
        payload: Map<String, Any?> = emptyMap(),
        authRequired: Boolean = true,
    ): Result<Unit> = call(functionName, payload, authRequired) { Unit }

    private suspend fun <T> call(
        functionName: String,
        payload: Map<String, Any?> = emptyMap(),
        authRequired: Boolean = true,
        parser: (Any?) -> T,
    ): Result<T> {
        if (!connectivityMonitor.hasInternet()) {
            return Result.failure(MapMateRemoteException(MapMateRemoteError.NoInternet))
        }

        if (authRequired && auth.currentUser == null) {
            return Result.failure(MapMateRemoteException(MapMateRemoteError.Unauthorized))
        }

        return try {
            val result = functions
                .getHttpsCallable(functionName)
                .call(payload.withoutNullValues())
                .await()

            Result.success(parser(result.data))
        } catch (exception: FirebaseFunctionsException) {
            Result.failure(MapMateRemoteException(exception.toRemoteError(), exception))
        } catch (exception: Exception) {
            Result.failure(MapMateRemoteException(MapMateRemoteError.Unknown(exception.message), exception))
        }
    }

    private fun FirebaseFunctionsException.toRemoteError(): MapMateRemoteError {
        return when (code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> MapMateRemoteError.Unauthorized
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> MapMateRemoteError.PermissionDenied
            FirebaseFunctionsException.Code.NOT_FOUND -> MapMateRemoteError.NotFound
            FirebaseFunctionsException.Code.INVALID_ARGUMENT,
            FirebaseFunctionsException.Code.FAILED_PRECONDITION -> MapMateRemoteError.InvalidRequest

            FirebaseFunctionsException.Code.UNAVAILABLE -> MapMateRemoteError.NoInternet
            FirebaseFunctionsException.Code.INTERNAL,
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
            FirebaseFunctionsException.Code.UNKNOWN -> MapMateRemoteError.Server

            else -> MapMateRemoteError.Unknown(message)
        }
    }
}

private fun parseHomeMapFeed(data: Any?): HomeMapFeed {
    val map = data.asStringMap()
    return HomeMapFeed(
        serverTimeMillis = map.long("serverTimeMillis") ?: 0L,
        friends = map.list("friends").map { parseMapFriend(it.asStringMap()) },
        nearbyFriendIds = map.list("nearbyFriendIds").mapNotNull { it as? String },
    )
}

private fun parseUserSearchResult(map: Map<String, Any?>): UserSearchResult {
    return UserSearchResult(
        uid = map.string("uid").orEmpty(),
        displayName = map.string("displayName").orEmpty(),
        username = map.string("username"),
        avatarUrl = map.string("avatarUrl"),
        initials = map.string("initials").orEmpty(),
    )
}

private fun parseMapFriend(map: Map<String, Any?>): MapFriend {
    return MapFriend(
        uid = map.string("uid").orEmpty(),
        displayName = map.string("displayName").orEmpty(),
        username = map.string("username"),
        avatarUrl = map.string("avatarUrl"),
        initials = map.string("initials").orEmpty(),
        privacyMode = PrivacyMode.fromWire(map.string("privacyMode")),
        streakDays = map.int("streakDays") ?: 0,
        battery = parseBattery(map.map("battery")),
        lastActiveAtMillis = map.long("lastActiveAtMillis"),
        location = map["location"]?.asStringMap()?.let(::parseFriendLocation),
    )
}

private fun parseFriendLocation(map: Map<String, Any?>): FriendLocation {
    return FriendLocation(
        lat = map.double("lat") ?: 0.0,
        lng = map.double("lng") ?: 0.0,
        accuracyMeters = map.double("accuracyMeters"),
        visibility = PrivacyMode.fromWire(map.string("visibility")),
        isStale = map.boolean("isStale") ?: true,
        updatedAtMillis = map.long("updatedAtMillis"),
        distanceMeters = map.double("distanceMeters"),
        speedMps = map.double("speedMps"),
        headingDegrees = map.double("headingDegrees"),
        transportMode = TransportMode.fromWire(map.string("transportMode")),
    )
}

private fun parseBattery(map: Map<String, Any?>): BatterySnapshot {
    return BatterySnapshot(
        percent = map.int("percent"),
        status = BatteryStatus.fromWire(map.string("status")),
        updatedAtMillis = map.long("updatedAtMillis"),
    )
}

private fun parseNotificationItem(map: Map<String, Any?>): NotificationItem {
    return NotificationItem(
        id = map.string("id").orEmpty(),
        type = map.string("type") ?: "notification",
        actorUid = map.string("actorUid"),
        title = map.string("title") ?: "MAPMATE",
        body = map.string("body").orEmpty(),
        read = map.boolean("read") ?: false,
        createdAtMillis = map.long("createdAtMillis"),
    )
}

private fun parseActivityItem(map: Map<String, Any?>): ActivityItem {
    return ActivityItem(
        id = map.string("id").orEmpty(),
        type = map.string("type") ?: "activity",
        title = map.string("title").orEmpty(),
        createdAtMillis = map.long("createdAtMillis"),
    )
}

private fun parseVisitedPlace(map: Map<String, Any?>): VisitedPlace {
    return VisitedPlace(
        id = map.string("id").orEmpty(),
        label = map.string("label"),
        placeType = map.string("placeType") ?: "unknown",
        lat = map.double("lat") ?: 0.0,
        lng = map.double("lng") ?: 0.0,
        visitCount = map.int("visitCount") ?: 0,
        firstVisitedAtMillis = map.long("firstVisitedAtMillis"),
        lastVisitedAtMillis = map.long("lastVisitedAtMillis"),
    )
}

private fun Map<String, Any?>.withoutNullValues(): Map<String, Any> {
    return entries
        .filter { it.value != null }
        .associate { it.key to it.value as Any }
}

private fun Any?.asStringMap(): Map<String, Any?> {
    return (this as? Map<*, *>)
        ?.entries
        ?.associate { it.key.toString() to it.value }
        .orEmpty()
}

private fun Map<String, Any?>.map(key: String): Map<String, Any?> {
    return this[key].asStringMap()
}

private fun Map<String, Any?>.list(key: String): List<Any?> {
    return this[key] as? List<Any?> ?: emptyList()
}

private fun Map<String, Any?>.string(key: String): String? {
    return this[key] as? String
}

private fun Map<String, Any?>.boolean(key: String): Boolean? {
    return this[key] as? Boolean
}

private fun Map<String, Any?>.double(key: String): Double? {
    return when (val value = this[key]) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}

private fun Map<String, Any?>.long(key: String): Long? {
    return when (val value = this[key]) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
}

private fun Map<String, Any?>.int(key: String): Int? {
    return when (val value = this[key]) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }
}
