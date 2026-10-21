package com.example.data.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Gestor robusto de reconocimiento de voz y entrada de micrófono para Android.
 * Admite resultados parciales, nivel RMS en vivo para visualización del orbe,
 * y manejo de errores de hardware y servicios ausentes.
 */
class VoiceRecognitionHelper(
    private val context: Context,
    private val onWakeWordDetected: (String) -> Unit,
    private val onSpeechPartialResult: (String) -> Unit = {},
    private val onSpeechFinalResult: (String) -> Unit = {},
    private val onRmsLevelChanged: (Float) -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    var isListening: Boolean = false
        private set

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    fun isRecognitionSupported(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Exception) {
            false
        }
    }

    private fun initRecognizer() {
        try {
            if (speechRecognizer != null) {
                try {
                    speechRecognizer?.destroy()
                } catch (_: Exception) {}
                speechRecognizer = null
            }

            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
                Log.d(TAG, "SpeechRecognizer inicializado con éxito.")
            } else {
                Log.w(TAG, "Speech recognition not available on this device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SpeechRecognizer", e)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                Log.d(TAG, "Reconocedor listo para capturar voz.")
            }

            override fun onBeginningOfSpeech() {
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                // El nivel rmsdB típicamente oscila entre -2 dB y 10 dB en SpeechRecognizer de Android
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                onRmsLevelChanged(normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                onRmsLevelChanged(0f)
            }

            override fun onError(error: Int) {
                isListening = false
                onRmsLevelChanged(0f)

                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Error de captura de micrófono de audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Error en el cliente de voz de la app"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de micrófono no concedido"
                    SpeechRecognizer.ERROR_NETWORK -> "Fallo en la conexión de red del motor de voz"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red agotado"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció ninguna frase o palabra"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocedor de voz está ocupado"
                    SpeechRecognizer.ERROR_SERVER -> "Error en el servidor de reconocimiento de voz"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Silencio detectado (sin entrada de voz)"
                    else -> "Aviso de audio ($error)"
                }
                Log.w(TAG, "SpeechRecognizer.onError: $errorMsg (code $error)")
                onError(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                onRmsLevelChanged(0f)

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.trim() ?: ""
                if (spokenText.isNotEmpty()) {
                    Log.d(TAG, "Resultado final de voz: $spokenText")
                    onSpeechFinalResult(spokenText)
                    onWakeWordDetected(spokenText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull()?.trim() ?: ""
                if (partialText.isNotEmpty()) {
                    onSpeechPartialResult(partialText)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initRecognizer()
                }

                if (speechRecognizer == null) {
                    isListening = false
                    onRmsLevelChanged(0f)
                    onError("El motor de reconocimiento de voz no está disponible en este dispositivo.")
                    return@post
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                Log.e(TAG, "Error starting listening", e)
                isListening = false
                onRmsLevelChanged(0f)
                onError(e.message ?: "No se pudo iniciar el micrófono")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                isListening = false
                onRmsLevelChanged(0f)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping listening", e)
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                isListening = false
                onRmsLevelChanged(0f)
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling listening", e)
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                isListening = false
                onRmsLevelChanged(0f)
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying SpeechRecognizer", e)
            }
        }
    }

    companion object {
        private const val TAG = "VoiceRecognitionHelper"
    }
}
