package com.mapmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.mapmate.ui.MapMateApp
import com.mapmate.ui.theme.MapMateTheme

// single entry point -> navx theme wraps the whole merged app
// DEMO_MODE (BuildConfig) is on, so MapMateViewModel runs everyone's logic on seeded data
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MapMateTheme {
                MapMateApp()
            }
        }
    }
}
