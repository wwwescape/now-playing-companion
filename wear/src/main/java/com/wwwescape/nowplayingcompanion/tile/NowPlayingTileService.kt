package com.wwwescape.nowplayingcompanion.tile

import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.wwwescape.nowplayingcompanion.MainActivity
import com.wwwescape.nowplayingcompanion.R
import com.wwwescape.nowplayingcompanion.data.Track
import com.wwwescape.nowplayingcompanion.data.TrackHistory
import kotlinx.coroutines.Dispatchers

/** Tile showing the latest recognized song, with a button to open it on the phone. */
class NowPlayingTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.Main) {
            val history = TrackHistory.get(this@NowPlayingTileService)
            val layout = materialScope(this@NowPlayingTileService, requestParams.deviceConfiguration) {
                tileLayout(history.latest, history.active.value)
            }
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                // Content only changes when a song arrives, which pushes an explicit update.
                .setFreshnessIntervalMillis(0)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
                .build()
        }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.Main) {
            ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
        }

    private fun MaterialScope.tileLayout(track: Track?, active: Boolean): LayoutElementBuilders.LayoutElement {
        val heading = getString(if (track != null && active) R.string.tile_now_playing else R.string.tile_last_heard)
        return primaryLayout(
            titleSlot = { text(heading.layoutString) },
            mainSlot = {
                if (track == null) {
                    text(getString(R.string.tile_empty).layoutString, maxLines = 3)
                } else {
                    LayoutElementBuilders.Column.Builder()
                        .addContent(text(track.title.layoutString, typography = Typography.TITLE_MEDIUM, maxLines = 2))
                        .addContent(
                            text(
                                track.artist.layoutString,
                                typography = Typography.BODY_MEDIUM,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        )
                        .build()
                }
            },
            bottomSlot = {
                textEdgeButton(
                    onClick = openAppClickable(openOnPhone = track != null),
                    labelContent = {
                        text(getString(if (track != null) R.string.tile_open else R.string.tile_open_app).layoutString)
                    },
                )
            },
        )
    }

    private fun openAppClickable(openOnPhone: Boolean): ModifiersBuilders.Clickable {
        val activity = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(packageName)
            .setClassName(MainActivity::class.java.name)
        if (openOnPhone) {
            activity.addKeyToExtraMapping(
                MainActivity.EXTRA_OPEN_LATEST,
                ActionBuilders.AndroidBooleanExtra.Builder().setValue(true).build(),
            )
        }
        return ModifiersBuilders.Clickable.Builder()
            .setId(if (openOnPhone) "open_on_phone" else "open_app")
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity.build()).build())
            .build()
    }

    companion object {
        private const val RESOURCES_VERSION = "1"
    }
}
