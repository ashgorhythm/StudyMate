package com.example.myandroidapp.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.model.StudyTask
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.data.preferences.UserPreferences
import com.example.myandroidapp.data.repository.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val urgentTasks: List<StudyTask> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val totalStudyMinutes: Int = 0,
    val streakDays: Int = 7, // placeholder
    val overallProgress: Float = 0f,
    val studentName: String = "Student",
    // Task dialog state
    val showAddTaskDialog: Boolean = false,
    val editingTask: StudyTask? = null,
    // Subject dialog state (merged from Progress)
    val showAddSubjectDialog: Boolean = false,
    val editingSubject: Subject? = null,
    val showDeleteConfirmation: Boolean = false,
    val subjectToDelete: Subject? = null
)

class DashboardViewModel(
    private val repository: StudyRepository,
    context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val userPreferences = UserPreferences(context)

    init {
        loadDashboardData()
        loadStudentName()
    }

    private fun loadStudentName() {
        viewModelScope.launch {
            userPreferences.studentName.collect { name ->
                _uiState.update { it.copy(studentName = name) }
            }
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                repository.urgentTasks,
                repository.allSubjects,
                repository.completedCount,
                repository.totalTaskCount,
                repository.totalStudyMinutes
            ) { tasks, subjects, completed, total, minutes ->
                val progress = if (total > 0) completed.toFloat() / total else 0f
                _uiState.value.copy(
                    urgentTasks = tasks,
                    subjects = subjects,
                    completedTasks = completed,
                    totalTasks = total,
                    totalStudyMinutes = minutes ?: 0,
                    overallProgress = progress
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    // ── Task Management ──
    fun toggleTaskCompletion(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId, completed)
        }
    }

    fun showAddTaskDialog() {
        _uiState.update { it.copy(showAddTaskDialog = true, editingTask = null) }
    }

    fun showEditTaskDialog(task: StudyTask) {
        _uiState.update { it.copy(showAddTaskDialog = true, editingTask = task) }
    }

    fun dismissTaskDialog() {
        _uiState.update { it.copy(showAddTaskDialog = false, editingTask = null) }
    }

    fun saveTask(task: StudyTask) {
        viewModelScope.launch {
            if (task.id == 0L) {
                repository.insertTask(task)
            } else {
                repository.updateTask(task)
            }
            _uiState.update { it.copy(showAddTaskDialog = false, editingTask = null) }
        }
    }

    fun deleteTask(task: StudyTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // ── Subject Management (merged from Progress) ──
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

    // ── Sample Data ──
    fun addSampleData() {
        viewModelScope.launch {
            val subjects = listOf(
                Subject(name = "Mathematics", icon = "📐", colorHex = "#13ECEC", totalTopics = 15, completedTopics = 12, totalStudyMinutes = 480),
                Subject(name = "Physics", icon = "🔬", colorHex = "#7C4DFF", totalTopics = 12, completedTopics = 8, totalStudyMinutes = 360),
                Subject(name = "English", icon = "📖", colorHex = "#FFAB40", totalTopics = 10, completedTopics = 7, totalStudyMinutes = 240),
                Subject(name = "History", icon = "🏛️", colorHex = "#FF4081", totalTopics = 8, completedTopics = 5, totalStudyMinutes = 180)
            )
            subjects.forEach { repository.insertSubject(it) }

            val tasks = listOf(
                StudyTask(title = "Complete Calculus Ch.5", subject = "Mathematics", priority = 2, dueDate = System.currentTimeMillis() + 3600000),
                StudyTask(title = "Physics Lab Report", subject = "Physics", priority = 2, dueDate = System.currentTimeMillis() + 7200000),
                StudyTask(title = "Essay Draft Review", subject = "English", priority = 1, dueDate = System.currentTimeMillis() + 86400000),
                StudyTask(title = "History Timeline", subject = "History", priority = 0, dueDate = System.currentTimeMillis() + 172800000),
                StudyTask(title = "Trigonometry Quiz Prep", subject = "Mathematics", priority = 1, dueDate = System.currentTimeMillis() + 43200000)
            )
            tasks.forEach { repository.insertTask(it) }
        }
    }
}

class DashboardViewModelFactory(
    private val repository: StudyRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
