package com.example.myandroidapp.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.data.model.StudyTask
import com.example.myandroidapp.data.repository.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProgressUiState(
    val subjects: List<Subject> = emptyList(),
    val allTasks: List<StudyTask> = emptyList(),
    val overallProgress: Float = 0f,
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val selectedFilter: String = "All Subjects",
    val showAddSubjectDialog: Boolean = false,
    val editingSubject: Subject? = null,
    val showDeleteConfirmation: Boolean = false,
    val subjectToDelete: Subject? = null,
    val showAddTaskDialog: Boolean = false
)

class ProgressViewModel(private val repository: StudyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadProgressData()
    }

    private fun loadProgressData() {
        viewModelScope.launch {
            combine(
                repository.allSubjects,
                repository.allTasks,
                repository.completedCount,
                repository.totalTaskCount
            ) { subjects, tasks, completed, total ->
                val progress = if (total > 0) completed.toFloat() / total else 0f
                _uiState.value.copy(
                    subjects = subjects,
                    allTasks = tasks,
                    overallProgress = progress,
                    completedTasks = completed,
                    totalTasks = total
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    // ── Task Management ──
    fun showAddTaskDialog() {
        _uiState.update { it.copy(showAddTaskDialog = true) }
    }

    fun dismissAddTaskDialog() {
        _uiState.update { it.copy(showAddTaskDialog = false) }
    }

    fun addTask(task: StudyTask) {
        viewModelScope.launch {
            repository.insertTask(task)
            _uiState.update { it.copy(showAddTaskDialog = false) }
        }
    }

    // ── Subject Management ──
    fun showAddSubjectDialog() {
        _uiState.update { it.copy(showAddSubjectDialog = true, editingSubject = null) }
    }

    fun showEditSubjectDialog(subject: Subject) {
        _uiState.update { it.copy(showAddSubjectDialog = true, editingSubject = subject) }
    }

    fun dismissSubjectDialog() {
        _uiState.update { it.copy(showAddSubjectDialog = false, editingSubject = null) }
    }

    fun saveSubject(subject: Subject) {
        viewModelScope.launch {
            if (subject.id == 0L) {
                repository.insertSubject(subject)
            } else {
                repository.updateSubject(subject)
            }
            _uiState.update { it.copy(showAddSubjectDialog = false, editingSubject = null) }
        }
    }

    fun showDeleteConfirmation(subject: Subject) {
        _uiState.update { it.copy(showDeleteConfirmation = true, subjectToDelete = subject) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false, subjectToDelete = null) }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            _uiState.update { it.copy(showDeleteConfirmation = false, subjectToDelete = null) }
        }
    }
}

class ProgressViewModelFactory(private val repository: StudyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgressViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
