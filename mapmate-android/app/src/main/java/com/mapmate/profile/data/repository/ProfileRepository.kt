package com.mapmate.profile.data.repository

import com.mapmate.profile.data.model.UserProfile

interface ProfileRepository {
    suspend fun getCurrentProfile(): Result<UserProfile>
    suspend fun updateProfile(profile: UserProfile): Result<Unit>
}
