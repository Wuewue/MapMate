package com.mapmate.ui.navigate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapmate.ui.components.RoundIconButton
import com.mapmate.ui.map.StylizedMapCanvas
import com.mapmate.ui.theme.DMSans
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne

// turn-by-turn navigation screen, mirrors the Navigate component in 00-design-sample/map.jsx
@Composable
fun NavigateScreen(destinationName: String = "Destination", distanceText: String = "", onEndTrip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavX.mapBg),
    ) {
        // dimmed map with route drawn
        StylizedMapCanvas(
            friends = emptyList(),
            modifier = Modifier.fillMaxSize(),
            showRoute = true,
            dimmed = true,
        )

        // maneuver banner near top
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(22.dp), ambientColor = NavX.accentGlow, spotColor = NavX.accentGlow)
                    .clip(RoundedCornerShape(22.dp))
                    .background(NavX.accent)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // turn icon 38dp white
                Icon(
                    imageVector = Icons.Rounded.Navigation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "250 m",
                        fontFamily = Syne,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = Color.White,
                        lineHeight = 26.sp,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "Toward $destinationName",
                        fontFamily = DMSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // next-step glass pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavX.glass)
                    .border(1.dp, NavX.glassBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (distanceText.isNotEmpty()) "$distanceText to your friend" else "Then continue 1.2 km",
                    color = Color.White,
                    fontFamily = DMSans,
                    fontSize = 12.5.sp,
                )
            }
        }

        // right side: 3 control buttons, centered vertically toward bottom
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .padding(bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RoundIconButton(icon = Icons.Rounded.VolumeUp, onClick = {})
            RoundIconButton(icon = Icons.Rounded.Share, onClick = {})
            RoundIconButton(icon = Icons.Rounded.Explore, onClick = {})
        }

        // speed badge bottom-left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 18.dp, bottom = 108.dp)
                .size(56.dp)
                .shadow(elevation = 12.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "48",
                    fontFamily = Syne,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = NavX.chipText,
                    lineHeight = 20.sp,
                )
                Text(
                    text = "km/h",
                    fontFamily = Syne,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 8.sp,
                    color = NavX.muted,
                )
            }
        }

        // bottom trip bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(NavX.surface)
                .border(
                    width = 1.dp,
                    color = NavX.border,
                    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // ETA row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "8:36",
                            fontFamily = Syne,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = NavX.success,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "AM",
                            fontFamily = DMSans,
                            fontSize = 13.sp,
                            color = NavX.muted,
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    // meta row: 11 min · 3.8 km
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "11 min", color = NavX.muted, fontFamily = DMSans, fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(3.dp).clip(CircleShape).background(NavX.muted))
                        Spacer(Modifier.width(8.dp))
                        Text(text = "3.8 km", color = NavX.muted, fontFamily = DMSans, fontSize = 13.sp)
                    }
                }

                // end trip button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = NavX.danger.copy(alpha = 0.4f), spotColor = NavX.danger.copy(alpha = 0.4f))
                        .clip(CircleShape)
                        .background(NavX.danger)
                        .clickable(onClick = onEndTrip),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "end trip",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

