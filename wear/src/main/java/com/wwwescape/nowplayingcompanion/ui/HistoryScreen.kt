package com.wwwescape.nowplayingcompanion.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import com.wwwescape.nowplayingcompanion.R
import com.wwwescape.nowplayingcompanion.data.Track
import com.wwwescape.nowplayingcompanion.data.TrackHistory
import com.wwwescape.nowplayingcompanion.sync.NowPlayingSync
import com.wwwescape.nowplayingcompanion.sync.PhoneLauncher
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val history = remember { TrackHistory.get(context) }
    val tracks by history.tracks.collectAsStateWithLifecycle()
    val active by history.active.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Shared by every row: one painter and one clock read per composition, not one per row.
    val noteIcon = painterResource(R.drawable.ic_music_note)
    val now = remember(tracks) { System.currentTimeMillis() }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            val latest = tracks.firstOrNull()
            item {
                ListHeader {
                    Text(stringResource(if (latest != null && active) R.string.now_playing else R.string.last_heard))
                }
            }
            if (latest == null) {
                item {
                    Text(
                        stringResource(R.string.history_empty),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    TitleCard(
                        onClick = { PhoneLauncher.open(context, latest) },
                        title = { Text(latest.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        subtitle = { Text(latest.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.tap_to_open_on_phone),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            // Settings and Clear sit right under the current song, so they're one short scroll
            // away no matter how long the history grows.
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilledTonalIconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    if (latest != null) {
                        FilledTonalIconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.clear_history))
                        }
                    }
                }
            }
            if (tracks.size > 1) {
                item { ListHeader { Text(stringResource(R.string.earlier)) } }
                // Index into the list rather than copying it with drop(1) on every recomposition.
                items(
                    count = tracks.size - 1,
                    key = { tracks[it + 1].recognizedAt },
                ) { index ->
                    val track = tracks[index + 1]
                    HistoryRow(track, noteIcon, now, onRemove = { history.remove(track) })
                }
            }
        }
    }

    AlertDialog(
        visible = confirmClear,
        onDismissRequest = { confirmClear = false },
        title = { Text(stringResource(R.string.clear_history_title)) },
        text = { Text(stringResource(R.string.clear_history_body)) },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(onClick = {
                scope.launch { NowPlayingSync.clearEverywhere(context) }
                confirmClear = false
            })
        },
        dismissButton = { AlertDialogDefaults.DismissButton(onClick = { confirmClear = false }) },
    )
}

@Composable
private fun HistoryRow(track: Track, noteIcon: Painter, now: Long, onRemove: () -> Unit) {
    val context = LocalContext.current
    // Formatting dates is the costliest part of a row; only redo it when the inputs change.
    val subtitle = remember(track, now) { "${track.artist} · ${relativeTime(track.recognizedAt, now)}" }
    SwipeToReveal(
        primaryAction = {
            PrimaryActionButton(
                onClick = onRemove,
                icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                text = { Text(stringResource(R.string.remove)) },
            )
        },
        onSwipePrimaryAction = onRemove,
    ) {
        Button(
            onClick = { PhoneLauncher.open(context, track) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(),
            icon = { Icon(noteIcon, contentDescription = null) },
            label = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            secondaryLabel = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
    }
}

private fun relativeTime(time: Long, now: Long): String =
    DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS).toString()
