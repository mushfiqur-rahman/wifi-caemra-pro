package com.example.server

import android.util.Log
import com.example.model.StreamConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class WebcamHttpServer(
    private val broadcaster: StreamBroadcaster,
    private val e2eeManager: E2eeCryptoManager,
    private var config: StreamConfig,
    private val onControlAction: (action: String, value: Any) -> Unit
) {
    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private val threadPool = Executors.newCachedThreadPool()

    var currentMotionScore: Float = 0f
    var motionThreshold: Int = 35
    var currentFps: Int = 25
    var batteryLevel: Int = 100
    var isCharging: Boolean = false
    var isLowBattery: Boolean = false

    fun updateConfig(newConfig: StreamConfig) {
        this.config = newConfig
    }

    fun start(port: Int = config.port) {
        if (isRunning.get()) return

        try {
            serverSocket = ServerSocket(port)
            serverSocket?.reuseAddress = true
            isRunning.set(true)

            threadPool.execute {
                while (isRunning.get() && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val client = serverSocket!!.accept()
                        client.tcpNoDelay = true
                        threadPool.execute {
                            handleClient(client)
                        }
                    } catch (e: SocketException) {
                        break
                    } catch (e: Exception) {
                        Log.e("WebcamHttpServer", "Error accepting client", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WebcamHttpServer", "Failed to start server on port $port", e)
            isRunning.set(false)
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {
        }
        serverSocket = null
    }

    fun isServerRunning(): Boolean = isRunning.get()

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val uri = parts[1]

            // Read headers
            var line: String?
            var contentLength = 0
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            when {
                uri == "/" || uri.startsWith("/index") -> {
                    serveHtmlDashboard(output)
                }
                uri.startsWith("/stream.mjpg") -> {
                    serveMjpegStream(socket, output)
                }
                uri.startsWith("/snapshot.jpg") -> {
                    serveSnapshot(output)
                }
                uri.startsWith("/audio.wav") -> {
                    serveAudioStream(socket, output)
                }
                uri.startsWith("/status") -> {
                    serveStatusJson(output)
                }
                uri.startsWith("/api/control") && method.equals("POST", ignoreCase = true) -> {
                    handleApiControl(reader, contentLength, output)
                }
                else -> {
                    val notFound = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"
                    output.write(notFound.toByteArray())
                    output.flush()
                }
            }
        } catch (e: Exception) {
            // Client disconnected or pipe broken
        } finally {
            try {
                if (!socket.isClosed && !socket.isOutputShutdown) {
                    socket.close()
                }
            } catch (ignored: Exception) {
            }
        }
    }

    private fun serveHtmlDashboard(output: OutputStream) {
        val html = WebDashboardHtml.getHtml()
        val bytes = html.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"

        output.write(header.toByteArray())
        output.write(bytes)
        output.flush()
    }

    private fun serveStatusJson(output: OutputStream) {
        val json = JSONObject().apply {
            put("fps", currentFps)
            put("resolution", config.resolution.label)
            put("quality", config.jpegQuality)
            put("audioEnabled", config.audioEnabled)
            put("torchOn", config.isTorchOn)
            put("motionScore", currentMotionScore)
            put("motionThreshold", motionThreshold)
            put("e2eeEnabled", config.isE2eeEnabled)
            put("batteryLevel", batteryLevel)
            put("isCharging", isCharging)
            put("isLowBattery", isLowBattery)
            put("clientCount", broadcaster.getSubscriberCount().coerceAtLeast(1))
        }.toString()

        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"

        output.write(header.toByteArray())
        output.write(bytes)
        output.flush()
    }

    private fun serveSnapshot(output: OutputStream) {
        val frame = broadcaster.getLatestFrame()
        if (frame != null) {
            val finalBytes = if (config.isE2eeEnabled) {
                e2eeManager.encryptFrame(frame) ?: frame
            } else frame

            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: image/jpeg\r\n" +
                    "Content-Length: ${finalBytes.size}\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"

            output.write(header.toByteArray())
            output.write(finalBytes)
            output.flush()
        } else {
            val empty = "HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\n\r\n"
            output.write(empty.toByteArray())
            output.flush()
        }
    }

    private fun serveMjpegStream(socket: Socket, output: OutputStream) {
        val boundary = "--frame"
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: multipart/x-mixed-replace; boundary=$boundary\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Cache-Control: no-cache, private, no-store, must-revalidate\r\n" +
                "Pragma: no-cache\r\n" +
                "Connection: close\r\n\r\n"

        output.write(header.toByteArray())
        output.flush()

        val lock = Object()
        var newFrameBytes: ByteArray? = null

        val consumer = StreamBroadcaster.FrameConsumer { frameBytes, _ ->
            synchronized(lock) {
                newFrameBytes = frameBytes
                lock.notifyAll()
            }
        }

        broadcaster.addSubscriber(consumer)

        try {
            while (isRunning.get() && !socket.isClosed && socket.isConnected) {
                var frame: ByteArray?
                synchronized(lock) {
                    while (newFrameBytes == null && isRunning.get() && !socket.isClosed) {
                        lock.wait(500)
                    }
                    frame = newFrameBytes
                    newFrameBytes = null
                }

                if (frame != null) {
                    val frameToSend = if (config.isE2eeEnabled) {
                        e2eeManager.encryptFrame(frame!!) ?: frame!!
                    } else {
                        frame!!
                    }

                    val frameHeader = "$boundary\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Content-Length: ${frameToSend.size}\r\n\r\n"

                    output.write(frameHeader.toByteArray())
                    output.write(frameToSend)
                    output.write("\r\n".toByteArray())
                    output.flush()
                }
            }
        } catch (e: Exception) {
            // Client closed stream connection
        } finally {
            broadcaster.removeSubscriber(consumer)
        }
    }

    private fun serveAudioStream(socket: Socket, output: OutputStream) {
        if (!config.audioEnabled) {
            val disabled = "HTTP/1.1 403 Forbidden\r\nContent-Length: 17\r\n\r\nAudio is disabled"
            output.write(disabled.toByteArray())
            output.flush()
            return
        }

        val sampleRate = 16000
        val channels = 1
        val bitsPerSample = 16

        // Send streaming WAV header
        val wavHeader = createStreamingWavHeader(sampleRate, channels, bitsPerSample)
        val httpHeader = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: audio/wav\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Cache-Control: no-cache, no-store\r\n" +
                "Connection: close\r\n\r\n"

        output.write(httpHeader.toByteArray())
        output.write(wavHeader)
        output.flush()

        broadcaster.addAudioSubscriber(output)

        try {
            while (isRunning.get() && !socket.isClosed && socket.isConnected) {
                Thread.sleep(1000)
            }
        } catch (ignored: Exception) {
        } finally {
            broadcaster.removeAudioSubscriber(output)
        }
    }

    private fun handleApiControl(reader: BufferedReader, contentLength: Int, output: OutputStream) {
        try {
            val bodyChars = CharArray(contentLength)
            var totalRead = 0
            while (totalRead < contentLength) {
                val read = reader.read(bodyChars, totalRead, contentLength - totalRead)
                if (read == -1) break
                totalRead += read
            }
            val body = String(bodyChars)
            val json = JSONObject(body)

            val action = json.optString("action")
            val value = json.opt("value") ?: ""

            onControlAction(action, value)

            val response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n" +
                    "{\"status\":\"ok\"}"
            output.write(response.toByteArray())
            output.flush()
        } catch (e: Exception) {
            val err = "HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n"
            output.write(err.toByteArray())
            output.flush()
        }
    }

    private fun createStreamingWavHeader(sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val header = ByteArray(44)
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        // ChunkSize (unknown / streaming, placeholder 0x7FFFFFFF)
        val totalDataLen = 0x7FFFFFF0
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        // WAVE
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // fmt chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        // Subchunk1Size (16 for PCM)
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        // AudioFormat (1 for PCM)
        header[20] = 1
        header[21] = 0
        // NumChannels
        header[22] = channels.toByte()
        header[23] = 0
        // SampleRate
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        // ByteRate
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        // BlockAlign
        header[32] = (blockAlign.toInt() and 0xff).toByte()
        header[33] = (blockAlign.toInt() shr 8 and 0xff).toByte()
        // BitsPerSample
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        // data chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        val dataChunkLen = 0x7FFFFF00
        header[40] = (dataChunkLen and 0xff).toByte()
        header[41] = (dataChunkLen shr 8 and 0xff).toByte()
        header[42] = (dataChunkLen shr 16 and 0xff).toByte()
        header[43] = (dataChunkLen shr 24 and 0xff).toByte()

        return header
    }
}
