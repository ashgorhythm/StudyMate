package com.example.myandroidapp.data.local

import androidx.room.*
import com.example.myandroidapp.data.model.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("UPDATE subjects SET completedTopics = completedTopics + 1 WHERE id = :subjectId")
    suspend fun incrementCompletedTopics(subjectId: Long)

    @Query("UPDATE subjects SET totalStudyMinutes = totalStudyMinutes + :minutes WHERE id = :subjectId")
    suspend fun addStudyMinutes(subjectId: Long, minutes: Long)
}
