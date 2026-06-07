package com.mapmate.profile.domain.usecase

import com.mapmate.profile.data.model.UserProfile
import com.mapmate.profile.data.repository.ProfileRepository
import com.mapmate.profile.validation.ValidationRules

class GetCurrentProfileUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(): Result<UserProfile> = profileRepository.getCurrentProfile()
}

class UpdateProfileUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(profile: UserProfile): Result<Unit> {
        if (!ValidationRules.isProfileNamePresent(profile.name)) {
            return Result.failure(IllegalArgumentException("Profile name cannot be empty."))
        }
        return profileRepository.updateProfile(profile)
    }
}
