package com.mapmate.ui.social

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapmate.demo.AlertState
import com.mapmate.demo.DemoState
import com.mapmate.demo.FriendState
import com.mapmate.privacy.OffTheGridReason
import com.mapmate.privacy.Precision
import com.mapmate.telemetry.KTransportMode
import com.mapmate.ui.components.MapChip
import com.mapmate.ui.components.NavXAvatar
import com.mapmate.ui.components.Pill
import com.mapmate.ui.components.RoundIconButton
import com.mapmate.ui.components.SectionHeader
import com.mapmate.ui.map.StylizedMapCanvas
import com.mapmate.ui.theme.DMSans
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne

@Composable
fun FriendsScreen(
    state: DemoState,
    onFriendClick: (FriendState) -> Unit = {},
    onFriendGo: (FriendState) -> Unit = {},
    onAddFriendClick: () -> Unit = {},
    onViewMap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavX.bg)
            .statusBarsPadding(),
    ) {
        // header: title + subtitle + add-friend button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Friends",
                    fontFamily = Syne,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 25.sp,
                    color = NavX.text,
                )
                Text(
                    "${state.nearbyCount} sharing nearby now",
                    fontFamily = DMSans,
                    fontSize = 13.sp,
                    color = NavX.muted,
                )
            }
            Spacer(Modifier.weight(1f))
            RoundIconButton(
                icon = Icons.Rounded.PersonAdd,
                onClick = onAddFriendClick,
                accent = true,
            )
        }

        // scrollable body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 96.dp),
        ) {
            // mini-map preview card — 168dp tall, clipped at 22dp corners, 1dp border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, NavX.border, RoundedCornerShape(22.dp)),
            ) {
                StylizedMapCanvas(
                    friends = state.friends,
                    modifier = Modifier.fillMaxSize(),
                    onFriendClick = onFriendClick,
                ) {
                    // glass chip bottom-start linking to the live map
                    MapChip(
                        text = "View live map",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .clickable { onViewMap() },
                        dotColor = NavX.success,
                    )
                }
            }

            // off-the-grid alerts — only shown when the list is non-empty
            if (state.alerts.isNotEmpty()) {
                SectionHeader("Off the grid")
                state.alerts.forEach { alert -> AlertCard(alert) }
            }

            // online section
            SectionHeader("Sharing now")
            state.friends.filter { it.online }.forEach { f ->
                FriendRow(f = f, onClick = { onFriendClick(f) }, onGo = { onFriendGo(f) })
            }

            // offline section
            SectionHeader("Offline")
            state.friends.filter { !it.online }.forEach { f ->
                FriendRow(f = f, onClick = { onFriendClick(f) }, offline = true)
            }
        }
    }
}

// off-the-grid alert card with danger border
@Composable
private fun AlertCard(alert: AlertState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(NavX.card, RoundedCornerShape(16.dp))
            .border(1.dp, NavX.danger, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = NavX.danger,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                alert.friendName,
                fontFamily = Syne,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = NavX.text,
            )
            Text(
                reasonLabel(alert.reason),
                fontFamily = DMSans,
                fontSize = 12.5.sp,
                color = NavX.danger,
            )
        }
    }
}

// shared row for both online & offline friends
@Composable
private fun FriendRow(f: FriendState, onClick: () -> Unit, onGo: () -> Unit = {}, offline: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(NavX.card, RoundedCornerShape(16.dp))
            .border(1.dp, NavX.border, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 11.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        NavXAvatar(initials = f.initials, size = 46, online = f.online)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                f.name,
                fontFamily = Syne,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = NavX.text,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    transportIcon(f.transport),
                    contentDescription = null,
                    tint = NavX.muted,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${transportLabel(f.transport)} · ${distanceLabel(f)} · ${privacyLabel(f.precision)}",
                    fontFamily = DMSans,
                    fontSize = 12.5.sp,
                    color = NavX.muted,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        if (offline) {
            // muted ping pill for offline friends
            Pill(
                text = "Ping",
                bg = NavX.card,
                fg = NavX.muted,
            )
        } else {
            // go pill with nav arrow for online friends -> routes to them
            Pill(
                text = "Go",
                bg = NavX.accentSoft,
                fg = NavX.accentLight,
                leadingIcon = Icons.Rounded.Navigation,
                modifier = Modifier.clickable { onGo() },
            )
        }
    }
}

// --- private helpers ---

private fun reasonLabel(r: OffTheGridReason): String = when (r) {
    OffTheGridReason.BATTERY_DEAD -> "phone battery died"
    OffTheGridReason.SIGNAL_LOST -> "lost signal"
    OffTheGridReason.NO_RECENT_UPDATE -> "no recent update"
}

private fun transportIcon(mode: KTransportMode): ImageVector = when (mode) {
    KTransportMode.WALK -> Icons.Rounded.DirectionsWalk
    KTransportMode.BIKE -> Icons.Rounded.DirectionsBike
    KTransportMode.CAR -> Icons.Rounded.DirectionsCar
    KTransportMode.SHIP -> Icons.Rounded.DirectionsBoat
    else -> Icons.Rounded.DirectionsWalk
}

private fun transportLabel(mode: KTransportMode): String = when (mode) {
    KTransportMode.WALK -> "Walking"
    KTransportMode.BIKE -> "Cycling"
    KTransportMode.CAR -> "Driving"
    KTransportMode.SHIP -> "Boat"
    else -> "On foot"
}

// prefix ~ for non-exact precision, "hidden" if no distance data
private fun distanceLabel(f: FriendState): String {
    val d = f.distanceMeters ?: return "hidden"
    val prefix = if (f.precision == Precision.EXACT) "" else "~"
    return if (d >= 1000) "${prefix}${String.format("%.1f", d / 1000.0)} km"
    else "${prefix}${d} m"
}

private fun privacyLabel(p: Precision): String = when (p) {
    Precision.EXACT -> "exact"
    Precision.APPROXIMATE -> "approx"
    Precision.FROZEN -> "frozen"
    Precision.LAST_KNOWN -> "last known"
    Precision.HIDDEN -> "hidden"
}
