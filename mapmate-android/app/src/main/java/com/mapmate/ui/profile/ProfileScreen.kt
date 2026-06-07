package com.mapmate.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapmate.demo.DemoState
import com.mapmate.ui.components.NavXAvatar
import com.mapmate.ui.components.Pill
import com.mapmate.ui.components.RoundIconButton
import com.mapmate.ui.components.SectionHeader
import com.mapmate.ui.theme.DMSans
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne

@Composable
fun ProfileScreen(
    state: DemoState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(NavX.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        // --- gradient header panel ---
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    // diagonal fade from accent to a deep purple-black, like the design's 155deg
                    Brush.linearGradient(listOf(NavX.accent, androidx.compose.ui.graphics.Color(0xFF14111F))),
                ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 48.dp, // rough statusBarsPadding substitute; real apps use WindowInsets
                        start = 22.dp,
                        end = 22.dp,
                        bottom = 22.dp,
                    ),
            ) {
                // settings button row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    RoundIconButton(
                        icon = Icons.Rounded.Settings,
                        onClick = onOpenSettings,
                        accent = false,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // avatar + name block
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // bg-colored ring around the avatar, like the design
                    Box(Modifier.clip(CircleShape).background(NavX.bg).padding(4.dp)) {
                        NavXAvatar(initials = "YOU", size = 84)
                    }

                    Column(Modifier.padding(start = 16.dp)) {
                        Text(
                            "You",
                            fontFamily = Syne,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 23.sp,
                            color = Color.White,
                        )
                        Text(
                            "@you",
                            fontFamily = DMSans,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Pill(
                            text = "MapMate Plus",
                            bg = Color.White.copy(alpha = 0.15f),
                            fg = Color.White,
                            leadingIcon = Icons.Rounded.Star,
                        )
                    }
                }
            }
        }

        // --- stats strip ---
        Row(
            Modifier
                .fillMaxWidth()
                .background(NavX.surface),
        ) {
            StatCell(value = "${state.friends.size}", label = "FRIENDS", showDivider = false)
            StatCell(value = "${state.nearbyCount}", label = "NEARBY", showDivider = true)
            StatCell(value = "7", label = "STREAK", showDivider = true)
            StatCell(value = "4.9", label = "RATING", showDivider = true)
        }

        // --- main content ---
        Column(Modifier.padding(18.dp)) {

            // edit profile card row
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavX.card)
                    .border(1.dp, NavX.border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = NavX.accentLight,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Edit Profile",
                    fontFamily = Syne,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = NavX.text,
                )
            }

            // personal info section
            SectionHeader("Personal info")
            InfoRow(icon = Icons.Rounded.Email, label = "Email", value = "you@mapmate.app")
            InfoRow(icon = Icons.Rounded.Phone, label = "Phone", value = "+84 ••• ••• 12")

            // friends section
            SectionHeader("Friends · ${state.friends.size}")

            // horizontal scroll row of friend avatars
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                state.friends.forEach { f ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 14.dp),
                    ) {
                        NavXAvatar(initials = f.initials, size = 52)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            f.name.substringBefore(" "),
                            fontFamily = DMSans,
                            fontSize = 11.sp,
                            color = NavX.muted,
                        )
                    }
                }
            }
        }
    }
}

// one stat cell in the stats strip
@Composable
private fun RowScope.StatCell(value: String, label: String, showDivider: Boolean) {
    Row(Modifier.weight(1f)) {
        if (showDivider) {
            Box(
                Modifier
                    .width(1.dp)
                    .height(52.dp)
                    .background(NavX.border),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                fontFamily = Syne,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = NavX.text,
            )
            Text(
                label,
                fontFamily = Syne,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                color = NavX.muted,
            )
        }
    }
}

// icon + label/value pair used in personal info section
@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 38dp icon well
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(NavX.card)
                .border(1.dp, NavX.border, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = NavX.accentLight, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(label, fontFamily = DMSans, fontSize = 11.sp, color = NavX.muted)
            Text(value, fontFamily = DMSans, fontSize = 14.sp, color = NavX.text)
        }
    }
}
