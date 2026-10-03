package com.wwwescape.nowplayingcompanion.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.wearable.Wearable
import com.wwwescape.nowplayingcompanion.R
import com.wwwescape.nowplayingcompanion.data.Track
import com.wwwescape.nowplayingcompanion.data.TrackHistory
import com.wwwescape.nowplayingcompanion.sync.WatchSync
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val history = remember { TrackHistory.get(context) }
    val tracks by history.tracks.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var hasAccess by remember { mutableStateOf(false) }
    var watchName by remember { mutableStateOf<String?>(null) }

    // Re-check on resume: the user grants access in system settings and comes back.
    LifecycleResumeEffect(Unit) {
        hasAccess = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
        scope.launch { watchName = connectedWatchName(context) }
        onPauseOrDispose { }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                StatusCard(
                    ok = hasAccess,
                    title = stringResource(
                        if (hasAccess) R.string.status_access_granted else R.string.status_access_needed
                    ),
                    body = stringResource(R.string.status_access_body),
                    action = if (hasAccess) null else stringResource(R.string.action_grant_access),
                    onAction = { context.startSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                )
            }
            item {
                StatusCard(
                    ok = watchName != null,
                    title = watchName?.let { stringResource(R.string.status_watch_connected, it) }
                        ?: stringResource(R.string.status_watch_missing),
                    body = stringResource(R.string.status_watch_body),
                    action = stringResource(R.string.action_send_test),
                    onAction = {
                        val track = Track("Here Comes the Sun", "The Beatles", System.currentTimeMillis())
                        history.add(track)
                        scope.launch { WatchSync.publish(context, track, active = true) }
                    },
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.history_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (tracks.isNotEmpty()) {
                        TextButton(onClick = history::clear) { Text(stringResource(R.string.action_clear)) }
                    }
                }
            }
            if (tracks.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(tracks, key = { it.recognizedAt }) { track ->
                HistoryRow(
                    track = track,
                    onOpen = { openInMusicApp(context, track) },
                    onDismiss = { history.remove(track) },
                )
            }
        }
    }
}

@Composable
private fun StatusCard(ok: Boolean, title: String, body: String, action: String?, onAction: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(if (ok) R.drawable.ic_check_circle else R.drawable.ic_warning),
                    contentDescription = null,
                    tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (action != null) {
                if (ok) {
                    OutlinedButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(action) }
                } else {
                    Button(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(action) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryRow(track: Track, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        onDismiss = { onDismiss() },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.action_remove))
            }
        },
    ) {
        ListItem(
            modifier = Modifier.clickable(onClick = onOpen),
            leadingContent = { Icon(painterResource(R.drawable.ic_music_note), contentDescription = null) },
            headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingContent = {
                Text(
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(track.recognizedAt)),
                    style = MaterialTheme.typography.labelSmall,
                )
            },
        )
    }
}

private suspend fun connectedWatchName(context: Context): String? = try {
    Wearable.getNodeClient(context).connectedNodes.await().firstOrNull()?.displayName
} catch (e: Exception) {
    null
}

private fun openInMusicApp(context: Context, track: Track) {
    val query = Uri.encode("${track.title} ${track.artist}")
    context.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=$query")))
}

/** Some devices (work profiles, stripped ROMs) have no handler for these intents. */
private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Nothing to open it with; leave the user on this screen.
    }
}
