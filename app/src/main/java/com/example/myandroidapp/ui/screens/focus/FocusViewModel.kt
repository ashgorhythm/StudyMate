package com.example.myandroidapp.ui.screens.focus

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.model.AllowedContact
import com.example.myandroidapp.data.model.StudySession
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.data.repository.StudyRepository
import com.example.myandroidapp.service.AmbientSoundPlayer
import com.example.myandroidapp.service.FocusModeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class FocusUiState(
    val timerSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val selectedDuration: Int = 25, // minutes (0 = custom)
    val customDurationMinutes: Int = 25, // actual value when custom
    val currentSubject: String = "Select Subject",
    val todaySessionCount: Int = 0,
    val todayTotalMinutes: Int = 0,
    val selectedAmbientSound: String = "Silence",
    // Focus Mode Settings
    val isDndEnabled: Boolean = true,
    val hasDndPermission: Boolean = false,
    val allowedContacts: List<AllowedContact> = emptyList(),
    val showAllowedContactsDialog: Boolean = false,
    val showSubjectPicker: Boolean = false,
    val showCustomDurationDialog: Boolean = false,
    val subjects: List<Subject> = emptyList(),
    // App Lock state
    val isAppLocked: Boolean = false,
    val showExitBlockedSnack: Boolean = false
)

