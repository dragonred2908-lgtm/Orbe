package com.example.data.media

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaMetadataHelper {

    const val DEFAULT_SOFTWARE = "IA Student Multiplatform v1.0.0"
    const val DEFAULT_COPYRIGHT = "Copyright (c) 2026 IA Student. Licenciado bajo Apache-2.0"
    const val DEFAULT_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"

    private const val TAG = "MediaMetadataHelper"

    /**
     * Inyecta metadatos EXIF estándar en un archivo de imagen existente.
     */
    fun injectImageMetadata(
        imageFile: File,
        author: String = "Usuario IA Student",
        prompt: String = "",
        copyright: String = DEFAULT_COPYRIGHT,
        software: String = DEFAULT_SOFTWARE
    ): Boolean {
        return try {
            val exif = ExifInterface(imageFile.absolutePath)
            val currentDate = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())

            exif.setAttribute(ExifInterface.TAG_ARTIST, author.trim())
            exif.setAttribute(ExifInterface.TAG_COPYRIGHT, copyright.trim())
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, software)
            exif.setAttribute(ExifInterface.TAG_DATETIME, currentDate)

            if (prompt.isNotBlank()) {
                exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "Prompt: ${prompt.trim()}")
                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "Generado con Google Gemini. $DEFAULT_LICENSE_URL")
            }

            exif.saveAttributes()
            true
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron inyectar los metadatos EXIF", e)
            false
        }
    }

    /**
     * Guarda una imagen desde Base64 e inyecta metadatos EXIF si includeMetadata es true.
     */
    fun saveBase64ImageWithMetadata(
        context: Context,
        base64Data: String,
        fileName: String,
        author: String = "Usuario IA Student",
        prompt: String = "",
        includeMetadata: Boolean = true
    ): File? {
        return try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val outputDir = File(context.filesDir, "exported_media").apply { if (!exists()) mkdirs() }
            val outputFile = File(outputDir, fileName)

            FileOutputStream(outputFile).use { it.write(bytes) }

            if (includeMetadata) {
                injectImageMetadata(
                    imageFile = outputFile,
                    author = author,
                    prompt = prompt
                )
            }

            outputFile
        } catch (e: Exception) {
            Log.w(TAG, "Error guardando imagen con metadatos", e)
            null
        }
    }

    /**
     * Lee un resumen estructurado de los metadatos EXIF presentes en la imagen.
     */
    fun readExifSummary(file: File): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val exif = ExifInterface(file.absolutePath)
            exif.getAttribute(ExifInterface.TAG_ARTIST)?.let { result["Autor"] = it }
            exif.getAttribute(ExifInterface.TAG_COPYRIGHT)?.let { result["Copyright"] = it }
            exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.let { result["Software"] = it }
            exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)?.let { result["Descripción"] = it }
            exif.getAttribute(ExifInterface.TAG_USER_COMMENT)?.let { result["Comentarios"] = it }
            exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { result["Fecha"] = it }
        } catch (e: Exception) {
            Log.w(TAG, "Error leyendo metadatos EXIF", e)
        }
        return result
    }
}
