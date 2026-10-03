package com.wwwescape.nowplayingcompanion.data

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import com.wwwescape.nowplayingcompanion.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HapticPattern(@StringRes val label: Int) {
    SUBTLE(R.string.haptic_subtle),
    PROMINENT(R.string.haptic_prominent),
    HEARTBEAT(R.string.haptic_heartbeat),
    MUTE(R.string.haptic_mute),
}

/** Where "Open on phone" sends a song. Each is a web link the phone hands to the matching app. */
enum class MusicApp(@StringRes val label: Int, private val searchUrl: String) {
    YOUTUBE_MUSIC(R.string.music_app_youtube_music, "https://music.youtube.com/search?q="),
    SPOTIFY(R.string.music_app_spotify, "https://open.spotify.com/search/"),
    GOOGLE(R.string.music_app_google, "https://www.google.com/search?q="),
    ;

    fun searchUri(track: Track): Uri = Uri.parse(searchUrl + Uri.encode("${track.title} ${track.artist}"))
}

class Settings private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _alertsEnabled = MutableStateFlow(prefs.getBoolean(KEY_ALERTS, true))
    private val _haptic = MutableStateFlow(enumPref(KEY_HAPTIC, HapticPattern.SUBTLE))
    private val _musicApp = MutableStateFlow(enumPref(KEY_MUSIC_APP, MusicApp.YOUTUBE_MUSIC))

    val alertsEnabled: StateFlow<Boolean> = _alertsEnabled.asStateFlow()
    val haptic: StateFlow<HapticPattern> = _haptic.asStateFlow()
    val musicApp: StateFlow<MusicApp> = _musicApp.asStateFlow()

    fun setAlertsEnabled(enabled: Boolean) {
        _alertsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ALERTS, enabled).apply()
    }

    fun setHaptic(pattern: HapticPattern) {
        _haptic.value = pattern
        prefs.edit().putString(KEY_HAPTIC, pattern.name).apply()
    }

    fun setMusicApp(app: MusicApp) {
        _musicApp.value = app
        prefs.edit().putString(KEY_MUSIC_APP, app.name).apply()
    }

    private inline fun <reified T : Enum<T>> enumPref(key: String, default: T): T =
        prefs.getString(key, null)?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    companion object {
        private const val KEY_ALERTS = "alerts_enabled"
        private const val KEY_HAPTIC = "haptic"
        private const val KEY_MUSIC_APP = "music_app"

        @Volatile
        private var instance: Settings? = null

        fun get(context: Context): Settings = instance ?: synchronized(this) {
            instance ?: Settings(context.applicationContext).also { instance = it }
        }
    }
}
