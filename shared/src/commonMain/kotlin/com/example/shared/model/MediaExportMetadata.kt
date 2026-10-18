package com.example.shared.model

data class MediaExportMetadata(
    val id: String,
    val fileName: String,
    val author: String = "IA Student Desktop User",
    val prompt: String = "",
    val copyright: String = "Copyright (c) 2026 IA Student. Licensed under Apache-2.0",
    val licenseUrl: String = "https://www.apache.org/licenses/LICENSE-2.0",
    val generator: String = "Google Gemini Multimodal API via IA Student Desktop",
    val timestamp: Long = 0L,
    val format: String = "JPEG"
)
