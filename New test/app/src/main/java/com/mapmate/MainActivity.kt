package com.mapmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapmate.data.remote.MapMateRepository
import com.mapmate.ui.home.HomeMapViewModel
import com.mapmate.ui.home.MapMateHomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MaterialTheme {
                val repository = remember { MapMateRepository(applicationContext) }
                val homeViewModel: HomeMapViewModel = viewModel(
                    factory = HomeMapViewModel.Factory(repository),
                )

                LaunchedEffect(Unit) {
                    homeViewModel.loadHomeMapFeed()
                }

                MapMateHomeScreen()
            }
        }
    }
}
