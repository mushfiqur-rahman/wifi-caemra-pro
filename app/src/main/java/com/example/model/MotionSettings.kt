package com.example.model

data class MotionSettings(
    val sensitivity: Int = 35, // 1 to 100, threshold percentage
    val minTriggerDurationMs: Long = 300L,
    val cooldownSeconds: Int = 4,
    val triggerAlertSound: Boolean = true,
    val autoCaptureSnapshot: Boolean = true,
    val autoCloudUpload: Boolean = true,
    val showBoundingBoxes: Boolean = true
)
