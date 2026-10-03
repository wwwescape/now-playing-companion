package com.wwwescape.nowplayingcompanion.sync

/** Wearable Data Layer contract shared by the phone and watch apps. Keep both copies in sync. */
object DataLayer {
    /** DataItem holding the most recently recognized song. */
    const val PATH_NOW_PLAYING = "/now_playing"

    const val KEY_TITLE = "title"
    const val KEY_ARTIST = "artist"
    const val KEY_RECOGNIZED_AT = "recognized_at"

    /** True while the phone's Now Playing notification is still showing (song still audible). */
    const val KEY_ACTIVE = "active"
}
