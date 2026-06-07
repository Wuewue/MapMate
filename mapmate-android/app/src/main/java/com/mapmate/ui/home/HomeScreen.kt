package com.mapmate.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapmate.demo.AlertState
import com.mapmate.demo.DemoState
import com.mapmate.demo.FriendState
import com.mapmate.device.BatteryLevel
import com.mapmate.privacy.OffTheGridReason
import com.mapmate.privacy.Precision
import com.mapmate.telemetry.KTransportMode
import com.mapmate.ui.components.MapChip
import com.mapmate.ui.components.NavXAvatar
import com.mapmate.ui.components.OutlineButton
import com.mapmate.ui.components.Pill
import com.mapmate.ui.components.PrimaryButton
import com.mapmate.ui.components.RoundIconButton
import com.mapmate.ui.map.StylizedMapCanvas
import com.mapmate.ui.theme.DMSans
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne

// full home screen — map + search pill + layers + destination sheet + nearby list
// matches the "sheet" layout from map.jsx Home component 1:1
@Composable
fun HomeScreen(
    state: DemoState,
    onFriendClick: (FriendState) -> Unit,
    onSearchClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    onStartTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(NavX.mapBg)) {

        // 1. full-screen map with route + friend pins + chip overlays
        StylizedMapCanvas(
            friends = state.friends,
            modifier = Modifier.fillMaxSize(),
            showRoute = true,
            onFriendClick = onFriendClick,
        ) {
            // "Home" chip near top-center
            MapChip(
                text = "Home",
                modifier = Modifier.align(Alignment.TopCenter).offset(x = (-40).dp, y = 130.dp),
            )
            // "12 min" ETA chip mid-map, accent colored
            MapChip(
                text = "12 min",
                modifier = Modifier.align(Alignment.Center).offset(x = 20.dp, y = (-40).dp),
                accent = true,
            )
            // destination chip with danger dot
            MapChip(
                text = "Riverside Office",
                modifier = Modifier.align(Alignment.CenterStart).offset(x = 12.dp, y = 100.dp),
                dotColor = NavX.danger,
            )
        }

        // 2. top overlay: search pill + layers button, then self chip, then optional off-grid banner
        Column(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // glass search pill (tappable -> opens search screen)
                Row(
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(NavX.glass)
                        .border(1.dp, NavX.glassBorder, RoundedCornerShape(23.dp))
                        .clickable(onClick = onSearchClick)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, null, tint = NavX.muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Where to?", color = NavX.muted, fontFamily = DMSans, fontSize = 14.sp)
                }
                Spacer(Modifier.width(10.dp))
                RoundIconButton(Icons.Rounded.Layers, {})
            }

            Spacer(Modifier.height(12.dp))

            // self chip — transport mode + battery from the SelfState
            MapChip(
                text = if (state.self.sharing) "You · ${transportLabel(state.self.transport)} · ${state.self.batteryPercent}%" else "You · location off",
                dotColor = if (state.self.sharing) NavX.success else NavX.muted,
            )

            // off-grid alert banner — only rendered when alerts list is non-empty
            if (state.alerts.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                OffGridBanner(state.alerts.first())
            }
        }

        // 3. add-friend FAB — 52dp accent circle, above the bottom sheet
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 296.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(NavX.accent)
                .shadow(16.dp, CircleShape, ambientColor = NavX.accentGlow, spotColor = NavX.accentGlow)
                .clickable(onClick = onAddFriendClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PersonAdd, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }

        // 4. bottom sheet — destination card + actions + nearby section
        DestinationSheet(
            state = state,
            onFriendClick = onFriendClick,
            onStartTrip = onStartTrip,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// off-grid warning banner — danger tinted with icon + friend name + reason
@Composable
private fun OffGridBanner(alert: AlertState) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NavX.danger.copy(alpha = 0.16f))
            .border(1.dp, NavX.danger.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WarningAmber, null, tint = NavX.danger, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "${alert.friendName} · ${reasonLabel(alert.reason)}",
            color = NavX.text,
            fontFamily = DMSans,
            fontSize = 13.sp,
        )
    }
}

