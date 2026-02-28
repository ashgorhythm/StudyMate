package com.example.myandroidapp.ui.screens.settings

import android.content.Context
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    val totalFiles: Int = 0
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
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            prefs.updateName(name)
            _uiState.update { it.copy(studentName = name) }
        }
    }

    fun exportToUri(uri: android.net.Uri) {
        _uiState.update { it.copy(isExporting = true, exportSuccess = false, errorMessage = null) }
        viewModelScope.launch {
            val result = backupService.exportToUri(context, uri)
            result.fold(
                onSuccess = { count ->
                    _uiState.update { it.copy(isExporting = false, exportSuccess = true, errorMessage = null) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isExporting = false, errorMessage = "Export failed: ${e.localizedMessage}") }
                }
            )
        }
    }

    fun importFromUri(uri: android.net.Uri) {
        _uiState.update { it.copy(isImporting = true, importResult = null, errorMessage = null) }
        viewModelScope.launch {
            val result = backupService.importFromUri(context, uri)
            result.fold(
                onSuccess = { importResult ->
                    _uiState.update { it.copy(isImporting = false, importResult = importResult, errorMessage = null) }
                    loadData() // reload counts
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isImporting = false, errorMessage = "Import failed: ${e.localizedMessage}") }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(exportSuccess = false, importResult = null, errorMessage = null) }
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
