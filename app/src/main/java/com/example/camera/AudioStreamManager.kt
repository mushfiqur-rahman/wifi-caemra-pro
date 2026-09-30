package com.example.camera

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.example.server.StreamBroadcaster
import java.util.concurrent.atomic.AtomicBoolean

class AudioStreamManager(
    private val broadcaster: StreamBroadcaster
) {
    private val isRecording = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @SuppressLint("MissingPermission")
    fun startCapture() {
        if (isRecording.get()) return

        try {
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = minBufSize.coerceAtLeast(4096)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioStreamManager", "AudioRecord not initialized")
                return
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            recordingThread = Thread({
                val buffer = ByteArray(2048)
                while (isRecording.get()) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        val chunk = ByteArray(read)
                        System.arraycopy(buffer, 0, chunk, 0, read)
                        broadcaster.broadcastAudioChunk(chunk)
                    }
                }
            }, "WiFiCamAudioCaptureThread").apply {
                priority = Thread.NORM_PRIORITY
                start()
            }
        } catch (e: Exception) {
            Log.e("AudioStreamManager", "Failed to start audio recording", e)
            isRecording.set(false)
        }
    }

    fun stopCapture() {
        isRecording.set(false)
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (ignored: Exception) {
        }
        audioRecord = null
        recordingThread = null
    }

    fun isCapturing(): Boolean = isRecording.get()
}
