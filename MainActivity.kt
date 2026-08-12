package com.hapticpath.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var startButton: Button
    
    private val ruleMatcher = SvaRuleMatcher()
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }

        statusText = TextView(this).apply { text = "Status: Redo"; textSize = 18f }
        resultText = TextView(this).apply { text = "Talad text visas här..."; textSize = 22f; setPadding(0, 40, 0, 40) }
        startButton = Button(this).apply { text = "Starta Lyssning" }

        layout.addView(statusText)
        layout.addView(resultText)
        layout.addView(startButton)
        setContentView(layout)

        checkPermissions()

        startButton.setOnClickListener {
            if (!isListening) {
                startListening()
            } else {
                stopListening()
            }
        }
    }

    private fun startListening() {
        isListening = true
        startButton.text = "Stoppa Lyssning"
        statusText.text = "Status: Lyssnar..."
    }

    private fun stopListening() {
        isListening = false
        startButton.text = "Starta Lyssning"
        statusText.text = "Status: Stoppad"
    }

    fun onNewTextRecognized(text: String) {
        runOnUiThread {
            resultText.text = text
            if (ruleMatcher.hasWordOrderError(text)) {
                statusText.text = "Status: V2-FEL DETEKTERAT!"
                triggerHapticFeedback()
            }
        }
    }

    private fun triggerHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(250)
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }
}
