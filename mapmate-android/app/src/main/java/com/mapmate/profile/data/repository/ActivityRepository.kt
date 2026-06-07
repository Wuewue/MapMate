package com.mapmate.profile.data.repository

import com.mapmate.profile.data.model.RecentActivity

interface ActivityRepository {
    suspend fun getRecentActivities(): Result<List<RecentActivity>>
}
