package com.hapticpath.app

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloader(private val context: Context) {

    private companion object {
        private const val TAG = "ModelDownloader"
        private const val TINY_MODEL_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin"
        private const val BASE_MODEL_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
    }

    /**
     * Väljer modell dynamiskt baserat på Hårdvaruprofil i Systeminstruktion (Projekt 2.0):
     * - Moto G24 (alla varianter) eller enheter med <= 4 GB RAM -> ggml-tiny.bin (~75 MB)
     * - Prestandaenheter (Flaggskepp / >= 6-8 GB RAM som inte är G24) -> ggml-base.bin (~140 MB)
     */
    private fun selectModel(): Pair<String, String> {
        val totalRamMb = getSystemRamMb()
        val deviceModel = Build.MODEL ?: ""

        val isBudgetBaseline = deviceModel.lowercase().contains("g24") || totalRamMb <= 4000

        return if (isBudgetBaseline) {
            Log.i(TAG, "Profil: Budget/Mellanklass ($deviceModel, $totalRamMb MB RAM) -> Hämtar ggml-tiny.bin")
            Pair("ggml-tiny.bin", TINY_MODEL_URL)
        } else {
            Log.i(TAG, "Profil: Prestanda ($deviceModel, $totalRamMb MB RAM) -> Hämtar ggml-base.bin")
            Pair("ggml-base.bin", BASE_MODEL_URL)
        }
    }

    private fun getSystemRamMb(): Long {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024)
    }

    fun isModelDownloaded(): Boolean {
        val (fileName, _) = selectModel()
        val file = File(context.filesDir, fileName)
        val exists = file.exists() && file.length() > 10 * 1024 * 1024
        Log.i(TAG, "Status för $fileName: Finns=$exists, Storlek=${file.length()} bytes")
        return exists
    }

    fun downloadModel(onProgress: (progress: Float, downloadedMb: Long, totalMb: Long, modelName: String) -> Unit): Boolean {
        val (fileName, downloadUrl) = selectModel()
        val outputFile = File(context.filesDir, fileName)

        // Rensa felaktig base-fil om den påbörjats eller installerats tidigare på en Moto G24
        val baseFile = File(context.filesDir, "ggml-base.bin")
        if (fileName == "ggml-tiny.bin" && baseFile.exists()) {
            Log.i(TAG, "Rensar gammal ggml-base.bin från enheten...")
            baseFile.delete()
        }

        if (outputFile.exists() && outputFile.length() > 10 * 1024 * 1024) {
            Log.i(TAG, "Rätt modell ($fileName) är redan verifierad på disk.")
            return true
        }

        val tempFile = File(context.filesDir, "$fileName.tmp")

        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP-fel vid nedladdning: ${connection.responseCode}")
                return false
            }

            val fileLength = connection.contentLengthLong
            val totalMb = if (fileLength > 0) fileLength / (1024 * 1024) else 1L

            val input: InputStream = connection.inputStream
            val output = FileOutputStream(tempFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            var lastReportTime = System.currentTimeMillis()

            while (input.read(data).also { count = it } != -1) {
                total += count
                output.write(data, 0, count)

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastReportTime > 100 || total == fileLength) {
                    lastReportTime = currentTime
                    val progress = if (fileLength > 0) total.toFloat() / fileLength.toFloat() else 0f
                    val downloadedMb = total / (1024 * 1024)
                    onProgress(progress, downloadedMb, totalMb, fileName)
                }
            }

            output.flush()
            output.close()
            input.close()

            if (tempFile.exists() && tempFile.length() > 0) {
                if (outputFile.exists()) outputFile.delete()
                if (tempFile.renameTo(outputFile)) {
                    Log.i(TAG, "Nedladdning färdig och sparad: ${outputFile.absolutePath}")
                    return true
                }
            }
            return false

        } catch (e: Exception) {
            Log.e(TAG, "Fel under nedladdning: ${e.localizedMessage}")
            if (tempFile.exists()) tempFile.delete()
            return false
        }
    }
}