package com.mapmate.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapmate.ui.components.PrimaryButton
import com.mapmate.ui.map.StylizedMapCanvas
import com.mapmate.ui.theme.DMSans
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne

// slide data: title + body copy, 1:1 with auth.jsx ONB array (adapted to contract body copy)
private data class Slide(val title: String, val body: String)

private val slides = listOf(
    Slide("Navigate smarter", "Find the fastest route with live traffic and friends along the way."),
    Slide("Travel together", "See where your circle is, share your live location, arrive together."),
    Slide("Arrive with ease", "Turn-by-turn guidance and off-the-grid alerts keep everyone safe."),
)

@Composable
fun SplashScreen(onGetStarted: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val slide = slides[step]
    val isLast = step == slides.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavX.bg)
            .statusBarsPadding(),
    ) {
        // ── top ~58%: map hero ────────────────────────────────────────────────
        StylizedMapCanvas(
            friends = emptyList(),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.58f),
            showRoute = true,
            dimmed = true,
        )

        // gradient scrim fading the map into the bg color below it
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.58f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, NavX.bg),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    ),
                ),
        )

        // ── logo top-left ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 26.dp, top = 90.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "Map",
                fontFamily = Syne,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                color = NavX.text,
            )
            Text(
                text = "Mate",
                fontFamily = Syne,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                color = NavX.accentLight,
            )
            Spacer(Modifier.width(6.dp))
            // accent dot, same vibe as the design's NavX dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(bottom = 4.dp)
                    .clip(CircleShape)
                    .background(NavX.accent)
                    .align(Alignment.Bottom),
            )
        }

        // ── skip top-right ────────────────────────────────────────────────────
        Text(
            text = "Skip",
            fontFamily = Syne,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = NavX.muted,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 54.dp)
                .clickable(onClick = onGetStarted),
        )

        // ── floating icon disc centered at ~52% height ────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight(0.52f)
                .wrapContentHeight(Alignment.Bottom),
            contentAlignment = Alignment.Center,
        ) {
            // outer accentSoft ring
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(RoundedCornerShape(42.dp))
                    .background(NavX.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                // inner accent disc
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(NavX.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NearMe,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }

        // ── bottom panel ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, bottom = 30.dp),
        ) {
            // slide heading
            Text(
                text = slide.title,
                fontFamily = Syne,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 27.sp,
                color = NavX.text,
                lineHeight = 32.sp,
            )

            Spacer(Modifier.height(10.dp))

            // slide body
            Text(
                text = slide.body,
                fontFamily = DMSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.5.sp,
                color = NavX.muted,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(22.dp))

            // pagination dots row
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                slides.indices.forEach { i ->
                    val isActive = i == step
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isActive) 26.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isActive) NavX.accent else NavX.border)
                            .clickable { step = i },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // cta button: "Next" + chevron on steps 0-1, "Get Started" on last
            PrimaryButton(
                text = if (isLast) "Get Started" else "Next",
                onClick = { if (isLast) onGetStarted() else step++ },
                leadingIcon = if (isLast) null else Icons.Rounded.ChevronRight,
            )

            Spacer(Modifier.height(16.dp))

            // sign in link
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = NavX.muted, fontFamily = DMSans, fontSize = 13.sp)) {
                        append("Already have an account? ")
                    }
                    withStyle(SpanStyle(color = NavX.accentLight, fontFamily = DMSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)) {
                        append("Sign in")
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(onClick = onGetStarted),
            )
        }
    }
}
