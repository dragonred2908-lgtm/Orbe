package com.example.data.repository

import android.content.Context
import android.os.Environment
import java.io.File

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

class FileSearchRepository(private val context: Context) {

    // Rutas comunes predeterminadas según el sistema operativo
    val standardPaths = mapOf(
        "Android (Descargas)" to "/storage/emulated/0/Download",
        "Android (Documentos)" to "/storage/emulated/0/Documents",
        "Windows (Documentos)" to "C:\\Users\\Usuario\\Documents",
        "Windows (Descargas)" to "C:\\Users\\Usuario\\Downloads",
        "macOS (Documentos)" to "/Users/usuario/Documents",
        "Linux (Documentos)" to "/home/usuario/Documentos"
    )

    fun searchLocalAndRemote(
        query: String,
        selectedCategory: String, // "Todos", "Documentos", "Imágenes", "Videos", "Audio", "Código"
        customPath: String?,
        includeRemoteDevices: Boolean,
        remoteDevices: List<LinkedDevice>
    ): List<ManagedFileItem> {
        val results = mutableListOf<ManagedFileItem>()
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

        // 3. Archivos Remotos en PC / Mac / Linux (Sincronización remota simulada y conectada)
        if (includeRemoteDevices) {
            remoteDevices.filter { it.isConnected }.forEach { dev ->
                val base = customPath?.ifBlank { dev.defaultSearchPath } ?: dev.defaultSearchPath
                val remoteItems = when (dev.platform) {
                    PlatformType.WINDOWS -> listOf(
                        ManagedFileItem("rem_win_1", "Informe_Financiero_Q3.xlsx", "$base\\Informe_Financiero_Q3.xlsx", "3.2 MB", "XLSX", false, "PC Windows Remota (${dev.name})", "Hoy, 09:12"),
                        ManagedFileItem("rem_win_2", "Contrato_Cliente_Firmado.pdf", "$base\\Contrato_Cliente_Firmado.pdf", "1.1 MB", "PDF", false, "PC Windows Remota (${dev.name})", "Ayer, 16:45"),
                        ManagedFileItem("rem_win_3", "Video_Demostracion_Producto.mp4", "C:\\Users\\Usuario\\Videos\\Video_Demostracion_Producto.mp4", "45.6 MB", "MP4", false, "PC Windows Remota (${dev.name})", "11 Oct 2026"),
                        ManagedFileItem("rem_win_4", "Diagrama_Arquitectura_AI.drawio", "$base\\Diagrama_Arquitectura_AI.drawio", "520 KB", "DRAWIO", false, "PC Windows Remota (${dev.name})", "10 Oct 2026")
                    )
                    PlatformType.MACOS -> listOf(
                        ManagedFileItem("rem_mac_1", "Design_System_Figma_Export.sketch", "$base/Design_System_Figma_Export.sketch", "18.4 MB", "SKETCH", false, "Mac Remota (${dev.name})", "13 Oct 2026"),
                        ManagedFileItem("rem_mac_2", "Podcast_Episodio_Master.wav", "/Users/usuario/Music/Podcast_Episodio_Master.wav", "68.2 MB", "WAV", false, "Mac Remota (${dev.name})", "Hoy, 11:00")
                    )
                    PlatformType.LINUX -> listOf(
                        ManagedFileItem("rem_lin_1", "deploy_docker_compose.yml", "$base/deploy_docker_compose.yml", "6 KB", "YML", false, "Linux Remota (${dev.name})", "Hoy, 08:30"),
                        ManagedFileItem("rem_lin_2", "backup_database.tar.gz", "/home/usuario/Backups/backup_database.tar.gz", "128.5 MB", "TAR.GZ", false, "Linux Remota (${dev.name})", "09 Oct 2026")
                    )
                    else -> emptyList()
                }

                remoteItems.forEach { rItem ->
                    val matchesQuery = cleanQuery.isEmpty() || rItem.name.lowercase().contains(cleanQuery)
                    val matchesCategory = matchesCategory(rItem.name, selectedCategory)
                    if (matchesQuery && matchesCategory) {
                        results.add(rItem)
                    }
                }
            }
        }

        return results
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
