package com.example.shared.stt

import com.example.shared.model.VoiceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

/**
 * Implementación de Speech-to-Text y procesamiento con Google Gemini API para JVM Desktop (Windows, Linux, macOS).
 * Utiliza java.net.http.HttpClient nativo de Java 11+, garantizando cero dependencias externas conflictivas.
 */
class JvmGeminiSttEngine(
    private val explicitApiKey: String? = null
) : SpeechToTextEngine {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(60))
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    /**
     * Resuelve la clave de API desde el valor explícito, variables de entorno o archivo .env.
     */
    fun resolveApiKey(configKey: String? = null): String {
        if (!configKey.isNullOrBlank() && configKey != "MY_GEMINI_API_KEY") {
            return configKey.trim()
        }
        if (!explicitApiKey.isNullOrBlank() && explicitApiKey != "MY_GEMINI_API_KEY") {
            return explicitApiKey.trim()
        }

        val envKey = System.getenv("GEMINI_API_KEY")
        if (!envKey.isNullOrBlank() && envKey != "MY_GEMINI_API_KEY") {
            return envKey.trim()
        }

        val propKey = System.getProperty("GEMINI_API_KEY")
        if (!propKey.isNullOrBlank() && propKey != "MY_GEMINI_API_KEY") {
            return propKey.trim()
        }

        // Intenta leer desde .env si existe en el directorio de trabajo
        try {
            val envFile = File(".env")
            if (envFile.exists()) {
                val lines = envFile.readLines()
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("GEMINI_API_KEY=") && !trimmed.startsWith("#")) {
                        val keyVal = trimmed.substringAfter("GEMINI_API_KEY=").trim().removeSurrounding("\"").removeSurrounding("'")
                        if (keyVal.isNotBlank() && keyVal != "MY_GEMINI_API_KEY") {
                            return keyVal
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignorar lectura si no se encuentra
        }

        return ""
    }

    override fun isAvailable(): Boolean {
        return resolveApiKey().isNotBlank()
    }

    override suspend fun transcribeAudio(audioBytes: ByteArray, mimeType: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("GEMINI_API_KEY no encontrada. Configúrala en la UI o en el archivo .env")
            )
        }

        if (audioBytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("El búfer de audio está vacío"))
        }

        try {
            val base64Audio = Base64.getEncoder().encodeToString(audioBytes)

            // Nota: la clave viaja en el encabezado 'x-goog-api-key' (nunca en la URL)
            // para evitar su filtración en logs de proxy o historial.
            val endpoint = "$baseUrl/models/gemini-2.5-flash:generateContent"

            val jsonPayload = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "inlineData": {
                        "mimeType": "$mimeType",
                        "data": "$base64Audio"
                      }
                    },
                    {
                      "text": "Transcribe exactamente en español las palabras habladas en este audio. Devuelve ÚNICAMENTE el texto transcrito sin comillas, sin explicaciones ni notas adicionales."
                    }
                  ]
                }
              ],
              "generationConfig": {
                "temperature": 0.0,
                "maxOutputTokens": 1024
              }
            }
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("x-goog-api-key", apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                val responseBody = response.body()
                val extractedText = extractTextFromGeminiResponse(responseBody)
                if (extractedText.isNotBlank()) {
                    Result.success(extractedText.trim())
                } else {
                    Result.success("No se detectó voz comprensible en la grabación.")
                }
            } else {
                val errorMsg = extractErrorMessage(response.body(), response.statusCode())
                Result.failure(RuntimeException("Error en API STT (${response.statusCode()}): $errorMsg"))
            }
        } catch (e: Exception) {
            // Re-lanzar la cancelación de la corrutina: nunca debe tragarse.
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun processQueryWithAI(query: String, config: VoiceConfig): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(config.geminiApiKey)
        if (apiKey.isBlank()) {
            // Fallback amigable cuando no hay clave aún configurada
            val fallbackMsg = """
                👋 **Hola, soy ${config.assistantName}**
                He detectado tu consulta: "*$query*".
                
                ⚠️ Para consultar los modelos de Google Gemini en tiempo real desde tu PC, por favor ingresa tu `GEMINI_API_KEY` en el botón de ajustes o configúrala en el archivo `.env`.
                
                💡 Tu frase de activación configurada es: **"${config.wakePhrase}"**.
            """.trimIndent()
            return@withContext Result.success(fallbackMsg)
        }

        try {
            val modelName = config.model.ifBlank { "gemini-3.8-flash" }
            // Key en encabezado 'x-goog-api-key' (nunca en query string)
            val endpoint = "$baseUrl/models/$modelName:generateContent"

            val escapedQuery = escapeJsonString(query)
            val escapedInstruction = escapeJsonString(config.systemInstruction)

            val jsonPayload = """
            {
              "contents": [
                {
                  "role": "user",
                  "parts": [
                    { "text": "$escapedQuery" }
                  ]
                }
              ],
              "systemInstruction": {
                "parts": [
                  { "text": "$escapedInstruction" }
                ]
              },
              "generationConfig": {
                "temperature": 0.7,
                "maxOutputTokens": 2048
              }
            }
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("x-goog-api-key", apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                val text = extractTextFromGeminiResponse(response.body())
                Result.success(text.ifBlank { "Respuesta recibida sin contenido textual." })
            } else {
                val errorMsg = extractErrorMessage(response.body(), response.statusCode())
                Result.failure(RuntimeException("Error Gemini API (${response.statusCode()}): $errorMsg"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    private fun escapeJsonString(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Extrae el texto principal desde la respuesta JSON de Gemini sin requerir librerías externas pesadas.
     */
    private fun extractTextFromGeminiResponse(json: String): String {
        val marker = "\"text\":"
        val sb = StringBuilder()
        var pos = 0
        while (true) {
            val idx = json.indexOf(marker, pos)
            if (idx == -1) break

            val afterMarker = idx + marker.length
            var quoteStart = json.indexOf('"', afterMarker)
            if (quoteStart == -1) break
            quoteStart++

            val strBuilder = StringBuilder()
            var i = quoteStart
            var escaped = false
            while (i < json.length) {
                val c = json[i]
                if (escaped) {
                    when (c) {
                        'n' -> strBuilder.append('\n')
                        'r' -> strBuilder.append('\r')
                        't' -> strBuilder.append('\t')
                        '\"' -> strBuilder.append('\"')
                        '\\' -> strBuilder.append('\\')
                        'u' -> {
                            if (i + 4 < json.length) {
                                val hex = json.substring(i + 1, i + 5)
                                try {
                                    strBuilder.append(hex.toInt(16).toChar())
                                    i += 4
                                } catch (e: Exception) {
                                    strBuilder.append("\\u").append(hex)
                                }
                            }
                        }
                        else -> strBuilder.append(c)
                    }
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '\"') {
                    break
                } else {
                    strBuilder.append(c)
                }
                i++
            }

            if (strBuilder.isNotEmpty()) {
                if (sb.isNotEmpty()) sb.append("\n")
                sb.append(strBuilder.toString())
            }
            pos = i + 1
        }
        return sb.toString().trim()
    }

    private fun extractErrorMessage(body: String, status: Int): String {
        return try {
            val msgMarker = "\"message\":"
            val idx = body.indexOf(msgMarker)
            if (idx != -1) {
                val start = body.indexOf('"', idx + msgMarker.length) + 1
                val end = body.indexOf('"', start)
                if (start in 1..end) {
                    body.substring(start, end)
                } else {
                    "HTTP $status"
                }
            } else {
                "HTTP $status"
            }
        } catch (e: Exception) {
            "HTTP $status"
        }
    }
}
