package com.example.server

import java.io.OutputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class StreamBroadcaster {
    private val latestFrame = AtomicReference<ByteArray?>(null)
    private val frameSequence = AtomicLong(0)
    private val subscribers = CopyOnWriteArrayList<FrameConsumer>()

    // Audio streaming listeners
    private val audioSubscribers = CopyOnWriteArrayList<OutputStream>()

    fun interface FrameConsumer {
        fun onNewFrame(frameBytes: ByteArray, sequence: Long)
    }

    fun submitFrame(jpegBytes: ByteArray) {
        latestFrame.set(jpegBytes)
        val seq = frameSequence.incrementAndGet()
        for (sub in subscribers) {
            try {
                sub.onNewFrame(jpegBytes, seq)
            } catch (ignored: Exception) {
            }
        }
    }

    fun getLatestFrame(): ByteArray? = latestFrame.get()

    fun addSubscriber(consumer: FrameConsumer) {
        subscribers.add(consumer)
    }

    fun removeSubscriber(consumer: FrameConsumer) {
        subscribers.remove(consumer)
    }

    fun getSubscriberCount(): Int = subscribers.size

    fun addAudioSubscriber(stream: OutputStream) {
        audioSubscribers.add(stream)
    }

    fun removeAudioSubscriber(stream: OutputStream) {
        audioSubscribers.remove(stream)
    }

    fun broadcastAudioChunk(pcmChunk: ByteArray) {
        for (sub in audioSubscribers) {
            try {
                sub.write(pcmChunk)
                sub.flush()
            } catch (e: Exception) {
                audioSubscribers.remove(sub)
            }
        }
    }
}
