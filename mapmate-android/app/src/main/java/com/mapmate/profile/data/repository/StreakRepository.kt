package com.mapmate.profile.data.repository

import com.mapmate.profile.data.model.FriendStreak

interface StreakRepository {
    suspend fun getFriendStreaks(): Result<List<FriendStreak>>
}
