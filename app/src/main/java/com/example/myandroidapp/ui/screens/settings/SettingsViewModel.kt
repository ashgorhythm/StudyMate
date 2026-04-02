@file:Suppress("DEPRECATION")
package com.example.myandroidapp.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.local.StudyFileDao
import com.example.myandroidapp.data.local.StudySessionDao
import com.example.myandroidapp.data.local.StudyTaskDao
import com.example.myandroidapp.data.local.SubjectDao
import com.example.myandroidapp.data.preferences.UserPreferences
import com.example.myandroidapp.service.BackupService
import com.example.myandroidapp.service.ImportResult
import com.example.myandroidapp.service.GoogleDriveService
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsUiState(
    val studentName: String = "Student",
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val importResult: ImportResult? = null,
    val errorMessage: String? = null,
    val streak: Int = 0,
    val totalTasks: Int = 0,
    val totalSubjects: Int = 0,
    val totalSessions: Int = 0,
    val totalFiles: Int = 0,
    // Security settings
    val requirePin: Boolean = false,
    val lockDuringFocus: Boolean = true,
    val dndDuringFocus: Boolean = true,
    // Beta
    val betaEnrolled: Boolean = false,
    // CSV
    val csvExportSuccess: Boolean = false,
    // Backup history
    val backupHistory: List<String> = emptyList()
)

