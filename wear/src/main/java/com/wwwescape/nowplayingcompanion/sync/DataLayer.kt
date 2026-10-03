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

    /** DataItem written by whichever device the user cleared history on; the other one follows. */
    const val PATH_HISTORY_CLEARED = "/history_cleared"

    /** Songs recognized at or before this time (ms) are cleared. */
    const val KEY_CLEARED_AT = "cleared_at"

    /**
     * Link the watch opens on the phone to start a song:
     * nowplayingcompanion://play?title=...&artist=...&app=youtube_music|spotify
     */
    const val PLAY_SCHEME = "nowplayingcompanion"
    const val PLAY_HOST = "play"
    const val PLAY_PARAM_TITLE = "title"
    const val PLAY_PARAM_ARTIST = "artist"
    const val PLAY_PARAM_APP = "app"

    /**
     * DataItem with the shared settings, written by whichever device changed them last. Values
     * are enum names; the newest [KEY_SETTINGS_UPDATED_AT] wins on both sides.
     */
    const val PATH_SETTINGS = "/settings"
    const val KEY_SETTINGS_UPDATED_AT = "updated_at"
    const val KEY_SETTING_MUSIC_APP = "music_app"
    const val KEY_SETTING_ALERTS = "alerts_enabled"
    const val KEY_SETTING_ALERT_LEVEL = "alert_level"
    const val KEY_SETTING_HAPTIC = "haptic"

    /**
     * Sent by the phone: false until it has received the watch's settings at least once. Its
     * alert values are then only defaults, so the watch takes just the music app from it.
     */
    const val KEY_SETTINGS_HAS_WATCH_VALUES = "has_watch_values"
}
