package com.wwwescape.nowplayingcompanion.play

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.wwwescape.nowplayingcompanion.R

/**
 * Where a tapped song opens; mirrors the watch's "Open songs in" setting. Music apps start the
 * song via Android's play-from-search request (the one Google Assistant uses); Google has no
 * [packageName] and just opens a web search.
 */
enum class MusicApp(
    val id: String,
    @param:StringRes val label: Int,
    val packageName: String?,
    private val searchUrl: String,
) {
    YOUTUBE_MUSIC("youtube_music", R.string.music_app_youtube_music, "com.google.android.apps.youtube.music", "https://music.youtube.com/search?q="),
    SPOTIFY("spotify", R.string.music_app_spotify, "com.spotify.music", "https://open.spotify.com/search/"),
    GOOGLE("google", R.string.music_app_google, null, "https://www.google.com/search?q="),
    ;

    fun searchUri(query: String): Uri = (searchUrl + Uri.encode(query)).toUri()

    companion object {
        fun fromId(id: String?): MusicApp = entries.firstOrNull { it.id == id } ?: YOUTUBE_MUSIC
    }
}

object SongPlayer {

    /**
     * Asks [app] to find and start playing the song. If that app isn't installed or can't handle
     * the request (or it's Google), opens the search page so the user can pick it.
     */
    fun play(context: Context, title: String, artist: String, app: MusicApp) {
        val query = "$title $artist".trim()
        if (app.packageName != null) {
            val playIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                .setPackage(app.packageName)
                .putExtra(SearchManager.QUERY, query)
                // "A specific song": lets the app match on title and artist, not just free text.
                .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
                .putExtra(MediaStore.EXTRA_MEDIA_TITLE, title)
                .putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (start(context, playIntent)) return
        }
        start(context, Intent(Intent.ACTION_VIEW, app.searchUri(query)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
