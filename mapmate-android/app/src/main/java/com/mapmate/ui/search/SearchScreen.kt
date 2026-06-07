package com.mapmate.ui.search

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapmate.demo.DemoData
import com.mapmate.demo.DemoData.Place
import com.mapmate.ui.components.RoundIconButton
import com.mapmate.ui.components.SectionHeader
import com.mapmate.ui.theme.DMSans
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne

@Composable
fun SearchScreen(onBack: () -> Unit, onPick: () -> Unit) {
    var query by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(NavX.bg)
    ) {
        // top bar: back button + focused search field
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 18.dp, end = 18.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = onBack,
            )
            Spacer(Modifier.width(11.dp))
            // search field pill
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavX.card)
                    .border(1.dp, NavX.accent, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = NavX.muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Where to?",
                            color = NavX.muted.copy(alpha = 0.7f),
                            fontFamily = DMSans,
                            fontSize = 15.sp,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = NavX.text, fontFamily = DMSans, fontSize = 15.sp),
                        cursorBrush = SolidColor(NavX.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Rounded.Mic, null, tint = NavX.accentLight, modifier = Modifier.size(18.dp))
            }
        }

        // scrollable body
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            // quick access row: Home / Work / Saved
            Row(Modifier.fillMaxWidth()) {
                QuickCard(Icons.Rounded.Home, "Home", Modifier.weight(1f), onPick)
                Spacer(Modifier.width(10.dp))
                QuickCard(Icons.Rounded.Work, "Work", Modifier.weight(1f), onPick)
                Spacer(Modifier.width(10.dp))
                QuickCard(Icons.Rounded.Star, "Saved", Modifier.weight(1f), onPick)
            }

            SectionHeader("Recent")

            DemoData.recentPlaces.forEach { place ->
                PlaceRow(
                    place = place,
                    leadingIcon = Icons.Rounded.History,
                    onClick = onPick,
                )
            }

            SectionHeader("Suggested nearby")

            DemoData.suggestedPlaces.forEach { place ->
                PlaceRow(
                    place = place,
                    leadingIcon = Icons.Rounded.Place,
                    iconTint = NavX.accentLight,
                    onClick = onPick,
                )
            }

            Spacer(Modifier.height(12.dp))

            // "choose on map" dashed button at bottom
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavX.card)
                    .border(1.dp, NavX.border, RoundedCornerShape(14.dp))
                    .clickable { onPick() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Map, null, tint = NavX.muted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Choose on map",
                    color = NavX.muted,
                    fontFamily = Syne,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// 3-icon quick access card (Home / Work / Saved)
@Composable
private fun QuickCard(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NavX.card)
            .border(1.dp, NavX.border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 13.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = NavX.accentLight, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(7.dp))
        Text(
            label,
            color = NavX.text,
            fontFamily = Syne,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

// single place row — recent uses muted History icon, suggested uses accentLight Place icon
@Composable
private fun PlaceRow(
    place: Place,
    leadingIcon: ImageVector,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = NavX.muted,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 0.dp, // bottom divider drawn via the bottom padding trick below
                color = androidx.compose.ui.graphics.Color.Transparent,
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 40dp icon well
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NavX.card)
                .border(1.dp, NavX.border, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(leadingIcon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(14.dp))

        // name + optional tag badge, then address below
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    place.name,
                    color = NavX.text,
                    fontFamily = DMSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                )
                if (place.tag != null) {
                    Spacer(Modifier.width(8.dp))
                    // pill badge
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NavX.accentSoft)
                            .padding(horizontal = 7.dp, vertical = 1.dp),
                    ) {
                        Text(
                            place.tag,
                            color = NavX.accentLight,
                            fontFamily = Syne,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(1.dp))
            Text(
                place.address,
                color = NavX.muted,
                fontFamily = DMSans,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            null,
            tint = NavX.muted,
            modifier = Modifier.size(18.dp),
        )
    }

    // bottom divider (borderSoft)
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NavX.borderSoft)
    )
}
