package com.example.myandroidapp.data.local

import androidx.room.*
import com.example.myandroidapp.data.model.StudyTask
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyTaskDao {
    @Query("SELECT * FROM study_tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<StudyTask>>

    @Query("SELECT * FROM study_tasks WHERE isCompleted = 0 ORDER BY priority DESC, dueDate ASC")
    fun getPendingTasks(): Flow<List<StudyTask>>

    @Query("SELECT * FROM study_tasks WHERE isCompleted = 0 ORDER BY dueDate ASC LIMIT 5")
    fun getUrgentTasks(): Flow<List<StudyTask>>

    @Query("SELECT * FROM study_tasks WHERE isCompleted = 0 AND priority = 2 ORDER BY dueDate ASC LIMIT 10")
    fun getHighPriorityTasks(): Flow<List<StudyTask>>

    @Query("SELECT * FROM study_tasks WHERE subject = :subject")
    fun getTasksBySubject(subject: String): Flow<List<StudyTask>>

    @Query("SELECT COUNT(*) FROM study_tasks WHERE isCompleted = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_tasks")
    fun getTotalCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTask): Long

    @Update
    suspend fun updateTask(task: StudyTask)

    @Delete
    suspend fun deleteTask(task: StudyTask)

    @Query("UPDATE study_tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun toggleTaskCompletion(taskId: Long, completed: Boolean)

    // Backup/Restore
    @Query("SELECT * FROM study_tasks")
    suspend fun getAllTasksOnce(): List<StudyTask>

    @Query("DELETE FROM study_tasks")
    suspend fun deleteAllTasks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<StudyTask>)
}
