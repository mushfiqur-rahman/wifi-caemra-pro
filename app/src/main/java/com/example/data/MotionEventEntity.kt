package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "motion_events")
data class MotionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val peakMotionScore: Float, // 0.0 to 100.0%
    val snapshotFilePath: String?,
    val durationSeconds: Int = 1,
    val cloudSyncStatus: String = "PENDING", // PENDING, SYNCED, FAILED
    val cloudUploadUrl: String? = null
)
