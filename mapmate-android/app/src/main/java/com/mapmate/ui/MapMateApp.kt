package com.mapmate.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapmate.demo.DemoState
import com.mapmate.demo.FriendState
import com.mapmate.privacy.Precision
import com.mapmate.telemetry.KTransportMode
import com.mapmate.ui.auth.LoginScreen
import com.mapmate.ui.auth.OtpScreen
import com.mapmate.ui.auth.SplashScreen
import com.mapmate.ui.components.FriendPrivacyControls
import com.mapmate.ui.home.HomeScreen
import com.mapmate.ui.navigate.NavigateScreen
import com.mapmate.ui.profile.ProfileScreen
import com.mapmate.ui.profile.SettingsScreen
import com.mapmate.ui.search.SearchScreen
import com.mapmate.ui.social.AddFriendScreen
import com.mapmate.ui.social.FriendsScreen
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne
import com.mapmate.ui.trips.TripsScreen

// top-level shell -> owns the one viewmodel, routes auth -> main tabs + overlay screens + the privacy sheet
// plain compose state nav (no nav-compose dep) keeps the merge dependency-light
@Composable
fun MapMateApp(vm: MapMateViewModel = viewModel()) {
    val loggedIn by vm.loggedIn.collectAsState()
    val state by vm.state.collectAsState()
    if (!loggedIn) AuthFlow(onAuthed = vm::login) else MainShell(state, vm)
}

// splash -> login -> otp -> in (teo's auth use-cases drive this in real mode)
@Composable
private fun AuthFlow(onAuthed: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    when (step) {
        0 -> SplashScreen(onGetStarted = { step = 1 })
        1 -> LoginScreen(onSignIn = { step = 2 })
        else -> OtpScreen(email = "you@mapmate.app", onVerify = onAuthed, onBack = { step = 1 })
    }
}

// full-screen pushes layered over the tab content
private enum class Overlay { None, Search, Navigate, AddFriend, Settings }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(state: DemoState, vm: MapMateViewModel) {
    var tab by remember { mutableStateOf(0) }              // 0 home, 1 friends, 2 trips, 3 profile
    var overlay by remember { mutableStateOf(Overlay.None) }
    var selectedFriendId by remember { mutableStateOf<String?>(null) }
    var navTo by remember { mutableStateOf<Pair<String, String>?>(null) }  // friend name + distance for Navigate
    val settings by vm.settings.collectAsState()

    // overlays take over the whole screen; system back closes them (not the app)
    if (overlay != Overlay.None) {
        BackHandler { overlay = Overlay.None }
        when (overlay) {
            Overlay.Search -> SearchScreen(onBack = { overlay = Overlay.None }, onPick = { overlay = Overlay.Navigate })
            Overlay.Navigate -> NavigateScreen(
                destinationName = navTo?.first ?: "Destination",
                distanceText = navTo?.second.orEmpty(),
                onEndTrip = { overlay = Overlay.None; navTo = null },
            )
            Overlay.AddFriend -> AddFriendScreen(onBack = { overlay = Overlay.None }, onAccept = vm::acceptRequest)
            Overlay.Settings -> SettingsScreen(
                state = state,
                settings = settings,
                onSetPrivacy = vm::setFriendPrivacyMode,
                onSimulateDying = vm::simulatePhoneDying,
                onToggle = vm::toggleSetting,
                onSetLocationMode = vm::setLocationMode,
                onLogout = { overlay = Overlay.None; vm.logout() },
                onBack = { overlay = Overlay.None },
            )
            Overlay.None -> {}
        }
        return
    }

    // on a non-home tab, system back returns to the map tab instead of exiting
    BackHandler(enabled = tab != 0) { tab = 0 }

