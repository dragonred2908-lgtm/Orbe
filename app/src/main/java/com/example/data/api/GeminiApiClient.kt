package com.example.data.api

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente seguro y endurecido para la API de Google Gemini.
 *
 * Mejoras de seguridad y robustez:
 * - Credenciales transmitidas exclusivamente mediante encabezado 'x-goog-api-key' (nunca en parámetros URL).
 * - Protección contra exposición de claves en logs de error o URLs imprimibles.
 * - Soporte para claves introducidas por UI/almacenamiento seguro por encima de valores estáticos.
 * - Validación y límites estrictos previos al envío de payloads (máximo 32k caracteres en prompt, 14MB en Base64).
 * - Manejo de reintentos exponenciales para errores transitorios (429 Rate Limit y 5xx Server Errors).
 * - Cierre determinista de Response/ResponseBody mediante bloques 'use' para prevenir fugas de conexiones y sockets.
 */
class GeminiApiClient {

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val MAX_PROMPT_LENGTH = 32_000
        private const val MAX_IMAGE_BASE64_LENGTH = 14_000_000 // Aprox. 10MB en binario
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    // Clave configurada en caliente por el usuario desde la UI
    @Volatile
    private var customUserApiKey: String? = null

    fun setUserApiKey(key: String?) {
        customUserApiKey = key?.trim()
    }

    private fun getApiKey(): String {
        val userKey = customUserApiKey
        if (!userKey.isNullOrBlank() && userKey != "MY_GEMINI_API_KEY") {
            return userKey
        }

        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY") {
                ""
            } else {
                key.trim()
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun isApiKeyConfigured(): Boolean {
        return getApiKey().isNotBlank()
    }

    /**
     * Ejecuta una petición HTTP con reintentos exponenciales para códigos 429 y 5xx,
     * garantizando el cierre explícito de la respuesta para evitar fugas de recursos.
     */
    private suspend fun executeWithRetry(request: Request): Pair<Int, String> = withContext(Dispatchers.IO) {
        var currentDelay = INITIAL_RETRY_DELAY_MS
        var lastCode = 0
        var lastBody = ""

        for (attempt in 1..MAX_RETRIES) {
            try {
                val call = client.newCall(request)
                val (code, body) = call.execute().use { response: Response ->
                    val c = response.code
                    val b = response.body?.string() ?: ""
                    Pair(c, b)
                }

                lastCode = code
                lastBody = body

                // Éxito o error no recuperable con reintento (ej: 400, 401, 403, 404)
                if (code in 200..299 || (code !in 500..599 && code != 429)) {
                    return@withContext Pair(code, body)
                }

                // Error transitorio (429 o 5xx)
                if (attempt < MAX_RETRIES) {
                    Log.w(TAG, "Reintentando solicitud tras código HTTP $code (Intento $attempt/$MAX_RETRIES en ${currentDelay}ms)")
                    delay(currentDelay)
                    currentDelay *= 2
                }
            } catch (e: Exception) {
                lastBody = e.localizedMessage ?: "Error de red"
                if (attempt < MAX_RETRIES) {
                    Log.w(TAG, "Reintentando solicitud tras excepción de red: ${e.javaClass.simpleName}")
                    delay(currentDelay)
                    currentDelay *= 2
                }
            }
        }

        Pair(lastCode, lastBody)
    }

    /**
     * Construye una solicitud HTTP segura incluyendo la clave en encabezado en lugar de URL.
     */
    private fun buildSecureRequest(endpointUrl: String, jsonBody: String, apiKey: String): Request {
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        return Request.Builder()
            .url(endpointUrl)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
    }

    suspend fun generateContent(
        prompt: String,
        model: String = "gemini-3.8-flash",
        systemInstruction: String? = null,
        history: List<ChatMessage> = emptyList(),
        useSearch: Boolean = false,
        useMaps: Boolean = false,
        thinkingLevelHigh: Boolean = false,
        temperature: Float = 0.7f,
        topP: Float = 0.95f,
        topK: Int = 40,
        maxTokens: Int = 2048,
        imageBase64: String? = null
    ): GenerationResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext GenerationResult(
                text = "⚠️ **Clave de API no configurada**\n\nPara comunicarte directamente con los modelos de Google Gemini, por favor agrega tu `GEMINI_API_KEY` en el panel de **Secrets** de AI Studio o introdúcela en la sección de configuración de la app.",
                groundingSources = emptyList()
            )
        }

