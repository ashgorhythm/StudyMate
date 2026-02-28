package com.example.myandroidapp.ui.screens.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.model.ChatMessage
import com.example.myandroidapp.service.GeminiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AiMode(val label: String, val emoji: String) {
    CHAT("Chat", "💬"),
    SUMMARIZE("Summarize", "📝"),
    QUIZ("Quiz", "🧠"),
    STUDY_PLAN("Study Plan", "📅"),
    EXPLAIN("Explain", "💡")
}

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val currentMode: AiMode = AiMode.CHAT,
    val errorMessage: String? = null
)

class AiChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        content = "👋 Hey there! I'm **Study AI**, powered by Gemini.\n\nI can help you:\n\n📝 **Summarize** your notes\n📅 Create **study plans** for exams\n🧠 **Quiz** you on any topic\n💡 **Explain** difficult concepts\n💬 Or just **chat** about anything!\n\nTap a mode above or just ask me something!",
                        isFromUser = false
                    )
                )
            )
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun setMode(mode: AiMode) {
        _uiState.update { it.copy(currentMode = mode) }

        // Send a contextual hint when switching modes
        val hint = when (mode) {
            AiMode.CHAT -> return // No hint needed for chat
            AiMode.SUMMARIZE -> "📝 **Summarize Mode** — Paste or type your notes and I'll create a concise summary with key points."
            AiMode.QUIZ -> "🧠 **Quiz Mode** — Tell me a topic and I'll generate practice questions with answers!"
            AiMode.STUDY_PLAN -> "📅 **Study Plan Mode** — Tell me your subject, exam date, and topics to cover. I'll create a day-by-day plan."
            AiMode.EXPLAIN -> "💡 **Explain Mode** — Ask me about any concept and I'll break it down step by step."
        }

        val hintMessage = ChatMessage(content = hint, isFromUser = false)
        _uiState.update { it.copy(messages = it.messages + hintMessage) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val userMessage = ChatMessage(content = text, isFromUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isTyping = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val response = when (_uiState.value.currentMode) {
                    AiMode.CHAT -> GeminiService.chat(text)
                    AiMode.SUMMARIZE -> GeminiService.summarizeNotes(text)
                    AiMode.QUIZ -> GeminiService.generateQuiz(text)
                    AiMode.STUDY_PLAN -> GeminiService.createStudyPlan(
                        subject = text,
                        examDate = "in 2 weeks",
                        topics = text,
                        hoursPerDay = 2
                    )
                    AiMode.EXPLAIN -> GeminiService.explainConcept(text)
                }

                val aiMessage = ChatMessage(content = response, isFromUser = false)
                _uiState.update {
                    it.copy(
                        messages = it.messages + aiMessage,
                        isTyping = false
                    )
                }
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    content = "⚠️ Something went wrong: ${e.localizedMessage ?: "Unknown error"}.\nPlease check your internet and try again.",
                    isFromUser = false
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMsg,
                        isTyping = false,
                        errorMessage = e.localizedMessage
                    )
                }
            }
        }
    }

    fun quickAction(action: String) {
        when {
            action.contains("Summarize", ignoreCase = true) -> {
                setMode(AiMode.SUMMARIZE)
            }
            action.contains("Quiz", ignoreCase = true) -> {
                setMode(AiMode.QUIZ)
            }
            action.contains("Study Plan", ignoreCase = true) -> {
                setMode(AiMode.STUDY_PLAN)
            }
            action.contains("Explain", ignoreCase = true) -> {
                setMode(AiMode.EXPLAIN)
            }
            else -> {
                _uiState.update { it.copy(inputText = action) }
                sendMessage()
            }
        }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        content = "🗑️ Chat cleared! How can I help you?",
                        isFromUser = false
                    )
                ),
                currentMode = AiMode.CHAT,
                errorMessage = null
            )
        }
    }
}
