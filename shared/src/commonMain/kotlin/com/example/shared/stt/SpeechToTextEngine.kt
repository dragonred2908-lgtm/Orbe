package com.example.shared.stt

import com.example.shared.model.VoiceConfig

/**
 * Interfaz multiplataforma para el motor de reconocimiento de voz (Speech-to-Text)
 * y procesamiento de lenguaje natural en tiempo real.
 */
interface SpeechToTextEngine {
    /**
     * Transcribe un búfer de audio grabado (formato WAV/PCM) a texto utilizando la API STT.
     */
    suspend fun transcribeAudio(audioBytes: ByteArray, mimeType: String = "audio/wav"): Result<String>

    /**
     * Procesa la consulta del usuario con el modelo de IA generativa (Gemini)
     * para obtener la respuesta en tiempo real.
     */
    suspend fun processQueryWithAI(query: String, config: VoiceConfig): Result<String>

    /**
     * Indica si el motor de reconocimiento de voz está disponible y configurado.
     */
    fun isAvailable(): Boolean
}
