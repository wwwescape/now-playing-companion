package com.wwwescape.nowplayingcompanion.sync

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.wwwescape.nowplayingcompanion.data.Track

/**
 * Extracts a song from Pixel's ambient "Now Playing" notification.
 *
 * Now Playing is posted by Android System Intelligence (older builds: Pixel Ambient Services) on
 * its "Recognised music notifications" channel, as a local-only notification, which is why it
 * never bridges to the watch on its own. The title reads "<song> by <artist>" (localized
 * connector); some builds instead put the song in the title and the artist in the text.
 *
 * Checked on a Pixel 8 (Nov 2026): with the standalone Now Playing app
 * (com.google.android.apps.pixel.nowplaying), ASI still posts this notification, but only while
 * that app's Settings › Notifications is on; otherwise it hands results to the app silently. The
 * standalone app itself only posts history-sync notices, so it is deliberately not a source here.
 * Seen with the toggle on: title "Bajo La Luz by M4GN and M4XXX", text "Tap to see your song
 * history", re-posted while the song plays.
 */
object NowPlayingParser {

    val SOURCE_PACKAGES = setOf(
        "com.google.android.as",
        "com.google.intelligence.sense",
    )

    private val NBSP = Char(0x00A0)

    // Connector words used by the localized "<song> by <artist>" title.
    private val CONNECTORS = listOf(" by ", " de ", " von ", " par ", " di ", " door ", " av ", " від ", " от ")

    // Now Playing's own channel (com.google.intelligence.sense.ambientmusic.MusicNotificationChannel);
    // other ASI notifications (Live Caption, downloads, etc.) use different ids.
    private val CHANNEL_HINTS = listOf("ambient", "music", "now_playing", "nowplaying", "song")

    // ASI's "Now Playing" channel (ambientmusic.UpgradeNotificationChannel) prompts to install the
    // standalone app; it matches the hints above but never carries a song.
    private val NON_SONG_CHANNELS = listOf("upgrade", "download", "caption")

    fun isNowPlaying(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName !in SOURCE_PACKAGES) return false
        val channel = sbn.notification.channelId.orEmpty().lowercase()
        if (NON_SONG_CHANNELS.any { it in channel }) return false
        return CHANNEL_HINTS.any { it in channel } || parse(sbn) != null
    }

    fun parse(sbn: StatusBarNotification): Track? {
        if (sbn.packageName !in SOURCE_PACKAGES) return null
        val notification = sbn.notification
        val extras = notification.extras
        return parse(
            packageName = sbn.packageName,
            channelId = notification.channelId,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            // Group summaries repeat a child's content.
            ignorable = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
        )
    }

    internal fun parse(
        packageName: String,
        channelId: String?,
        title: String?,
        text: String?,
        ignorable: Boolean,
    ): Track? {
        if (packageName !in SOURCE_PACKAGES || ignorable) return null
        val channel = channelId.orEmpty().lowercase()
        // Checked before the title split: a localized prompt like "... app de Now Playing" would
        // otherwise split at " de " into a fake song.
        if (NON_SONG_CHANNELS.any { it in channel }) return null
        // Some builds use non-breaking spaces around the connector.
        val cleanTitle = title.orEmpty().replace(NBSP, ' ').trim()
        val cleanText = text.orEmpty().replace(NBSP, ' ').trim()
        if (cleanTitle.isEmpty()) return null

        splitSongAndArtist(cleanTitle)?.let { (song, artist) -> return track(song, artist) }

        // Song in title, artist in text: only trust this on the Now Playing channel, since ASI
        // also posts unrelated notifications.
        if (cleanText.isNotEmpty() && CHANNEL_HINTS.any { it in channel }) return track(cleanTitle, cleanText)
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
