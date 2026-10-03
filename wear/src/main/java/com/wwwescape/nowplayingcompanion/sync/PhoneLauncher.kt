package com.wwwescape.nowplayingcompanion.sync

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.wwwescape.nowplayingcompanion.R
import com.wwwescape.nowplayingcompanion.data.Settings
import com.wwwescape.nowplayingcompanion.data.Track

/** Opens a song on the paired phone, in the music app chosen in settings. */
object PhoneLauncher {

    private const val TAG = "PhoneLauncher"

    fun open(context: Context, track: Track) {
        val uri = Settings.get(context).musicApp.value.searchUri(track)
        val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        val future = RemoteActivityHelper(context).startRemoteActivity(intent)
        Toast.makeText(context, R.string.opening_on_phone, Toast.LENGTH_SHORT).show()
        future.addListener({
            try {
                future.get()
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't open song on phone", e)
                Toast.makeText(context, R.string.open_on_phone_failed, Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
