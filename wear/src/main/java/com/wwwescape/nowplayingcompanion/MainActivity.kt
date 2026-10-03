package com.wwwescape.nowplayingcompanion

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.wwwescape.nowplayingcompanion.data.Track
import com.wwwescape.nowplayingcompanion.data.TrackHistory
import com.wwwescape.nowplayingcompanion.sync.NowPlayingSync
import com.wwwescape.nowplayingcompanion.sync.PhoneLauncher
import com.wwwescape.nowplayingcompanion.ui.WearApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearApp() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        lifecycleScope.launch { NowPlayingSync.pull(this@MainActivity) }
        if (savedInstanceState == null) handleOpenRequest(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenRequest(intent)
    }

    /**
     * Notification actions and the tile ask to open a song on the phone via intent extras.
     * This activity is exported (the launcher and tile need that), so extras only *select* a
     * song already in history; other apps can't make the phone open arbitrary text.
     */
    private fun handleOpenRequest(intent: Intent) {
        val history = TrackHistory.get(this)
        val track = when {
            intent.hasExtra(EXTRA_RECOGNIZED_AT) -> {
                val recognizedAt = intent.getLongExtra(EXTRA_RECOGNIZED_AT, 0L)
                history.tracks.value.firstOrNull { it.recognizedAt == recognizedAt }
            }
            intent.getBooleanExtra(EXTRA_OPEN_LATEST, false) -> history.latest
            else -> null
        } ?: return
        PhoneLauncher.open(this, track)
    }

    companion object {
        const val EXTRA_OPEN_LATEST = "open_latest_on_phone"
        private const val EXTRA_RECOGNIZED_AT = "open_recognized_at"

        /** Opens the app; with [openTrack], also opens that song on the phone. */
        fun intent(context: Context, openTrack: Track?): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .apply {
                    if (openTrack != null) putExtra(EXTRA_RECOGNIZED_AT, openTrack.recognizedAt)
                }
    }
}
