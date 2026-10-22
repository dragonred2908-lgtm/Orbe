package com.example.shared.stt

import com.example.shared.model.VoiceConfig
import com.example.shared.audio.WavUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Motor de Reconocimiento de Voz (Speech-to-Text) 100% OFFLINE basado en Vosk.
 * No requiere GEMINI_API_KEY ni conexión a internet en uso normal:
 *  - El modelo español pequeño (~50 MB) se descarga UNA sola vez a
 *    ~/.ia-student/models/ y luego funciona sin red.
 *  - Corre en CPU (funciona en equipos antiguos sin GPU).
 *
 * Implementa la misma interfaz [SpeechToTextEngine] que el motor Gemini,
 * por lo que es un sustituto directo en la cadena del asistente.
 */
class JvmVoskSttEngine(
    private val modelsRoot: File = File(System.getProperty("user.home"), ".ia-student/models")
) : SpeechToTextEngine {

    /** Estados de descarga/preparación del modelo. */
    sealed class ModelStatus {
        object Checking : ModelStatus()
        data class Downloading(val progressPercent: Int) : ModelStatus()
        object Ready : ModelStatus()
        data class Error(val message: String) : ModelStatus()
    }

    companion object {
        const val MODEL_NAME = "vosk-model-small-es-0.42"
        const val MODEL_URL = "https://alphacephei.com/vosk/models/$MODEL_NAME.zip"
        private const val SAMPLE_RATE = 16000f
    }

    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.Checking)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    @Volatile
    private var cachedModel: Model? = null
    private val modelLock = Any()

    val modelDir: File get() = File(modelsRoot, MODEL_NAME)

    init {
        _modelStatus.value = if (modelDir.exists()) {
            ModelStatus.Ready
        } else {
            // Sin modelo aún: se descargará en la primera transcripción.
            ModelStatus.Checking
        }
    }

    override fun isAvailable(): Boolean = modelDir.exists()

    /**
     * Asegura que el modelo esté descargado y extraído. Bloqueante (usar en IO).
     * Devuelve true si al final el modelo está listo.
     */
    fun ensureModelReady(): Boolean {
        if (modelDir.exists()) {
            _modelStatus.value = ModelStatus.Ready
            return true
        }

        return try {
            _modelStatus.value = ModelStatus.Downloading(0)
            modelsRoot.mkdirs()
            val zipFile = File(modelsRoot, "$MODEL_NAME.zip")
            downloadModel(zipFile)
            _modelStatus.value = ModelStatus.Downloading(-1) // extrayendo
            extractZip(zipFile, modelsRoot)
            zipFile.delete()
            val ok = modelDir.exists()
            _modelStatus.value = if (ok) ModelStatus.Ready else ModelStatus.Error("La extracción no produjo la carpeta del modelo.")
            ok
        } catch (e: Exception) {
            _modelStatus.value = ModelStatus.Error("No se pudo preparar el modelo de voz offline: ${e.message}")
            false
        }
    }

    private fun downloadModel(target: File) {
        val connection = URL(MODEL_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true
        try {
            if (connection.responseCode != 200) {
                throw IOException("El servidor de modelos respondió HTTP ${connection.responseCode}")
            }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var accumulated = 0L
                    var lastPercent = -1
                    while (input.read(buffer).also { read = it } >= 0) {
                        output.write(buffer, 0, read)
                        accumulated += read
                        if (total > 0) {
                            val percent = ((accumulated * 100) / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                _modelStatus.value = ModelStatus.Downloading(percent)
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractZip(zipFile: File, destination: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                // Protección básica contra Zip-Slip
                val outFile = File(destination, entry.name)
                val canonicalDest = destination.canonicalFile
                if (!outFile.canonicalPath.startsWith(canonicalDest.path + File.separator)) {
                    throw IOException("Entrada de ZIP insegura: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun getRecognizer(): Recognizer? {
        cachedModel?.let { return Recognizer(it, SAMPLE_RATE) }
        synchronized(modelLock) {
            cachedModel?.let { return Recognizer(it, SAMPLE_RATE) }
            val dir = modelDir
            if (!dir.exists()) return null
            return try {
                val model = Model(dir.absolutePath)
                cachedModel = model
                Recognizer(model, SAMPLE_RATE)
            } catch (t: Throwable) {
                // UnsatisfiedLinkError u otros fallos de carga nativa: degradar sin crashear.
                _modelStatus.value = ModelStatus.Error(
                    "No se pudo cargar el motor de voz offline en este sistema (${t.javaClass.simpleName})."
                )
                null
            }
        }
    }

    /**
     * Transcribe WAV PCM 16 kHz mono a texto usando el modelo local. Sin red.
     */
    override suspend fun transcribeAudio(audioBytes: ByteArray, mimeType: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (audioBytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("El búfer de audio está vacío"))
            }

            if (!isAvailable() && !ensureModelReady()) {
                val statusMsg = (_modelStatus.value as? ModelStatus.Error)?.message
                    ?: "Modelo de voz offline no disponible (se requiere internet solo para la primera descarga)."
                return@withContext Result.failure(IllegalStateException(statusMsg))
            }

            val recognizer = getRecognizer()
                ?: return@withContext Result.failure(
                    IllegalStateException(
                        (_modelStatus.value as? ModelStatus.Error)?.message
                            ?: "El motor de reconocimiento offline no pudo inicializarse."
                    )
                )

            try {
                // El grabador produce WAV con cabecera RIFF estándar; Vosk espera PCM crudo.
                val pcm = stripWavHeader(audioBytes)

                // Fragmentos de 32 KB para no pasar buffers gigantes al motor nativo.
                // (acceptWaveForm de Vosk recibe el búfer completo con su longitud.)
                var offset = 0
                while (offset < pcm.size) {
                    val end = minOf(offset + 32 * 1024, pcm.size)
                    val chunk = pcm.copyOfRange(offset, end)
                    recognizer.acceptWaveForm(chunk, chunk.size)
                    offset = end
                }

                // getFinalResult() devuelve JSON: {"text" : "palabras habladas"}
                val raw = recognizer.getFinalResult()
                val finalText = extractTextField(raw)

                if (finalText.isBlank()) {
                    Result.failure(IllegalStateException("No se reconoció voz clara en la grabación. Intenta de nuevo más cerca del micrófono."))
                } else {
                    Result.success(finalText)
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                Result.failure(RuntimeException("Error del reconocedor offline: ${t.message}"))
            } finally {
                try {
                    recognizer.close()
                } catch (_: Exception) {
                }
            }
        }

    /** En modo offline las consultas de texto las resuelve [OfflineAssistantBrain] vía el motor híbrido. */
    override suspend fun processQueryWithAI(query: String, config: VoiceConfig): Result<String> {
        return Result.failure(
            IllegalStateException("JvmVoskSttEngine es solo STT; las consultas las resuelve el motor híbrido.")
        )
    }

    /**
     * Extrae el campo "text" del JSON de resultado de Vosk
     * (p. ej. `{"text" : "hola aura"}` -> `hola aura`) sin depender de librerías externas.
     */
    private fun extractTextField(json: String): String {
        val marker = "\"text\""
        val idx = json.indexOf(marker)
        if (idx == -1) return json.trim()
        val colon = json.indexOf(':', idx + marker.length)
        if (colon == -1) return json.trim()
        val firstQuote = json.indexOf('"', colon + 1)
        if (firstQuote == -1) return json.trim()
        val secondQuote = json.indexOf('"', firstQuote + 1)
        if (secondQuote == -1) return json.trim()
        return json.substring(firstQuote + 1, secondQuote)
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Elimina la cabecera RIFF/WAV (estándar de 44 bytes con cabeceras fijas,
     * o la posición del chunk 'data' si viniera con chunks extra).
     */
    private fun stripWavHeader(wav: ByteArray): ByteArray {
        if (wav.size <= 44) return wav
        val riff = "RIFF".toByteArray()
        if (!wav.copyOfRange(0, 4).contentEquals(riff)) return wav // ya es PCM

        // Buscar el chunk "data" de forma robusta
        var i = 12
        while (i + 8 <= wav.size) {
            val chunkId = String(wav, i, 4)
            val chunkSize = (wav[i + 4].toInt() and 0xFF) or
                ((wav[i + 5].toInt() and 0xFF) shl 8) or
                ((wav[i + 6].toInt() and 0xFF) shl 16) or
                ((wav[i + 7].toInt() and 0xFF) shl 24)
            if (chunkId == "data") {
                val start = i + 8
                return if (start < wav.size) wav.copyOfRange(start, wav.size) else ByteArray(0)
            }
            i += 8 + chunkSize + (chunkSize % 2) // los chunks tienen alineación par
        }
        // Fallback: cabecera fija de 44 bytes (formato de WavUtils.createWavFile)
        return wav.copyOfRange(44, wav.size)
    }
}
