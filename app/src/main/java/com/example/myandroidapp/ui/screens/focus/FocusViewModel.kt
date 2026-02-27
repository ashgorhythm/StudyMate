package com.example.myandroidapp.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.model.StudySession
import com.example.myandroidapp.data.repository.StudyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class FocusUiState(
    val timerSeconds: Int = 25 * 60, // default 25 min
    val totalSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val selectedDuration: Int = 25, // minutes
    val currentSubject: String = "Select Subject",
    val todaySessionCount: Int = 0,
    val todayTotalMinutes: Int = 0,
    val selectedAmbientSound: String = "Silence"
)

class FocusViewModel(private val repository: StudyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadTodayStats()
    }

    private fun loadTodayStats() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis

        viewModelScope.launch {
            repository.getTodaySessionCount(startOfDay).collect { count ->
                _uiState.update { it.copy(todaySessionCount = count) }
            }
        }
    }

    fun setDuration(minutes: Int) {
        if (!_uiState.value.isRunning) {
            _uiState.update {
                it.copy(
                    selectedDuration = minutes,
                    timerSeconds = minutes * 60,
                    totalSeconds = minutes * 60
                )
            }
        }
    }

    fun setSubject(subject: String) {
        _uiState.update { it.copy(currentSubject = subject) }
    }

    fun setAmbientSound(sound: String) {
        _uiState.update { it.copy(selectedAmbientSound = sound) }
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        _uiState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerSeconds > 0 && _uiState.value.isRunning) {
                delay(1000)
                _uiState.update { it.copy(timerSeconds = it.timerSeconds - 1) }
            }
            if (_uiState.value.timerSeconds <= 0) {
                completeSession()
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isRunning = false,
                timerSeconds = it.selectedDuration * 60,
                totalSeconds = it.selectedDuration * 60
            )
        }
    }

    private fun completeSession() {
        _uiState.update { it.copy(isRunning = false) }
        viewModelScope.launch {
            val session = StudySession(
                subjectId = 0,
                subjectName = _uiState.value.currentSubject,
                durationMinutes = _uiState.value.selectedDuration,
                startTime = System.currentTimeMillis() - (_uiState.value.selectedDuration * 60 * 1000L),
                endTime = System.currentTimeMillis(),
                isCompleted = true
            )
            repository.insertSession(session)
        }
        resetTimer()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

class FocusViewModelFactory(private val repository: StudyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FocusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FocusViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
