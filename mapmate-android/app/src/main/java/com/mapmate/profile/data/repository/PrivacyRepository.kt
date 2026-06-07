package com.mapmate.profile.data.repository

import com.mapmate.profile.data.model.LocationPrivacyOption

interface PrivacyRepository {
    suspend fun updateLocationPrivacy(option: LocationPrivacyOption): Result<Unit>
}
