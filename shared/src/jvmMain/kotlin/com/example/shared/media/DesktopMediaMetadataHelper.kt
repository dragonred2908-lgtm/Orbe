package com.example.shared.media

import com.example.shared.model.MediaExportMetadata
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DesktopMediaMetadataHelper {

    /**
     * Exporta el contenido binario a disco y, opcionalmente, genera un archivo
     * complementario de metadatos (sidecar) en formato JSON estandarizado.
     */
    fun exportMediaWithCompanionMetadata(
        targetDirectory: File,
        baseFileName: String,
        contentBytes: ByteArray,
        metadata: MediaExportMetadata,
        includeMetadata: Boolean = true
    ): Pair<File, File?> {
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
        }

        val mediaFile = File(targetDirectory, baseFileName)
        FileOutputStream(mediaFile).use { it.write(contentBytes) }

        var metadataFile: File? = null
        if (includeMetadata) {
            val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(metadata.timestamp.takeIf { it > 0 } ?: System.currentTimeMillis()))
            val metaFileName = "$baseFileName.meta.json"
            metadataFile = File(targetDirectory, metaFileName)

            val jsonContent = """
{
  "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
  "id": "${metadata.id}",
  "fileName": "${metadata.fileName}",
  "format": "${metadata.format}",
  "author": "${escapeJson(metadata.author)}",
  "prompt": "${escapeJson(metadata.prompt)}",
  "copyright": "${escapeJson(metadata.copyright)}",
  "license": "${metadata.licenseUrl}",
  "generator": "${escapeJson(metadata.generator)}",
  "createdDate": "$isoDate",
  "metadataStandard": "EXIF/XMP Sidecar Compliant"
}
            """.trimIndent()

            metadataFile.writeText(jsonContent)
        }

        return Pair(mediaFile, metadataFile)
    }

    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
