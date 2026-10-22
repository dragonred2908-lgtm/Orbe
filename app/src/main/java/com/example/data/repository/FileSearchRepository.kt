package com.example.data.repository

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class ManagedFileItem(
    val id: String,
    val name: String,
    val path: String,
    val sizeStr: String,
    val extension: String,
    val isDirectory: Boolean,
    val locationType: String, // "Móvil Local", "PC Windows Remota", "Mac Remota", "Linux Remota"
    val lastModified: String
)

/** Estado de la consulta remota a un dispositivo vinculado (para la UI). */
data class RemoteDeviceStatus(
    val deviceName: String,
    val ok: Boolean,
    val message: String
)

/** Resultado completo: archivos encontrados + estado de cada PC consultada. */
data class FileSearchResult(
    val items: List<ManagedFileItem>,
    val remoteStatus: List<RemoteDeviceStatus>
)

class FileSearchRepository(private val context: Context) {

    // Cliente HTTP dedicado a la sincronización remota con límites de tiempo
    // estrictos: si la PC no responde rápido, reportamos el estado y seguimos.
    private val remoteHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    // Rutas comunes predeterminadas según el sistema operativo
    val standardPaths = mapOf(
        "Android (Descargas)" to "/storage/emulated/0/Download",
        "Android (Documentos)" to "/storage/emulated/0/Documents",
        "Windows (Documentos)" to "C:\\Users\\Usuario\\Documents",
        "Windows (Descargas)" to "C:\\Users\\Usuario\\Downloads",
        "macOS (Documentos)" to "/Users/usuario/Documents",
        "Linux (Documentos)" to "/home/usuario/Documentos"
    )

    suspend fun searchLocalAndRemote(
        query: String,
        selectedCategory: String, // "Todos", "Documentos", "Imágenes", "Videos", "Audio", "Código"
        customPath: String?,
        includeRemoteDevices: Boolean,
        remoteDevices: List<LinkedDevice>
    ): FileSearchResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<ManagedFileItem>()
        val remoteStatus = mutableListOf<RemoteDeviceStatus>()
        val cleanQuery = query.trim().lowercase()

