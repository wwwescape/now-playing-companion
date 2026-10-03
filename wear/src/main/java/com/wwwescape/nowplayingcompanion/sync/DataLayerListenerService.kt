package com.wwwescape.nowplayingcompanion.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.wwwescape.nowplayingcompanion.data.Settings

/** Woken by Play Services whenever the phone publishes a new song, a history clear or settings changed on the phone. */
class DataLayerListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.filter { it.type == DataEvent.TYPE_CHANGED }.forEach { event ->
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            when (event.dataItem.uri.path) {
                DataLayer.PATH_NOW_PLAYING -> NowPlayingSync.apply(this, dataMap, alert = true)
                DataLayer.PATH_HISTORY_CLEARED -> NowPlayingSync.applyClear(this, dataMap)
                DataLayer.PATH_SETTINGS -> Settings.get(this).applyRemote(dataMap)
            }
        }
    }
}
