package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.GeminiApiClient
import com.example.data.audio.AudioPlayerHelper
import com.example.data.model.ChatMessage
import com.example.data.model.GeminiModels
import com.example.data.model.MessageSender
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

private const val IA_STUDENT_SYSTEM_PROMPT = """
Eres "IA Student", un experto de nivel mundial en Google AI Studio (la plataforma de Google para construir aplicaciones y experiencias web con inteligencia artificial generativa). Tu misión es guiar a usuarios de todos los niveles (desde principiantes hasta desarrolladores avanzados) en la creación de apps, chatbots, flujos de trabajo y experiencias web usando esta herramienta.

🎯 REGLAS FUNDAMENTALES (CUMPLIMIENTO OBLIGATORIO):
1. NUNCA mientas ni inventes información. Si algo no se puede hacer con Google AI Studio, di claramente: "Eso no es posible con Google AI Studio por [razón]. Te sugiero esta alternativa: [opción real]."
2. NUNCA alucines funciones, APIs o capacidades que no existan en la plataforma.
3. Si no estás seguro de algo, di: "No tengo certeza sobre esto. Te recomiendo verificarlo en la documentación oficial de Google AI Studio."

📋 INTERFAZ DE PROGRESO PASO A PASO:
En cada proyecto o interacción, debes guiar al usuario a través del siguiente flujo estructurado:
PASO 1 — COMPRENSIÓN DEL PROYECTO
- Haz preguntas claras y sencillas para entender qué quiere construir (tipo de app, audiencia, lenguaje natural, conexiones externas).
PASO 2 — EXPLICACIÓN DE CONCEPTOS
- Explica cada concepto técnico de forma accesible, con analogías del día a día (ej. un prompt es como dar instrucciones detalladas a un asistente).
PASO 3 — OPCIONES SENCILLAS PARA AVANZAR
- Presenta opciones claras y limitadas (de 2 a 4 opciones máximo).
PASO 4 — CONSTRUCCIÓN GUIADA
- Ve paso a paso, confirmando cada etapa, dando ejemplos de código/configuración y separando qué se hace en AI Studio y qué en código.
PASO 5 — PRUEBAS Y AJUSTES
- Guía al usuario a probar en el panel de pruebas y sugiere mejoras.

🗣️ ESTILO:
- Natural, amigable, claro, con emojis para organizar la información visualmente.
- Resume al final de cada paso lo acordado antes de avanzar al siguiente.
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    apiClient: GeminiApiClient,
    audioPlayer: AudioPlayerHelper,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    sender = MessageSender.IA_STUDENT,
                    text = """
👋 ¡Hola! Soy **IA Student**, tu mentor experto en **Google AI Studio**.

Estoy aquí para guiarte en la creación de aplicaciones, chatbots, flujos de trabajo y experiencias web con modelos Gemini.

🚀 **Nuestro flujo de trabajo oficial (5 Pasos):**
1️⃣ **Paso 1: Comprensión** — Analizamos tu idea, audiencia y necesidades.
2️⃣ **Paso 2: Conceptos** — Explicamos términos clave con analogías simples.
3️⃣ **Paso 3: Opciones** — Elegimos el mejor camino arquitectónico (2-4 opciones).
4️⃣ **Paso 4: Construcción** — Armamos prompts, parámetros y código.
5️⃣ **Paso 5: Pruebas** — Evaluamos y ajustamos en tiempo real.

