package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.StreamConfig
import com.example.server.StreamBroadcaster
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CameraStreamManager(
    private val context: Context,
    private val broadcaster: StreamBroadcaster,
    private val motionDetector: MotionDetector
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private var currentConfig = StreamConfig()
    private val isStreaming = AtomicBoolean(false)

    private var frameCounter = 0
    private var lastFpsTimestamp = System.currentTimeMillis()
    var currentFps: Int = 0
        private set

    fun updateConfig(config: StreamConfig) {
        val needRebind = (config.isFrontCamera != currentConfig.isFrontCamera) ||
                (config.resolution != currentConfig.resolution)
        val torchChanged = (config.isTorchOn != currentConfig.isTorchOn)

        this.currentConfig = config

        if (torchChanged) {
            setTorch(config.isTorchOn)
        }
    }

    fun setTorch(enable: Boolean) {
        try {
            cameraControl?.enableTorch(enable)
        } catch (e: Exception) {
            Log.e("CameraStreamManager", "Error toggling torch", e)
        }
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        config: StreamConfig,
        onReady: () -> Unit = {}
    ) {
        this.currentConfig = config
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                onReady()
            } catch (e: Exception) {
                Log.e("CameraStreamManager", "Camera provider error", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?
    ) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val lensFacing = if (currentConfig.isFrontCamera) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        val targetResolution = Size(currentConfig.resolution.width, currentConfig.resolution.height)

        val preview = Preview.Builder()
            .setTargetResolution(targetResolution)
            .build()

        if (previewView != null) {
            preview.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(targetResolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
            processFrame(imageProxy)
        }

        try {
            camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
            cameraControl = camera?.cameraControl
            setTorch(currentConfig.isTorchOn)
            isStreaming.set(true)
        } catch (e: Exception) {
            Log.e("CameraStreamManager", "Use case binding failed", e)
        }
    }

    private fun processFrame(image: ImageProxy) {
        try {
            // First analyze motion
            motionDetector.analyze(image)

            // Convert to JPEG for low-latency network broadcasting
            val bitmap = image.toBitmap()
            val rotation = image.imageInfo.rotationDegrees

            val finalBitmap = if (rotation != 0 || currentConfig.isFrontCamera) {
                val matrix = Matrix()
                if (rotation != 0) matrix.postRotate(rotation.toFloat())
                if (currentConfig.isFrontCamera) matrix.postScale(-1f, 1f) // Mirror front camera
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            val outStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, currentConfig.jpegQuality, outStream)
            val jpegBytes = outStream.toByteArray()

            broadcaster.submitFrame(jpegBytes)

            // FPS calculation
            frameCounter++
            val now = System.currentTimeMillis()
            if (now - lastFpsTimestamp >= 1000) {
                currentFps = frameCounter
                frameCounter = 0
                lastFpsTimestamp = now
            }
        } catch (e: Exception) {
            // Frame processing catch
        } finally {
            image.close()
        }
    }

    fun saveSnapshotToFile(targetFile: File): Boolean {
        val frameBytes = broadcaster.getLatestFrame() ?: return false
        return try {
            FileOutputStream(targetFile).use { it.write(frameBytes) }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun stopCamera() {
        isStreaming.set(false)
        try {
            cameraProvider?.unbindAll()
        } catch (ignored: Exception) {
        }
    }
}
