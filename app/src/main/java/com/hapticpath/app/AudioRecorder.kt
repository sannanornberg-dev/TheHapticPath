package com.hapticpath.app

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlin.math.sqrt

class AudioRecorder {

    private val TAG = "AudioRecorder"
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    @SuppressLint("MissingPermission")
    fun startRecording(
        onBufferReady: (FloatArray) -> Unit,
        onRmsUpdated: (Float) -> Unit
    ) {
        if (isRecording) {
            Log.w(TAG, "startRecording anropades men inspelning pågår redan.")
            return
        }

        val minBufferSize = bufferSize.coerceAtLeast(48000 * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord kunde inte initialiseras. Kontrollera mikrofontillstånd och hårdvara.")
            return
        }

        audioRecord?.startRecording()
        isRecording = true
        Log.i(TAG, "Inspelning startad: SampleRate=$sampleRate, BufferSize=$minBufferSize")

        recordingThread = Thread {
            val windowSize = 48000 // 3 sekunder vid 16kHz
            val floatBuffer = FloatArray(windowSize)
            val shortBuffer = ShortArray(windowSize)
            var index = 0

            val readChunkSize = 1024
            val tempChunk = ShortArray(readChunkSize)

            while (isRecording) {
                val spaceLeft = windowSize - index
                val toRead = if (spaceLeft < readChunkSize) spaceLeft else readChunkSize
                val read = audioRecord?.read(tempChunk, 0, toRead) ?: 0

                if (read > 0) {
                    System.arraycopy(tempChunk, 0, shortBuffer, index, read)

                    // Beräkna RMS-volym i realtid
                    var sum = 0.0
                    for (i in 0 until read) {
                        sum += tempChunk[i] * tempChunk[i]
                    }
                    val rms = sqrt(sum / read)
                    val normalizedRms = (rms / 3000.0).toFloat().coerceIn(0f, 1f)

                    onRmsUpdated(normalizedRms)

                    index += read

                    if (index >= windowSize) {
                        Log.i(TAG, "3s buffert fylld (48k samples). Skickar till inferens.")
                        for (i in 0 until windowSize) {
                            floatBuffer[i] = shortBuffer[i] / 32768.0f
                        }
                        onBufferReady(floatBuffer.clone())
                        index = 0
                    }
                }
            }
            Log.i(TAG, "Inspelningstråd avslutad.")
        }.apply {
            name = "HapticPath-AudioThread"
            start()
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            Log.i(TAG, "Inspelning stoppad och resurser frigjorda.")
        } catch (e: Exception) {
            Log.e(TAG, "Fel vid nedstängning av AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
            recordingThread = null
        }
    }
}