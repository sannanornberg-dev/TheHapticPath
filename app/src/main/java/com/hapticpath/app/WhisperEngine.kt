package com.hapticpath.app

import android.util.Log

class WhisperEngine {

    private var contextPtr: Long = 0

    // Native-deklarationer som matchar din whisper-jni.cpp exakt
    private external fun initContextNative(modelPath: String): Long
    private external fun freeContextNative(contextPtr: Long)
    private external fun transcribeBufferNative(
        contextPtr: Long,
        samples: FloatArray,
        numSamples: Int
    ): String

    fun initEngine(modelPath: String): Boolean {
        if (contextPtr != 0L) {
            release()
        }
        contextPtr = initContextNative(modelPath)
        return contextPtr != 0L
    }

    fun transcribeBuffer(samples: FloatArray): String {
        if (contextPtr == 0L) {
            Log.e(TAG, "Kan inte transkribera: contextPtr är 0")
            return ""
        }
        return transcribeBufferNative(contextPtr, samples, samples.size)
    }

    fun release() {
        if (contextPtr != 0L) {
            freeContextNative(contextPtr)
            contextPtr = 0L
        }
    }

    companion object {
        private const val TAG = "WhisperEngine"

        init {
            try {
                System.loadLibrary("whisper-jni")
                Log.i(TAG, "libwhisper-jni.so laddades utan fel.")
            } catch (e: Throwable) {
                Log.e(TAG, "Misslyckades att ladda libwhisper-jni.so: ${e.message}", e)
            }
        }
    }
}