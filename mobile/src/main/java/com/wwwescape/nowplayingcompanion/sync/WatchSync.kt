package com.wwwescape.nowplayingcompanion.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.wwwescape.nowplayingcompanion.data.Settings
import com.wwwescape.nowplayingcompanion.data.Track
import com.wwwescape.nowplayingcompanion.data.TrackHistory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Phone side of the Wearable Data Layer: songs and clears out, clears and settings both ways. */
object WatchSync {

    private const val TAG = "WatchSync"

    suspend fun publish(context: Context, track: Track, active: Boolean) {
        val request = PutDataMapRequest.create(DataLayer.PATH_NOW_PLAYING).apply {
            dataMap.putString(DataLayer.KEY_TITLE, track.title)
            dataMap.putString(DataLayer.KEY_ARTIST, track.artist)
            dataMap.putLong(DataLayer.KEY_RECOGNIZED_AT, track.recognizedAt)
            dataMap.putBoolean(DataLayer.KEY_ACTIVE, active)
        }.asPutDataRequest()
        // Only a new song needs to reach the wrist right away (it buzzes). "Song ended" just
        // relabels the tile, so let Play Services batch it instead of waking the watch radio.
        if (active) request.setUrgent()

        try {
            Wearable.getDataClient(context).putDataItem(request).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No watch paired, or Play Services unavailable; the DataItem syncs when one connects.
            Log.w(TAG, "Failed to publish track to watch", e)
        }
    }

    /** Clears the phone's history and tells the watch to clear its own. */
    suspend fun clearEverywhere(context: Context) {
        val upTo = TrackHistory.get(context).clear()
        val request = PutDataMapRequest.create(DataLayer.PATH_HISTORY_CLEARED).apply {
            dataMap.putLong(DataLayer.KEY_CLEARED_AT, upTo)
        }.asPutDataRequest().setUrgent()
        try {
            Wearable.getDataClient(context).putDataItem(request).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't tell the watch to clear its history", e)
        }
    }

    /** Applies a history clear or settings change that came from the watch. */
    fun applyRemote(context: Context, path: String?, dataMap: DataMap) {
        when (path) {
            DataLayer.PATH_HISTORY_CLEARED -> TrackHistory.get(context).clearUpTo(dataMap.getLong(DataLayer.KEY_CLEARED_AT))
            DataLayer.PATH_SETTINGS -> Settings.get(context).applyRemote(dataMap)
        }
    }

    /**
     * Catches up on changes the listener couldn't see, e.g. the watch's settings when this app
     * was just (re)installed. Called when the app opens.
     */
    suspend fun pull(context: Context) {
        try {
            val items = Wearable.getDataClient(context).dataItems.await()
            try {
                items.forEach { applyRemote(context, it.uri.path, DataMapItem.fromDataItem(it).dataMap) }
            } finally {
                items.release()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't read the watch's data", e)
        }
    }
}
