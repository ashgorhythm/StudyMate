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
    val selectedFilter: String = "All Subjects"
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
                ProgressUiState(
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

    fun addTask(task: StudyTask) {
        viewModelScope.launch { repository.insertTask(task) }
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
