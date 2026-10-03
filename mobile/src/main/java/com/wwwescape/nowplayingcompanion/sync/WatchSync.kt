package com.wwwescape.nowplayingcompanion.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.wwwescape.nowplayingcompanion.data.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Pushes recognized songs to the watch over the Wearable Data Layer. */
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
}
