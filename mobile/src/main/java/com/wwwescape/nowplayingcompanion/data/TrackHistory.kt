package com.wwwescape.nowplayingcompanion.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Recently recognized songs, newest first, persisted as JSON in SharedPreferences.
 * The list is small (capped at [MAX_TRACKS]) so a single preference value is plenty.
 */
class TrackHistory private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)
    private val _tracks = MutableStateFlow(Track.listFromJson(prefs.getString(KEY_TRACKS, null)))
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    val latest: Track? get() = _tracks.value.firstOrNull()

    /** Songs recognized at or before this time were cleared (here or on the watch). */
    private var clearedUpTo = prefs.getLong(KEY_CLEARED_UP_TO, 0L)

    /**
     * Adds [track] to the top of the history. Returns false if it repeats the latest song within
     * [DEDUPE_WINDOW_MS], which happens when the source re-posts the same recognition.
     */
    @Synchronized
    fun add(track: Track): Boolean {
        if (track.recognizedAt <= clearedUpTo) return false
        val last = latest
        if (track.sameSongAs(last) && track.recognizedAt - last!!.recognizedAt < DEDUPE_WINDOW_MS) {
            return false
        }
        save((listOf(track) + _tracks.value).take(MAX_TRACKS))
        return true
    }

    @Synchronized
    fun remove(track: Track) = save(_tracks.value - track)

    /**
     * Clears every song and returns the cut-off to share with the watch. Never earlier than the
     * newest song, so a clock change can't leave it behind.
     */
    @Synchronized
    fun clear(): Long {
        val upTo = maxOf(System.currentTimeMillis(), latest?.recognizedAt ?: 0L)
        clearUpTo(upTo)
        return upTo
    }

    /** Drops songs recognized at or before [upTo]. Returns false if already applied (our own echo). */
    @Synchronized
    fun clearUpTo(upTo: Long): Boolean {
        if (upTo <= clearedUpTo) return false
        clearedUpTo = upTo
        prefs.edit { putLong(KEY_CLEARED_UP_TO, upTo) }
        save(_tracks.value.filter { it.recognizedAt > upTo })
        return true
    }

    private fun save(tracks: List<Track>) {
        _tracks.value = tracks
        prefs.edit { putString(KEY_TRACKS, Track.listToJson(tracks)) }
    }

    companion object {
        private const val KEY_TRACKS = "tracks"
        private const val KEY_CLEARED_UP_TO = "cleared_up_to"
        private const val MAX_TRACKS = 100
        private const val DEDUPE_WINDOW_MS = 10 * 60 * 1000L

        @Volatile
        private var instance: TrackHistory? = null

        fun get(context: Context): TrackHistory = instance ?: synchronized(this) {
            instance ?: TrackHistory(context.applicationContext).also { instance = it }
        }
    }
}
