package com.hapticpath.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.whispercpp.whisper.WhisperContext
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var downloadButton: Button
    private lateinit var recordButton: Button
    private lateinit var resultText: TextView

    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var whisperContext: WhisperContext? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val recordedAudioData = mutableListOf<Short>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
        }

        statusText = TextView(this).apply {
            text = "Whisper Status: Kontrollerar..."
            textSize = 18f
            setPadding(0, 0, 0, 20)
        }

        downloadButton = Button(this).apply {
            text = "Ladda ner Whisper Model"
            setOnClickListener { downloadWhisperModel() }
        }

        recordButton = Button(this).apply {
            text = "Starta inspelning"
            isEnabled = false
            setOnClickListener {
                if (isRecording) {
                    stopRecordingAndTranscribe()
                } else {
                    startRecording()
                }
            }
        }

        resultText = TextView(this).apply {
            text = "Tolkad text visas här..."
            textSize = 16f
            setPadding(0, 40, 0, 0)
        }

        layout.addView(statusText)
        layout.addView(downloadButton)
        layout.addView(recordButton)
        layout.addView(resultText)
        setContentView(layout)

        checkModelExists()
        requestAudioPermissions()
    }

    private fun requestAudioPermissions() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }

    private fun checkModelExists() {
        val modelFile = File(filesDir, "ggml-tiny.bin")
        if (modelFile.exists()) {
            statusText.text = "Whisper Status: Modell redo (${modelFile.length() / (1024 * 1024)} MB)"
            downloadButton.isEnabled = false
            recordButton.isEnabled = true
            initWhisperContext(modelFile.absolutePath)
        } else {
            statusText.text = "Whisper Status: Ej nedladdad"
            downloadButton.isEnabled = true
            recordButton.isEnabled = false
        }
    }

    private fun initWhisperContext(modelPath: String) {
        thread {
            try {
                whisperContext = WhisperContext.initFromFile(modelPath)
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Fel vid laddning av Whisper: ${e.message}"
                }
            }
        }
    }

    private fun downloadWhisperModel() {
        statusText.text = "Laddar ner Whisper-modell..."
        downloadButton.isEnabled = false

        thread {
            try {
                val url = URL("https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin")
                val connection = url.openConnection()
                val inputStream = connection.getInputStream()
                val outputFile = File(filesDir, "ggml-tiny.bin")

                outputFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }

                runOnUiThread {
                    statusText.text = "Nedladdning klar! Modell redo."
                    recordButton.isEnabled = true
                    initWhisperContext(outputFile.absolutePath)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Fel vid nedladdning: ${e.message}"
                    downloadButton.isEnabled = true
                }
            }
        }
    }

    private fun startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        recordedAudioData.clear()
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true
        recordButton.text = "Stoppa & Tolka"
        resultText.text = "Spelar in ljud..."

        thread {
            val buffer = ShortArray(bufferSize / 2)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    for (i in 0 until read) {
                        recordedAudioData.add(buffer[i])
                    }
                }
            }
        }
    }

    private fun stopRecordingAndTranscribe() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        recordButton.text = "Starta inspelning"
        resultText.text = "Bearbetar ljud med Whisper..."

        thread {
            val ctx = whisperContext
            if (ctx == null) {
                runOnUiThread {
                    resultText.text = "Whisper är inte initierat."
                }
                return@thread
            }

            // Konvertera PCM 16-bit Short till Float Array (32-bit float, krävs av Whisper)
            val floatArray = FloatArray(recordedAudioData.size)
            for (i in recordedAudioData.indices) {
                floatArray[i] = recordedAudioData[i] / 32768.0f
            }

            try {
                val result = ctx.transcribeData(floatArray)
                runOnUiThread {
                    resultText.text = "Tolkning: $result"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    resultText.text = "Tolkning misslyckades: ${e.message}"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        whisperContext?.release()
    }
}
