package com.wwwescape.nowplayingcompanion.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.wwwescape.nowplayingcompanion.R
import com.wwwescape.nowplayingcompanion.alert.Haptics
import com.wwwescape.nowplayingcompanion.data.HapticPattern
import com.wwwescape.nowplayingcompanion.data.MusicApp
import com.wwwescape.nowplayingcompanion.data.Settings

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = remember { Settings.get(context) }
    val alertsEnabled by settings.alertsEnabled.collectAsStateWithLifecycle()
    val haptic by settings.haptic.collectAsStateWithLifecycle()
    val musicApp by settings.musicApp.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(stringResource(R.string.settings)) } }
            item {
                SwitchButton(
                    checked = alertsEnabled,
                    onCheckedChange = settings::setAlertsEnabled,
                    label = { Text(stringResource(R.string.setting_alerts)) },
                    secondaryLabel = { Text(stringResource(R.string.setting_alerts_summary)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { ListHeader { Text(stringResource(R.string.setting_vibration)) } }
            items(HapticPattern.entries) { pattern ->
                RadioButton(
                    selected = haptic == pattern,
                    onSelect = {
                        settings.setHaptic(pattern)
                        Haptics.play(context, pattern) // Preview the pattern on selection.
                    },
                    enabled = alertsEnabled,
                    label = { Text(stringResource(pattern.label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { ListHeader { Text(stringResource(R.string.setting_open_with)) } }
            items(MusicApp.entries) { app ->
                RadioButton(
                    selected = musicApp == app,
                    onSelect = { settings.setMusicApp(app) },
                    label = { Text(stringResource(app.label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
