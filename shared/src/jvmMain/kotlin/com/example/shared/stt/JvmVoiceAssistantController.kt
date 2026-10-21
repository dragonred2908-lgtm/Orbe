package com.example.shared.stt

import com.example.shared.audio.JvmAudioRecorder
import com.example.shared.model.VoiceAssistantState
import com.example.shared.model.VoiceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Controlador de voz para el entorno de escritorio JVM (Windows, Linux, macOS).
 * Integra el grabador de audio nativo, el motor STT y la sesión conversacional.
 */
class JvmVoiceAssistantController(
    val engine: JvmGeminiSttEngine = JvmGeminiSttEngine(),
    val session: VoiceAssistantSession = VoiceAssistantSession(engine),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val recorder = JvmAudioRecorder(
        sampleRate = 16000f,
        onRmsLevel = { level ->
            session.updateRmsLevel(level)
        }
    )

    val audioPlayer: com.example.shared.audio.JvmAudioPlayer = com.example.shared.audio.JvmAudioPlayer()

    private val _isRecordingActive = MutableStateFlow(false)
    val isRecordingActive: StateFlow<Boolean> = _isRecordingActive.asStateFlow()

    private val _statusNotice = MutableStateFlow<String?>("Listo para escuchar")
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    private var autoStopJob: Job? = null

    init {
        // Inicializar en modo de escucha si está configurado
        session.setListeningForWakePhrase()
    }

    /**
     * Inicia la captura de audio desde el micrófono físico.
     */
    fun startListening() {
        if (_isRecordingActive.value) return

        val started = recorder.startRecording()
        if (started) {
            _isRecordingActive.value = true
            _statusNotice.value = "Grabando voz... Habla ahora (ej: \"${session.config.value.wakePhrase}, ¿qué es la fotosíntesis?\")"
            session.setListeningForWakePhrase()

            // Límite de seguridad de 10 segundos por toma
            autoStopJob?.cancel()
            autoStopJob = scope.launch {
                delay(10000)
                if (_isRecordingActive.value) {
                    stopListeningAndProcess()
                }
            }
        } else {
            _statusNotice.value = "Micrófono no disponible en este sistema. Puedes usar los botones de prueba rápida."
            session.updateRmsLevel(0f)
        }
    }

    /**
     * Detiene la grabación y procesa el audio con la API de Speech-to-Text.
     */
    fun stopListeningAndProcess() {
        autoStopJob?.cancel()
        if (!_isRecordingActive.value) return

        _isRecordingActive.value = false
        _statusNotice.value = "Transcribiendo audio con Speech-to-Text..."

        scope.launch {
            val wavBytes = recorder.stopRecording()
            if (wavBytes.isEmpty()) {
                _statusNotice.value = "No se capturó audio suficiente."
                session.setListeningForWakePhrase()
                return@launch
            }

            // Llamada al motor de Speech-to-Text
            val result = engine.transcribeAudio(wavBytes, "audio/wav")
            result.fold(
                onSuccess = { transcribedText ->
                    _statusNotice.value = "Texto reconocido: \"$transcribedText\""
                    session.processTranscribedText(transcribedText)
                },
                onFailure = { error ->
                    _statusNotice.value = "Error STT: ${error.message}"
                    // Si falla la API STT (ej: sin clave configurada en offline), ofrecer sugerencia
                    session.processTranscribedText("${session.config.value.wakePhrase}, ¿qué es IA Student?")
                }
            )
        }
    }

    /**
     * Alterna entre grabar y detener.
     */
    fun toggleListening() {
        if (_isRecordingActive.value) {
            stopListeningAndProcess()
        } else {
            startListening()
        }
    }

    /**
     * Simula la recepción de audio con una frase para pruebas directas en PC/CI sin necesidad de hardware de micrófono.
     */
    fun simulateVoiceInput(spokenText: String) {
        scope.launch {
            _statusNotice.value = "Simulando entrada de voz: \"$spokenText\""
            // Pequeña animación de nivel de audio
            session.updateRmsLevel(0.85f)
            delay(300)
            session.updateRmsLevel(0.4f)
            delay(200)
            session.updateRmsLevel(0f)
            session.processTranscribedText(spokenText)
        }
    }

    /**
     * Actualiza la frase de activación u otras opciones de configuración.
     */
    fun updateWakePhrase(newPhrase: String) {
        val current = session.config.value
        session.updateConfig(current.copy(wakePhrase = newPhrase.trim()))
        _statusNotice.value = "Frase de activación actualizada a: \"${newPhrase.trim()}\""
    }

    fun updateApiKey(apiKey: String) {
        val current = session.config.value
        session.updateConfig(current.copy(geminiApiKey = apiKey.trim()))
        _statusNotice.value = "Clave de API de Gemini configurada con éxito"
    }
}
