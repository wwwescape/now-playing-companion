package com.wwwescape.nowplayingcompanion.play

import android.app.Activity
import android.os.Bundle
import com.wwwescape.nowplayingcompanion.data.Track
import com.wwwescape.nowplayingcompanion.sync.DataLayer

/**
 * Invisible hop that lets the watch start a song on the phone. The watch can only open links on
 * the phone (RemoteActivityHelper), and a background service isn't allowed to start the music
 * app, so the watch opens this activity's link and it starts playback from the foreground.
 *
 * The link is public (any app or page could open it), so it can only ever start a song search
 * in one of the known music apps (or a web search), with bounded text.
 */
class PlayActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The manifest filter already limits this to the play link; the check keeps the two in step.
        val uri = intent?.data?.takeIf { it.scheme == DataLayer.PLAY_SCHEME && it.host == DataLayer.PLAY_HOST }
        val title = uri?.getQueryParameter(DataLayer.PLAY_PARAM_TITLE)?.trim()?.take(Track.MAX_TEXT_LENGTH)
        if (!title.isNullOrEmpty()) {
            val artist = uri.getQueryParameter(DataLayer.PLAY_PARAM_ARTIST)?.trim()?.take(Track.MAX_TEXT_LENGTH).orEmpty()
            SongPlayer.play(this, title, artist, MusicApp.fromId(uri.getQueryParameter(DataLayer.PLAY_PARAM_APP)))
        }
        finish()
    }
}
