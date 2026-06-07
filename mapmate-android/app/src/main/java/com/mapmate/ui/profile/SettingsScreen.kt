package com.mapmate.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapmate.demo.AppSettings
import com.mapmate.demo.DemoState
import com.mapmate.device.LocationSharingConfig
import com.mapmate.device.LocationSharingMode
import com.mapmate.privacy.Precision
import com.mapmate.ui.components.NavXAvatar
import com.mapmate.ui.components.NavXToggle
import com.mapmate.ui.components.OutlineButton
import com.mapmate.ui.components.RoundIconButton
import com.mapmate.ui.components.SectionHeader
import com.mapmate.ui.components.SegmentedControl
import com.mapmate.ui.theme.DMSans
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne

// maps precision enum -> segment string for SegmentedControl
private fun toSegment(p: Precision): String = when (p) {
    Precision.APPROXIMATE -> "blurred"
    Precision.FROZEN -> "frozen"
    else -> "precise"
}

@Composable
fun SettingsScreen(
    state: DemoState,
    settings: AppSettings,
    onSetPrivacy: (friendId: String, mode: String) -> Unit,
    onSimulateDying: (friendId: String) -> Unit,
    onToggle: (key: String, value: Boolean) -> Unit,
    onSetLocationMode: (LocationSharingMode) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(NavX.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 40.dp),
    ) {

        // 1. header
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(icon = Icons.AutoMirrored.Rounded.ArrowBack, onClick = onBack)
            Text(
                "Settings",
                fontFamily = Syne,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 25.sp,
                color = NavX.text,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        // 2. profile card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(NavX.card)
                .border(1.dp, NavX.border, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavXAvatar(initials = "YOU", size = 50)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    "You",
                    fontFamily = Syne,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NavX.text,
                )
                Text(
                    "you@mapmate.app",
                    fontFamily = DMSans,
                    fontSize = 12.5.sp,
                    color = NavX.muted,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = NavX.muted,
            )
        }

        Spacer(Modifier.height(4.dp))

        // 3. location & privacy — wires Mike's LocationSharingMode module
        SectionHeader("Location & privacy")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NavX.card)
                .border(1.dp, NavX.border, RoundedCornerShape(16.dp)),
        ) {
            // radio row: foreground only
            LocationModeRow(
                label = "While using the app",
                selected = settings.locationMode == LocationSharingMode.FOREGROUND_ONLY,
                onClick = { onSetLocationMode(LocationSharingMode.FOREGROUND_ONLY) },
            )

            HorizontalDivider(color = NavX.borderSoft, thickness = 1.dp)

            // radio row: always / background
            LocationModeRow(
                label = "Always",
                selected = settings.locationMode == LocationSharingMode.INCLUDE_BACKGROUND,
                onClick = { onSetLocationMode(LocationSharingMode.INCLUDE_BACKGROUND) },
            )

            HorizontalDivider(color = NavX.borderSoft, thickness = 1.dp)

            // status hint derived from the LocationSharingConfig computed props
            val cfg = LocationSharingConfig(settings.locationMode, settings.hasBackgroundPermission)
            val hintText = when {
                cfg.needsBackgroundPermission -> "⚠ Always-on needs background location permission"
                cfg.isForegroundOnly -> "Foreground-only — shared while the app is open"
                else -> "Sharing in the background"
            }
            val hintColor = if (cfg.needsBackgroundPermission) NavX.danger else NavX.muted
            Text(
                hintText,
                fontFamily = DMSans,
                fontSize = 12.sp,
                color = hintColor,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )

            HorizontalDivider(color = NavX.borderSoft, thickness = 1.dp)

            // live location toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(NavX.accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    // reuse the RadioButtonChecked icon as a location-share indicator
                    Icon(
                        Icons.Rounded.RadioButtonChecked,
                        contentDescription = null,
                        tint = NavX.accentLight,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    "Share my live location",
                    fontFamily = DMSans,
                    fontSize = 14.5.sp,
                    color = NavX.text,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                )
                NavXToggle(
                    checked = settings.shareLocation,
                    onCheckedChange = { onToggle("share", it) },
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // 4. per-friend privacy
        SectionHeader("Per-friend privacy")
        Text(
            "Control exactly what each friend sees",
            fontFamily = DMSans,
            fontSize = 12.5.sp,
            color = NavX.muted,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        state.friends.forEach { f ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NavX.card)
                    .border(1.dp, NavX.border, RoundedCornerShape(16.dp))
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NavXAvatar(initials = f.initials, size = 40)
                    Text(
                        f.name,
                        fontFamily = Syne,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        color = NavX.text,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                SegmentedControl(
                    selected = toSegment(f.precision),
                    onSelect = { onSetPrivacy(f.id, it) },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Simulate phone dying",
                    fontFamily = DMSans,
                    fontSize = 12.sp,
                    color = NavX.danger,
                    modifier = Modifier.clickable { onSimulateDying(f.id) },
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // 5. appearance
        SectionHeader("Appearance")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NavX.card)
                .border(1.dp, NavX.border, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(NavX.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.DarkMode,
                    contentDescription = null,
                    tint = NavX.accentLight,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                "Dark mode",
                fontFamily = DMSans,
                fontSize = 14.5.sp,
                color = NavX.text,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            )
            // live — bound to settings.darkMode
            NavXToggle(
                checked = settings.darkMode,
                onCheckedChange = { onToggle("dark", it) },
            )
        }

        Spacer(Modifier.height(4.dp))

        // 6. alerts & notifications — all three toggles live
        SectionHeader("Alerts & notifications")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NavX.card)
                .border(1.dp, NavX.border, RoundedCornerShape(16.dp)),
        ) {
            AlertToggleRow(
                icon = { Icon(Icons.Rounded.NotificationsActive, null, tint = NavX.accentLight, modifier = Modifier.size(18.dp)) },
                label = "Off-the-grid alerts",
                checked = settings.offGridAlerts,
                onCheckedChange = { onToggle("offgrid", it) },
            )
            HorizontalDivider(color = NavX.borderSoft, thickness = 1.dp)
            AlertToggleRow(
                icon = { Icon(Icons.Rounded.Notifications, null, tint = NavX.accentLight, modifier = Modifier.size(18.dp)) },
                label = "Push notifications",
                checked = settings.notifications,
                onCheckedChange = { onToggle("notifications", it) },
            )
            HorizontalDivider(color = NavX.borderSoft, thickness = 1.dp)
            AlertToggleRow(
                icon = { Icon(Icons.Rounded.VolumeUp, null, tint = NavX.accentLight, modifier = Modifier.size(18.dp)) },
                label = "Voice guidance",
                checked = settings.voiceGuidance,
                onCheckedChange = { onToggle("voice", it) },
            )
        }

        Spacer(Modifier.height(22.dp))

        // 7. log out
        OutlineButton(
            text = "Log Out",
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.AutoMirrored.Rounded.Logout,
            tint = NavX.danger,
        )

        Spacer(Modifier.height(16.dp))

        // 8. version footer
        Text(
            "MapMate v1.0 · merged build",
            fontFamily = DMSans,
            fontSize = 12.sp,
            color = NavX.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// radio-style row for location mode selection
@Composable
private fun LocationModeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontFamily = DMSans,
            fontSize = 14.5.sp,
            color = NavX.text,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) NavX.accent else NavX.muted,
            modifier = Modifier.size(22.dp),
        )
    }
}

// reusable icon-well + label + toggle row used in the alerts section
@Composable
private fun AlertToggleRow(
    icon: @Composable () -> Unit,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(NavX.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            label,
            fontFamily = DMSans,
            fontSize = 14.5.sp,
            color = NavX.text,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        )
        NavXToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}
