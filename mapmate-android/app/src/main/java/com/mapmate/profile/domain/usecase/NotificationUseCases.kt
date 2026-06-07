package com.mapmate.profile.domain.usecase

import com.mapmate.profile.data.model.NotificationItem
import com.mapmate.profile.data.repository.NotificationRepository
import com.mapmate.profile.validation.ValidationRules

class GetNotificationsUseCase(private val notificationRepository: NotificationRepository) {
    suspend operator fun invoke(): Result<List<NotificationItem>> = notificationRepository.getNotifications()
}

class MarkNotificationReadUseCase(private val notificationRepository: NotificationRepository) {
    suspend operator fun invoke(notificationId: String): Result<Unit> {
        if (!ValidationRules.isValidDocumentId(notificationId)) {
            return Result.failure(IllegalArgumentException("Notification id is required."))
        }
        return notificationRepository.markAsRead(notificationId)
    }
}

class UpdateNotificationPreferenceUseCase(private val notificationRepository: NotificationRepository) {
    suspend operator fun invoke(enabled: Boolean?): Result<Unit> {
        if (!ValidationRules.isValidNotificationPreference(enabled)) {
            return Result.failure(IllegalArgumentException("Notification preference must be true or false."))
        }
        return notificationRepository.updateNotificationPreference(enabled ?: false)
    }
}
