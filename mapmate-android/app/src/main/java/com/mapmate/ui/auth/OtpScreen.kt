package com.mapmate.ui.auth

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapmate.ui.components.PrimaryButton
import com.mapmate.ui.theme.DMSans
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne

@Composable
fun OtpScreen(email: String, onVerify: () -> Unit, onBack: () -> Unit) {
    // tracks what the user has typed so far (digits only, max 6)
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavX.bg)
            .statusBarsPadding()
            .padding(24.dp),
    ) {
        // back row
        Row(
            modifier = Modifier.clickable(onClick = onBack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "back",
                tint = NavX.muted,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Back",
                color = NavX.muted,
                fontFamily = DMSans,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        Spacer(Modifier.height(32.dp))

        // lock icon badge
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(NavX.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = NavX.accentLight,
                modifier = Modifier.size(26.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        // title
        Text(
            text = "Verify it's you",
            fontFamily = Syne,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 27.sp,
            color = NavX.text,
        )

        Spacer(Modifier.height(8.dp))

        // subtitle + email
        Text(
            text = "Enter the 6-digit code we sent to",
            fontFamily = DMSans,
            fontSize = 14.sp,
            color = NavX.muted,
        )
        Text(
            text = email,
            fontFamily = Syne,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = NavX.accentLight,
        )

        Spacer(Modifier.height(32.dp))

        // otp digit boxes — single invisible BasicTextField overlaid on a visual row
        Box(modifier = Modifier.fillMaxWidth()) {
            // the 6 visual boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(6) { index ->
                    val filled = index < code.length
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(if (filled) NavX.accentSoft else NavX.card)
                            .border(
                                width = 1.5.dp,
                                color = if (filled) NavX.accent else NavX.border,
                                shape = RoundedCornerShape(15.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (filled) {
                            Text(
                                text = code[index].toString(),
                                fontFamily = Syne,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = NavX.text,
                            )
                        }
                    }
                }
            }

            // invisible field stretched over the whole row — tap anywhere to focus, type to fill
            BasicTextField(
                value = code,
                onValueChange = { input ->
                    // only keep digits, cap at 6
                    code = input.filter { it.isDigit() }.take(6)
                },
                modifier = Modifier
                    .matchParentSize(),
                textStyle = TextStyle(color = Color.Transparent),
                cursorBrush = SolidColor(Color.Transparent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
            )
        }

        Spacer(Modifier.height(20.dp))

        // progress dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            repeat(6) { index ->
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (index < code.length) NavX.accent else NavX.border),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // verify button, only active once all 6 digits are in
        PrimaryButton(
            text = "Verify Code",
            onClick = onVerify,
            enabled = code.length == 6,
        )

        Spacer(Modifier.height(20.dp))

        // static resend countdown
        Text(
            text = "Resend code in 0:42",
            color = NavX.muted,
            fontFamily = DMSans,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
