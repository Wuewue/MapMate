package com.mapmate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapmate.ui.theme.DMSans
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne

// ---- shared navx widget kit, every screen builds from these so the look stays consistent ----

// the big purple call-to-action button (Sign In / Verify / Start ...)
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // purple bloom under the cta, like the design's box-shadow
            .shadow(if (enabled) 16.dp else 0.dp, RoundedCornerShape(16.dp), ambientColor = NavX.accentGlow, spotColor = NavX.accentGlow)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) NavX.accent else NavX.accent.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Color.White, fontFamily = Syne, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

// 3-way privacy segmented control (precise / blurred / frozen), used in Settings + the tap-a-friend sheet
@Composable
fun SegmentedControl(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val opts = listOf("precise" to "Precise", "blurred" to "Blurred", "frozen" to "Frozen")
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NavX.surface)
            .padding(4.dp),
    ) {
        opts.forEach { (id, label) ->
            val sel = id == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (sel) NavX.accent else Color.Transparent)
                    .clickable { onSelect(id) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (sel) Color.White else NavX.muted, fontFamily = Syne, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

// body of the tap-a-friend privacy bottom sheet
@Composable
fun FriendPrivacyControls(
    initials: String,
    name: String,
    subtitle: String,
    selected: String,
    onSelect: (String) -> Unit,
    onSimulateDying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NavXAvatar(initials, size = 48)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, color = NavX.text, fontFamily = Syne, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = NavX.muted, fontFamily = DMSans, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "WHO SEES YOUR LOCATION", color = NavX.muted, fontFamily = Syne,
            fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(8.dp))
        SegmentedControl(selected, onSelect)
        Spacer(Modifier.height(18.dp))
        Text(
            "Simulate phone dying", color = NavX.danger, fontFamily = DMSans, fontSize = 13.sp,
            modifier = Modifier.clickable { onSimulateDying() },
        )
    }
}

// white/accent floating chip for map overlays ("Home", "ETA", labels)
@Composable
fun MapChip(text: String, modifier: Modifier = Modifier, accent: Boolean = false, dotColor: Color? = null) {
    Row(
        modifier
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(if (accent) NavX.accent else NavX.chipBg)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = if (accent) Color.White else NavX.chipText, fontFamily = Syne, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

// outline / secondary button (Save, Log Out ...)
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    tint: Color = NavX.text,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NavX.border, RoundedCornerShape(14.dp))
            .background(NavX.card)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = tint, fontFamily = Syne, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
    }
}

// labelled text field with leading icon, used on the auth screens
@Composable
fun NavXTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var hidden by remember { mutableStateOf(isPassword) }
    Column(modifier) {
        Text(
            label.uppercase(),
            color = NavX.muted, fontFamily = Syne, fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp, letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NavX.card)
                .border(1.dp, NavX.border, RoundedCornerShape(14.dp))
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(leadingIcon, null, tint = NavX.muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(11.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, color = NavX.muted.copy(alpha = 0.7f), fontFamily = DMSans, fontSize = 15.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = NavX.text, fontFamily = DMSans, fontSize = 15.sp,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(NavX.accent),
                    visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (isPassword) {
                Icon(
                    if (hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    "toggle", tint = NavX.muted,
                    modifier = Modifier.size(18.dp).clickable { hidden = !hidden },
                )
            }
        }
    }
}

// circular avatar with initials, a per-friend gradient, and an optional online dot
@Composable
fun NavXAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
    gradient: Brush = avatarGradient(initials),
    online: Boolean? = null,
) {
    Box(modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size.dp).clip(CircleShape).background(gradient),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials, color = Color.White, fontFamily = Syne,
                fontWeight = FontWeight.ExtraBold, fontSize = (size * 0.36f).sp,
            )
        }
        if (online != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.28f).dp)
                    .clip(CircleShape)
                    .background(NavX.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(if (online) NavX.success else NavX.muted),
            )
        }
    }
}

// uppercase section label ("SHARING NOW", "RECENT" ...)
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.uppercase(), color = NavX.muted, fontFamily = Syne,
            fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.4.sp,
        )
        trailing?.invoke()
    }
}

// small round icon button used in headers + on the map
@Composable
fun RoundIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    size: Int = 42,
) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (accent) NavX.accent else NavX.card)
            .then(if (accent) Modifier else Modifier.border(1.dp, NavX.border, CircleShape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (accent) Color.White else NavX.text, modifier = Modifier.size((size * 0.46f).dp))
    }
}

// navx switch (settings toggles + per-friend alert)
@Composable
fun NavXToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = NavX.accent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = NavX.border,
            uncheckedBorderColor = NavX.border,
        ),
    )
}

// a rounded chip / pill with optional leading dot (map labels, status pills)
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    bg: Color = NavX.accentSoft,
    fg: Color = NavX.accentLight,
    leadingIcon: ImageVector? = null,
    dotColor: Color? = null,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(6.dp))
        }
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = fg, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = fg, fontFamily = Syne, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

// deterministic gradient per person so avatars feel distinct but stable
fun avatarGradient(seed: String): Brush {
    val palettes = listOf(
        listOf(Color(0xFF7C3AED), Color(0xFFB57BFF)),
        listOf(Color(0xFFEC4899), Color(0xFFF9A8D4)),
        listOf(Color(0xFFF59E0B), Color(0xFFFcd34d)),
        listOf(Color(0xFF10B981), Color(0xFF6EE7B7)),
        listOf(Color(0xFF3B82F6), Color(0xFF93C5FD)),
        listOf(Color(0xFFEF4444), Color(0xFFFCA5A5)),
    )
    val idx = (kotlin.math.abs(seed.hashCode())) % palettes.size
    return Brush.linearGradient(palettes[idx])
}
