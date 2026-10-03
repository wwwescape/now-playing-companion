package com.wwwescape.nowplayingcompanion.sync

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.wwwescape.nowplayingcompanion.data.Track
import com.wwwescape.nowplayingcompanion.data.TrackHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Watches for Pixel's Now Playing notification and forwards each new song to the watch.
 * Requires the user to grant notification access in system settings.
 */
class NowPlayingListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Song whose Now Playing notification is currently showing, if any. */
    private var showing: Track? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val track = NowPlayingParser.parse(sbn) ?: return
        // Now Playing re-posts the same notification as it refreshes; only forward new songs.
        if (track.sameSongAs(showing)) return
        showing = track
        if (!TrackHistory.get(this).add(track)) return
        scope.launch { WatchSync.publish(this@NowPlayingListenerService, track, active = true) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (!NowPlayingParser.isNowPlaying(sbn)) return
        showing = null
        val latest = TrackHistory.get(this).latest ?: return
        scope.launch { WatchSync.publish(this@NowPlayingListenerService, latest, active = false) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
