package com.hapticpath.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var downloadButton: Button
    private lateinit var recordButton: Button
    private lateinit var resultText: TextView

    private var isRecording = false

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
                    stopRecording()
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
        } else {
            statusText.text = "Whisper Status: Ej nedladdad"
            downloadButton.isEnabled = true
            recordButton.isEnabled = false
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
        isRecording = true
        recordButton.text = "Stoppa & Tolka"
        resultText.text = "Spelar in ljud..."
    }

    private fun stopRecording() {
        isRecording = false
        recordButton.text = "Starta inspelning"
        resultText.text = "Bearbetar ljud..."
        
        thread {
            Thread.sleep(1000)
            runOnUiThread {
                resultText.text = "Tolkning redo!"
            }
        }
    }
}