// the bottom sheet — drag handle + destination card + start/save + nearby list
@Composable
private fun DestinationSheet(
    state: DemoState,
    onFriendClick: (FriendState) -> Unit,
    onStartTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(NavX.surface)
            .border(
                width = 1.dp,
                color = NavX.border,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            )
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 28.dp),
    ) {
        // drag handle pill
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(38.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NavX.border),
        )

        Spacer(Modifier.height(16.dp))

        // destination card
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(NavX.card)
                .border(1.dp, NavX.border, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // icon well
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(NavX.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Place, null, tint = NavX.accentLight, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(13.dp))

            // destination name + route meta
            Column(Modifier.weight(1f)) {
                Text(
                    "Riverside Office",
                    color = NavX.text,
                    fontFamily = Syne,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Text(
                    "Via Bay Ave · 4.2 km",
                    color = NavX.muted,
                    fontFamily = DMSans,
                    fontSize = 12.5.sp,
                )
            }

            Spacer(Modifier.width(10.dp))

            // ETA column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "12",
                    color = NavX.accentLight,
                    fontFamily = Syne,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                )
                Text(
                    "MIN",
                    color = NavX.muted,
                    fontFamily = Syne,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // action row — Start (weight 2) + Save (weight 1)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            PrimaryButton(
                text = "Start",
                onClick = onStartTrip,
                leadingIcon = Icons.Rounded.Navigation,
                modifier = Modifier.weight(2f),
            )
            OutlineButton(
                text = "Save",
                onClick = {},
                leadingIcon = Icons.Rounded.BookmarkBorder,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(18.dp))

        // nearby section header
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Nearby",
                color = NavX.text,
                fontFamily = Syne,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.width(8.dp))
            Pill(text = "${state.nearbyCount} within 1 km")
        }

        Spacer(Modifier.height(10.dp))

        // scrollable nearby list, capped at ~150dp so sheet doesn't eat screen
        Column(
            Modifier
                .height(150.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.friends.forEach { f ->
                NearbyRow(f, onFriendClick)
            }
        }
    }
}

// one friend row in the nearby list — avatar, name/meta, battery pill
@Composable
private fun NearbyRow(f: FriendState, onFriendClick: (FriendState) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onFriendClick(f) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavXAvatar(f.initials, size = 40, online = f.online)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(f.name, color = NavX.text, fontFamily = Syne, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
            Text(
                "${transportLabel(f.transport)} · ${distanceLabel(f)} · ${privacyLabel(f.precision)}",
                color = NavX.muted,
                fontFamily = DMSans,
                fontSize = 12.sp,
            )
        }
        Pill(
            text = "${f.batteryPercent}%",
            bg = NavX.accentSoft,
            fg = batteryColor(f.batteryLevel),
        )
    }
}

// ---- private label helpers ----

private fun transportLabel(t: KTransportMode): String = when (t) {
    KTransportMode.WALK -> "Walking"
    KTransportMode.BIKE -> "Cycling"
    KTransportMode.CAR -> "Driving"
    KTransportMode.SHIP -> "Boat"
    KTransportMode.UNKNOWN -> "—"
}

// prefix "~" unless exact; "hidden" when no distance data
private fun distanceLabel(f: FriendState): String {
    val d = f.distanceMeters ?: return "hidden"
    val prefix = if (f.precision == Precision.EXACT) "" else "~"
    return if (d >= 1000) "$prefix${"%.1f".format(d / 1000.0)} km" else "$prefix${d} m"
}

private fun privacyLabel(p: Precision): String = when (p) {
    Precision.EXACT -> "precise"
    Precision.APPROXIMATE -> "blurred"
    Precision.FROZEN -> "frozen"
    Precision.LAST_KNOWN -> "last known"
    Precision.HIDDEN -> "hidden"
}

private fun reasonLabel(r: OffTheGridReason): String = when (r) {
    OffTheGridReason.BATTERY_DEAD -> "phone battery died"
    OffTheGridReason.SIGNAL_LOST -> "lost signal"
    OffTheGridReason.NO_RECENT_UPDATE -> "no recent update"
}

private fun batteryColor(level: BatteryLevel): Color = when (level) {
    BatteryLevel.LOW -> NavX.danger
    BatteryLevel.CHARGING, BatteryLevel.FULL -> NavX.success
    BatteryLevel.NORMAL -> NavX.text
}
