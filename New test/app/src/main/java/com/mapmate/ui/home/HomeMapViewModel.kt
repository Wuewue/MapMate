package com.mapmate.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapmate.data.remote.BatteryStatus
import com.mapmate.data.remote.HomeMapFeed
import com.mapmate.data.remote.LocationUpdateRequest
import com.mapmate.data.remote.MapMateRemoteException
import com.mapmate.data.remote.MapMateRepository
import com.mapmate.data.remote.PrivacyMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeMapUiState(
    val isLoading: Boolean = false,
    val isSyncingLocation: Boolean = false,
    val feed: HomeMapFeed? = null,
    val errorMessage: String? = null,
) {
    val hasFriends: Boolean = feed?.friends?.isNotEmpty() == true
}

class HomeMapViewModel(
    private val repository: MapMateRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeMapUiState())
    val uiState: StateFlow<HomeMapUiState> = _uiState.asStateFlow()

    fun loadHomeMapFeed(
        currentLat: Double? = null,
        currentLng: Double? = null,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            repository
                .getHomeMapFeed(currentLat = currentLat, currentLng = currentLng)
                .onSuccess { feed ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            feed = feed,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure(::showError)
        }
    }

    fun syncLocation(request: LocationUpdateRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingLocation = true) }

            repository
                .updateMyLocation(request)
                .onSuccess {
                    _uiState.update { it.copy(isSyncingLocation = false) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isSyncingLocation = false) }
                    showError(throwable)
                }
        }
    }

    fun updateBattery(
        percent: Int,
        status: BatteryStatus,
    ) {
        viewModelScope.launch {
            repository
                .updateBatteryStatus(percent, status)
                .onFailure(::showError)
        }
    }

    fun changeFriendPrivacy(
        friendUid: String,
        privacyMode: PrivacyMode,
    ) {
        viewModelScope.launch {
            repository
                .updateFriendPrivacy(friendUid, privacyMode)
                .onSuccess { loadHomeMapFeed() }
                .onFailure(::showError)
        }
    }

    fun sendEmoji(
        friendUid: String,
        emoji: String,
    ) {
        viewModelScope.launch {
            repository
                .sendEmoji(friendUid, emoji)
                .onFailure(::showError)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun showError(throwable: Throwable) {
        val message = (throwable as? MapMateRemoteException)
            ?.reason
            ?.userMessage
            ?: "Something went wrong. Please try again."

        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = message,
            )
        }
    }

    class Factory(
        private val repository: MapMateRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeMapViewModel(repository) as T
        }
    }
}
