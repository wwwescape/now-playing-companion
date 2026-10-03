package com.wwwescape.nowplayingcompanion.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
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
import com.wwwescape.nowplayingcompanion.sync.PhoneLauncher

@Composable
fun HistoryScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val history = remember { TrackHistory.get(context) }
    val tracks by history.tracks.collectAsStateWithLifecycle()
    val active by history.active.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

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
            val earlier = tracks.drop(1)
            if (earlier.isNotEmpty()) {
                item { ListHeader { Text(stringResource(R.string.earlier)) } }
                items(earlier, key = { it.recognizedAt }) { track ->
                    HistoryRow(track, onRemove = { history.remove(track) })
                }
            }
            item {
                FilledTonalButton(
                    onClick = onOpenSettings,
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings)) },
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(track: Track, onRemove: () -> Unit) {
    val context = LocalContext.current
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
            icon = { Icon(painterResource(R.drawable.ic_music_note), contentDescription = null) },
            label = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            secondaryLabel = {
                Text(
                    "${track.artist} · ${relativeTime(track.recognizedAt)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
    }
}

private fun relativeTime(time: Long): String =
    DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
