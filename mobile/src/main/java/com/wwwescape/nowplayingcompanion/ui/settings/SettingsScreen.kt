package com.wwwescape.nowplayingcompanion.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wwwescape.nowplayingcompanion.R
import com.wwwescape.nowplayingcompanion.data.AlertLevel
import com.wwwescape.nowplayingcompanion.data.HapticPattern
import com.wwwescape.nowplayingcompanion.data.Settings
import com.wwwescape.nowplayingcompanion.play.MusicApp
import com.wwwescape.nowplayingcompanion.util.openUrl

private val SettingsRowHeight = 72.dp

private const val PRIVACY_POLICY_URL = "https://www.ericppereira.co.in/apps/now-playing-companion/privacy-policy.html"

private enum class Picker { MUSIC_APP, ALERT_LEVEL, HAPTIC }

/**
 * Phone settings, laid out like the sibling apps (Photo Slideshow, Stream Anything): section
 * labels over rounded group cards, icon rows that open picker dialogs, and an About card.
 * Playback and Watch settings sync with the watch app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigateToLicenses: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings.get(context) }
    val musicApp by settings.musicApp.collectAsStateWithLifecycle()
    val alertsEnabled by settings.alertsEnabled.collectAsStateWithLifecycle()
    val alertLevel by settings.alertLevel.collectAsStateWithLifecycle()
    val haptic by settings.haptic.collectAsStateWithLifecycle()
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "—"
    }
    var picker by rememberSaveable { mutableStateOf<Picker?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel(stringResource(R.string.section_playback))
            SettingsGroupCard {
                NavigationRow(
                    icon = R.drawable.ic_music_note,
                    title = stringResource(R.string.setting_open_with),
                    subtitle = stringResource(musicApp.label),
                    onClick = { picker = Picker.MUSIC_APP },
                )
            }

            SectionLabel(stringResource(R.string.section_watch))
            SettingsGroupCard {
                ToggleRow(
                    icon = R.drawable.ic_notifications,
                    title = stringResource(R.string.setting_alerts),
                    subtitle = stringResource(R.string.setting_alerts_summary),
                    checked = alertsEnabled,
                    onCheckedChange = settings::setAlertsEnabled,
                )
                // Hidden rather than shown-but-inert when they don't apply, as in the siblings.
                if (alertsEnabled) {
                    RowDivider()
                    NavigationRow(
                        icon = R.drawable.ic_tune,
                        title = stringResource(R.string.setting_alert_level),
                        subtitle = stringResource(alertLevel.label),
                        onClick = { picker = Picker.ALERT_LEVEL },
                    )
                    if (alertLevel != AlertLevel.QUIET) {
                        RowDivider()
                        NavigationRow(
                            icon = R.drawable.ic_vibration,
                            title = stringResource(R.string.setting_vibration),
                            subtitle = stringResource(haptic.label),
                            onClick = { picker = Picker.HAPTIC },
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.section_about))
            SettingsGroupCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppIcon()
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = stringResource(R.string.about_version, versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RowDivider()
                Text(
                    text = stringResource(R.string.privacy_statement_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                RowDivider()
                NavigationRow(
                    icon = R.drawable.ic_shield,
                    title = stringResource(R.string.section_privacy_policy),
                    subtitle = null,
                    trailingIcon = R.drawable.ic_open_in_new,
                    onClick = { openUrl(context, PRIVACY_POLICY_URL) },
                )
                RowDivider()
                NavigationRow(
                    icon = R.drawable.ic_code,
                    title = stringResource(R.string.section_open_source_licenses),
                    subtitle = stringResource(R.string.settings_row_licenses_subtitle),
                    onClick = onNavigateToLicenses,
                )
            }
        }
    }

    when (picker) {
        Picker.MUSIC_APP -> SettingsPickerDialog(
            title = stringResource(R.string.setting_open_with),
            options = MusicApp.entries,
            selected = musicApp,
            label = { stringResource(it.label) },
            onSelect = settings::setMusicApp,
            onDismiss = { picker = null },
        )
        Picker.ALERT_LEVEL -> SettingsPickerDialog(
            title = stringResource(R.string.setting_alert_level),
            options = AlertLevel.entries,
            selected = alertLevel,
            label = { stringResource(it.label) },
            description = { stringResource(it.summary) },
            onSelect = settings::setAlertLevel,
            onDismiss = { picker = null },
        )
        Picker.HAPTIC -> SettingsPickerDialog(
            title = stringResource(R.string.setting_vibration),
            options = HapticPattern.entries,
            selected = haptic,
            label = { stringResource(it.label) },
            onSelect = settings::setHaptic,
            onDismiss = { picker = null },
        )
        null -> Unit
    }
}

/** The adaptive launcher icon's two layers, cropped to the visible 72 of their 108 dp. */
@Composable
private fun AppIcon() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.requiredSize(108.dp))
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.requiredSize(108.dp))
    }
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun RowIcon(@DrawableRes icon: Int) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ToggleRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SettingsRowHeight)
            // The whole row toggles, not just the switch.
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            RowIcon(icon)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun NavigationRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    @DrawableRes trailingIcon: Int = R.drawable.ic_chevron_right,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(SettingsRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            painter = painterResource(trailingIcon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
