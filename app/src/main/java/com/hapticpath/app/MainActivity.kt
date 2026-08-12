package com.hapticpath.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            text = "Appen fungerar!"
            textSize = 24f
            setPadding(80, 80, 80, 80)
        }
        setContentView(textView)
    }
}
