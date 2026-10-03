package com.wwwescape.nowplayingcompanion.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Songs received from the phone, newest first, persisted as JSON in SharedPreferences.
 * Also tracks whether the newest song is still playing (the phone's notification is showing).
 */
class TrackHistory private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)
    private val _tracks = MutableStateFlow(Track.listFromJson(prefs.getString(KEY_TRACKS, null)))
    private val _active = MutableStateFlow(prefs.getBoolean(KEY_ACTIVE, false))

    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    /** True while the latest song is still being heard by the phone. */
    val active: StateFlow<Boolean> = _active.asStateFlow()

    val latest: Track? get() = _tracks.value.firstOrNull()

    /** Adds [track] if it's a new recognition. Returns false for an update of one already stored. */
    @Synchronized
    fun add(track: Track): Boolean {
        if (_tracks.value.any { it.recognizedAt == track.recognizedAt }) return false
        save((listOf(track) + _tracks.value).sortedByDescending { it.recognizedAt }.take(MAX_TRACKS))
        return true
    }

    /** Returns true if the value changed; unchanged updates skip the disk write. */
    @Synchronized
    fun setActive(active: Boolean): Boolean {
        if (_active.value == active) return false
        _active.value = active
        prefs.edit().putBoolean(KEY_ACTIVE, active).apply()
        return true
    }

    @Synchronized
    fun remove(track: Track) {
        if (track == latest) setActive(false)
        save(_tracks.value - track)
    }

    @Synchronized
    fun clear() {
        setActive(false)
        save(emptyList())
    }

    private fun save(tracks: List<Track>) {
        _tracks.value = tracks
        prefs.edit().putString(KEY_TRACKS, Track.listToJson(tracks)).apply()
    }

    companion object {
        private const val KEY_TRACKS = "tracks"
        private const val KEY_ACTIVE = "active"
        private const val MAX_TRACKS = 50

        @Volatile
        private var instance: TrackHistory? = null

        fun get(context: Context): TrackHistory = instance ?: synchronized(this) {
            instance ?: TrackHistory(context.applicationContext).also { instance = it }
        }
    }
}
