package com.wwwescape.nowplayingcompanion.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wwwescape.nowplayingcompanion.MainActivity
import com.wwwescape.nowplayingcompanion.R
import com.wwwescape.nowplayingcompanion.data.Track

/** Posts the on-wrist "song detected" notification. Vibration is played separately by [Haptics]. */
object SongAlerts {

    private const val CHANNEL_ID = "song_detected"
    private const val NOTIFICATION_ID = 1

    fun show(context: Context, track: Track) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(activityIntent(context, requestCode = 0, openTrack = null))
            .addAction(
                R.drawable.ic_phone,
                context.getString(R.string.action_open_on_phone),
                activityIntent(context, requestCode = 1, openTrack = track),
            )
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS was revoked between the check and the post; nothing to show.
        }
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_song_detected),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            // The app plays the user's chosen haptic pattern itself, so keep the channel silent.
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun activityIntent(context: Context, requestCode: Int, openTrack: Track?): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            MainActivity.intent(context, openTrack),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
