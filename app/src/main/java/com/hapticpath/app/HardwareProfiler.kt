package com.hapticpath.app

import android.app.ActivityManager
import android.content.Context
import android.os.Build

enum class WhisperModelType(
    val fileName: String,
    val downloadUrl: String
) {
    TINY(
        fileName = "ggml-tiny.bin",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin"
    ),
    BASE(
        fileName = "ggml-base.bin",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
    )
}

class HardwareProfiler(private val context: Context) {

    fun getRecommendedModel(): WhisperModelType {
        // 1. Enhets-/SoC-kontroll för Moto G24 (Helio G85 / MT6769V)
        val deviceModel = Build.MODEL.lowercase()
        val deviceHardware = Build.HARDWARE.lowercase()

        val isMotoG24 = deviceModel.contains("moto g24") ||
                deviceModel.contains("g24") ||
                deviceHardware.contains("mt6769")

        if (isMotoG24) {
            return WhisperModelType.TINY
        }

        // 2. RAM-avläsning (fysiskt minne)
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamGb = memoryInfo.totalMem / (1024f * 1024f * 1024f)

        // 3. Strikt gräns: Under 6 GB RAM kör alltid TINY (täcker 4 GB-enheter med RAM Boost)
        return if (totalRamGb < 6.0f) {
            WhisperModelType.TINY
        } else {
            // Under utveckling av Fas 3 kör vi strikt TINY för att säkerställa prestanda på testmobilen
            WhisperModelType.TINY
        }
    }
}