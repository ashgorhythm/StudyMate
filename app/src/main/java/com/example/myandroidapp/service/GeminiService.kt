package com.example.myandroidapp.service

import com.example.myandroidapp.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig

/**
 * Wraps the Google Generative AI (Gemini) SDK for the Student Companion app.
 * Uses the free-tier Gemini 2.0 Flash model.
 */
object GeminiService {

    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
                topP = 0.95f
                topK = 40
                maxOutputTokens = 2048
            },
            systemInstruction = content {
                text(SYSTEM_PROMPT)
            }
        )
    }

    /**
     * Send a general chat message and get an AI response.
     */
    suspend fun chat(userMessage: String): String {
        return try {
            val response = model.generateContent(userMessage)
            response.text ?: "I couldn't generate a response. Please try again."
        } catch (e: Exception) {
            "⚠️ Error: ${e.localizedMessage ?: "Something went wrong. Check your internet connection."}"
        }
    }

    /**
     * Summarize notes/text content.
     */
    suspend fun summarizeNotes(content: String, subject: String = ""): String {
        val prompt = buildString {
            append("Summarize the following ")
            if (subject.isNotBlank()) append("$subject ")
            append("study notes into clear, concise bullet points. ")
            append("Highlight key concepts, formulas, and important facts.\n\n")
            append("--- NOTES ---\n")
            append(content)
        }
        return chat(prompt)
    }

    /**
     * Generate a quiz from study content or a topic.
     */
    suspend fun generateQuiz(topic: String, questionCount: Int = 5): String {
        val prompt = buildString {
            append("Generate a study quiz about \"$topic\" with exactly $questionCount questions.\n\n")
            append("Format each question as:\n")
            append("**Q[number]**: [question]\n")
            append("a) [option]  b) [option]  c) [option]  d) [option]\n\n")
            append("After ALL questions, provide an **Answer Key** section with the correct answers and brief explanations.\n")
            append("Make the questions progressively harder. Mix conceptual and application-based questions.")
        }
        return chat(prompt)
    }

    /**
     * Create a study plan based on exam dates and topics.
     */
    suspend fun createStudyPlan(
        subject: String,
        examDate: String,
        topics: String,
        hoursPerDay: Int = 2
    ): String {
        val prompt = buildString {
            append("Create a detailed study plan for the subject \"$subject\".\n\n")
            append("📅 Exam Date: $examDate\n")
            append("📚 Topics to cover: $topics\n")
            append("⏰ Available study time: $hoursPerDay hours per day\n\n")
            append("Please create a day-by-day schedule that:\n")
            append("1. Distributes topics evenly\n")
            append("2. Includes revision days before the exam\n")
            append("3. Suggests specific activities (read, practice, review)\n")
            append("4. Leaves buffer time for difficult topics\n")
            append("Format with emojis and clear day headers.")
        }
        return chat(prompt)
    }

    /**
     * Explain a concept step by step.
     */
    suspend fun explainConcept(concept: String, level: String = "high school"): String {
        val prompt = buildString {
            append("Explain the concept of \"$concept\" at a $level level.\n\n")
            append("Structure your explanation as:\n")
            append("1. 🎯 **Simple Definition** - one sentence\n")
            append("2. 📖 **Detailed Explanation** - break it down step by step\n")
            append("3. 💡 **Real-World Example** - a practical analogy\n")
            append("4. ✏️ **Practice** - a simple exercise to test understanding\n")
            append("5. 🔗 **Related Concepts** - what to learn next\n\n")
            append("Use simple language and include formulas or diagrams descriptions where helpful.")
        }
        return chat(prompt)
    }

    private const val SYSTEM_PROMPT = """You are Study AI, a friendly and knowledgeable study assistant built into a student companion app. Your role is to help students learn more effectively.

Key behaviors:
- Be encouraging and supportive
- Use emojis sparingly to make responses engaging (📚 🎯 💡 ✅)
- Format responses with markdown: **bold** for key terms, bullet points for lists
- Keep explanations clear and concise
- When generating quizzes, always include answer keys
- When creating study plans, be realistic about time management
- If a student seems stressed, offer motivational tips
- Adapt complexity to the student's apparent level
- For math/science, include step-by-step solutions
- Always be accurate — never make up facts"""
}
