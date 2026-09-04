package com.charlie.ticklist

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

fun performSuccessHapticFeedback(
    context: Context
) {
    val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(
                    Context.VIBRATOR_MANAGER_SERVICE
                ) as VibratorManager

            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(
                Context.VIBRATOR_SERVICE
            ) as Vibrator
        }

    if (!vibrator.hasVibrator()) {
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(
                70L,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(70L)
    }
}
