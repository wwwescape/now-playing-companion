package com.wwwescape.nowplayingcompanion.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

private val NBSP = Char(0x00A0)

class NowPlayingParserTest {

    @Test
    fun splitsSongAndArtist() {
        assertEquals("Bohemian Rhapsody" to "Queen", NowPlayingParser.splitSongAndArtist("Bohemian Rhapsody by Queen"))
    }

    @Test
    fun splitsAtLastConnectorWhenSongContainsBy() {
        assertEquals("Stand by Me" to "Ben E. King", NowPlayingParser.splitSongAndArtist("Stand by Me by Ben E. King"))
    }

    @Test
    fun prefersEnglishConnectorOverLocalizedOnes() {
        assertEquals("Hello" to "Paco de Lucía", NowPlayingParser.splitSongAndArtist("Hello by Paco de Lucía"))
    }

    @Test
    fun handlesLocalizedConnector() {
        assertEquals("Despacito" to "Luis Fonsi", NowPlayingParser.splitSongAndArtist("Despacito de Luis Fonsi"))
    }

    @Test
    fun rejectsTitleWithoutArtist() {
        assertNull(NowPlayingParser.splitSongAndArtist("Now Playing"))
        assertNull(NowPlayingParser.splitSongAndArtist("Song by "))
    }

    private val asi = "com.google.android.as"
    private val musicChannel = "com.google.intelligence.sense.ambientmusic.MusicNotificationChannel"
    private val upgradeChannel = "com.google.intelligence.sense.ambientmusic.UpgradeNotificationChannel"

    @Test
    fun parsesSongOnMusicChannel() {
        val track = NowPlayingParser.parse(asi, musicChannel, "Yellow by Coldplay", null, ignorable = false)
        assertEquals("Yellow" to "Coldplay", track!!.title to track.artist)
    }

    @Test
    fun parsesArtistInTextOnMusicChannel() {
        val track = NowPlayingParser.parse(asi, musicChannel, "Yellow", "Coldplay", ignorable = false)
        assertEquals("Yellow" to "Coldplay", track!!.title to track.artist)
    }

    @Test
    fun handlesNonBreakingSpaces() {
        val track = NowPlayingParser.parse(asi, musicChannel, "Yellow${NBSP}by${NBSP}Coldplay", null, ignorable = false)
        assertEquals("Yellow" to "Coldplay", track!!.title to track.artist)
    }

    @Test
    fun ignoresUpgradePromptEvenWhenItLooksLikeASong() {
        assertNull(NowPlayingParser.parse(asi, upgradeChannel, "Download the new Now Playing app", "Tap to install", ignorable = false))
        assertNull(NowPlayingParser.parse(asi, upgradeChannel, "Descarga la nueva app de Now Playing", null, ignorable = false))
    }

    @Test
    fun ignoresStandaloneAppAndUnknownSources() {
        assertNull(NowPlayingParser.parse("com.google.android.apps.pixel.nowplaying", "x", "Yellow by Coldplay", null, ignorable = false))
        assertNull(NowPlayingParser.parse("com.example.spam", "music", "Yellow by Coldplay", null, ignorable = false))
    }

    @Test
    fun ignoresGroupSummaries() {
        assertNull(NowPlayingParser.parse(asi, musicChannel, "Yellow by Coldplay", null, ignorable = true))
    }

    @Test
    fun keepsArtistInTextOnNowPlayingChannelOnly() {
        assertNull(NowPlayingParser.parse(asi, "live_caption", "Captions", "On", ignorable = false))
        assertNotNull(NowPlayingParser.parse(asi, "ambient_music", "Yellow", "Coldplay", ignorable = false))
    }
}
