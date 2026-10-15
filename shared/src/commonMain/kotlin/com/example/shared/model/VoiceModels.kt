package com.example.shared.model

/**
 * Estados del ciclo de vida del asistente de voz multiplataforma.
 */
sealed class VoiceAssistantState {
    object Idle : VoiceAssistantState()
    object ListeningForWakePhrase : VoiceAssistantState()
    data class WakePhraseDetected(val phrase: String) : VoiceAssistantState()
    object RecordingQuery : VoiceAssistantState()
    object TranscribingSpeech : VoiceAssistantState()
    data class ProcessingWithAI(val query: String) : VoiceAssistantState()
    data class Responding(val query: String, val answer: String) : VoiceAssistantState()
    data class Error(val message: String) : VoiceAssistantState()
}

/**
 * Configuración personalizable del asistente y frase de activación.
 */
data class VoiceConfig(
    val assistantName: String = "Aura",
    val wakePhrase: String = "Oye Aura",
    val matchContains: Boolean = true,
    val autoRestartListening: Boolean = true,
    val geminiApiKey: String = "",
    val model: String = "gemini-3.5-flash",
    val languageCode: String = "es-ES",
    val systemInstruction: String = "Eres Aura (IA Student), una asistente educativa y técnica inteligente, amigable, precisa y de alta tecnología. Responde en español de forma concisa y clara a las consultas del usuario."
)

/**
 * Elemento de historial de conversación por voz.
 */
data class VoiceConversationItem(
    val id: String,
    val timestamp: Long,
    val wakePhraseUsed: String,
    val transcribedQuery: String,
    val assistantResponse: String,
    val executionTimeMs: Long = 0
)

/**
 * Resultado de una transcripción STT.
 */
data class SttTranscriptionResult(
    val text: String,
    val isFinal: Boolean = true,
    val confidence: Float = 1.0f,
    val detectedWakePhrase: Boolean = false,
    val extractedQuery: String = ""
)
