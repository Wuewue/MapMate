package com.mapmate.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mapmate.demo.FriendState
import com.mapmate.privacy.Precision
import com.mapmate.ui.components.NavXAvatar
import com.mapmate.ui.theme.NavX

// stylized procedural map -> reproduces the NavX design's canvas map in compose, no google maps key
// friend pins placed by their privacy-filtered position, rendered per privacy mode
// showRoute draws the signature accent route line, onFriendClick makes pins tappable, content overlays chips
@Composable
fun StylizedMapCanvas(
    friends: List<FriendState>,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    showRoute: Boolean = false,
    onFriendClick: ((FriendState) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    BoxWithConstraints(modifier.background(NavX.mapBg)) {
        val w = maxWidth
        val h = maxHeight

        Canvas(Modifier.size(w, h)) {
            val sx = size.width / 384f
            val sy = size.height / 760f

            // grid
            val step = 32f * sx
            var gx = 0f
            while (gx < size.width) { drawLine(NavX.mapGrid, Offset(gx, 0f), Offset(gx, size.height), 1f); gx += step }
            var gy = 0f
            while (gy < size.height) { drawLine(NavX.mapGrid, Offset(0f, gy), Offset(size.width, gy), 1f); gy += step }

            // park blob
            drawPath(Path().apply {
                moveTo(-20f * sx, 120f * sy)
                quadraticTo(60f * sx, 90f * sy, 120f * sx, 140f * sy)
                quadraticTo(150f * sx, 200f * sy, 90f * sx, 240f * sy)
                quadraticTo(10f * sx, 250f * sy, -20f * sx, 200f * sy)
                close()
            }, NavX.mapPark)
            // water blob
            drawPath(Path().apply {
                moveTo(-20f * sx, 600f * sy)
                quadraticTo(120f * sx, 560f * sy, 220f * sx, 620f * sy)
                quadraticTo(320f * sx, 680f * sy, 420f * sx, 640f * sy)
                lineTo(420f * sx, 800f * sy); lineTo(-20f * sx, 800f * sy); close()
            }, NavX.mapWater)

            // roads
            listOf(
                listOf(192f, -20f, 192f, 780f, 22f), listOf(-20f, 300f, 404f, 300f, 18f),
                listOf(80f, -20f, 80f, 780f, 13f), listOf(300f, -20f, 300f, 780f, 13f),
                listOf(-20f, 160f, 404f, 160f, 11f), listOf(-20f, 470f, 404f, 470f, 11f),
                listOf(-20f, 620f, 404f, 620f, 9f),
            ).forEach { r ->
                drawLine(NavX.mapRoad, Offset(r[0] * sx, r[1] * sy), Offset(r[2] * sx, r[3] * sy),
                    strokeWidth = r[4] * sx, cap = StrokeCap.Round)
            }
            val dash = PathEffect.dashPathEffect(floatArrayOf(2f * sx, 6f * sx))
            drawLine(NavX.mapGrid, Offset(192f * sx, 0f), Offset(192f * sx, size.height), 1.5f * sx, pathEffect = dash)
            drawLine(NavX.mapGrid, Offset(0f, 300f * sy), Offset(size.width, 300f * sy), 1.5f * sx, pathEffect = dash)

            // signature route line (glow pass + line pass) + origin/dest markers
            if (showRoute) {
                val route = Path().apply {
                    moveTo(214f * sx, 110f * sy)
                    cubicTo(214f * sx, 180f * sy, 262f * sx, 200f * sy, 262f * sx, 270f * sy)
                    cubicTo(262f * sx, 350f * sy, 196f * sx, 380f * sy, 150f * sx, 430f * sy)
                }
                drawPath(route, NavX.accentGlow, style = Stroke(width = 14f * sx, cap = StrokeCap.Round))
                drawPath(route, NavX.accent, style = Stroke(width = 5.5f * sx, cap = StrokeCap.Round))
                drawCircle(Color.White, 9f * sx, Offset(214f * sx, 110f * sy))
                drawCircle(NavX.accent, 4.5f * sx, Offset(214f * sx, 110f * sy))
                drawCircle(NavX.accent, 13f * sx, Offset(150f * sx, 430f * sy))
                drawCircle(Color.White, 5.5f * sx, Offset(150f * sx, 430f * sy))
            }

            if (dimmed) drawRect(NavX.mapScrim)
        }

        // self marker at the center
        Box(
            Modifier.offset(x = w * 0.5f - 9.dp, y = h * 0.5f - 9.dp).size(18.dp)
                .clip(CircleShape).background(NavX.accent).border(3.dp, Color.White, CircleShape),
        )

        // friend pins, placed + rendered per privacy mode, with a pin tail + optional tap
        friends.forEach { f ->
            val fx = f.mapX ?: return@forEach
            val fy = f.mapY ?: return@forEach

            if (f.precision == Precision.APPROXIMATE && f.blurRadiusMeters != null) {
                val radiusFrac = (f.blurRadiusMeters / 111_000.0 / (2 * 0.018)).toFloat()
                val diameter = (radiusFrac * 2f) * w.value
                Box(
                    Modifier.offset(x = (w.value * fx).dp - (diameter / 2).dp, y = (h.value * fy).dp - (diameter / 2).dp)
                        .size(diameter.dp).clip(CircleShape)
                        .background(NavX.accent.copy(alpha = 0.12f))
                        .border(1.5.dp, NavX.accent.copy(alpha = 0.55f), CircleShape),
                )
            }

            val pinMod = Modifier.offset(x = (w.value * fx).dp - 21.dp, y = (h.value * fy).dp - 24.dp)
                .let { if (onFriendClick != null) it.clickable { onFriendClick(f) } else it }
            Box(pinMod, contentAlignment = Alignment.TopCenter) {
                MapFriendPin(f)
            }
        }

        content()
    }
}

// avatar pin with a white ring + a little downward triangle tail, like a real map pin
@Composable
private fun MapFriendPin(f: FriendState) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(Modifier.size(42.dp).clip(CircleShape).border(3.dp, Color.White, CircleShape)) {
                NavXAvatar(f.initials, size = 42, online = if (f.precision == Precision.EXACT) f.online else null)
            }
            if (f.precision == Precision.FROZEN) {
                Box(Modifier.size(16.dp).clip(CircleShape).background(NavX.surface), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, "frozen", tint = NavX.accentLight, modifier = Modifier.size(10.dp))
                }
            }
        }
        // tail triangle
        Canvas(Modifier.size(width = 12.dp, height = 7.dp)) {
            val p = Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, size.height); close() }
            drawPath(p, Color.White)
        }
    }
}