class SettingsViewModel(
    private val taskDao: StudyTaskDao,
    private val subjectDao: SubjectDao,
    private val sessionDao: StudySessionDao,
    private val fileDao: StudyFileDao,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val backupService = BackupService(taskDao, subjectDao, sessionDao, fileDao)
    private val prefs = UserPreferences(context)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            prefs.studentName.collect { name ->
                _uiState.update { it.copy(studentName = name) }
            }
        }
        viewModelScope.launch {
            val streak = backupService.calculateStreak()
            _uiState.update { it.copy(streak = streak) }
        }
        viewModelScope.launch {
            combine(
                taskDao.getTotalCount(),
                subjectDao.getAllSubjects(),
                sessionDao.getAllSessions(),
                fileDao.getAllFiles()
            ) { tasks, subjects, sessions, files ->
                _uiState.value.copy(
                    totalTasks = tasks,
                    totalSubjects = subjects.size,
                    totalSessions = sessions.size,
                    totalFiles = files.size
                )
            }.collect { state -> _uiState.value = state }
        }
        // Security settings
        viewModelScope.launch {
            prefs.requirePin.collect { v -> _uiState.update { it.copy(requirePin = v) } }
        }
        viewModelScope.launch {
            prefs.lockDuringFocus.collect { v -> _uiState.update { it.copy(lockDuringFocus = v) } }
        }
        viewModelScope.launch {
            prefs.dndDuringFocus.collect { v -> _uiState.update { it.copy(dndDuringFocus = v) } }
        }
        // Beta
        viewModelScope.launch {
            prefs.betaEnrolled.collect { v -> _uiState.update { it.copy(betaEnrolled = v) } }
        }
        // Backup history
        viewModelScope.launch {
            prefs.backupHistoryJson.collect { json ->
                try {
                    val arr = JSONArray(json)
                    val list = (0 until arr.length()).map { arr.getString(it) }
                    _uiState.update { it.copy(backupHistory = list) }
                } catch (_: Exception) {
                    _uiState.update { it.copy(backupHistory = emptyList()) }
                }
            }
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            prefs.updateName(name)
            _uiState.update { it.copy(studentName = name) }
        }
    }

    // ── Security Settings ──

    fun updateRequirePin(require: Boolean) {
        viewModelScope.launch { prefs.updateRequirePin(require) }
    }

    fun updateLockDuringFocus(lock: Boolean) {
        viewModelScope.launch { prefs.updateLockDuringFocus(lock) }
    }

    fun updateDndDuringFocus(dnd: Boolean) {
        viewModelScope.launch { prefs.updateDndDuringFocus(dnd) }
    }

    // ── Beta ──

    fun toggleBetaEnrollment() {
        viewModelScope.launch {
            val current = _uiState.value.betaEnrolled
            prefs.updateBetaEnrolled(!current)
        }
    }

    // ── Export / Import ──

    fun exportToUri(uri: Uri) {
        _uiState.update { it.copy(isExporting = true, exportSuccess = false, errorMessage = null) }
        viewModelScope.launch {
            val result = backupService.exportToUri(context, uri)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isExporting = false, exportSuccess = true, errorMessage = null) }
                    addBackupHistoryEntry("Export: Local backup created")
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isExporting = false, errorMessage = "Export failed: ${e.localizedMessage}") }
                }
            )
        }
    }

    fun importFromUri(uri: Uri) {
        _uiState.update { it.copy(isImporting = true, importResult = null, errorMessage = null) }
        viewModelScope.launch {
            val result = backupService.importFromUri(context, uri)
            result.fold(
                onSuccess = { importResult ->
                    _uiState.update { it.copy(isImporting = false, importResult = importResult, errorMessage = null) }
                    addBackupHistoryEntry("Import: Restored ${importResult.totalItems} items")
                    loadData()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isImporting = false, errorMessage = "Import failed: ${e.localizedMessage}") }
                }
            )
        }
    }

    // ── CSV Export ──

    fun exportToCsv(uri: Uri) {
        _uiState.update { it.copy(csvExportSuccess = false, errorMessage = null) }
        viewModelScope.launch {
            try {
                val tasks = taskDao.getAllTasksOnce()
                val subjects = subjectDao.getAllSubjectsOnce()
                val sessions = sessionDao.getAllSessionsOnce()

                val sb = StringBuilder()

                // Tasks section
                sb.appendLine("=== TASKS ===")
                sb.appendLine("ID,Title,Subject,Description,Completed,Due Date,Priority,Created At")
                tasks.forEach { t ->
                    sb.appendLine("${t.id},\"${t.title}\",\"${t.subject}\",\"${t.description}\",${t.isCompleted},${t.dueDate},${t.priority},${t.createdAt}")
                }
                sb.appendLine()

                // Subjects section
                sb.appendLine("=== SUBJECTS ===")
                sb.appendLine("ID,Name,Icon,Color,Total Topics,Completed Topics,Study Minutes")
                subjects.forEach { s ->
                    sb.appendLine("${s.id},\"${s.name}\",\"${s.icon}\",\"${s.colorHex}\",${s.totalTopics},${s.completedTopics},${s.totalStudyMinutes}")
                }
                sb.appendLine()

                // Sessions section
                sb.appendLine("=== STUDY SESSIONS ===")
                sb.appendLine("ID,Subject ID,Subject Name,Duration (min),Start Time,End Time,Completed")
                sessions.forEach { s ->
                    sb.appendLine("${s.id},${s.subjectId},\"${s.subjectName}\",${s.durationMinutes},${s.startTime},${s.endTime},${s.isCompleted}")
                }

                val outputStream = context.contentResolver.openOutputStream(uri, "w")
                    ?: throw Exception("Could not open file for writing")
                outputStream.use { stream ->
                    stream.write(sb.toString().toByteArray(Charsets.UTF_8))
                    stream.flush()
                }

                _uiState.update { it.copy(csvExportSuccess = true) }
                addBackupHistoryEntry("Export: CSV spreadsheet created")
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "CSV export failed: ${e.localizedMessage}") }
            }
        }
    }

    // ── Clear All Data ──

    fun clearAllData() {
        viewModelScope.launch {
            try {
                taskDao.deleteAllTasks()
                subjectDao.deleteAllSubjects()
                sessionDao.deleteAllSessions()
                fileDao.deleteAllFiles()
                prefs.clearAll()
                // Re-mark onboarding as complete so user isn't sent back
                prefs.completeOnboarding("Student")
                prefs.markFolderSetupShown()
                addBackupHistoryEntry("Clear: All data deleted")
                loadData()
                _uiState.update { it.copy(exportSuccess = false, importResult = null, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Clear failed: ${e.localizedMessage}") }
            }
        }
    }

    // ── Backup History ──

    private fun addBackupHistoryEntry(entry: String) {
        viewModelScope.launch {
            val timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
            val fullEntry = "$entry • $timestamp"
            val current = _uiState.value.backupHistory.toMutableList()
            current.add(fullEntry)
            // Keep last 20 entries
            val trimmed = current.takeLast(20)
            val jsonArray = JSONArray()
            trimmed.forEach { jsonArray.put(it) }
            prefs.updateBackupHistory(jsonArray.toString())
        }
    }

    // ── Google Drive ──

    fun uploadToGoogleDrive(account: GoogleSignInAccount) {
        _uiState.update { it.copy(isExporting = true, exportSuccess = false, errorMessage = null) }
        viewModelScope.launch {
            try {
                val jsonContent = backupService.exportToJson(context)
                val driveService = GoogleDriveService(context, account)
                val result = driveService.uploadBackup(jsonContent)

                if (result.isSuccess) {
                    _uiState.update { it.copy(isExporting = false, exportSuccess = true, errorMessage = null) }
                    addBackupHistoryEntry("Upload: Google Drive backup")
                } else {
                    _uiState.update { it.copy(isExporting = false, errorMessage = "Drive upload failed: ${result.exceptionOrNull()?.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, errorMessage = "Drive upload error: ${e.message}") }
            }
        }
    }

    fun downloadFromGoogleDrive(account: GoogleSignInAccount) {
        _uiState.update { it.copy(isImporting = true, importResult = null, errorMessage = null) }
        viewModelScope.launch {
            try {
                val driveService = GoogleDriveService(context, account)
                val downloadResult = driveService.downloadBackup()

                if (downloadResult.isSuccess) {
                    val jsonContent = downloadResult.getOrThrow()
                    val importResult = backupService.importFromJson(context, jsonContent)
                    _uiState.update { it.copy(isImporting = false, importResult = importResult, errorMessage = null) }
                    addBackupHistoryEntry("Download: Restored from Google Drive (${importResult.totalItems} items)")
                    loadData()
                } else {
                    _uiState.update { it.copy(isImporting = false, errorMessage = "Drive download failed: ${downloadResult.exceptionOrNull()?.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, errorMessage = "Drive download error: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(exportSuccess = false, importResult = null, errorMessage = null, csvExportSuccess = false) }
    }
}

class SettingsViewModelFactory(
    private val taskDao: StudyTaskDao,
    private val subjectDao: SubjectDao,
    private val sessionDao: StudySessionDao,
    private val fileDao: StudyFileDao,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(taskDao, subjectDao, sessionDao, fileDao, context) as T
    }
}
