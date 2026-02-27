package com.example.myandroidapp.data.local

import androidx.room.*
import com.example.myandroidapp.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE startTime >= :startOfDay ORDER BY startTime DESC")
    fun getTodaySessions(startOfDay: Long): Flow<List<StudySession>>

    @Query("SELECT SUM(durationMinutes) FROM study_sessions WHERE isCompleted = 1")
    fun getTotalStudyMinutes(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM study_sessions WHERE isCompleted = 1 AND startTime >= :startOfDay")
    fun getTodaySessionCount(startOfDay: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Update
    suspend fun updateSession(session: StudySession)

    @Delete
    suspend fun deleteSession(session: StudySession)
}
