package com.wwwescape.nowplayingcompanion.sync

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.wwwescape.nowplayingcompanion.data.Track

/**
 * Extracts a song from Pixel's ambient "Now Playing" notification.
 *
 * Now Playing is posted by Android System Intelligence (older builds: Pixel Ambient Services)
 * as a local-only notification, which is why it never bridges to the watch on its own. The
 * title reads "<song> by <artist>" (localized connector); some builds instead put the song in
 * the title and the artist in the text.
 */
object NowPlayingParser {

    val SOURCE_PACKAGES = setOf(
        "com.google.android.as",
        "com.google.intelligence.sense",
    )

    // Connector words used by the localized "<song> by <artist>" title.
    private val CONNECTORS = listOf(" by ", " de ", " von ", " par ", " di ", " door ", " av ", " від ", " от ")

    // Now Playing's own channel; other ASI notifications (Live Caption, etc.) use different ids.
    private val CHANNEL_HINTS = listOf("ambient", "music", "now_playing", "nowplaying", "song")

    fun isNowPlaying(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName !in SOURCE_PACKAGES) return false
        val channel = sbn.notification.channelId.orEmpty().lowercase()
        return CHANNEL_HINTS.any { it in channel } || parse(sbn) != null
    }

    fun parse(sbn: StatusBarNotification): Track? {
        if (sbn.packageName !in SOURCE_PACKAGES) return null
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isEmpty()) return null

        splitSongAndArtist(title)?.let { (song, artist) -> return track(song, artist) }

        // Song in title, artist in text: only trust this on the Now Playing channel.
        val channel = sbn.notification.channelId.orEmpty().lowercase()
        if (text.isNotEmpty() && CHANNEL_HINTS.any { it in channel }) return track(title, text)
        return null
    }

    private fun track(song: String, artist: String) = Track(
        song.take(Track.MAX_TEXT_LENGTH),
        artist.take(Track.MAX_TEXT_LENGTH),
        System.currentTimeMillis(),
    )

    /**
     * Tries connectors in priority order (English first, so "Hello by Paco de Lucía" isn't split
     * at " de "), splitting at the last occurrence so "Stand by Me by Ben E. King" works.
     */
    internal fun splitSongAndArtist(title: String): Pair<String, String>? {
        for (connector in CONNECTORS) {
            val index = title.lastIndexOf(connector)
            if (index <= 0) continue
            val song = title.substring(0, index).trim()
            val artist = title.substring(index + connector.length).trim()
            if (song.isNotEmpty() && artist.isNotEmpty()) return song to artist
        }
        return null
    }
}
