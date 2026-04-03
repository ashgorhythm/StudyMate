package com.example.myandroidapp.data.repository

import com.example.myandroidapp.data.local.StudyTaskDao
import com.example.myandroidapp.data.local.SubjectDao
import com.example.myandroidapp.data.local.StudySessionDao
import com.example.myandroidapp.data.local.StudyFileDao
import com.example.myandroidapp.data.model.StudyTask
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.data.model.StudySession
import com.example.myandroidapp.data.model.StudyFile
import kotlinx.coroutines.flow.Flow

class StudyRepository(
    private val taskDao: StudyTaskDao,
    private val subjectDao: SubjectDao,
    private val sessionDao: StudySessionDao,
    private val fileDao: StudyFileDao
) {
    // ── Tasks ──
    val allTasks: Flow<List<StudyTask>> = taskDao.getAllTasks()
    val pendingTasks: Flow<List<StudyTask>> = taskDao.getPendingTasks()
    val urgentTasks: Flow<List<StudyTask>> = taskDao.getUrgentTasks()
    val highPriorityTasks: Flow<List<StudyTask>> = taskDao.getHighPriorityTasks()
    val completedCount: Flow<Int> = taskDao.getCompletedCount()
    val totalTaskCount: Flow<Int> = taskDao.getTotalCount()

    fun getTasksBySubject(subject: String) = taskDao.getTasksBySubject(subject)

    suspend fun insertTask(task: StudyTask): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: StudyTask) = taskDao.updateTask(task)
    suspend fun deleteTask(task: StudyTask) = taskDao.deleteTask(task)
    suspend fun toggleTaskCompletion(taskId: Long, completed: Boolean) =
        taskDao.toggleTaskCompletion(taskId, completed)

    // ── Subjects ──
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()

    suspend fun insertSubject(subject: Subject) = subjectDao.insertSubject(subject)
    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)
    suspend fun deleteSubject(subject: Subject) = subjectDao.deleteSubject(subject)
    suspend fun getSubjectById(id: Long) = subjectDao.getSubjectById(id)
    suspend fun incrementCompletedTopics(subjectId: Long) = subjectDao.incrementCompletedTopics(subjectId)
    suspend fun addStudyMinutes(subjectId: Long, minutes: Long) = subjectDao.addStudyMinutes(subjectId, minutes)

    // ── Study Sessions ──
    val allSessions: Flow<List<StudySession>> = sessionDao.getAllSessions()
    val totalStudyMinutes: Flow<Int?> = sessionDao.getTotalStudyMinutes()

    fun getTodaySessions(startOfDay: Long) = sessionDao.getTodaySessions(startOfDay)
    fun getTodaySessionCount(startOfDay: Long) = sessionDao.getTodaySessionCount(startOfDay)

    suspend fun insertSession(session: StudySession) = sessionDao.insertSession(session)
    suspend fun updateSession(session: StudySession) = sessionDao.updateSession(session)

    // ── Files ──
    val allFiles: Flow<List<StudyFile>> = fileDao.getAllFiles()
    val favoriteFiles: Flow<List<StudyFile>> = fileDao.getFavoriteFiles()

    fun getFilesByType(type: String) = fileDao.getFilesByType(type)
    fun searchFiles(query: String) = fileDao.searchFiles(query)

    suspend fun insertFile(file: StudyFile) = fileDao.insertFile(file)
    suspend fun updateFile(file: StudyFile) = fileDao.updateFile(file)
    suspend fun deleteFile(file: StudyFile) = fileDao.deleteFile(file)
    suspend fun toggleFileFavorite(fileId: Long, fav: Boolean) = fileDao.toggleFavorite(fileId, fav)
}