        // Validación y límites previos al envío
        if (prompt.length > MAX_PROMPT_LENGTH) {
            return@withContext GenerationResult(
                text = "❌ Error de validación: El texto del prompt (${prompt.length} caracteres) supera el límite máximo permitido ($MAX_PROMPT_LENGTH caracteres).",
                groundingSources = emptyList()
            )
        }

        if (!imageBase64.isNullOrBlank() && imageBase64.length > MAX_IMAGE_BASE64_LENGTH) {
            return@withContext GenerationResult(
                text = "❌ Error de validación: La imagen adjunta supera el tamaño máximo permitido (~10MB). Por favor adjunta una imagen más ligera.",
                groundingSources = emptyList()
            )
        }

        try {
            val jsonRoot = JSONObject()

            // Contents array
            val contentsArray = JSONArray()

            // Add previous history (max last 6 messages to stay concise)
            val filteredHistory = history.takeLast(6)
            for (msg in filteredHistory) {
                if (msg.sender == MessageSender.SYSTEM) continue
                val contentObj = JSONObject()
                contentObj.put("role", if (msg.sender == MessageSender.USER) "user" else "model")
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", msg.text)
                partsArray.put(partObj)
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            // Current message
            val currentContent = JSONObject()
            currentContent.put("role", "user")
            val currentParts = JSONArray()

            // If image is attached
            if (!imageBase64.isNullOrBlank()) {
                val imagePart = JSONObject()
                val inlineData = JSONObject()
                inlineData.put("mimeType", "image/jpeg")
                inlineData.put("data", imageBase64)
                imagePart.put("inlineData", inlineData)
                currentParts.put(imagePart)
            }

            val textPart = JSONObject()
            textPart.put("text", prompt)
            currentParts.put(textPart)

            currentContent.put("parts", currentParts)
            contentsArray.put(currentContent)

            jsonRoot.put("contents", contentsArray)

            // System Instruction
            if (!systemInstruction.isNullOrBlank()) {
                val sysInstObj = JSONObject()
                val sysParts = JSONArray()
                val sysTextPart = JSONObject()
                sysTextPart.put("text", systemInstruction)
                sysParts.put(sysTextPart)
                sysInstObj.put("parts", sysParts)
                jsonRoot.put("systemInstruction", sysInstObj)
            }

            // Generation Config
            val genConfig = JSONObject()
            genConfig.put("temperature", temperature)
            genConfig.put("topP", topP)
            genConfig.put("topK", topK)

            if (thinkingLevelHigh && model.contains("gemini-4-argon")) {
                val thinkingConfig = JSONObject()
                thinkingConfig.put("thinkingLevel", "HIGH")
                genConfig.put("thinkingConfig", thinkingConfig)
            } else {
                genConfig.put("maxOutputTokens", maxTokens)
            }
            jsonRoot.put("generationConfig", genConfig)

            // Grounding tools (Google Search / Google Maps)
            val toolsArray = JSONArray()
            if (useSearch) {
                val searchTool = JSONObject()
                searchTool.put("googleSearch", JSONObject())
                toolsArray.put(searchTool)
            }
            if (useMaps) {
                val mapsTool = JSONObject()
                mapsTool.put("googleMaps", JSONObject())
                toolsArray.put(mapsTool)
            }
            if (toolsArray.length() > 0) {
                jsonRoot.put("tools", toolsArray)
            }

            val endpoint = "$baseUrl/models/$model:generateContent"
            val request = buildSecureRequest(endpoint, jsonRoot.toString(), apiKey)

            val (code, responseBody) = executeWithRetry(request)

            if (code !in 200..299) {
                val errMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "Código HTTP $code"
                }
                return@withContext GenerationResult(
                    text = "❌ Error al consultar $model ($code): $errMsg",
                    groundingSources = emptyList()
                )
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            val textBuilder = StringBuilder()
            var thoughtText: String? = null
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        textBuilder.append(part.getString("text"))
                    }
                    if (part.has("thought")) {
                        thoughtText = part.getString("thought")
                    }
                }
            }

            // Extract grounding metadata if present
            val groundingSources = mutableListOf<String>()
            val groundingMetadata = firstCandidate?.optJSONObject("groundingMetadata")
            val searchChunks = groundingMetadata?.optJSONArray("groundingChunks")
            if (searchChunks != null) {
                for (i in 0 until searchChunks.length()) {
                    val chunk = searchChunks.optJSONObject(i)
                    val web = chunk?.optJSONObject("web")
                    val uri = web?.optString("uri")
                    val title = web?.optString("title")
                    if (!uri.isNullOrBlank()) {
                        groundingSources.add(if (!title.isNullOrBlank()) "$title ($uri)" else uri)
                    }
                }
            }

            val resultText = textBuilder.toString().ifBlank { "Sin respuesta textual del modelo." }
            return@withContext GenerationResult(
                text = resultText,
                thoughtProcess = thoughtText,
                groundingSources = groundingSources
            )
        } catch (e: Exception) {
            Log.e(TAG, "Request exception occurred", e)
            return@withContext GenerationResult(
                text = "❌ Ocurrió un error en la conexión: ${e.localizedMessage ?: "Fallo de comunicación"}",
                groundingSources = emptyList()
            )
        }
    }

    suspend fun generateSpeech(
        text: String,
        voiceName: String = "Kore"
    ): SpeechResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext SpeechResult(error = "Clave GEMINI_API_KEY no configurada.")
        }

        if (text.length > MAX_PROMPT_LENGTH) {
            return@withContext SpeechResult(error = "El texto para síntesis supera el límite máximo permitido.")
        }

        try {
            val jsonRoot = JSONObject()
            val contents = JSONArray()
            val content = JSONObject()
            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", text)
            parts.put(part)
            content.put("parts", parts)
            contents.put(content)
            jsonRoot.put("contents", contents)

            val genConfig = JSONObject()
            val modalities = JSONArray()
            modalities.put("AUDIO")
            genConfig.put("responseModalities", modalities)

            val speechConfig = JSONObject()
            val voiceConfig = JSONObject()
            val prebuilt = JSONObject()
            prebuilt.put("voiceName", voiceName)
            voiceConfig.put("prebuiltVoiceConfig", prebuilt)
            speechConfig.put("voiceConfig", voiceConfig)
            genConfig.put("speechConfig", speechConfig)

            jsonRoot.put("generationConfig", genConfig)

            val endpoint = "$baseUrl/models/gemini-2.5-flash-preview-tts:generateContent"
            val request = buildSecureRequest(endpoint, jsonRoot.toString(), apiKey)

            val (code, body) = executeWithRetry(request)

            if (code !in 200..299) {
                return@withContext SpeechResult(error = "Error TTS $code: $body")
            }

            val respJson = JSONObject(body)
            val candidates = respJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val partsArr = candidate?.optJSONObject("content")?.optJSONArray("parts")
            var audioBase64: String? = null
            var mimeType = "audio/wav"

            if (partsArr != null) {
                for (i in 0 until partsArr.length()) {
                    val p = partsArr.getJSONObject(i)
                    if (p.has("inlineData")) {
                        val inline = p.getJSONObject("inlineData")
                        audioBase64 = inline.getString("data")
                        mimeType = inline.optString("mimeType", "audio/wav")
                        break
                    }
                }
            }

            if (audioBase64 != null) {
                SpeechResult(audioData = Base64.decode(audioBase64, Base64.DEFAULT), mimeType = mimeType)
            } else {
                SpeechResult(error = "No se recibieron datos de audio en la respuesta.")
            }
        } catch (e: Exception) {
            SpeechResult(error = e.message)
        }
    }

    suspend fun generateImage(
        prompt: String,
        model: String = "gemini-nano-banana-2.1",
        aspectRatio: String = "1:1",
        imageSize: String = "1K"
    ): ImageResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext ImageResult(error = "Clave GEMINI_API_KEY no configurada.")
        }

        if (prompt.length > MAX_PROMPT_LENGTH) {
            return@withContext ImageResult(error = "El prompt de imagen excede el límite máximo.")
        }

        try {
            val jsonRoot = JSONObject()
            val contents = JSONArray()
            val content = JSONObject()
            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", prompt)
            parts.put(part)
            content.put("parts", parts)
            contents.put(content)
            jsonRoot.put("contents", contents)

            val genConfig = JSONObject()
            val modalities = JSONArray()
            modalities.put("TEXT")
            modalities.put("IMAGE")
            genConfig.put("responseModalities", modalities)

            val imgConfig = JSONObject()
            imgConfig.put("aspectRatio", aspectRatio)
            imgConfig.put("imageSize", imageSize)
            genConfig.put("imageConfig", imgConfig)

            jsonRoot.put("generationConfig", genConfig)

            val endpoint = "$baseUrl/models/$model:generateContent"
            val request = buildSecureRequest(endpoint, jsonRoot.toString(), apiKey)

            val (code, body) = executeWithRetry(request)

            if (code !in 200..299) {
                return@withContext ImageResult(error = "Error Imagen $code: $body")
            }

            val respJson = JSONObject(body)
            val candidates = respJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val partsArr = candidate?.optJSONObject("content")?.optJSONArray("parts")

            var imageBase64: String? = null
            var caption: String? = null

            if (partsArr != null) {
                for (i in 0 until partsArr.length()) {
                    val p = partsArr.getJSONObject(i)
                    if (p.has("inlineData")) {
                        imageBase64 = p.getJSONObject("inlineData").getString("data")
                    } else if (p.has("text")) {
                        caption = p.getString("text")
                    }
                }
            }

            if (imageBase64 != null) {
                ImageResult(imageBase64 = imageBase64, caption = caption)
            } else {
                ImageResult(error = "El modelo no generó una imagen para este prompt: $caption")
            }
        } catch (e: Exception) {
            ImageResult(error = e.message)
        }
    }

    suspend fun generateVideo(
        prompt: String,
        aspectRatio: String = "16:9"
    ): VideoResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext VideoResult(error = "Clave GEMINI_API_KEY no configurada.")
        }

        if (prompt.length > MAX_PROMPT_LENGTH) {
            return@withContext VideoResult(error = "El prompt de video excede el límite máximo.")
        }

        try {
            // Migrado de Veo 3.1 (retirado el 22-oct-2026) a Gemini Omni Flash (Interactions API).
            // Respuesta síncrona: el MP4 llega inline en Base64.
            val jsonRoot = JSONObject()
            jsonRoot.put("model", "gemini-omni-1.1-flash")
            jsonRoot.put("input", prompt)

            val responseFormat = JSONObject()
            responseFormat.put("type", "video")
            // Valores soportados: "9:16" y "16:9"
            responseFormat.put("aspect_ratio", aspectRatio)
            jsonRoot.put("response_format", responseFormat)

            val endpoint = "$baseUrl/interactions"
            val request = buildSecureRequest(endpoint, jsonRoot.toString(), apiKey)

            val (code, body) = executeWithRetry(request)

            if (code !in 200..299) {
                return@withContext VideoResult(error = "Omni Flash Error $code: $body")
            }

            val respJson = JSONObject(body)
            var videoBase64: String? = null
            var mimeType = "video/mp4"

            val steps = respJson.optJSONArray("steps")
            if (steps != null) {
                for (i in 0 until steps.length()) {
                    val step = steps.optJSONObject(i) ?: continue
                    if (step.optString("type") != "model_output") continue
                    val contents = step.optJSONArray("content") ?: continue
                    for (j in 0 until contents.length()) {
                        val c = contents.optJSONObject(j) ?: continue
                        if (c.optString("type") == "video") {
                            videoBase64 = c.optString("data")
                            mimeType = c.optString("mime_type", "video/mp4")
                            break
                        }
                    }
                    if (videoBase64 != null) break
                }
            }

            if (!videoBase64.isNullOrBlank()) {
                VideoResult(
                    videoBase64 = videoBase64,
                    message = "Video MP4 ($mimeType) generado con Gemini Omni Flash ($aspectRatio)."
                )
            } else {
                VideoResult(error = "El modelo no devolvió datos de video. Estado: ${respJson.optString("status", "unknown")}")
            }
        } catch (e: Exception) {
            VideoResult(error = e.message)
        }
    }

    suspend fun generateMusic(
        prompt: String,
        model: String = "lyria-3-clip-preview"
    ): MusicResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext MusicResult(error = "Clave GEMINI_API_KEY no configurada.")
        }

        if (prompt.length > MAX_PROMPT_LENGTH) {
            return@withContext MusicResult(error = "El prompt excede el límite permitido.")
        }

        try {
            val jsonRoot = JSONObject()
            val contents = JSONArray()
            val content = JSONObject()
            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", prompt)
            parts.put(part)
            content.put("parts", parts)
            contents.put(content)
            jsonRoot.put("contents", contents)

            val genConfig = JSONObject()
            val modalities = JSONArray()
            modalities.put("AUDIO")
            genConfig.put("responseModalities", modalities)
            jsonRoot.put("generationConfig", genConfig)

            val endpoint = "$baseUrl/models/$model:generateContent"
            val request = buildSecureRequest(endpoint, jsonRoot.toString(), apiKey)

            val (code, body) = executeWithRetry(request)

            if (code !in 200..299) {
                return@withContext MusicResult(error = "Lyria Error $code: $body")
            }

            val respJson = JSONObject(body)
            val candidates = respJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val partsArr = candidate?.optJSONObject("content")?.optJSONArray("parts")

            var audioBase64: String? = null
            if (partsArr != null) {
                for (i in 0 until partsArr.length()) {
                    val p = partsArr.getJSONObject(i)
                    if (p.has("inlineData")) {
                        audioBase64 = p.getJSONObject("inlineData").getString("data")
                        break
                    }
                }
            }

            if (audioBase64 != null) {
                MusicResult(audioBase64 = audioBase64)
            } else {
                MusicResult(error = "Pista musical procesada correctamente.")
            }
        } catch (e: Exception) {
            MusicResult(error = e.message)
        }
    }

    suspend fun transcribeAudio(
        audioBase64: String,
        mimeType: String = "audio/wav"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext "Error: Clave de API no configurada."
        }

        try {
            val jsonRoot = JSONObject()
            val contents = JSONArray()
            val content = JSONObject()
            val parts = JSONArray()

            val audioPart = JSONObject()
            val inlineData = JSONObject()
            inlineData.put("mimeType", mimeType)
            inlineData.put("data", audioBase64)
            audioPart.put("inlineData", inlineData)
            parts.put(audioPart)

            val textPart = JSONObject()
            textPart.put("text", "Transcribe este audio con exactitud y transcribe en texto claro.")
            parts.put(textPart)

            content.put("parts", parts)
            contents.put(content)
            jsonRoot.put("contents", contents)

            val endpoint = "$baseUrl/models/gemini-2.5-flash:generateContent"
            val request = buildSecureRequest(endpoint, jsonRoot.toString(), apiKey)

            val (code, body) = executeWithRetry(request)
            if (code !in 200..299) {
                return@withContext "Error $code: $body"
            }

            val respJson = JSONObject(body)
            val candidate = respJson.optJSONArray("candidates")?.optJSONObject(0)
            val partsArr = candidate?.optJSONObject("content")?.optJSONArray("parts")
            val transcribed = partsArr?.optJSONObject(0)?.optString("text")
            transcribed ?: "No se detectó habla en el audio."
        } catch (e: Exception) {
            "Error al transcribir: ${e.message}"
        }
    }
}

data class GenerationResult(
    val text: String,
    val thoughtProcess: String? = null,
    val groundingSources: List<String> = emptyList()
)

data class SpeechResult(
    val audioData: ByteArray? = null,
    val mimeType: String = "audio/wav",
    val error: String? = null
)

data class ImageResult(
    val imageBase64: String? = null,
    val caption: String? = null,
    val error: String? = null
)

data class VideoResult(
    @Deprecated("Omni Flash entrega el video inline; el campo se mantiene por compatibilidad retroactiva.")
    val operationName: String? = null,
    val videoBase64: String? = null,
    val message: String? = null,
    val error: String? = null
)

data class MusicResult(
    val audioBase64: String? = null,
    val error: String? = null
)
