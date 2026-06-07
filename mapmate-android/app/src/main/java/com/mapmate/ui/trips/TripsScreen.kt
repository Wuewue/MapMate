package com.mapmate.ui.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapmate.demo.DemoData
import com.mapmate.ui.components.RoundIconButton
import com.mapmate.ui.components.SectionHeader
import com.mapmate.ui.theme.DMSans
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne

@Composable
fun TripsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavX.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        // status bar top spacing
        Spacer(Modifier.height(54.dp))

        // header row: title + filter button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Your Trips",
                    fontFamily = Syne,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 25.sp,
                    color = NavX.text,
                )
                Text(
                    "This week",
                    fontFamily = DMSans,
                    fontSize = 13.sp,
                    color = NavX.muted,
                )
            }
            RoundIconButton(icon = Icons.Rounded.FilterList, onClick = {})
        }

        Spacer(Modifier.height(18.dp))

        // weekly stats gradient banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(listOf(NavX.accent, NavX.accentLight)),
                )
                .padding(18.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                DemoData.weeklyStats.forEachIndexed { index, stat ->
                    if (index > 0) {
                        // thin white divider between cells
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(48.dp)
                                .background(Color.White.copy(alpha = 0.22f))
                                .align(Alignment.CenterVertically),
                        )
                        Spacer(Modifier.width(14.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            stat.value,
                            fontFamily = Syne,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.White,
                            lineHeight = 22.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stat.label,
                            fontFamily = Syne,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        // recent trips section
        SectionHeader("Recent trips")

        Column {
            DemoData.trips.forEachIndexed { index, trip ->
                TripRow(trip = trip, isFirst = index == 0)
                if (index < DemoData.trips.lastIndex) {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        // bottom padding so last row clears the bottom nav bar
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun TripRow(trip: DemoData.Trip, isFirst: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NavX.card)
            .border(1.dp, NavX.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // icon well: first trip gets solid accent bg, rest get soft bg
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (isFirst) NavX.accent else NavX.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Place,
                contentDescription = null,
                tint = if (isFirst) Color.White else NavX.accentLight,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(13.dp))

        // destination + date
        Column(Modifier.weight(1f)) {
            Text(
                trip.destination,
                fontFamily = Syne,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = NavX.text,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                trip.date,
                fontFamily = DMSans,
                fontSize = 12.sp,
                color = NavX.muted,
            )
        }

        // duration + distance (right-aligned)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                trip.duration,
                fontFamily = Syne,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = NavX.text,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                trip.distance,
                fontFamily = DMSans,
                fontSize = 11.5.sp,
                color = NavX.muted,
            )
        }
    }
}
