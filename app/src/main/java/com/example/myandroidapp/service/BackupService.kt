package com.example.myandroidapp.service

import android.content.Context
import android.net.Uri
import com.example.myandroidapp.data.local.StudyFileDao
import com.example.myandroidapp.data.local.StudySessionDao
import com.example.myandroidapp.data.local.StudyTaskDao
import com.example.myandroidapp.data.local.SubjectDao
import com.example.myandroidapp.data.model.StudyFile
import com.example.myandroidapp.data.model.StudySession
import com.example.myandroidapp.data.model.StudyTask
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles all data export (backup) and import (restore) operations.
 * Uses JSON format — can be saved to Google Drive via SAF.
 */
class BackupService(
    private val taskDao: StudyTaskDao,
    private val subjectDao: SubjectDao,
    private val sessionDao: StudySessionDao,
    private val fileDao: StudyFileDao
) {
    companion object {
        const val BACKUP_VERSION = 1
        const val BACKUP_FILE_NAME = "studymate_backup.json"
        const val BACKUP_MIME_TYPE = "application/json"
    }

    // ═══════════════════════════════════════════════════════
    // ── Export ──
    // ═══════════════════════════════════════════════════════

    /**
     * Exports all data to a JSON string.
     */
    suspend fun exportToJson(context: Context): String {
        val prefs = UserPreferences(context)
        val name = prefs.studentName.first()

        val root = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("appName", "StudyMate")
            put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(Date()))
            put("studentName", name)
            put("tasks", tasksToJson(taskDao.getAllTasksOnce()))
            put("subjects", subjectsToJson(subjectDao.getAllSubjectsOnce()))
            put("sessions", sessionsToJson(sessionDao.getAllSessionsOnce()))
            put("files", filesToJson(fileDao.getAllFilesOnce()))
        }
        return root.toString(2) // pretty-printed
    }

    /**
     * Writes exported JSON to a URI (chosen via SAF — could be Google Drive, local, etc.)
     */
    suspend fun exportToUri(context: Context, uri: Uri): Result<Int> {
        return try {
            val json = exportToJson(context)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray(Charsets.UTF_8))
            }
            val root = JSONObject(json)
            val total = root.getJSONArray("tasks").length() +
                    root.getJSONArray("subjects").length() +
                    root.getJSONArray("sessions").length() +
                    root.getJSONArray("files").length()
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════
    // ── Import ──
    // ═══════════════════════════════════════════════════════

    /**
     * Reads JSON from a URI and restores all data (replaces existing).
     */
    suspend fun importFromUri(context: Context, uri: Uri): Result<ImportResult> {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return Result.failure(Exception("Could not read file"))

            val result = importFromJson(context, json)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports data from a JSON string — clears existing data and replaces it.
     */
    suspend fun importFromJson(context: Context, json: String): ImportResult {
        val root = JSONObject(json)

        // Validate
        val version = root.optInt("version", -1)
        if (version < 1) throw IllegalArgumentException("Invalid backup file")

        // Restore student name
        val studentName = root.optString("studentName", "Student")
        val prefs = UserPreferences(context)
        prefs.updateName(studentName)

        // Clear and restore tasks
        val tasks = jsonToTasks(root.getJSONArray("tasks"))
        taskDao.deleteAllTasks()
        taskDao.insertAll(tasks)

        // Clear and restore subjects
        val subjects = jsonToSubjects(root.getJSONArray("subjects"))
        subjectDao.deleteAllSubjects()
        subjectDao.insertAll(subjects)

        // Clear and restore sessions
        val sessions = jsonToSessions(root.getJSONArray("sessions"))
        sessionDao.deleteAllSessions()
        sessionDao.insertAll(sessions)

        // Clear and restore files (metadata only)
        val files = jsonToFiles(root.getJSONArray("files"))
        fileDao.deleteAllFiles()
        fileDao.insertAll(files)

        return ImportResult(
            tasksCount = tasks.size,
            subjectsCount = subjects.size,
            sessionsCount = sessions.size,
            filesCount = files.size,
            studentName = studentName
        )
    }

    // ═══════════════════════════════════════════════════════
    // ── Streak Calculation ──
    // ═══════════════════════════════════════════════════════

    suspend fun calculateStreak(): Int {
        val studyDays = sessionDao.getStudyDayTimestamps()
        if (studyDays.isEmpty()) return 0

        val todayDay = System.currentTimeMillis() / 86400000L
        var streak = 0
        var expectedDay = todayDay

        for (day in studyDays) {
            when {
                day == expectedDay -> {
                    streak++
                    expectedDay--
                }
                day == expectedDay - 1 -> {
                    // Allow checking if today hasn't been studied yet
                    if (streak == 0) {
                        streak++
                        expectedDay = day - 1
                    } else break
                }
                day < expectedDay -> break
            }
        }
        return streak
    }

    // ═══════════════════════════════════════════════════════
    // ── Serialization Helpers ──
    // ═══════════════════════════════════════════════════════

    private fun tasksToJson(tasks: List<StudyTask>): JSONArray {
        return JSONArray().apply {
            tasks.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("title", t.title); put("subject", t.subject)
                    put("description", t.description); put("isCompleted", t.isCompleted)
                    put("dueDate", t.dueDate); put("priority", t.priority); put("createdAt", t.createdAt)
                })
            }
        }
    }

    private fun subjectsToJson(subjects: List<Subject>): JSONArray {
        return JSONArray().apply {
            subjects.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("name", s.name); put("icon", s.icon)
                    put("colorHex", s.colorHex); put("totalTopics", s.totalTopics)
                    put("completedTopics", s.completedTopics); put("totalStudyMinutes", s.totalStudyMinutes)
                })
            }
        }
    }

    private fun sessionsToJson(sessions: List<StudySession>): JSONArray {
        return JSONArray().apply {
            sessions.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("subjectId", s.subjectId); put("subjectName", s.subjectName)
                    put("durationMinutes", s.durationMinutes); put("startTime", s.startTime)
                    put("endTime", s.endTime); put("isCompleted", s.isCompleted)
                })
            }
        }
    }

    private fun filesToJson(files: List<StudyFile>): JSONArray {
        return JSONArray().apply {
            files.forEach { f ->
                put(JSONObject().apply {
                    put("id", f.id); put("fileName", f.fileName); put("filePath", f.filePath)
                    put("fileType", f.fileType); put("subject", f.subject)
                    put("fileSize", f.fileSize); put("isFavorite", f.isFavorite); put("addedAt", f.addedAt)
                })
            }
        }
    }

    private fun jsonToTasks(arr: JSONArray): List<StudyTask> = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        StudyTask(
            id = o.getLong("id"), title = o.getString("title"), subject = o.getString("subject"),
            description = o.optString("description", ""), isCompleted = o.getBoolean("isCompleted"),
            dueDate = o.getLong("dueDate"), priority = o.optInt("priority", 0),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun jsonToSubjects(arr: JSONArray): List<Subject> = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Subject(
            id = o.getLong("id"), name = o.getString("name"), icon = o.optString("icon", "📚"),
            colorHex = o.optString("colorHex", "#13ECEC"), totalTopics = o.optInt("totalTopics", 0),
            completedTopics = o.optInt("completedTopics", 0), totalStudyMinutes = o.optLong("totalStudyMinutes", 0)
        )
    }

    private fun jsonToSessions(arr: JSONArray): List<StudySession> = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        StudySession(
            id = o.getLong("id"), subjectId = o.getLong("subjectId"),
            subjectName = o.getString("subjectName"), durationMinutes = o.getInt("durationMinutes"),
            startTime = o.getLong("startTime"), endTime = o.optLong("endTime", 0),
            isCompleted = o.optBoolean("isCompleted", false)
        )
    }

    private fun jsonToFiles(arr: JSONArray): List<StudyFile> = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        StudyFile(
            id = o.getLong("id"), fileName = o.getString("fileName"), filePath = o.optString("filePath", ""),
            fileType = o.optString("fileType", "NOTE"), subject = o.optString("subject", ""),
            fileSize = o.optLong("fileSize", 0), isFavorite = o.optBoolean("isFavorite", false),
            addedAt = o.optLong("addedAt", System.currentTimeMillis())
        )
    }
}

data class ImportResult(
    val tasksCount: Int,
    val subjectsCount: Int,
    val sessionsCount: Int,
    val filesCount: Int,
    val studentName: String
) {
    val totalItems: Int get() = tasksCount + subjectsCount + sessionsCount + filesCount
}
