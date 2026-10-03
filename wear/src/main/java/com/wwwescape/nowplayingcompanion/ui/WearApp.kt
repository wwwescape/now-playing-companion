package com.wwwescape.nowplayingcompanion.ui

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

private const val ROUTE_HISTORY = "history"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun WearApp() {
    MaterialTheme {
        AppScaffold {
            val navController = rememberSwipeDismissableNavController()
            SwipeDismissableNavHost(navController = navController, startDestination = ROUTE_HISTORY) {
                composable(ROUTE_HISTORY) {
                    HistoryScreen(onOpenSettings = { navController.navigate(ROUTE_SETTINGS) })
                }
                composable(ROUTE_SETTINGS) {
                    SettingsScreen()
                }
            }
        }
    }
}