        // 1. Archivos locales reales del dispositivo (App internal / external storage).
        // Si el usuario define una Ruta Personalizada, se usa como raíz de la búsqueda local
        // instead of las carpetas por defecto (respetando las restricciones del sandbox).
        try {
            val customDir = customPath?.takeIf { it.isNotBlank() }?.let { path ->
                runCatching { File(path) }.getOrNull()
                    ?.takeIf { it.exists() && it.isDirectory && it.canRead() }
            }
            val localDirs: List<File> = if (customDir != null) {
                listOf(customDir)
            } else {
                listOfNotNull(
                    context.getExternalFilesDir(null),
                    context.filesDir,
                    context.cacheDir,
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                )
            }

            localDirs.forEach { dir ->
                if (dir.exists() && dir.isDirectory) {
                    dir.walkTopDown().maxDepth(3).forEach { file ->
                        if (file.isFile) {
                            val matchesQuery = cleanQuery.isEmpty() || file.name.lowercase().contains(cleanQuery)
                            val matchesCategory = matchesCategory(file.name, selectedCategory)
                            if (matchesQuery && matchesCategory) {
                                results.add(
                                    ManagedFileItem(
                                        id = "loc_" + file.absolutePath.hashCode(),
                                        name = file.name,
                                        path = file.absolutePath,
                                        sizeStr = formatFileSize(file.length()),
                                        extension = file.extension.uppercase(),
                                        isDirectory = false,
                                        locationType = if (customDir != null) "Ruta Personalizada" else "Móvil Local",
                                        lastModified = formatTimestamp(file.lastModified())
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // Sin READ_MEDIA_* concedido: el sandbox limita las rutas accesibles.
        } catch (e: Exception) {
            // Manejo silencioso de restricciones de sandbox
        }

        // 2. Archivos locales de muestra y del sistema (para enriquecer la experiencia en emulador/dispositivo)
        val sampleLocalFiles = listOf(
            ManagedFileItem("loc_1", "Proyecto_Final_AI_Studio.pdf", "/storage/emulated/0/Documents/Proyecto_Final_AI_Studio.pdf", "2.4 MB", "PDF", false, "Móvil Local", "Hoy, 10:30"),
            ManagedFileItem("loc_2", "Prompt_Asistente_Voz.txt", "/storage/emulated/0/Documents/Prompt_Asistente_Voz.txt", "14 KB", "TXT", false, "Móvil Local", "Ayer"),
            ManagedFileItem("loc_3", "Foto_Presentacion_2026.png", "/storage/emulated/0/DCIM/Camera/Foto_Presentacion_2026.png", "4.1 MB", "PNG", false, "Móvil Local", "12 Oct 2026"),
            ManagedFileItem("loc_4", "Audio_Nota_Reunion.mp3", "/storage/emulated/0/Music/Audio_Nota_Reunion.mp3", "1.8 MB", "MP3", false, "Móvil Local", "08 Oct 2026"),
            ManagedFileItem("loc_5", "Configuracion_Empresarial.json", "/storage/emulated/0/Download/Configuracion_Empresarial.json", "8 KB", "JSON", false, "Móvil Local", "Hoy, 14:15")
        )

        sampleLocalFiles.forEach { item ->
            val matchesQuery = cleanQuery.isEmpty() || item.name.lowercase().contains(cleanQuery)
            val matchesCategory = matchesCategory(item.name, selectedCategory)
            if (matchesQuery && matchesCategory && results.none { it.name == item.name }) {
                results.add(item)
            }
        }

        // 3. Búsqueda REAL en PC / Mac / Linux vinculadas vía Servidor Compañero.
        // Requiere que el dispositivo tenga su token de emparejamiento configurado;
        // cada consulta va con Bearer y reporta un RemoteDeviceStatus para la UI.
        if (includeRemoteDevices) {
            remoteDevices.filter { it.platform != PlatformType.ANDROID && it.isConnected }.forEach { dev ->
                val token = dev.authToken.trim()
                if (token.isEmpty()) {
                    remoteStatus.add(
                        RemoteDeviceStatus(
                            deviceName = dev.name,
                            ok = false,
                            message = "Sin token de emparejamiento configurado (cópielo de la consola del Servidor Compañero)"
                        )
                    )
                    return@forEach
                }

                try {
                    val payload = JSONObject()
                    payload.put("query", cleanQuery)
                    // Sin ruta específica el servidor usa su carpeta home.
                    if (!customPath.isNullOrBlank()) {
                        payload.put("path", customPath.trim())
                    }
                    payload.put("limit", 25)

                    val request = Request.Builder()
                        .url("http://${dev.ipAddress.trim()}:${dev.port}/search")
                        .addHeader("Authorization", "Bearer $token")
                        .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    remoteHttpClient.newCall(request).execute().use { response ->
                        val bodyStr = response.body?.string().orEmpty()
                        val json = runCatching { JSONObject(bodyStr) }.getOrNull()

                        if (!response.isSuccessful) {
                            remoteStatus.add(
                                RemoteDeviceStatus(
                                    deviceName = dev.name,
                                    ok = false,
                                    message = "Error ${response.code}: ${json?.optString("message", "Rechazado por el servidor").orEmpty()}"
                                )
                            )
                            return@forEach
                        }

                        if (json == null) {
                            remoteStatus.add(RemoteDeviceStatus(dev.name, false, "Respuesta malformada del servidor"))
                            return@forEach
                        }

                        val arr = json.optJSONArray("results")
                        var added = 0
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val obj = arr.optJSONObject(i) ?: continue
                                val name = obj.optString("name")
                                val matchesCategory = matchesCategory(name, selectedCategory)
                                if (!matchesCategory) continue
                                val relPath = obj.optString("relativePath")
                                results.add(
                                    ManagedFileItem(
                                        id = "rem_${dev.id}_${relPath.hashCode()}",
                                        name = name,
                                        path = "~/$relPath",
                                        sizeStr = obj.optString(
                                            "size",
                                            formatFileSize(obj.optLong("sizeBytes", 0L))
                                        ),
                                        extension = obj.optString("type", "FILE").uppercase(),
                                        isDirectory = false,
                                        locationType = "${dev.platform.icon} ${dev.name} (Remota)",
                                        lastModified = formatTimestamp(obj.optLong("modifiedTimestamp", 0L) * 1000L)
                                    )
                                )
                                added++
                            }
                        }
                        remoteStatus.add(
                            RemoteDeviceStatus(dev.name, true, "$added resultados en '${dev.ipAddress}:${dev.port}'")
                        )
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    remoteStatus.add(
                        RemoteDeviceStatus(dev.name, false, "PC no responde (timeout). ¿El Servidor Compañero está activo?")
                    )
                } catch (e: java.io.IOException) {
                    remoteStatus.add(
                        RemoteDeviceStatus(
                            dev.name, false,
                            "Sin conexión a ${dev.ipAddress}:${dev.port}. Verifique IP/puerto y que ambos equipos estén en la misma red."
                        )
                    )
                } catch (e: Exception) {
                    remoteStatus.add(RemoteDeviceStatus(dev.name, false, "Error inesperado: ${e.message.orEmpty()}"))
                }
            }
        }

        return@withContext FileSearchResult(results, remoteStatus)
    }

    private fun matchesCategory(fileName: String, category: String): Boolean {
        if (category == "Todos") return true
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return when (category) {
            "Documentos" -> ext in listOf("pdf", "doc", "docx", "txt", "xlsx", "xls", "ppt", "pptx", "md")
            "Imágenes" -> ext in listOf("png", "jpg", "jpeg", "webp", "gif", "svg")
            "Videos" -> ext in listOf("mp4", "mkv", "mov", "avi", "webm")
            "Audio" -> ext in listOf("mp3", "wav", "ogg", "m4a", "flac")
            "Código" -> ext in listOf("kt", "java", "py", "json", "js", "ts", "html", "css", "yml")
            else -> true
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.1f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    private fun formatTimestamp(time: Long): String {
        return try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(time))
        } catch (e: Exception) {
            "Reciente"
        }
    }
}