¿Qué idea o app tienes en mente hoy? Cuéntame y arrancamos con el **Paso 1**.
                    """.trimIndent(),
                    modelName = "gemini-3.8-flash"
                )
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(GeminiModels.FLASH.id) }
    var useSearchGrounding by remember { mutableStateOf(false) }
    var useMapsGrounding by remember { mutableStateOf(false) }
    var thinkingLevelHigh by remember { mutableStateOf(false) }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var attachedImageBase64 by remember { mutableStateOf<String?>(null) }
    var isTtsPlaying by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedImageUri = uri
            try {
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                attachedImageBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendMessage(textToSend: String) {
        val trimmed = textToSend.trim()
        if (trimmed.isEmpty() && attachedImageBase64 == null) return

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = trimmed.ifEmpty { "Analiza esta imagen adjunta con Google AI Studio." },
            imageBase64 = attachedImageBase64
        )

        messages = messages + userMessage
        val imageToSend = attachedImageBase64
        inputText = ""
        attachedImageUri = null
        attachedImageBase64 = null
        isLoading = true

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size)
            val result = apiClient.generateContent(
                prompt = userMessage.text,
                model = selectedModel,
                systemInstruction = IA_STUDENT_SYSTEM_PROMPT,
                history = messages,
                useSearch = useSearchGrounding,
                useMaps = useMapsGrounding,
                thinkingLevelHigh = thinkingLevelHigh && selectedModel == GeminiModels.ARGON.id,
                imageBase64 = imageToSend
            )

            val studentMessage = ChatMessage(
                sender = MessageSender.IA_STUDENT,
                text = result.text,
                modelName = selectedModel,
                thoughtProcess = result.thoughtProcess,
                groundingSources = result.groundingSources
            )

            messages = messages + studentMessage
            isLoading = false
            listState.animateScrollToItem(messages.size)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "IA Student",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "IA Student Chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mentor Experto en Google AI Studio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            messages = listOf(messages.first())
                            audioPlayer.stop()
                        },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Limpiar chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Model Selector & Grounding Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Model Switcher Button
                        Box {
                            FilterChip(
                                selected = true,
                                onClick = { showModelMenu = true },
                                label = {
                                    val currentModelObj = GeminiModels.ALL.firstOrNull { it.id == selectedModel }
                                    Text(
                                        text = currentModelObj?.displayName ?: selectedModel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("model_selector_chip")
                            )

                            DropdownMenu(
                                expanded = showModelMenu,
                                onDismissRequest = { showModelMenu = false }
                            ) {
                                GeminiModels.ALL.forEach { modelOption ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(modelOption.displayName, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(modelOption.badge, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Text(
                                                    modelOption.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedModel = modelOption.id
                                            showModelMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Thinking mode toggle (if Argon) or quick latency badge
                        if (selectedModel == GeminiModels.ARGON.id) {
                            FilterChip(
                                selected = thinkingLevelHigh,
                                onClick = { thinkingLevelHigh = !thinkingLevelHigh },
                                label = { Text("Thinking HIGH", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (thinkingLevelHigh) GeminiAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier.testTag("thinking_mode_chip")
                            )
                        } else if (selectedModel == GeminiModels.FLASH_LITE.id) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Baja Latencia ⚡", fontSize = 11.sp) }
                            )
                        }
                    }

                    // Grounding Toggles Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = useSearchGrounding,
                            onClick = { useSearchGrounding = !useSearchGrounding },
                            label = { Text("Google Search", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            modifier = Modifier.testTag("search_grounding_chip")
                        )

                        FilterChip(
                            selected = useMapsGrounding,
                            onClick = { useMapsGrounding = !useMapsGrounding },
                            label = { Text("Google Maps", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            modifier = Modifier.testTag("maps_grounding_chip")
                        )
                    }
                }
            }

            // Quick Action Prompt Chips (5-Step Framework)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SuggestionChip(
                        onClick = { sendMessage("Quiero iniciar el PASO 1 para desarrollar una app en AI Studio") },
                        label = { Text("🧭 Iniciar Paso 1") },
                        modifier = Modifier.testTag("quick_step1_chip")
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { sendMessage("Explícame qué son las System Instructions y la Temperatura con analogías (Paso 2)") },
                        label = { Text("💡 Paso 2: Conceptos") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { sendMessage("Dame 3 opciones de arquitectura para mi proyecto (Paso 3)") },
                        label = { Text("🔀 Paso 3: Opciones") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { sendMessage("Ayúdame con el Paso 4: Construcción guiada del System Prompt y código") },
                        label = { Text("🛠️ Paso 4: Construcción") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { sendMessage("¿Cómo realizo las pruebas y ajustes de mi modelo en AI Studio? (Paso 5)") },
                        label = { Text("🧪 Paso 5: Pruebas") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { sendMessage("¿Cuál es la diferencia entre Google AI Studio y Vertex AI?") },
                        label = { Text("⚖️ AI Studio vs Vertex") }
                    )
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        onPlayTts = { text ->
                            coroutineScope.launch {
                                isTtsPlaying = true
                                val result = apiClient.generateSpeech(text, voiceName = "Kore")
                                if (result.audioData != null) {
                                    audioPlayer.playAudioBytes(result.audioData) {
                                        isTtsPlaying = false
                                    }
                                } else {
                                    isTtsPlaying = false
                                    Toast.makeText(context, result.error ?: "Error TTS", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCopyText = { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("IA Student", text))
                            Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (isLoading) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "IA Student está pensando...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Consultando modelo $selectedModel con las 5 reglas metodológicas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Image Preview if attached
            if (attachedImageUri != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = attachedImageUri,
                                contentDescription = "Imagen adjunta",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Imagen lista para analizar con Gemini",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = {
                            attachedImageUri = null
                            attachedImageBase64 = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Remover imagen")
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("attach_image_button")
                    ) {
                        Icon(
                            Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "Adjuntar foto para visión",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        placeholder = { Text("Escribe tu pregunta o idea...", fontSize = 14.sp) },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { sendMessage(inputText) },
                        enabled = (inputText.isNotBlank() || attachedImageBase64 != null) && !isLoading,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("send_message_button")
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Enviar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onPlayTts: (String) -> Unit,
    onCopyText: (String) -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    var showThoughts by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                color = GeminiVioletLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "IA Student",
                        tint = GeminiVioletDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Sender name / model badge
            if (!isUser) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "IA Student",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (message.modelName != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.modelName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Thought process collapsible (if available)
            if (!message.thoughtProcess.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clickable { showThoughts = !showThoughts },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp), tint = GeminiAmber)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showThoughts) "Ocultar razonamiento" else "Ver razonamiento profundo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        AnimatedVisibility(visible = showThoughts) {
                            Text(
                                text = message.thoughtProcess,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Message Body Card
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag(if (isUser) "user_message_bubble" else "ia_student_message_bubble")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )

                    // Grounding sources
                    if (message.groundingSources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "🌐 Fuentes consultadas:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        for (source in message.groundingSources.take(3)) {
                            Text(
                                text = "• $source",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Assistant Actions (TTS Voice & Copy)
            if (!isUser) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onPlayTts(message.text) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VolumeUp,
                            contentDescription = "Escuchar respuesta con Gemini TTS",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onCopyText(message.text) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copiar texto",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
