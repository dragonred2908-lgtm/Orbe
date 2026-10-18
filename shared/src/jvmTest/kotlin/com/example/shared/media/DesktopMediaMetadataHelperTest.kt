package com.example.shared.media

import com.example.shared.model.MediaExportMetadata
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopMediaMetadataHelperTest {

    @Test
    fun testExportWithCompanionMetadataCreatesSidecar() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test_media_export_${System.currentTimeMillis()}")
        try {
            val metadata = MediaExportMetadata(
                id = "test-uuid-1234",
                fileName = "sample_artwork.jpg",
                author = "Dra. Elena Ramos",
                prompt = "Un asteroide con cristales luminosos en órbita",
                copyright = "Copyright (c) 2026 Dra. Elena Ramos. Apache-2.0",
                licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
                generator = "Google Gemini via IA Student",
                timestamp = 1792000000000L,
                format = "JPEG"
            )

            val dummyBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())

            val (mediaFile, metaFile) = DesktopMediaMetadataHelper.exportMediaWithCompanionMetadata(
                targetDirectory = tempDir,
                baseFileName = "sample_artwork.jpg",
                contentBytes = dummyBytes,
                metadata = metadata,
                includeMetadata = true
            )

            assertTrue(mediaFile.exists(), "Media file should exist on disk")
            assertEquals(4, mediaFile.length(), "Media file length should match written bytes")

            assertNotNull(metaFile, "Metadata sidecar file should not be null")
            assertTrue(metaFile.exists(), "Metadata sidecar file should exist")
            assertTrue(metaFile.name.endsWith(".meta.json"), "Metadata filename should end with .meta.json")

            val jsonContent = metaFile.readText()
            assertTrue(jsonContent.contains("\"id\": \"test-uuid-1234\""))
            assertTrue(jsonContent.contains("\"author\": \"Dra. Elena Ramos\""))
            assertTrue(jsonContent.contains("\"prompt\": \"Un asteroide con cristales luminosos en órbita\""))
            assertTrue(jsonContent.contains("\"license\": \"https://www.apache.org/licenses/LICENSE-2.0\""))
            assertTrue(jsonContent.contains("\"metadataStandard\": \"EXIF/XMP Sidecar Compliant\""))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testExportWithoutMetadataDoesNotCreateSidecar() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test_media_export_no_meta_${System.currentTimeMillis()}")
        try {
            val metadata = MediaExportMetadata(
                id = "test-uuid-5678",
                fileName = "plain_sound.wav"
            )
            val dummyBytes = byteArrayOf(0x52, 0x49, 0x46, 0x46)

            val (mediaFile, metaFile) = DesktopMediaMetadataHelper.exportMediaWithCompanionMetadata(
                targetDirectory = tempDir,
                baseFileName = "plain_sound.wav",
                contentBytes = dummyBytes,
                metadata = metadata,
                includeMetadata = false
            )

            assertTrue(mediaFile.exists())
            assertNull(metaFile, "Sidecar should be null when includeMetadata is false")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
