package com.example.model

enum class Resolution(
    val width: Int,
    val height: Int,
    val label: String,
    val recommendedFps: Int
) {
    LOW_360P(640, 360, "360p (Fastest)", 30),
    SD_480P(854, 480, "480p (Balanced)", 30),
    HD_720P(1280, 720, "720p HD (Optimal)", 25),
    FHD_1080P(1920, 1080, "1080p FHD (High Res)", 20)
}
