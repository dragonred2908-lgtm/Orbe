package com.example.data.api

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY") {
                ""
            } else {
                key
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun isApiKeyConfigured(): Boolean {
        return getApiKey().isNotBlank()
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
                text = "⚠️ **Clave de API no configurada**\n\nPara comunicarte directamente con los modelos de Google Gemini, por favor agrega tu `GEMINI_API_KEY` en el panel de **Secrets** de AI Studio.\n\n💡 *Mientras tanto, puedes explorar todas las secciones educativas del paso a paso, generadores de código y guías en IA Student.*",
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

            // Thinking mode: if High Thinking is enabled and model is gemini-4-argon,
            // set thinkingLevel to HIGH and do NOT set maxOutputTokens
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

            val url = "$baseUrl/models/$model:generateContent?key=$apiKey"
            val requestBody = jsonRoot.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiApiClient", "Error ${response.code}: $responseBody")
                val errMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    response.message
                }
                return@withContext GenerationResult(
                    text = "❌ Error al consultar $model (${response.code}): $errMsg",
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
            Log.e("GeminiApiClient", "Request exception", e)
            return@withContext GenerationResult(
                text = "❌ Ocurrió un error en la conexión: ${e.localizedMessage ?: e.message}",
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
            return@withContext SpeechResult(error = "Clave GEMINI_API_KEY no configurada en Secrets.")
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

            val url = "$baseUrl/models/gemini-3.8-flash-tts:generateContent?key=$apiKey"
            val requestBody = jsonRoot.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext SpeechResult(error = "Error TTS ${response.code}: $body")
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

            val url = "$baseUrl/models/$model:generateContent?key=$apiKey"
            val requestBody = jsonRoot.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext ImageResult(error = "Error Imagen ${response.code}: $body")
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
        aspectRatio: String = "16:9" // 16:9 or 9:16
    ): VideoResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext VideoResult(error = "Clave GEMINI_API_KEY no configurada.")
        }

        try {
            val jsonRoot = JSONObject()
            jsonRoot.put("prompt", prompt)

            val config = JSONObject()
            config.put("numberOfVideos", 1)
            config.put("resolution", "720p")
            config.put("aspectRatio", aspectRatio)
            jsonRoot.put("config", config)

            val url = "$baseUrl/models/veo-3.1-fast-generate-preview:generateVideos?key=$apiKey"
            val requestBody = jsonRoot.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext VideoResult(error = "Veo Error ${response.code}: $body")
            }

            val respJson = JSONObject(body)
            val operationName = respJson.optString("name", "Operación iniciada con éxito")
            VideoResult(operationName = operationName, message = "Generación de video iniciada en Veo 3.1 ($aspectRatio). Enlace/Operación: $operationName")
        } catch (e: Exception) {
            VideoResult(error = e.message)
        }
    }

    suspend fun generateMusic(
        prompt: String,
        model: String = "lyria-3-clip-preview" // or lyria-3-pro-preview
    ): MusicResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext MusicResult(error = "Clave GEMINI_API_KEY no configurada.")
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

            val url = "$baseUrl/models/$model:generateContent?key=$apiKey"
            val requestBody = jsonRoot.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext MusicResult(error = "Lyria Error ${response.code}: $body")
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

            val url = "$baseUrl/models/gemini-3.5-transcribe:generateContent?key=$apiKey"
            val requestBody = jsonRoot.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "Error ${response.code}: $body"
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
    val operationName: String? = null,
    val message: String? = null,
    val error: String? = null
)

data class MusicResult(
    val audioBase64: String? = null,
    val error: String? = null
)
