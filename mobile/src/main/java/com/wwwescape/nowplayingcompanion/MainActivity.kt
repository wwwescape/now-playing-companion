package com.wwwescape.nowplayingcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.wwwescape.nowplayingcompanion.sync.WatchSync
import com.wwwescape.nowplayingcompanion.ui.CompanionTheme
import com.wwwescape.nowplayingcompanion.ui.HomeScreen
import com.wwwescape.nowplayingcompanion.ui.settings.LicensesScreen
import com.wwwescape.nowplayingcompanion.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

private enum class Screen { HOME, SETTINGS, LICENSES }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Pick up settings or clears made on the watch while this app wasn't listening.
        lifecycleScope.launch { WatchSync.pull(this@MainActivity) }
        setContent {
            CompanionTheme {
                // Three screens in a straight line don't need a navigation library.
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                when (screen) {
                    Screen.HOME -> HomeScreen(onOpenSettings = { screen = Screen.SETTINGS })
                    Screen.SETTINGS -> {
                        BackHandler { screen = Screen.HOME }
                        SettingsScreen(
                            onBack = { screen = Screen.HOME },
                            onNavigateToLicenses = { screen = Screen.LICENSES },
                        )
                    }
                    Screen.LICENSES -> {
                        BackHandler { screen = Screen.SETTINGS }
                        LicensesScreen(onBack = { screen = Screen.SETTINGS })
                    }
                }
            }
        }
    }
}
