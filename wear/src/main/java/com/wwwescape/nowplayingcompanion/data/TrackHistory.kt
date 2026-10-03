package com.wwwescape.nowplayingcompanion.data

import android.content.Context
import androidx.core.content.edit
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

    // The phone's latest song stays in the Data Layer and is re-read on every app launch, so
    // remember what the user got rid of: everything up to the last clear, plus swiped-away songs.
    private var clearedUpTo = prefs.getLong(KEY_CLEARED_UP_TO, 0L)
    private val dismissed = prefs.getStringSet(KEY_DISMISSED, null).orEmpty()
        .mapNotNullTo(mutableSetOf()) { it.toLongOrNull() }

    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    /** True while the latest song is still being heard by the phone. */
    val active: StateFlow<Boolean> = _active.asStateFlow()

    val latest: Track? get() = _tracks.value.firstOrNull()

    /** Adds [track] if it's a new recognition. Returns false for an update of one already stored. */
    @Synchronized
    fun add(track: Track): Boolean {
        if (track.recognizedAt <= clearedUpTo || track.recognizedAt in dismissed) return false
        if (_tracks.value.any { it.recognizedAt == track.recognizedAt }) return false
        save((listOf(track) + _tracks.value).sortedByDescending { it.recognizedAt }.take(MAX_TRACKS))
        return true
    }

    /** Returns true if the value changed; unchanged updates skip the disk write. */
    @Synchronized
    fun setActive(active: Boolean): Boolean {
        if (_active.value == active) return false
        _active.value = active
        prefs.edit { putBoolean(KEY_ACTIVE, active) }
        return true
    }

    @Synchronized
    fun remove(track: Track) {
        if (track == latest) setActive(false)
        // Only the newest few can come back from the Data Layer; cap the set to stay small.
        dismissed += track.recognizedAt
        if (dismissed.size > MAX_DISMISSED) dismissed -= dismissed.sorted().take(dismissed.size - MAX_DISMISSED).toSet()
        prefs.edit { putStringSet(KEY_DISMISSED, dismissed.mapTo(mutableSetOf()) { it.toString() }) }
        save(_tracks.value - track)
    }

    /**
     * Clears every song in this history and returns the cut-off to share with the phone. The
     * cut-off is never earlier than the newest song, so a phone clock running a little ahead
     * can't leave it behind.
     */
    @Synchronized
    fun clear(): Long {
        val upTo = maxOf(System.currentTimeMillis(), latest?.recognizedAt ?: 0L)
        clearUpTo(upTo)
        return upTo
    }

    /**
     * Drops songs recognized at or before [upTo], and ignores them if they arrive again.
     * Returns false if an equal or later clear was already applied (e.g. our own echo).
     */
    @Synchronized
    fun clearUpTo(upTo: Long): Boolean {
        if (upTo <= clearedUpTo) return false
        clearedUpTo = upTo
        dismissed.removeAll { it <= upTo }
        val remaining = _tracks.value.filter { it.recognizedAt > upTo }
        if (remaining.firstOrNull() != latest) setActive(false)
        prefs.edit {
            putLong(KEY_CLEARED_UP_TO, upTo)
            putStringSet(KEY_DISMISSED, dismissed.mapTo(mutableSetOf()) { it.toString() })
        }
        save(remaining)
        return true
    }

    private fun save(tracks: List<Track>) {
        _tracks.value = tracks
        prefs.edit { putString(KEY_TRACKS, Track.listToJson(tracks)) }
    }

    companion object {
        private const val KEY_TRACKS = "tracks"
        private const val KEY_ACTIVE = "active"
        private const val MAX_TRACKS = 50
        private const val MAX_DISMISSED = 20
        private const val KEY_CLEARED_UP_TO = "cleared_up_to"
        private const val KEY_DISMISSED = "dismissed"

        @Volatile
        private var instance: TrackHistory? = null

        fun get(context: Context): TrackHistory = instance ?: synchronized(this) {
            instance ?: TrackHistory(context.applicationContext).also { instance = it }
        }
    }
}
