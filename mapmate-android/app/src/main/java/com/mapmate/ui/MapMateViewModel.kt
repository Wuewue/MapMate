package com.mapmate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapmate.demo.AppSettings
import com.mapmate.demo.DemoState
import com.mapmate.demo.MapMateEngine
import com.mapmate.demo.SelfState
import com.mapmate.device.LocationSharingMode
import com.mapmate.privacy.PrivacyMode
import com.mapmate.telemetry.KTransportMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// drives the whole app -> owns the merge engine, runs the live tick, holds login state
// BuildConfig.DEMO_MODE = true -> we run everyone's logic on seeded data (this VM)
// in real mode the same screens would bind to com.mapmate.data.remote.MapMateRepository instead
class MapMateViewModel : ViewModel() {

    private val engine = MapMateEngine()

    private val _state = MutableStateFlow(emptyState())
    val state: StateFlow<DemoState> = _state.asStateFlow()

    // simple demo auth/nav -> real mode would use the profile module's AuthUseCases (teo)
    private val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    // app settings the settings screen reads + edits
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        // first frame, then a 1.5s live loop that re-runs everyone's pipeline
        _state.value = engine.tick()
        viewModelScope.launch {
            while (isActive) {
                delay(1500L)
                _state.value = engine.tick()
            }
        }
    }

    fun login() { _loggedIn.value = true }
    fun logout() { _loggedIn.value = false }

    fun friendIds(): List<String> = engine.friendIds()

    // settings screen -> change a friend's privacy mode, next tick re-renders the map
    fun setFriendPrivacy(friendId: String, mode: PrivacyMode) {
        engine.setPrivacy(friendId, mode)
        _state.value = engine.tick(0L)  // immediate re-render, no time advance
    }

    // ui-friendly wrapper -> screen passes "precise" / "blurred" / "frozen", we build the mode
    // frozen needs an anchor + time but the engine re-anchors to the friend's live spot, so dummies are fine
    fun setFriendPrivacyMode(friendId: String, mode: String) {
        val pm: PrivacyMode = when (mode) {
            "blurred" -> PrivacyMode.Blurred(500)
            "frozen" -> PrivacyMode.Frozen(
                com.mapmate.device.Coordinates(0.0, 0.0),
                java.time.Instant.EPOCH,
            )
            else -> PrivacyMode.Precise
        }
        setFriendPrivacy(friendId, pm)
    }

    // settings/home demo button -> fire mike's off-the-grid alert
    fun simulatePhoneDying(friendId: String) {
        engine.simulatePhoneDying(friendId)
        _state.value = engine.tick(0L)
    }

    // settings toggles -> off-grid + share gate real merged behavior, the rest hold preference
    fun toggleSetting(key: String, value: Boolean) {
        _settings.value = when (key) {
            "offgrid" -> { engine.setAlertsEnabled(value); _settings.value.copy(offGridAlerts = value) }
            "share" -> { engine.setShareLocation(value); _settings.value.copy(shareLocation = value) }
            "notifications" -> _settings.value.copy(notifications = value)
            "voice" -> _settings.value.copy(voiceGuidance = value)
            "dark" -> _settings.value.copy(darkMode = value)
            else -> _settings.value
        }
        _state.value = engine.tick(0L)
    }

    // mike's LocationSharingConfig mode -> foreground-only vs always-on
    fun setLocationMode(mode: LocationSharingMode) {
        _settings.value = _settings.value.copy(locationMode = mode)
    }

    // accept a friend request -> add them to the live group
    fun acceptRequest(name: String, initials: String) {
        engine.addFriend(name, initials)
        _state.value = engine.tick(0L)
    }

    private fun emptyState() = DemoState(
        self = SelfState(KTransportMode.WALK, 82, com.mapmate.device.BatteryStatus(82, false).level),
        friends = emptyList(),
        alerts = emptyList(),
        nearbyCount = 0,
    )
}
