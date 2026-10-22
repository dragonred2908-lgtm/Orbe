package com.example.shared.stt

import com.example.shared.model.VoiceConfig
import com.example.shared.offline.OfflineAssistantBrain
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Motor de voz HÍBRIDO: usa Gemini (en línea) cuando hay GEMINI_API_KEY configurada
 * y cae automáticamente al modo 100% offline (STT Vosk + [OfflineAssistantBrain])
 * cuando no la hay — o cuando la red falla.
 *
 * Esto permite que "Oye Aura" funcione SIEMPRE, incluso sin API key y en
 * equipos antiguos sin conexión.
 */
class JvmHybridSttEngine(
    private val online: JvmGeminiSttEngine = JvmGeminiSttEngine(),
    val offlineStt: JvmVoskSttEngine = JvmVoskSttEngine()
) : SpeechToTextEngine {

    private val esLocale = Locale.forLanguageTag("es-ES")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", esLocale)
    private val dateFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", esLocale)

    private val brain = OfflineAssistantBrain(
        currentTimeProvider = { LocalDateTime.now().format(timeFormat) },
        currentDateProvider = { LocalDateTime.now().format(dateFormat).replaceFirstChar { it.uppercase(esLocale) } }
    )

    /** true si hay clave de API y el asistente operará con Gemini. */
    fun isOnlineConfigured(): Boolean = online.isAvailable()

    /** El motor siempre está disponible: offline es el mínimo común denominador. */
    override fun isAvailable(): Boolean = true

    override suspend fun transcribeAudio(audioBytes: ByteArray, mimeType: String): Result<String> {
        return if (online.isAvailable()) {
            val onlineResult = online.transcribeAudio(audioBytes, mimeType)
            onlineResult.fold(
                onSuccess = { Result.success(it) },
                onFailure = {
                    // Sin red o error de la API: respaldo offline para no dejar al usuario mudo.
                    offlineStt.transcribeAudio(audioBytes, mimeType)
                }
            )
        } else {
            offlineStt.transcribeAudio(audioBytes, mimeType)
        }
    }

    override suspend fun processQueryWithAI(query: String, config: VoiceConfig): Result<String> {
        return if (online.isAvailable()) {
            val onlineResult = online.processQueryWithAI(query, config)
            onlineResult.fold(
                onSuccess = { Result.success(it) },
                onFailure = { Result.success(brain.answer(query)) }
            )
        } else {
            Result.success(brain.answer(query))
        }
    }
}
