package com.wwwescape.nowplayingcompanion.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/** Woken by Play Services only for history clears and settings changes (from the watch, or our own echo). */
class WatchListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.filter { it.type == DataEvent.TYPE_CHANGED }.forEach { event ->
            WatchSync.applyRemote(this, event.dataItem.uri.path, DataMapItem.fromDataItem(event.dataItem).dataMap)
        }
    }
}
