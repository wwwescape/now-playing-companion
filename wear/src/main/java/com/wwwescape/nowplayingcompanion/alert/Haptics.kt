package com.wwwescape.nowplayingcompanion.alert

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.wwwescape.nowplayingcompanion.data.HapticPattern

object Haptics {

    fun play(context: Context, pattern: HapticPattern) {
        val effect = when (pattern) {
            HapticPattern.SUBTLE -> VibrationEffect.createOneShot(40, 90)
            HapticPattern.PROMINENT -> VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
            // Lub-dub: two quick pulses, the second softer, mimicking a heartbeat.
            HapticPattern.HEARTBEAT -> VibrationEffect.createWaveform(
                longArrayOf(0, 70, 110, 50),
                intArrayOf(0, 255, 0, 140),
                -1,
            )
            HapticPattern.MUTE -> return
        }
        vibrator(context).vibrate(effect)
    }

    /** On/off timings for a notification channel, which can't vary strength; null for MUTE. */
    fun timings(pattern: HapticPattern): LongArray? = when (pattern) {
        HapticPattern.SUBTLE -> longArrayOf(0, 40)
        HapticPattern.PROMINENT -> longArrayOf(0, 300)
        HapticPattern.HEARTBEAT -> longArrayOf(0, 70, 110, 50)
        HapticPattern.MUTE -> null
    }

    private fun vibrator(context: Context): Vibrator =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
}
