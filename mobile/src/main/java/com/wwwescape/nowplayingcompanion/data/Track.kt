package com.wwwescape.nowplayingcompanion.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class Track(
    val title: String,
    val artist: String,
    val recognizedAt: Long,
) {
    /** Two recognitions are the same song if title and artist match, regardless of when. */
    fun sameSongAs(other: Track?): Boolean =
        other != null && title.equals(other.title, ignoreCase = true) &&
            artist.equals(other.artist, ignoreCase = true)

    fun toJson(): JSONObject = JSONObject()
        .put("title", title)
        .put("artist", artist)
        .put("recognizedAt", recognizedAt)

    companion object {
        fun fromJson(json: JSONObject) = Track(
            title = json.getString("title"),
            artist = json.optString("artist"),
            recognizedAt = json.getLong("recognizedAt"),
        )

        /** Longest title or artist kept; guards storage and UI against oversized input. */
        const val MAX_TEXT_LENGTH = 200

        /** Parses stored history; corrupt data yields an empty list rather than a crash on every launch. */
        fun listFromJson(raw: String?): List<Track> {
            if (raw.isNullOrEmpty()) return emptyList()
            return try {
                val array = JSONArray(raw)
                (0 until array.length()).mapNotNull { i ->
                    array.optJSONObject(i)?.let { runCatching { fromJson(it) }.getOrNull() }
                }
            } catch (e: JSONException) {
                emptyList()
            }
        }

        fun listToJson(tracks: List<Track>): String =
            JSONArray().apply { tracks.forEach { put(it.toJson()) } }.toString()
    }
}
