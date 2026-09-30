package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MotionEventDao {
    @Query("SELECT * FROM motion_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<MotionEventEntity>>

    @Query("SELECT * FROM motion_events WHERE cloudSyncStatus = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingUploads(): List<MotionEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: MotionEventEntity): Long

    @Update
    suspend fun updateEvent(event: MotionEventEntity)

    @Query("DELETE FROM motion_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM motion_events")
    suspend fun clearAllEvents()
}
