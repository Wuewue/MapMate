package com.mapmate.profile.data.repository

import com.mapmate.profile.data.model.NotificationItem

interface NotificationRepository {
    suspend fun getNotifications(): Result<List<NotificationItem>>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun updateNotificationPreference(enabled: Boolean): Result<Unit>
}
