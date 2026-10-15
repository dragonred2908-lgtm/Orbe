package com.example.shared.stt

import com.example.shared.model.VoiceAssistantState
import com.example.shared.model.VoiceConfig
import com.example.shared.model.VoiceConversationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gestor del ciclo de vida y sesión del asistente de voz.
 * Administra la detección de la frase clave, la transcripción y la invocación de IA.
 */
class VoiceAssistantSession(
    private val sttEngine: SpeechToTextEngine,
    initialConfig: VoiceConfig = VoiceConfig(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _config = MutableStateFlow(initialConfig)
    val config: StateFlow<VoiceConfig> = _config.asStateFlow()

    private val _state = MutableStateFlow<VoiceAssistantState>(VoiceAssistantState.Idle)
    val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _rmsAudioLevel = MutableStateFlow(0f)
    val rmsAudioLevel: StateFlow<Float> = _rmsAudioLevel.asStateFlow()

    private val _history = MutableStateFlow<List<VoiceConversationItem>>(emptyList())
    val history: StateFlow<List<VoiceConversationItem>> = _history.asStateFlow()

    fun updateConfig(newConfig: VoiceConfig) {
        _config.value = newConfig
    }

    fun updateRmsLevel(level: Float) {
        _rmsAudioLevel.value = level.coerceIn(0f, 1f)
    }

    fun setListeningForWakePhrase() {
        _state.value = VoiceAssistantState.ListeningForWakePhrase
        _currentTranscript.value = ""
    }

    fun setIdle() {
        _state.value = VoiceAssistantState.Idle
        _rmsAudioLevel.value = 0f
    }

    /**
     * Procesa texto transcrito (desde micrófono en tiempo real o simulado).
     */
    fun processTranscribedText(transcript: String, isFinal: Boolean = true) {
        val trimmed = transcript.trim()
        if (trimmed.isEmpty()) return

        _currentTranscript.value = trimmed
        val currentCfg = _config.value
        val currentState = _state.value

        when (currentState) {
            is VoiceAssistantState.ListeningForWakePhrase, VoiceAssistantState.Idle -> {
                // Comprobar si coincide con la frase de activación
                if (WakePhraseDetector.matches(trimmed, currentCfg.wakePhrase, currentCfg.matchContains)) {
                    val extractedQuery = WakePhraseDetector.extractQuery(trimmed, currentCfg.wakePhrase)
                    if (extractedQuery.isNotBlank()) {
                        // El usuario dijo frase + consulta en la misma frase ("Oye Aura qué es la física")
                        triggerQuery(extractedQuery, currentCfg.wakePhrase)
                    } else {
                        // El usuario solo dijo la frase ("Oye Aura") -> esperar consulta
                        _state.value = VoiceAssistantState.WakePhraseDetected(currentCfg.wakePhrase)
                    }
                }
            }
            is VoiceAssistantState.WakePhraseDetected, VoiceAssistantState.RecordingQuery -> {
                // En este estado, el audio transcrito corresponde a la consulta del usuario
                triggerQuery(trimmed, currentCfg.wakePhrase)
            }
            else -> {
                // Si el usuario vuelve a invocar la frase clave mientras está en otro estado
                if (WakePhraseDetector.matches(trimmed, currentCfg.wakePhrase, currentCfg.matchContains)) {
                    val extractedQuery = WakePhraseDetector.extractQuery(trimmed, currentCfg.wakePhrase)
                    if (extractedQuery.isNotBlank()) {
                        triggerQuery(extractedQuery, currentCfg.wakePhrase)
                    } else {
                        _state.value = VoiceAssistantState.WakePhraseDetected(currentCfg.wakePhrase)
                    }
                }
            }
        }
    }

    /**
     * Ejecuta la consulta contra el motor de IA en tiempo real.
     */
    fun triggerQuery(queryText: String, wakePhraseUsed: String = _config.value.wakePhrase) {
        val cleanQuery = queryText.trim()
        if (cleanQuery.isEmpty()) return

        _state.value = VoiceAssistantState.ProcessingWithAI(cleanQuery)
        val startTime = System.currentTimeMillis()

        scope.launch {
            val result = sttEngine.processQueryWithAI(cleanQuery, _config.value)
            val duration = System.currentTimeMillis() - startTime

            result.fold(
                onSuccess = { answerText ->
                    _state.value = VoiceAssistantState.Responding(cleanQuery, answerText)

                    val item = VoiceConversationItem(
                        id = "${System.currentTimeMillis()}_${(1000..9999).random()}",
                        timestamp = System.currentTimeMillis(),
                        wakePhraseUsed = wakePhraseUsed,
                        transcribedQuery = cleanQuery,
                        assistantResponse = answerText,
                        executionTimeMs = duration
                    )
                    _history.value = listOf(item) + _history.value

                    // Si autoRestartListening está activo, volver a escuchar frase clave
                    if (_config.value.autoRestartListening) {
                        kotlinx.coroutines.delay(1200)
                        if (_state.value is VoiceAssistantState.Responding) {
                            _state.value = VoiceAssistantState.ListeningForWakePhrase
                        }
                    }
                },
                onFailure = { error ->
                    _state.value = VoiceAssistantState.Error("Error al procesar consulta: ${error.message}")
                }
            )
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }
}
