package com.mapmate.profile.domain.usecase

import com.mapmate.profile.data.model.FriendStreak
import com.mapmate.profile.data.repository.StreakRepository

class GetFriendStreaksUseCase(private val streakRepository: StreakRepository) {
    suspend operator fun invoke(): Result<List<FriendStreak>> = streakRepository.getFriendStreaks()
}
