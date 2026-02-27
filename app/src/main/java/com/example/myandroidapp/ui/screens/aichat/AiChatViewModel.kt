package com.example.myandroidapp.ui.screens.aichat

import androidx.lifecycle.ViewModel
import com.example.myandroidapp.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false
)

class AiChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    init {
        // Welcome message
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        content = "👋 Hello! I'm your Study AI assistant. I can help you:\n\n• Summarize your notes\n• Create study plans\n• Quiz you on topics\n• Explain difficult concepts\n\nHow can I help you today?",
                        isFromUser = false
                    )
                )
            )
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val userMessage = ChatMessage(content = text, isFromUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isTyping = true
            )
        }

        // Simulate AI response (placeholder - would connect to real AI API)
        val aiResponse = generatePlaceholderResponse(text)
        val aiMessage = ChatMessage(content = aiResponse, isFromUser = false)
        _uiState.update {
            it.copy(
                messages = it.messages + aiMessage,
                isTyping = false
            )
        }
    }

    fun quickAction(action: String) {
        _uiState.update { it.copy(inputText = action) }
        sendMessage()
    }

    private fun generatePlaceholderResponse(query: String): String {
        return when {
            query.contains("summarize", ignoreCase = true) ->
                "📝 **Summary**\n\nHere's a concise summary of the key points:\n\n• **Main Concept**: The fundamental principles covered in this topic\n• **Key Formula**: Important equations and relationships\n• **Applications**: Real-world use cases and examples\n\n_Note: Connect me to an AI API for real summaries!_"

            query.contains("study plan", ignoreCase = true) ->
                "📅 **Study Plan**\n\n**Monday**: Review core concepts (2h)\n**Tuesday**: Practice problems (1.5h)\n**Wednesday**: Summarize notes (1h)\n**Thursday**: Mock quiz (2h)\n**Friday**: Revision & weak areas (1.5h)\n\n_Adjust based on your pace!_"

            query.contains("quiz", ignoreCase = true) ->
                "🧠 **Quick Quiz**\n\n**Q1**: What is the derivative of x²?\na) x  b) 2x  c) 2  d) x²\n\n**Q2**: Newton's second law states F = ?\na) ma  b) mv  c) mg  d) mc²\n\n_Reply with your answers!_"

            query.contains("explain", ignoreCase = true) ->
                "💡 **Explanation**\n\nLet me break this down step by step:\n\n1. **Foundation**: Start with the basic definition\n2. **Building up**: How it connects to what you know\n3. **Example**: A practical illustration\n4. **Key takeaway**: The most important thing to remember\n\n_Connect an AI API for detailed explanations!_"

            else ->
                "I understand your question about \"$query\". Here's what I can help with:\n\n• Try asking me to **summarize** a topic\n• Request a **study plan** for an exam\n• Ask me to **quiz** you\n• Ask me to **explain** a concept\n\n_For full AI responses, integrate with Gemini API!_"
        }
    }
}
