package com.wwwescape.nowplayingcompanion.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.wwwescape.nowplayingcompanion.R
import com.wwwescape.nowplayingcompanion.sync.DataLayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HapticPattern(@param:StringRes val label: Int) {
    SUBTLE(R.string.haptic_subtle),
    PROMINENT(R.string.haptic_prominent),
    HEARTBEAT(R.string.haptic_heartbeat),
    MUTE(R.string.haptic_mute),
}

/**
 * How loudly a new song is announced, mapped to the notification channel's importance.
 * ON_SCREEN lets the system vibrate (an alerting notification is what Wear OS shows on screen);
 * the other levels stay silent and the app plays the chosen [HapticPattern] itself.
 */
enum class AlertLevel(@param:StringRes val label: Int, @param:StringRes val summary: Int) {
    ON_SCREEN(R.string.level_on_screen, R.string.level_on_screen_summary),
    DOT(R.string.level_dot, R.string.level_dot_summary),
    QUIET(R.string.level_quiet, R.string.level_quiet_summary),
}

/**
 * Where "Open on phone" sends a song. Music apps get [playerId], which the phone app uses to
 * start the song playing; Google has none and just opens a web search.
 */
enum class MusicApp(@param:StringRes val label: Int, val playerId: String?, private val searchUrl: String) {
    YOUTUBE_MUSIC(R.string.music_app_youtube_music, "youtube_music", "https://music.youtube.com/search?q="),
    SPOTIFY(R.string.music_app_spotify, "spotify", "https://open.spotify.com/search/"),
    GOOGLE(R.string.music_app_google, null, "https://www.google.com/search?q="),
    ;

    fun searchUri(track: Track): Uri = (searchUrl + Uri.encode("${track.title} ${track.artist}")).toUri()
}

/**
 * Watch preferences. They're shared with the phone app, which can edit them too: every local
 * change is stamped and published to the Data Layer, and a newer copy from the phone replaces
 * these values (see [applyRemote]).
 */
class Settings private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _alertsEnabled = MutableStateFlow(prefs.getBoolean(KEY_ALERTS, true))
    private val _haptic = MutableStateFlow(enumPref(KEY_HAPTIC, HapticPattern.SUBTLE))
    private val _musicApp = MutableStateFlow(enumPref(KEY_MUSIC_APP, MusicApp.YOUTUBE_MUSIC))
    // DOT matches how alerts behaved before this setting existed.
    private val _alertLevel = MutableStateFlow(enumPref(KEY_ALERT_LEVEL, AlertLevel.DOT))

    /** When these values last changed on either device; 0 = never synced. */
    private var updatedAt = prefs.getLong(KEY_UPDATED_AT, 0L)

    val alertsEnabled: StateFlow<Boolean> = _alertsEnabled.asStateFlow()
    val haptic: StateFlow<HapticPattern> = _haptic.asStateFlow()
    val musicApp: StateFlow<MusicApp> = _musicApp.asStateFlow()
    val alertLevel: StateFlow<AlertLevel> = _alertLevel.asStateFlow()

    fun setAlertsEnabled(enabled: Boolean) = edit { _alertsEnabled.value = enabled }
    fun setHaptic(pattern: HapticPattern) = edit { _haptic.value = pattern }
    fun setMusicApp(app: MusicApp) = edit { _musicApp.value = app }
    fun setAlertLevel(level: AlertLevel) = edit { _alertLevel.value = level }

    /**
     * Shares the watch's settings once, so a newly installed phone app shows what's already set
     * here. Stamped 1 ms after the epoch: it beats a phone that never changed anything, but any
     * real edit on the phone still wins.
     */
    @Synchronized
    fun publishIfNeverSynced() {
        if (updatedAt != 0L) return
        updatedAt = 1L
        persist()
        publish()
    }

    /** Applies settings from the phone if they're newer than ours. Our own echo is ignored. */
    @Synchronized
    fun applyRemote(map: DataMap) {
        val remoteUpdatedAt = map.getLong(DataLayer.KEY_SETTINGS_UPDATED_AT)
        if (remoteUpdatedAt <= updatedAt) return
        _musicApp.value = enumValue(map.getString(DataLayer.KEY_SETTING_MUSIC_APP), _musicApp.value)
        if (!map.getBoolean(DataLayer.KEY_SETTINGS_HAS_WATCH_VALUES, true)) {
            // The phone's alert values are just its defaults: keep ours and send them over.
            updatedAt = maxOf(System.currentTimeMillis(), remoteUpdatedAt + 1)
            persist()
            publish()
            return
        }
        updatedAt = remoteUpdatedAt
        _alertsEnabled.value = map.getBoolean(DataLayer.KEY_SETTING_ALERTS, _alertsEnabled.value)
        _alertLevel.value = enumValue(map.getString(DataLayer.KEY_SETTING_ALERT_LEVEL), _alertLevel.value)
        _haptic.value = enumValue(map.getString(DataLayer.KEY_SETTING_HAPTIC), _haptic.value)
        persist()
    }

    @Synchronized
    private fun edit(change: () -> Unit) {
        change()
        // Never go backwards, even if the clock was adjusted since the last change.
        updatedAt = maxOf(System.currentTimeMillis(), updatedAt + 1)
        persist()
        publish()
    }

    private fun persist() {
        prefs.edit {
            putBoolean(KEY_ALERTS, _alertsEnabled.value)
            putString(KEY_HAPTIC, _haptic.value.name)
            putString(KEY_MUSIC_APP, _musicApp.value.name)
            putString(KEY_ALERT_LEVEL, _alertLevel.value.name)
            putLong(KEY_UPDATED_AT, updatedAt)
        }
    }

    /** Fire-and-forget: Play Services queues the item and delivers it when the phone connects. */
    private fun publish() {
        val request = PutDataMapRequest.create(DataLayer.PATH_SETTINGS).apply {
            dataMap.putLong(DataLayer.KEY_SETTINGS_UPDATED_AT, updatedAt)
            dataMap.putString(DataLayer.KEY_SETTING_MUSIC_APP, _musicApp.value.name)
            dataMap.putBoolean(DataLayer.KEY_SETTING_ALERTS, _alertsEnabled.value)
            dataMap.putString(DataLayer.KEY_SETTING_ALERT_LEVEL, _alertLevel.value.name)
            dataMap.putString(DataLayer.KEY_SETTING_HAPTIC, _haptic.value.name)
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(request)
            .addOnFailureListener { Log.w(TAG, "Couldn't share settings with the phone", it) }
    }

    private inline fun <reified T : Enum<T>> enumPref(key: String, default: T): T =
        enumValue(prefs.getString(key, null), default)

    private inline fun <reified T : Enum<T>> enumValue(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    companion object {
        private const val TAG = "Settings"
        private const val KEY_ALERTS = "alerts_enabled"
        private const val KEY_HAPTIC = "haptic"
        private const val KEY_MUSIC_APP = "music_app"
        private const val KEY_ALERT_LEVEL = "alert_level"
        private const val KEY_UPDATED_AT = "updated_at"

        // Holds the application context only (see get()), so it can't leak an Activity.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: Settings? = null

        fun get(context: Context): Settings = instance ?: synchronized(this) {
            instance ?: Settings(context.applicationContext).also { instance = it }
        }
    }
}
