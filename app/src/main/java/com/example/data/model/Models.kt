package com.example.data.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelName: String? = null,
    val isThinking: Boolean = false,
    val thoughtProcess: String? = null,
    val imageBase64: String? = null,
    val audioBase64: String? = null,
    val groundingSources: List<String> = emptyList(),
    val stepTag: String? = null // e.g. "PASO 1", "PASO 2", etc.
)

enum class MessageSender {
    USER,
    IA_STUDENT,
    SYSTEM
}

data class GeminiModelOption(
    val id: String,
    val displayName: String,
    val description: String,
    val badge: String,
    val isDefault: Boolean = false
)

object GeminiModels {
    val ARGON = GeminiModelOption(
        id = "gemini-4-argon",
        displayName = "Gemini 4 Argon",
        description = "Razonamiento complejo, Modo Pensamiento Alto y tareas de código",
        badge = "🧠 Razonamiento Alto"
    )
    val FLASH = GeminiModelOption(
        id = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        description = "Uso general, rápido, compatible con Google Search y Maps",
        badge = "⚡ Recomendado",
        isDefault = true
    )
    val FLASH_LITE = GeminiModelOption(
        id = "gemini-3.1-flash-lite",
        displayName = "Gemini 3.1 Flash Lite",
        description = "Respuestas ultra rápidas con baja latencia",
        badge = "🚀 Baja Latencia"
    )

    val ALL = listOf(FLASH, ARGON, FLASH_LITE)
}

data class ProjectRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val appType: String,
    val targetAudience: String,
    val naturalLanguage: String,
    val externalConnections: String,
    val chosenOption: String,
    val systemPrompt: String,
    val model: String = "gemini-3.8-flash",
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val maxTokens: Int = 2048,
    val createdAt: Long = System.currentTimeMillis()
)
