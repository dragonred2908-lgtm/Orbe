package com.example.ui.multimodal

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiApiClient
import com.example.data.audio.AudioPlayerHelper
import com.example.data.media.MediaMetadataHelper
import com.example.ui.theme.GeminiAmber
import com.example.ui.theme.GeminiViolet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimodalLabScreen(
    apiClient: GeminiApiClient,
    audioPlayer: AudioPlayerHelper,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeLabTab by remember { mutableIntStateOf(0) } // 0: Imagen, 1: Video (Veo), 2: Música (Lyria), 3: Voz (TTS), 4: Transcripción

    val labTabs = listOf(
        "🖼️ Imágenes",
        "🎬 Video Veo",
        "🎵 Música Lyria",
        "🗣️ Voz TTS",
        "🎙️ Transcribir"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Laboratorio Multimodal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Generación de imagen, video Veo, música Lyria y audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = activeLabTab,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                labTabs.forEachIndexed { index, tabTitle ->
                    Tab(
                        selected = activeLabTab == index,
                        onClick = { activeLabTab = index },
                        text = { Text(tabTitle, fontWeight = if (activeLabTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (activeLabTab) {
                    0 -> item { ImageLabSection(apiClient) }
                    1 -> item { VideoLabSection(apiClient) }
                    2 -> item { MusicLabSection(apiClient, audioPlayer) }
                    3 -> item { TtsLabSection(apiClient, audioPlayer) }
                    4 -> item { TranscriptionLabSection(apiClient) }
                }
            }
        }
    }
}

@Composable
fun ImageLabSection(apiClient: GeminiApiClient) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("Un laboratorio futurista de inteligencia artificial con hologramas de Google AI Studio") }
    var selectedModel by remember { mutableStateOf("gemini-nano-banana-2.1") }
    var selectedRatio by remember { mutableStateOf("1:1") }
    var selectedSize by remember { mutableStateOf("1K") }
    var isGenerating by remember { mutableStateOf(false) }
    var resultImageBase64 by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var includeMetadata by remember { mutableStateOf(true) }
    var authorName by remember { mutableStateOf("Usuario IA Student") }
    var exportedExifSummary by remember { mutableStateOf<Map<String, String>?>(null) }

    val aspectRatios = listOf("1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "21:9")
    val imageSizes = listOf("1K", "2K", "4K")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            var imageMode by remember { mutableIntStateOf(0) } // 0: Generar, 1: Editar / Retocar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { imageMode = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (imageMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (imageMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Crear Foto", fontSize = 12.sp)
                }

                Button(
                    onClick = { imageMode = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (imageMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (imageMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar / Retocar", fontSize = 12.sp)
                }
            }

            if (imageMode == 1) {
                // MODO EDICIÓN / RETOQUE DE FOTOS
                Text("🎨 Retoque y Filtros de Edición con IA", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Selecciona un estilo de retoque o escribe las modificaciones deseadas:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                val editPresets = listOf(
                    "Estilo Cyberpunk Neón" to "Transformar la imagen con iluminación de neón cian y magenta, estética cyberpunk futurista de alta definición",
                    "Iluminación Cinemática" to "Mejorar la iluminación a nivel cinematográfico, tonos cálidos dorados, contraste profundo y bokeh sutil",
                    "Efecto Acuarela Artística" to "Convertir la composición en una pintura al óleo y acuarela impresionista detallada",
                    "Modo Ultra Nitidez 4K" to "Mejorar la resolución y nitidez de detalles faciales y texturas al máximo detalle fotográfico"
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(editPresets) { (label, presetPrompt) ->
                        FilterChip(
                            selected = prompt.contains(label),
                            onClick = { prompt = presetPrompt },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Text(
                if (imageMode == 0) "Modelos soportados: gemini-nano-banana-2.1 (uso general) y gemini-3-pro-image-preview (estudio)" else "El asistente aplicará los cambios estilísticos y fotográficos solicitados:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Model Selection
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedModel == "gemini-nano-banana-2.1",
                    onClick = { selectedModel = "gemini-nano-banana-2.1" },
                    label = { Text("Nano Banana 2.1") }
                )
                FilterChip(
                    selected = selectedModel == "gemini-3-pro-image-preview",
                    onClick = { selectedModel = "gemini-3-pro-image-preview" },
                    label = { Text("Pro Image Preview") }
                )
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text(if (imageMode == 0) "Prompt de la imagen a crear" else "Instrucciones de edición / retoque") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Aspect Ratio Selector
            Column {
                Text("Relación de aspecto (Aspect Ratio):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(aspectRatios) { ratio ->
                        FilterChip(
                            selected = selectedRatio == ratio,
                            onClick = { selectedRatio = ratio },
                            label = { Text(ratio, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Image Size Selector (1K, 2K, 4K)
            Column {
                Text("Resolución:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    imageSizes.forEach { size ->
                        FilterChip(
                            selected = selectedSize == size,
                            onClick = { selectedSize = size },
                            label = { Text(size, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isGenerating = true
                        errorMessage = null
                        resultImageBase64 = null
                        val res = apiClient.generateImage(
                            prompt = prompt,
                            model = selectedModel,
                            aspectRatio = selectedRatio,
                            imageSize = selectedSize
                        )
                        if (res.imageBase64 != null) {
                            resultImageBase64 = res.imageBase64
                        } else {
                            errorMessage = res.error ?: "No se pudo procesar la imagen."
                        }
                        isGenerating = false
                    }
                },
                enabled = prompt.isNotBlank() && !isGenerating,
                modifier = Modifier.fillMaxWidth().testTag("generate_image_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (imageMode == 0) "Generando foto..." else "Aplicando edición con IA...")
                } else {
                    Icon(if (imageMode == 0) Icons.Default.AutoFixHigh else Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (imageMode == 0) "Crear Foto ($selectedRatio, $selectedSize)" else "Aplicar Edición a la Foto")
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            if (resultImageBase64 != null) {
                val bytes = remember(resultImageBase64) {
                    try {
                        Base64.decode(resultImageBase64, Base64.DEFAULT)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bytes != null) {
                    val bitmap = remember(bytes) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Imagen generada",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "🏷️ Exportación con Metadatos Legales (EXIF)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { includeMetadata = !includeMetadata }
                                ) {
                                    Checkbox(
                                        checked = includeMetadata,
                                        onCheckedChange = { includeMetadata = it },
                                        modifier = Modifier.testTag("checkbox_include_metadata")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Incluir metadatos de copyright y autoría",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (includeMetadata) {
                                    OutlinedTextField(
                                        value = authorName,
                                        onValueChange = { authorName = it },
                                        label = { Text("Autor / Titular de la obra", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Button(
                                    onClick = {
                                        val savedFile = MediaMetadataHelper.saveBase64ImageWithMetadata(
                                            context = context,
                                            base64Data = resultImageBase64 ?: "",
                                            fileName = "gemini_gen_${System.currentTimeMillis()}.jpg",
                                            author = authorName,
                                            prompt = prompt,
                                            includeMetadata = includeMetadata
                                        )
                                        if (savedFile != null) {
                                            exportedExifSummary = MediaMetadataHelper.readExifSummary(savedFile)
                                            Toast.makeText(context, "Archivo guardado con éxito: ${savedFile.name}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Error al guardar el archivo", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("export_image_metadata_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Guardar Imagen en Dispositivo", fontSize = 13.sp)
                                }

                                if (exportedExifSummary != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "✅ Metadatos EXIF Estampados:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        exportedExifSummary?.forEach { (tag, value) ->
                                            Text(
                                                text = "• $tag: $value",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoLabSection(apiClient: GeminiApiClient) {
    val coroutineScope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("Un dron de Google sobrevolando una ciudad futurista con rascacielos holográficos al atardecer") }
    var selectedRatio by remember { mutableStateOf("16:9") } // 16:9 or 9:16
    var isGenerating by remember { mutableStateOf(false) }
    var operationResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = GeminiViolet)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🎬 Generación de Video con Veo 3.1", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Modelo: veo-3.1-fast-generate-preview. Genera clips a partir de descripciones textuales o animación de fotos.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Prompt para Veo 3.1") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Text("Relación de aspecto del video (Aspect Ratio):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedRatio == "16:9",
                    onClick = { selectedRatio = "16:9" },
                    label = { Text("16:9 (Horizontal / Landscape)") }
                )
                FilterChip(
                    selected = selectedRatio == "9:16",
                    onClick = { selectedRatio = "9:16" },
                    label = { Text("9:16 (Vertical / Portrait)") }
                )
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isGenerating = true
                        errorMessage = null
                        operationResult = null
                        val res = apiClient.generateVideo(prompt, selectedRatio)
                        if (res.message != null) {
                            operationResult = res.message
                        } else {
                            errorMessage = res.error ?: "Error al iniciar generación de video."
                        }
                        isGenerating = false
                    }
                },
                enabled = prompt.isNotBlank() && !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciando generación en Veo...")
                } else {
                    Icon(Icons.Default.VideoCall, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generar Video en Veo ($selectedRatio)")
                }
            }

            if (operationResult != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✅ $operationResult",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (errorMessage != null) {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MusicLabSection(apiClient: GeminiApiClient, audioPlayer: AudioPlayerHelper) {
    val coroutineScope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("Banda sonora orquestal épica inspirada en exploración espacial y tecnología") }
    var selectedModel by remember { mutableStateOf("lyria-3-clip-preview") }
    var isGenerating by remember { mutableStateOf(false) }
    var audioBase64Result by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = GeminiAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🎵 Generación de Música con Lyria", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Modelos: lyria-3-clip-preview (clips de hasta 30s) y lyria-3-pro-preview (pistas completas)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedModel == "lyria-3-clip-preview",
                    onClick = { selectedModel = "lyria-3-clip-preview" },
                    label = { Text("Clip Preview (30s)") }
                )
                FilterChip(
                    selected = selectedModel == "lyria-3-pro-preview",
                    onClick = { selectedModel = "lyria-3-pro-preview" },
                    label = { Text("Pro Preview (Completo)") }
                )
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Descripción de la pista musical") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        isGenerating = true
                        errorMessage = null
                        audioBase64Result = null
                        val res = apiClient.generateMusic(prompt, selectedModel)
                        if (res.audioBase64 != null) {
                            audioBase64Result = res.audioBase64
                        } else {
                            errorMessage = res.error ?: "Error al generar audio con Lyria."
                        }
                        isGenerating = false
                    }
                },
                enabled = prompt.isNotBlank() && !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Componiendo música...")
                } else {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generar Pista Musical")
                }
            }

            if (audioBase64Result != null) {
                FilledTonalButton(
                    onClick = {
                        audioPlayer.playBase64Audio(audioBase64Result ?: "")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reproducir Pista Generada")
                }
            }

            if (errorMessage != null) {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TtsLabSection(apiClient: GeminiApiClient, audioPlayer: AudioPlayerHelper) {
    val coroutineScope = rememberCoroutineScope()
    var textToSpeak by remember { mutableStateOf("¡Hola! Estás aprendiendo a crear aplicaciones con Google AI Studio y el modelo Gemini 3.8 Flash TTS.") }
    var selectedVoice by remember { mutableStateOf("Kore") }
    var isGenerating by remember { mutableStateOf(false) }
    var audioBytesResult by remember { mutableStateOf<ByteArray?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val voices = listOf("Kore", "Puck", "Fenrir", "Aoede")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🗣️ Texto a Voz (TTS)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Modelo: gemini-3.8-flash-tts. Genera voz natural y expresiva en múltiples idiomas.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = textToSpeak,
                onValueChange = { textToSpeak = it },
                label = { Text("Texto para vocalizar") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text("Voz predeterminada:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                voices.forEach { voice ->
                    FilterChip(
                        selected = selectedVoice == voice,
                        onClick = { selectedVoice = voice },
                        label = { Text(voice) }
                    )
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isGenerating = true
                        errorMessage = null
                        val res = apiClient.generateSpeech(textToSpeak, selectedVoice)
                        if (res.audioData != null) {
                            audioBytesResult = res.audioData
                            audioPlayer.playAudioBytes(res.audioData)
                        } else {
                            errorMessage = res.error ?: "Error al generar voz."
                        }
                        isGenerating = false
                    }
                },
                enabled = textToSpeak.isNotBlank() && !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sintetizando voz...")
                } else {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sintetizar y Reproducir con Gemini TTS")
                }
            }

            if (errorMessage != null) {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TranscriptionLabSection(apiClient: GeminiApiClient) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = GeminiViolet)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🎙️ Transcripción de Audio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Modelo: gemini-3.5-transcribe. Especializado en convertir grabaciones y audios a texto limpio y estructurado.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("💡 Cómo utilizarlo en tu app:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1. Captura el audio con AudioRecord o MediaRecorder en formato WAV/AAC.", fontSize = 11.sp)
                    Text("2. Conviértelo a Base64 en inlineData con mimeType 'audio/wav'.", fontSize = 11.sp)
                    Text("3. Envía al modelo gemini-3.5-transcribe para obtener la transcripción inmediata.", fontSize = 11.sp)
                }
            }
        }
    }
}