class FocusViewModel(private val repository: StudyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var appContext: Context? = null

    init {
        loadTodayStats()
        loadSubjects()
    }

    fun setAppContext(context: Context) {
        appContext = context.applicationContext
        checkDndPermission()
    }

    private fun checkDndPermission() {
        appContext?.let { ctx ->
            val hasPerm = FocusModeManager.hasNotificationPolicyAccess(ctx)
            _uiState.update { it.copy(hasDndPermission = hasPerm) }
        }
    }

    private fun loadTodayStats() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        viewModelScope.launch {
            repository.getTodaySessionCount(startOfDay).collect { count ->
                _uiState.update { it.copy(todaySessionCount = count) }
            }
        }
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            repository.allSubjects.collect { subjects ->
                _uiState.update { it.copy(subjects = subjects) }
            }
        }
    }

    // ── Duration ──
    fun setDuration(minutes: Int) {
        if (!_uiState.value.isRunning) {
            _uiState.update {
                it.copy(
                    selectedDuration = minutes,
                    customDurationMinutes = minutes,
                    timerSeconds = minutes * 60,
                    totalSeconds = minutes * 60
                )
            }
        }
    }

    fun showCustomDurationDialog() {
        if (!_uiState.value.isRunning)
            _uiState.update { it.copy(showCustomDurationDialog = true) }
    }

    fun dismissCustomDurationDialog() {
        _uiState.update { it.copy(showCustomDurationDialog = false) }
    }

    fun setCustomDuration(minutes: Int) {
        if (!_uiState.value.isRunning && minutes in 1..300) {
            _uiState.update {
                it.copy(
                    selectedDuration = 0, // 0 means custom
                    customDurationMinutes = minutes,
                    timerSeconds = minutes * 60,
                    totalSeconds = minutes * 60,
                    showCustomDurationDialog = false
                )
            }
        }
    }

    // ── Subject ──
    fun setSubject(subject: String) {
        _uiState.update { it.copy(currentSubject = subject, showSubjectPicker = false) }
    }

    fun showSubjectPicker() { _uiState.update { it.copy(showSubjectPicker = true) } }
    fun dismissSubjectPicker() { _uiState.update { it.copy(showSubjectPicker = false) } }

    // ── Ambient Sound ──
    fun setAmbientSound(sound: String) {
        val wasRunning = _uiState.value.isRunning
        _uiState.update { it.copy(selectedAmbientSound = sound) }
        if (wasRunning) {
            appContext?.let { ctx -> AmbientSoundPlayer.play(ctx, sound) }
        }
    }

    // ── DND ──
    fun toggleDnd(enabled: Boolean) { _uiState.update { it.copy(isDndEnabled = enabled) } }

    fun requestDndPermission(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun refreshDndPermission() { checkDndPermission() }

    // ── Allowed Contacts ──
    fun showAllowedContactsDialog() { _uiState.update { it.copy(showAllowedContactsDialog = true) } }
    fun dismissAllowedContactsDialog() { _uiState.update { it.copy(showAllowedContactsDialog = false) } }

    fun addAllowedContact(contact: AllowedContact) {
        // Avoid duplicates by phone number
        val existing = _uiState.value.allowedContacts.any { it.phoneNumber == contact.phoneNumber }
        if (!existing) {
            _uiState.update { it.copy(allowedContacts = it.allowedContacts + contact) }
        }
    }

    fun removeAllowedContact(contact: AllowedContact) {
        _uiState.update { it.copy(allowedContacts = it.allowedContacts - contact) }
    }

    // ── App Lock ──
    /** Called when user tries to navigate away during focus — show snack */
    fun onBackPressedDuringFocus() {
        _uiState.update { it.copy(showExitBlockedSnack = true) }
    }

    fun clearExitBlockedSnack() {
        _uiState.update { it.copy(showExitBlockedSnack = false) }
    }

    // ── Timer Controls ──
    fun toggleTimer() {
        if (_uiState.value.isRunning) pauseTimer() else startTimer()
    }

    private fun startTimer() {
        _uiState.update { it.copy(isRunning = true, isAppLocked = true) }

        if (_uiState.value.isDndEnabled) {
            appContext?.let { ctx ->
                if (FocusModeManager.hasNotificationPolicyAccess(ctx)) {
                    FocusModeManager.enableDndMode(ctx, allowPriorityCalls = _uiState.value.allowedContacts.isNotEmpty())
                    FocusModeManager.silencePhone(ctx)
                }
            }
        }

        timerJob = viewModelScope.launch {
            appContext?.let { ctx ->
                AmbientSoundPlayer.play(ctx, _uiState.value.selectedAmbientSound)
            }
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
        _uiState.update { it.copy(isRunning = false, isAppLocked = false) }
        AmbientSoundPlayer.stop()
        appContext?.let { ctx ->
            if (FocusModeManager.hasNotificationPolicyAccess(ctx)) {
                FocusModeManager.disableDndMode(ctx)
                FocusModeManager.restoreRinger(ctx)
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        val dur = _uiState.value.customDurationMinutes
        _uiState.update {
            it.copy(
                isRunning = false,
                isAppLocked = false,
                timerSeconds = dur * 60,
                totalSeconds = dur * 60
            )
        }
        AmbientSoundPlayer.stop()
        appContext?.let { ctx ->
            if (FocusModeManager.hasNotificationPolicyAccess(ctx)) {
                FocusModeManager.disableDndMode(ctx)
                FocusModeManager.restoreRinger(ctx)
            }
        }
    }

    private fun completeSession() {
        _uiState.update { it.copy(isRunning = false, isAppLocked = false) }
        AmbientSoundPlayer.stop()
        appContext?.let { ctx ->
            if (FocusModeManager.hasNotificationPolicyAccess(ctx)) {
                FocusModeManager.disableDndMode(ctx)
                FocusModeManager.restoreRinger(ctx)
            }
        }
        viewModelScope.launch {
            val session = StudySession(
                subjectId = 0,
                subjectName = _uiState.value.currentSubject,
                durationMinutes = _uiState.value.customDurationMinutes,
                startTime = System.currentTimeMillis() - (_uiState.value.customDurationMinutes * 60 * 1000L),
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
        AmbientSoundPlayer.stop()
        appContext?.let { ctx ->
            if (FocusModeManager.hasNotificationPolicyAccess(ctx)) {
                FocusModeManager.disableDndMode(ctx)
                FocusModeManager.restoreRinger(ctx)
            }
        }
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
