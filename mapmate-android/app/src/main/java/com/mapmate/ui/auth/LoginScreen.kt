package com.mapmate.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapmate.ui.components.NavXTextField
import com.mapmate.ui.components.OutlineButton
import com.mapmate.ui.components.PrimaryButton
import com.mapmate.ui.map.StylizedMapCanvas
import com.mapmate.ui.theme.DMSans
import com.mapmate.ui.theme.NavX
import com.mapmate.ui.theme.Syne

@Composable
fun LoginScreen(onSignIn: () -> Unit, onSignUp: () -> Unit = {}) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavX.bg)
            .statusBarsPadding()
    ) {
        // hero — the stylized map with route, fading into the form (matches the design)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            StylizedMapCanvas(
                friends = emptyList(),
                modifier = Modifier.fillMaxSize(),
                showRoute = true,
                dimmed = true,
            )
            // scrim fades the map down into the body background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to androidx.compose.ui.graphics.Color.Transparent,
                            0.5f to androidx.compose.ui.graphics.Color.Transparent,
                            1f to NavX.bg,
                        )
                    )
            )

            // logo bottom-left
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 20.dp)
            ) {
                Text(
                    text = "Map",
                    fontFamily = Syne,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = NavX.text
                )
                Text(
                    text = "Mate",
                    fontFamily = Syne,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = NavX.accentLight
                )
                Spacer(modifier = Modifier.width(8.dp))
                // small accent dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = NavX.accent,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }

        // scrollable body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Welcome back",
                fontFamily = Syne,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 27.sp,
                color = NavX.text
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Sign in to keep sharing with your circle",
                fontFamily = DMSans,
                fontSize = 14.sp,
                color = NavX.muted
            )

            Spacer(modifier = Modifier.height(28.dp))

            NavXTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "you@example.com",
                leadingIcon = Icons.Rounded.Email,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(14.dp))

            NavXTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "••••••••",
                leadingIcon = Icons.Rounded.Lock,
                isPassword = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // forgot password — right aligned
            Text(
                text = "Forgot password?",
                fontFamily = DMSans,
                fontSize = 13.sp,
                color = NavX.accentLight,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* no-op */ }
            )

            Spacer(modifier = Modifier.height(20.dp))

            PrimaryButton(
                text = "Sign In",
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // divider row with "or continue with"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp,
                    color = NavX.border
                )
                Text(
                    text = "or continue with",
                    fontFamily = DMSans,
                    fontSize = 12.sp,
                    color = NavX.muted,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp,
                    color = NavX.border
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // google & apple side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlineButton(
                    text = "Google",
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                OutlineButton(
                    text = "Apple",
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // create account row
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "New here? ",
                    fontFamily = DMSans,
                    fontSize = 13.sp,
                    color = NavX.muted
                )
                Text(
                    text = "Create account",
                    fontFamily = DMSans,
                    fontSize = 13.sp,
                    color = NavX.accentLight,
                    modifier = Modifier.clickable { onSignUp() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
