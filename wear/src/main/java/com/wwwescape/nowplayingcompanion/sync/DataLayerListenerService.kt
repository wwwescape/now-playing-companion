package com.wwwescape.nowplayingcompanion.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/** Woken by Play Services whenever the phone publishes a new song. */
class DataLayerListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == DataLayer.PATH_NOW_PLAYING }
            .forEach { NowPlayingSync.apply(this, DataMapItem.fromDataItem(it.dataItem).dataMap, alert = true) }
    }
}
