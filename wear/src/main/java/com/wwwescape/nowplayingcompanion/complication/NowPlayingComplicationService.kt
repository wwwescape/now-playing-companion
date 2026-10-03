package com.wwwescape.nowplayingcompanion.complication

import android.app.PendingIntent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.wwwescape.nowplayingcompanion.MainActivity
import com.wwwescape.nowplayingcompanion.R
import com.wwwescape.nowplayingcompanion.data.Track
import com.wwwescape.nowplayingcompanion.data.TrackHistory

/** Shows the latest song on the watch face. Updated on demand by NowPlayingSync, never polled. */
class NowPlayingComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        build(type, Track("Here Comes the Sun", "The Beatles", 0L))

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val track = TrackHistory.get(this).latest
            ?: return NoDataComplicationData()
        return build(request.complicationType, track)
    }

    private fun build(type: ComplicationType, track: Track): ComplicationData? {
        val description = PlainComplicationText.Builder("${track.title}, ${track.artist}").build()
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_music_note)).build()
        val tapAction = PendingIntent.getActivity(
            this,
            0,
            MainActivity.intent(this, openTrack = null),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(track.title).build(),
                contentDescription = description,
            )
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder(track.title).build(),
                contentDescription = description,
            )
                .setTitle(PlainComplicationText.Builder(track.artist).build())
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            else -> null
        }
    }
}
