package com.wwwescape.nowplayingcompanion.data

import android.content.Context
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

    /**
     * Adds [track] to the top of the history. Returns false if it repeats the latest song within
     * [DEDUPE_WINDOW_MS], which happens when the source re-posts the same recognition.
     */
    @Synchronized
    fun add(track: Track): Boolean {
        val last = latest
        if (track.sameSongAs(last) && track.recognizedAt - last!!.recognizedAt < DEDUPE_WINDOW_MS) {
            return false
        }
        save((listOf(track) + _tracks.value).take(MAX_TRACKS))
        return true
    }

    @Synchronized
    fun remove(track: Track) = save(_tracks.value - track)

    @Synchronized
    fun clear() = save(emptyList())

    private fun save(tracks: List<Track>) {
        _tracks.value = tracks
        prefs.edit().putString(KEY_TRACKS, Track.listToJson(tracks)).apply()
    }

    companion object {
        private const val KEY_TRACKS = "tracks"
        private const val MAX_TRACKS = 100
        private const val DEDUPE_WINDOW_MS = 10 * 60 * 1000L

        @Volatile
        private var instance: TrackHistory? = null

        fun get(context: Context): TrackHistory = instance ?: synchronized(this) {
            instance ?: TrackHistory(context.applicationContext).also { instance = it }
        }
    }
}
