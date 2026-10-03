package com.wwwescape.nowplayingcompanion.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wwwescape.nowplayingcompanion.MainActivity
import com.wwwescape.nowplayingcompanion.R
import com.wwwescape.nowplayingcompanion.data.AlertLevel
import com.wwwescape.nowplayingcompanion.data.HapticPattern
import com.wwwescape.nowplayingcompanion.data.Track

/**
 * Posts the on-wrist "song detected" notification.
 *
 * A channel's importance and vibration are fixed once created, so each [AlertLevel] (and, for
 * ON_SCREEN, each vibration pattern) gets its own channel, and the ones no longer in use are
 * deleted so system settings only ever lists the current one.
 */
object SongAlerts {

    private const val CHANNEL_PREFIX = "song_"
    // Kept from before alert levels existed, so DOT users keep any tweaks they made to it.
    private const val CHANNEL_DOT = "song_detected"
    private const val CHANNEL_QUIET = "song_quiet"
    private const val CHANNEL_ON_SCREEN = "song_on_screen_"
    private const val NOTIFICATION_ID = 1

    /**
     * Shows [track]. Returns true if the system will vibrate for it, so the caller only plays
     * [Haptics] itself when it won't (silent levels, notifications blocked, or channel turned off).
     */
    fun show(context: Context, track: Track, level: AlertLevel, haptic: HapticPattern): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val channel = ensureChannel(context, level, haptic)
        if (channel.importance == NotificationManager.IMPORTANCE_NONE) return false

        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(
                when (level) {
                    AlertLevel.ON_SCREEN -> NotificationCompat.PRIORITY_HIGH
                    AlertLevel.DOT -> NotificationCompat.PRIORITY_DEFAULT
                    AlertLevel.QUIET -> NotificationCompat.PRIORITY_LOW
                }
            )
            .setAutoCancel(true)
            .setContentIntent(activityIntent(context, requestCode = 0, openTrack = null))
            .addAction(
                R.drawable.ic_phone,
                context.getString(R.string.action_open_on_phone),
                activityIntent(context, requestCode = 1, openTrack = track),
            )
            .build()

        return try {
            manager.notify(NOTIFICATION_ID, notification)
            channel.shouldVibrate()
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS was revoked between the check and the post; nothing to show.
            false
        }
    }

    /** Creates the channel for [level] (returning the user's current version of it) and drops stale ones. */
    private fun ensureChannel(context: Context, level: AlertLevel, haptic: HapticPattern): NotificationChannel {
        val manager = context.getSystemService(NotificationManager::class.java)
        val timings = Haptics.timings(haptic)
        val wanted = when (level) {
            AlertLevel.ON_SCREEN -> NotificationChannel(
                CHANNEL_ON_SCREEN + haptic.name.lowercase(),
                context.getString(R.string.channel_song_on_screen),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                // Wear OS only puts alerting notifications on screen, so the channel itself
                // vibrates here instead of the app (pattern timing kept, custom strength isn't).
                enableVibration(timings != null)
                if (timings != null) vibrationPattern = timings
            }
            AlertLevel.DOT -> NotificationChannel(
                CHANNEL_DOT,
                context.getString(R.string.channel_song_detected),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { enableVibration(false) }
            AlertLevel.QUIET -> NotificationChannel(
                CHANNEL_QUIET,
                context.getString(R.string.channel_song_quiet),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { enableVibration(false) }
        }.apply { setSound(null, null) }

        manager.notificationChannels
            .filter { it.id.startsWith(CHANNEL_PREFIX) && it.id != wanted.id }
            .forEach { manager.deleteNotificationChannel(it.id) }
        // No-op if it already exists; the user's edits to it are kept.
        manager.createNotificationChannel(wanted)
        return manager.getNotificationChannel(wanted.id) ?: wanted
    }

    private fun activityIntent(context: Context, requestCode: Int, openTrack: Track?): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            MainActivity.intent(context, openTrack),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
