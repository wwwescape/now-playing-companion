package com.wwwescape.nowplayingcompanion.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
