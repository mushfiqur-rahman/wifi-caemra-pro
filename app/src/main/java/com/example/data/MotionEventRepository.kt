package com.example.data

import com.example.model.CloudConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class MotionEventRepository(
    private val dao: MotionEventDao,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    val allEvents: Flow<List<MotionEventEntity>> = dao.getAllEvents()

    suspend fun recordMotionEvent(event: MotionEventEntity): Long = withContext(Dispatchers.IO) {
        dao.insertEvent(event)
    }

    suspend fun deleteEvent(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteEventById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAllEvents()
    }

    suspend fun uploadPendingEvents(config: CloudConfig): Pair<Int, String> = withContext(Dispatchers.IO) {
        val pending = dao.getPendingUploads()
        if (pending.isEmpty()) {
            return@withContext Pair(0, "No pending motion events to sync")
        }

        var uploadedCount = 0
        var lastError = ""

        for (item in pending) {
            val success = tryUploadEvent(item, config)
            if (success) {
                uploadedCount++
                dao.updateEvent(item.copy(cloudSyncStatus = "SYNCED"))
            } else {
                dao.updateEvent(item.copy(cloudSyncStatus = "FAILED"))
                lastError = "Upload failed for event #${item.id}"
            }
        }

        val msg = if (uploadedCount > 0) "Synced $uploadedCount event(s) to cloud" else lastError
        Pair(uploadedCount, msg)
    }

    private suspend fun tryUploadEvent(event: MotionEventEntity, config: CloudConfig): Boolean {
        return try {
            val file = event.snapshotFilePath?.let { File(it) }
            val requestBuilder = Request.Builder().url(config.endpointUrl)

            if (config.apiToken.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${config.apiToken}")
            }

            val body = if (file != null && file.exists()) {
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("eventId", event.id.toString())
                    .addFormDataPart("timestamp", event.timestamp.toString())
                    .addFormDataPart("motionScore", event.peakMotionScore.toString())
                    .addFormDataPart("bucket", config.bucketOrFolderName)
                    .addFormDataPart(
                        "snapshot",
                        file.name,
                        file.asRequestBody(mediaType)
                    )
                    .build()
            } else {
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("eventId", event.id.toString())
                    .addFormDataPart("timestamp", event.timestamp.toString())
                    .addFormDataPart("motionScore", event.peakMotionScore.toString())
                    .build()
            }

            val response = okHttpClient.newCall(requestBuilder.post(body).build()).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
