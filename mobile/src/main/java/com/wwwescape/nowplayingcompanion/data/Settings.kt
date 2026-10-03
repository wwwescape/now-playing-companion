package com.wwwescape.nowplayingcompanion.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.wwwescape.nowplayingcompanion.R
import com.wwwescape.nowplayingcompanion.play.MusicApp
import com.wwwescape.nowplayingcompanion.sync.DataLayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Mirrors the watch's alert styles; names must match the watch enum (they're synced by name). */
enum class AlertLevel(@param:StringRes val label: Int, @param:StringRes val summary: Int) {
    ON_SCREEN(R.string.level_on_screen, R.string.level_on_screen_summary),
    DOT(R.string.level_dot, R.string.level_dot_summary),
    QUIET(R.string.level_quiet, R.string.level_quiet_summary),
}

/** Mirrors the watch's vibration patterns; names must match the watch enum. */
enum class HapticPattern(@param:StringRes val label: Int) {
    SUBTLE(R.string.haptic_subtle),
    PROMINENT(R.string.haptic_prominent),
    HEARTBEAT(R.string.haptic_heartbeat),
    MUTE(R.string.haptic_mute),
}

/**
 * Settings shared with the watch: the music app both devices play songs in, plus the watch's
 * alert settings so they can be changed from here. Every local change is stamped and published
 * to the Data Layer; a newer copy from the watch replaces these values (see [applyRemote]).
 */
class Settings private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _musicApp = MutableStateFlow(enumPref(KEY_MUSIC_APP, MusicApp.YOUTUBE_MUSIC))
    private val _alertsEnabled = MutableStateFlow(prefs.getBoolean(KEY_ALERTS, true))
    private val _alertLevel = MutableStateFlow(enumPref(KEY_ALERT_LEVEL, AlertLevel.DOT))
    private val _haptic = MutableStateFlow(enumPref(KEY_HAPTIC, HapticPattern.SUBTLE))

    /** When these values last changed on either device; 0 = never (watch's copy wins). */
    private var updatedAt = prefs.getLong(KEY_UPDATED_AT, 0L)

    /** Whether the watch alert values here came from the watch, rather than being defaults. */
    private var hasWatchValues = prefs.getBoolean(KEY_HAS_WATCH_VALUES, false)

    val musicApp: StateFlow<MusicApp> = _musicApp.asStateFlow()
    val alertsEnabled: StateFlow<Boolean> = _alertsEnabled.asStateFlow()
    val alertLevel: StateFlow<AlertLevel> = _alertLevel.asStateFlow()
    val haptic: StateFlow<HapticPattern> = _haptic.asStateFlow()

    fun setMusicApp(app: MusicApp) = edit { _musicApp.value = app }
    fun setAlertsEnabled(enabled: Boolean) = edit { _alertsEnabled.value = enabled }
    fun setAlertLevel(level: AlertLevel) = edit { _alertLevel.value = level }
    fun setHaptic(pattern: HapticPattern) = edit { _haptic.value = pattern }

    /** Applies settings from the watch if they're newer than ours. Our own echo is ignored. */
    @Synchronized
    fun applyRemote(map: DataMap) {
        val remoteUpdatedAt = map.getLong(DataLayer.KEY_SETTINGS_UPDATED_AT)
        if (remoteUpdatedAt <= updatedAt) return
        updatedAt = remoteUpdatedAt
        _musicApp.value = enumValue(map.getString(DataLayer.KEY_SETTING_MUSIC_APP), _musicApp.value)
        _alertsEnabled.value = map.getBoolean(DataLayer.KEY_SETTING_ALERTS, _alertsEnabled.value)
        _alertLevel.value = enumValue(map.getString(DataLayer.KEY_SETTING_ALERT_LEVEL), _alertLevel.value)
        _haptic.value = enumValue(map.getString(DataLayer.KEY_SETTING_HAPTIC), _haptic.value)
        hasWatchValues = true
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
            putString(KEY_MUSIC_APP, _musicApp.value.name)
            putBoolean(KEY_ALERTS, _alertsEnabled.value)
            putString(KEY_ALERT_LEVEL, _alertLevel.value.name)
            putString(KEY_HAPTIC, _haptic.value.name)
            putLong(KEY_UPDATED_AT, updatedAt)
            putBoolean(KEY_HAS_WATCH_VALUES, hasWatchValues)
        }
    }

    /** Fire-and-forget: Play Services queues the item and delivers it when the watch connects. */
    private fun publish() {
        val request = PutDataMapRequest.create(DataLayer.PATH_SETTINGS).apply {
            dataMap.putLong(DataLayer.KEY_SETTINGS_UPDATED_AT, updatedAt)
            dataMap.putString(DataLayer.KEY_SETTING_MUSIC_APP, _musicApp.value.name)
            dataMap.putBoolean(DataLayer.KEY_SETTING_ALERTS, _alertsEnabled.value)
            dataMap.putString(DataLayer.KEY_SETTING_ALERT_LEVEL, _alertLevel.value.name)
            dataMap.putString(DataLayer.KEY_SETTING_HAPTIC, _haptic.value.name)
            dataMap.putBoolean(DataLayer.KEY_SETTINGS_HAS_WATCH_VALUES, hasWatchValues)
        }.asPutDataRequest().setUrgent() // The user is likely watching the wrist for the change.
        Wearable.getDataClient(context).putDataItem(request)
            .addOnFailureListener { Log.w(TAG, "Couldn't share settings with the watch", it) }
    }

    private inline fun <reified T : Enum<T>> enumPref(key: String, default: T): T =
        enumValue(prefs.getString(key, null), default)

    private inline fun <reified T : Enum<T>> enumValue(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    companion object {
        private const val TAG = "Settings"
        private const val KEY_MUSIC_APP = "music_app_name"
        private const val KEY_ALERTS = "alerts_enabled"
        private const val KEY_ALERT_LEVEL = "alert_level"
        private const val KEY_HAPTIC = "haptic"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val KEY_HAS_WATCH_VALUES = "has_watch_values"

        // Holds the application context only (see get()), so it can't leak an Activity.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: Settings? = null

        fun get(context: Context): Settings = instance ?: synchronized(this) {
            instance ?: Settings(context.applicationContext).also { instance = it }
        }
    }
}
