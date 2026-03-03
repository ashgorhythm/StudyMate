package com.example.myandroidapp.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.model.StudyTask
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.data.preferences.UserPreferences
import com.example.myandroidapp.data.repository.StudyRepository
import com.example.myandroidapp.service.TaskReminderManager
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
    // New dashboard fields
    val isCloudBackupEnabled: Boolean = true,
    val upcomingDeadlines: Int = 3,
    val focusHoursTarget: Float = 40f,
    val focusHoursCurrent: Float = 33.8f,
    val weeklyTasksCompleted: Int = 5,
    val weeklyTasksTotal: Int = 7,
    val weeklyFocusHours: Float = 20.5f,
    val pendingExams: Int = 0,
    val pendingAssignments: Int = 5,
    val studyPlanSet: Boolean = false,
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
    private val appContext: Context = context.applicationContext

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
            val savedId: Long
            if (task.id == 0L) {
                savedId = repository.insertTask(task)
            } else {
                repository.updateTask(task)
                savedId = task.id
                // Cancel old reminder before rescheduling
                TaskReminderManager.cancelReminder(appContext, task.id)
            }
            // Schedule reminder 1 hour before due
            TaskReminderManager.scheduleReminder(
                context = appContext,
                taskId = savedId,
                taskTitle = task.title,
                dueDateMillis = task.dueDate
            )
            _uiState.update { it.copy(showAddTaskDialog = false, editingTask = null) }
        }
    }

    fun deleteTask(task: StudyTask) {
        viewModelScope.launch {
            TaskReminderManager.cancelReminder(appContext, task.id)
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
