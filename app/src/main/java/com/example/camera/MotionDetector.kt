package com.example.camera

import android.graphics.RectF
import androidx.camera.core.ImageProxy
import com.example.model.MotionSettings
import java.nio.ByteBuffer
import kotlin.math.abs

class MotionDetector(
    private var settings: MotionSettings,
    private val onMotionDetected: (peakScore: Float, regions: List<RectF>) -> Unit
) {
    private val gridCols = 24
    private val gridRows = 18
    private var previousGrid: IntArray? = null
    private var motionActiveStartTime = 0L
    private var lastTriggeredTimestamp = 0L

    var currentMotionScore: Float = 0f
        private set

    var activeRegions: List<RectF> = emptyList()
        private set

    fun updateSettings(newSettings: MotionSettings) {
        this.settings = newSettings
    }

    fun analyze(image: ImageProxy) {
        try {
            val planes = image.planes
            if (planes.isEmpty()) return

            val yBuffer: ByteBuffer = planes[0].buffer
            val rowStride = planes[0].rowStride
            val pixelStride = planes[0].pixelStride
            val width = image.width
            val height = image.height

            val currentGrid = IntArray(gridCols * gridRows)
            val cellWidth = width / gridCols
            val cellHeight = height / gridRows

            // Sample each grid cell center luminance
            for (row in 0 until gridRows) {
                val sampleY = (row * cellHeight) + (cellHeight / 2)
                for (col in 0 until gridCols) {
                    val sampleX = (col * cellWidth) + (cellWidth / 2)
                    val offset = (sampleY * rowStride) + (sampleX * pixelStride)
                    if (offset < yBuffer.remaining()) {
                        val lum = yBuffer.get(offset).toInt() and 0xFF
                        currentGrid[row * gridCols + col] = lum
                    }
                }
            }

            val prev = previousGrid
            if (prev == null) {
                previousGrid = currentGrid
                return
            }

            // Difference calculation
            var changedCells = 0
            val detectedRegions = mutableListOf<RectF>()

            // Luminance delta threshold per cell (filters out minor sensor noise)
            val luminanceDeltaThreshold = 22

            for (row in 0 until gridRows) {
                for (col in 0 until gridCols) {
                    val idx = row * gridCols + col
                    val diff = abs(currentGrid[idx] - prev[idx])
                    if (diff > luminanceDeltaThreshold) {
                        changedCells++
                        detectedRegions.add(
                            RectF(
                                col.toFloat() / gridCols,
                                row.toFloat() / gridRows,
                                (col + 1).toFloat() / gridCols,
                                (row + 1).toFloat() / gridRows
                            )
                        )
                    }
                }
            }

            previousGrid = currentGrid

            val totalCells = gridCols * gridRows
            val rawScore = (changedCells.toFloat() / totalCells) * 100f

            // Smooth motion score
            currentMotionScore = (currentMotionScore * 0.4f) + (rawScore * 0.6f)
            activeRegions = if (settings.showBoundingBoxes) detectedRegions else emptyList()

            // Check against user sensitivity setting
            // Sensitivity 100% means trigger easily (low threshold ~5%)
            // Sensitivity 1% means trigger only on massive movement (threshold ~70%)
            val dynamicThreshold = (105 - settings.sensitivity).coerceIn(5, 80).toFloat()

            val now = System.currentTimeMillis()
            if (currentMotionScore >= dynamicThreshold) {
                if (motionActiveStartTime == 0L) {
                    motionActiveStartTime = now
                } else {
                    val duration = now - motionActiveStartTime
                    val cooldownMs = settings.cooldownSeconds * 1000L
                    if (duration >= settings.minTriggerDurationMs && (now - lastTriggeredTimestamp >= cooldownMs)) {
                        lastTriggeredTimestamp = now
                        onMotionDetected(currentMotionScore, activeRegions)
                    }
                }
            } else {
                motionActiveStartTime = 0L
            }

        } catch (e: Exception) {
            // Frame analysis catch
        }
    }
}
