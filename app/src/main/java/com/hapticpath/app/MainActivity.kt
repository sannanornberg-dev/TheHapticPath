package com.hapticpath.app

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var downloadButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
        }

        statusText = TextView(this).apply {
            text = "Whisper Status: Ej nedladdad"
            textSize = 18f
            setPadding(0, 0, 0, 40)
        }

        downloadButton = Button(this).apply {
            text = "Ladda ner Whisper Model"
            setOnClickListener {
                downloadWhisperModel()
            }
        }

        layout.addView(statusText)
        layout.addView(downloadButton)
        setContentView(layout)

        checkModelExists()
    }

    private fun checkModelExists() {
        val modelFile = File(filesDir, "ggml-tiny.bin")
        if (modelFile.exists()) {
            statusText.text = "Whisper Status: Modell redo (${modelFile.length() / (1024 * 1024)} MB)"
            downloadButton.isEnabled = false
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
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Fel vid nedladdning: ${e.message}"
                    downloadButton.isEnabled = true
                }
            }
        }
    }
}
