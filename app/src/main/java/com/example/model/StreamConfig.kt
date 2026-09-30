package com.example.model

data class StreamConfig(
    val port: Int = 8080,
    val resolution: Resolution = Resolution.HD_720P,
    val targetFps: Int = 25,
    val jpegQuality: Int = 75,
    val audioEnabled: Boolean = true,
    val isFrontCamera: Boolean = false,
    val isTorchOn: Boolean = false,
    val isE2eeEnabled: Boolean = false,
    val e2eePasskey: String = "WiFiCamSecure123",
    val isScreenDimmed: Boolean = false,
    val isMotionDetectionEnabled: Boolean = true
)
