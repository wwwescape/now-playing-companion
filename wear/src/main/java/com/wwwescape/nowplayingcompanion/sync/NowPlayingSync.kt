package com.wwwescape.nowplayingcompanion.sync

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.wwwescape.nowplayingcompanion.alert.Haptics
import com.wwwescape.nowplayingcompanion.alert.SongAlerts
import com.wwwescape.nowplayingcompanion.complication.NowPlayingComplicationService
import com.wwwescape.nowplayingcompanion.data.Settings
import com.wwwescape.nowplayingcompanion.data.Track
import com.wwwescape.nowplayingcompanion.data.TrackHistory
import com.wwwescape.nowplayingcompanion.tile.NowPlayingTileService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Applies song updates from the phone and refreshes everything that shows the current song. */
object NowPlayingSync {

    private const val TAG = "NowPlayingSync"

    /**
     * Stores the song in [dataMap]. When [alert] is true and it's a newly recognized song,
     * also buzzes and posts a notification (per the user's settings).
     */
    fun apply(context: Context, dataMap: DataMap, alert: Boolean) {
        val title = dataMap.getString(DataLayer.KEY_TITLE)?.take(Track.MAX_TEXT_LENGTH)
        if (title.isNullOrBlank()) return
        val track = Track(
            title = title,
            artist = dataMap.getString(DataLayer.KEY_ARTIST).orEmpty().take(Track.MAX_TEXT_LENGTH),
            recognizedAt = dataMap.getLong(DataLayer.KEY_RECOGNIZED_AT),
        )
        val active = dataMap.getBoolean(DataLayer.KEY_ACTIVE)

        val history = TrackHistory.get(context)
        val isNew = history.add(track)
        // An "ended" update for an older song shouldn't mark the newest one as stopped.
        val activeChanged = track == history.latest && history.setActive(active)

        if (alert && isNew && active) {
            val settings = Settings.get(context)
            if (settings.alertsEnabled.value) {
                SongAlerts.show(context, track)
                Haptics.play(context, settings.haptic.value)
            }
        }
        // Re-pulls (every app launch) and repeat events usually change nothing; skip waking the
        // tile and complication renderers for those.
        if (isNew || activeChanged) refreshSurfaces(context)
    }

    /** Pulls the phone's current song, for when the watch app opens after missing updates. */
    suspend fun pull(context: Context) {
        try {
            val items = Wearable.getDataClient(context).dataItems.await()
            try {
                items.filter { it.uri.path == DataLayer.PATH_NOW_PLAYING }
                    .forEach { apply(context, DataMapItem.fromDataItem(it).dataMap, alert = false) }
            } finally {
                items.release()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't read current song from the Data Layer", e)
        }
    }

    fun refreshSurfaces(context: Context) {
        TileService.getUpdater(context).requestUpdate(NowPlayingTileService::class.java)
        ComplicationDataSourceUpdateRequester
            .create(context, ComponentName(context, NowPlayingComplicationService::class.java))
            .requestUpdateAll()
    }
}
