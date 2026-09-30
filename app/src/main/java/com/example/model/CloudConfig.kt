package com.example.model

enum class CloudProvider(val displayName: String) {
    S3_COMPATIBLE("S3 / Cloudflare R2 / MinIO"),
    WEBHOOK("HTTP Webhook / REST Endpoint"),
    GOOGLE_DRIVE("Google Drive / Cloud Functions")
}

data class CloudConfig(
    val provider: CloudProvider = CloudProvider.WEBHOOK,
    val endpointUrl: String = "https://api.cloudstorage.example.com/v1/motion-events",
    val apiToken: String = "",
    val bucketOrFolderName: String = "wificam-recordings",
    val autoSyncEnabled: Boolean = true,
    val lastSyncStatus: String = "Idle",
    val lastSyncTime: Long = 0L
)
