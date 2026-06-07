package com.mapmate.ui.social

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapmate.demo.DemoData
import com.mapmate.ui.components.NavXAvatar
import com.mapmate.ui.components.Pill
import com.mapmate.ui.components.RoundIconButton
import com.mapmate.ui.components.SectionHeader
import com.mapmate.ui.theme.DMSans
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne

@Composable
fun AddFriendScreen(onBack: () -> Unit, onAccept: (name: String, initials: String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    // local mutable lists so accept/dismiss removes the item instantly
    val requests = remember {
        mutableStateListOf(
            Triple("Liam Foster", "LF", 3),
            Triple("Zoe Adams", "ZA", 1),
        )
    }
    val suggestions = remember { DemoData.suggestions.toMutableStateList() }

    Column(
        Modifier
            .fillMaxSize()
            .background(NavX.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        // header: back arrow + title
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = onBack,
            )
            Text(
                "Add Friends",
                fontFamily = Syne,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 25.sp,
                color = NavX.text,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        Spacer(Modifier.height(14.dp))

        // search field
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NavX.card)
                .border(1.dp, NavX.border, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = NavX.muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "Search name or @username",
                        color = NavX.muted.copy(alpha = 0.7f),
                        fontFamily = DMSans,
                        fontSize = 14.5.sp,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = NavX.text, fontFamily = DMSans, fontSize = 14.5.sp),
                    cursorBrush = SolidColor(NavX.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // two action cards: qr code & share invite
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionCard(
                label = "QR Code",
                icon = Icons.Rounded.QrCode2,
                modifier = Modifier.weight(1f),
                onClick = {
                    Toast.makeText(context, "QR invite — coming soon", Toast.LENGTH_SHORT).show()
                },
            )
            ActionCard(
                label = "Share Invite",
                icon = Icons.Rounded.Share,
                modifier = Modifier.weight(1f),
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Join me on MapMate: https://mapmate.app/invite/you")
                    }
                    context.startActivity(Intent.createChooser(send, "Invite a friend"))
                },
            )
        }

        Spacer(Modifier.height(4.dp))

        // requests section — pill shows count, hidden when empty
        SectionHeader(
            text = "Requests",
            trailing = if (requests.isNotEmpty()) {
                {
                    Pill(
                        text = "${requests.size}",
                        bg = NavX.accent,
                        fg = Color.White,
                    )
                }
            } else null,
        )

        if (requests.isEmpty()) {
            Text(
                "No pending requests",
                fontFamily = DMSans,
                fontSize = 13.sp,
                color = NavX.muted,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // iterate over a snapshot to avoid mutation during iteration
                requests.toList().forEach { item ->
                    val (name, initials, mutual) = item
                    RequestRow(
                        name = name,
                        initials = initials,
                        mutual = mutual,
                        onAccept = {
                            onAccept(name, initials)
                            requests.remove(item)
                            Toast.makeText(context, "Added $name", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = {
                            requests.remove(item)
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // suggestions section
        SectionHeader(text = "Suggested")

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            suggestions.toList().forEach { s ->
                SuggestionRow(
                    initials = s.initials,
                    name = s.name,
                    mutual = s.mutual,
                    onAdd = {
                        onAccept(s.name, s.initials)
                        suggestions.remove(s)
                        Toast.makeText(context, "Added ${s.name}", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// action card -- icon above label, tappable
@Composable
private fun ActionCard(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NavX.card)
            .border(1.dp, NavX.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, null, tint = NavX.accentLight, modifier = Modifier.size(21.dp))
        Text(label, fontFamily = Syne, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = NavX.text)
    }
}

// one incoming friend request row with functional accept & dismiss
@Composable
private fun RequestRow(
    name: String,
    initials: String,
    mutual: Int,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NavX.card)
            .border(1.dp, NavX.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavXAvatar(initials = initials)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontFamily = Syne, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = NavX.text)
            Text("$mutual mutual friends", fontFamily = DMSans, fontSize = 12.5.sp, color = NavX.muted)
        }
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundIconButton(icon = Icons.Rounded.Check, onClick = onAccept, accent = true, size = 38)
            RoundIconButton(icon = Icons.Rounded.Close, onClick = onDismiss, size = 38)
        }
    }
}

// one suggestion row with functional add pill
@Composable
private fun SuggestionRow(
    initials: String,
    name: String,
    mutual: Int,
    onAdd: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavXAvatar(initials = initials)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontFamily = Syne, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = NavX.text)
            Text("$mutual mutual", fontFamily = DMSans, fontSize = 12.5.sp, color = NavX.muted)
        }
        Pill(
            text = "Add",
            bg = NavX.accentSoft,
            fg = NavX.accentLight,
            modifier = Modifier.clickable(onClick = onAdd),
        )
    }
}