    Box(Modifier.fillMaxSize().background(NavX.bg)) {
        Box(Modifier.fillMaxSize().padding(bottom = 88.dp)) {
            when (tab) {
                0 -> HomeScreen(
                    state = state,
                    onFriendClick = { selectedFriendId = it.id },
                    onSearchClick = { overlay = Overlay.Search },
                    onAddFriendClick = { overlay = Overlay.AddFriend },
                    onStartTrip = { overlay = Overlay.Navigate },
                )
                1 -> FriendsScreen(
                    state = state,
                    onFriendClick = { selectedFriendId = it.id },
                    onFriendGo = { f -> navTo = f.name to distanceLabel(f); overlay = Overlay.Navigate },
                    onAddFriendClick = { overlay = Overlay.AddFriend },
                    onViewMap = { tab = 0 },
                )
                2 -> TripsScreen()
                else -> ProfileScreen(state, onOpenSettings = { overlay = Overlay.Settings })
            }
        }
        NavXBottomBar(
            current = tab,
            onSelect = { tab = it },
            onSearch = { overlay = Overlay.Search },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // tap-a-friend anywhere -> privacy bottom sheet, looked up live by id so it reflects the current mode
    val friend = selectedFriendId?.let { id -> state.friends.firstOrNull { it.id == id } }
    if (friend != null) {
        ModalBottomSheet(onDismissRequest = { selectedFriendId = null }, containerColor = NavX.surface) {
            FriendPrivacyControls(
                initials = friend.initials,
                name = friend.name,
                subtitle = "${transportLabel(friend.transport)} · ${distanceLabel(friend)}",
                selected = precisionToSegment(friend.precision),
                onSelect = { vm.setFriendPrivacyMode(friend.id, it) },
                onSimulateDying = { vm.simulatePhoneDying(friend.id) },
            )
        }
    }
}

// 5-slot bottom nav -> Map · Friends · [floating search FAB] · Trips · Profile
@Composable
private fun NavXBottomBar(current: Int, onSelect: (Int) -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(NavX.surface)
            .border(width = 1.dp, color = NavX.border, shape = RectangleShape)
            .navigationBarsPadding()
            .height(76.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavTab("Map", Icons.Rounded.Map, current == 0, Modifier.weight(1f)) { onSelect(0) }
        NavTab("Friends", Icons.Rounded.Group, current == 1, Modifier.weight(1f)) { onSelect(1) }
        // center floating search FAB, raised above the bar
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .offset(y = (-18).dp)
                    .size(52.dp)
                    .shadow(12.dp, CircleShape, ambientColor = NavX.accentGlow, spotColor = NavX.accentGlow)
                    .clip(CircleShape)
                    .background(NavX.accent)
                    .border(4.dp, NavX.surface, CircleShape)
                    .clickable(onClick = onSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Navigation, "search", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        NavTab("Trips", Icons.Rounded.Route, current == 2, Modifier.weight(1f)) { onSelect(2) }
        NavTab("Profile", Icons.Rounded.Person, current == 3, Modifier.weight(1f)) { onSelect(3) }
    }
}

@Composable
private fun NavTab(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, label, tint = if (selected) NavX.accentLight else NavX.muted, modifier = Modifier.size(24.dp))
        Text(label, color = if (selected) NavX.accentLight else NavX.muted, fontFamily = Syne, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
    }
}

// small label helpers for the privacy sheet
private fun transportLabel(t: KTransportMode): String = when (t) {
    KTransportMode.WALK -> "Walking"
    KTransportMode.BIKE -> "Cycling"
    KTransportMode.CAR -> "Driving"
    KTransportMode.SHIP -> "Boat"
    KTransportMode.UNKNOWN -> "—"
}

private fun precisionToSegment(p: Precision): String = when (p) {
    Precision.APPROXIMATE -> "blurred"
    Precision.FROZEN -> "frozen"
    else -> "precise"
}

private fun distanceLabel(f: FriendState): String {
    val d = f.distanceMeters ?: return "hidden"
    val prefix = if (f.precision == Precision.EXACT) "" else "~"
    return if (d >= 1000) "$prefix${"%.1f".format(d / 1000.0)} km" else "$prefix$d m"
}
