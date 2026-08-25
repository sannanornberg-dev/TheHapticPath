package com.hapticpath.app

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class HapticFeedbackManager(private val context: Context) {

    private companion object {
        private const val TAG = "HapticPath_Test"
        private const val DEBOUNCE_INTERVAL_MS = 2000L
    }

    private var lastTriggerTime = 0L

    /**
     * Skickar en skarp dubbelvibration (Neural Pattern Interruption) till enheten.
     */
    fun triggerPatternInterruption() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTriggerTime < DEBOUNCE_INTERVAL_MS) {
            Log.d(TAG, "Vibration avbröts av debouncing-spärr.")
            return
        }
        lastTriggerTime = currentTime

        Log.d(TAG, "Exekverar triggerPatternInterruption()...")

        try {
            // Mönster: Vänta 0ms, vibrera 150ms, paus 100ms, vibrera 150ms
            val timings = longArrayOf(0, 150, 100, 150)
            // Full styrka (255)
            val amplitudes = intArrayOf(0, 255, 0, 255)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator

                if (vibrator.hasVibrator()) {
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY)
                    vibrator.vibrate(effect, attributes)
                    Log.d(TAG, "Vibration skickad via VibratorManager (API 31+)")
                } else {
                    Log.e(TAG, "Hårdvaruvarning: Vibrator saknas på enheten")
                }
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator.hasVibrator()) {
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator.vibrate(effect)
                    Log.d(TAG, "Vibration skickad via legacy Vibrator")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kritiskt fel vid utlösning av vibration: ${e.message}", e)
        }
    }
}