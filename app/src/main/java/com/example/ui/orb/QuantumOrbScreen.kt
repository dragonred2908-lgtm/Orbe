package com.example.ui.orb

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.api.GeminiApiClient
import com.example.data.audio.AudioPlayerHelper
import com.example.data.audio.VoiceRecognitionHelper
import com.example.data.repository.AssistantSettingsRepository
import com.example.ui.settings.AssistantCustomizationDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuantumOrbScreen(
    apiClient: GeminiApiClient,
    audioPlayer: AudioPlayerHelper,
    settingsRepository: AssistantSettingsRepository,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by settingsRepository.settings.collectAsState()

    var orbState by remember { mutableStateOf(OrbState.REPOSO) }
    var showMenu by remember { mutableStateOf(false) }
    var showCustomizationDialog by remember { mutableStateOf(false) }

    // Live interaction telemetry
    var recognizedText by remember { mutableStateOf("") }
    var assistantReply by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("En espera") }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Micrófono habilitado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Se requiere permiso para escuchar tu frase de llamada", Toast.LENGTH_LONG).show()
        }
    }

    // Assistant response cycle
    fun processAssistantResponse(spokenUtterance: String = "") {
        coroutineScope.launch {
            orbState = OrbState.PROCESAMIENTO
            statusMessage = "Procesando..."

            val prompt = if (spokenUtterance.isNotBlank()) {
                "Eres '${settings.assistantName}' con estilo '${settings.personality}'. El usuario te acaba de decir: \"$spokenUtterance\". Responde de manera breve, concisa y elegante en una sola frase confirmando que estás activo y listo para asistirlo."
            } else {
                "Eres '${settings.assistantName}' con estilo '${settings.personality}'. Genera una confirmación de activación breve y elegante en una sola oración indicando que estás listo y a la orden de tu usuario."
            }

            val res = apiClient.generateContent(
                prompt = prompt,
                model = "gemini-3.8-flash"
            )

            val replyText = res.text.ifBlank { settings.customGreeting }
            assistantReply = replyText
            statusMessage = "Hablando..."

            // Reproducir con la voz personalizada seleccionada (Kore, Aoede, Fenrir, Puck, Charon)
            val ttsRes = apiClient.generateSpeech(replyText, voiceName = settings.voiceName)
            if (ttsRes.audioData != null) {
                audioPlayer.playAudioBytes(ttsRes.audioData) {
                    orbState = OrbState.REPOSO
                    statusMessage = "En espera"
                }
            } else {
                delay(2500)
                orbState = OrbState.REPOSO
                statusMessage = "En espera"
            }
        }
    }

    // Setup Voice Recognition Helper
    val voiceHelper = remember {
        VoiceRecognitionHelper(
            context = context,
            onWakeWordDetected = { text ->
                recognizedText = text
                if (settingsRepository.isWakePhraseMatched(text)) {
                    statusMessage = "¡Frase detectada!"
                    processAssistantResponse(text)
                } else {
                    // Si se tocó para hablar, procesar directamente la consulta
                    processAssistantResponse(text)
                }
            },
            onSpeechPartialResult = { partial ->
                recognizedText = partial
                statusMessage = "Escuchando: \"$partial\""
            },
            onError = { err ->
                statusMessage = "Audio: $err"
                if (orbState == OrbState.ESCUCHA) {
                    // Fallback to direct activation if voice input timed out or errored
                    processAssistantResponse()
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceHelper.destroy()
        }
    }

    fun startListeningCycle() {
        if (orbState != OrbState.REPOSO) return

        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            // Even without mic yet, trigger demo interaction
            orbState = OrbState.ESCUCHA
            statusMessage = "Escuchando..."
            coroutineScope.launch {
                delay(2000)
                processAssistantResponse()
            }
            return
        }

        orbState = OrbState.ESCUCHA
        statusMessage = "Escuchando... Di: \"${settings.wakePhrase}\""
        recognizedText = ""
        assistantReply = ""
        voiceHelper.startListening()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040A0D))
            .testTag("quantum_pure_orb_screen")
    ) {
        val isWide = maxWidth > 650.dp
        val orbSize = if (isWide) 370.dp else 300.dp

        // --- 1. ÚNICO ELEMENTO CENTRAL: EL ORBE CUÁNTICO 3D ---
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(orbSize),
                contentAlignment = Alignment.Center
            ) {
                // Textura 3D de fondo
                Image(
                    painter = painterResource(id = R.drawable.quantum_cyan_orb_1791993174579),
                    contentDescription = "Túnel cuántico",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .alpha(if (orbState == OrbState.PROCESAMIENTO) 0.38f else 0.22f)
                )

                // Simulación viva de ondas y partículas cian
                CyanOrbCanvas(
                    state = orbState,
                    size = orbSize,
                    onClick = { startListeningCycle() }
                )
            }
        }

        // --- 2. SUTIL DISTINTIVO SUPERIOR CON EL NOMBRE Y FRASE DE LLAMADA ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(20.dp))
                    .clickable { showCustomizationDialog = true }
                    .testTag("assistant_info_pill"),
                color = Color(0x1A09141B)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (orbState) {
                                    OrbState.REPOSO -> Color(0xFF00F0FF)
                                    OrbState.ESCUCHA -> Color(0xFFFFEB3B)
                                    OrbState.PROCESAMIENTO -> Color(0xFFE040FB)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = settings.assistantName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = " • ",
                        color = Color(0x66FFFFFF),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "\"${settings.wakePhrase}\"",
                        color = Color(0xFF80D8FF),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Personalizar",
                        tint = Color(0x9900F0FF),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // --- 3. SUBTÍTULO / HUD FLOTANTE TRANSPARENTE EN LA PARTE INFERIOR ---
        AnimatedVisibility(
            visible = assistantReply.isNotEmpty() || recognizedText.isNotEmpty() || orbState != OrbState.REPOSO,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xD9050C10))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = statusMessage,
                        color = Color(0xFF00F0FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (recognizedText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"$recognizedText\"",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }

                    if (assistantReply.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = assistantReply,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- 4. SUTIL HAMBURGUESA EN LA ESQUINA SUPERIOR DERECHA ---
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp)
        ) {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x1A00F0FF))
                    .border(1.dp, Color(0x3300F0FF), CircleShape)
                    .testTag("subtle_hamburger_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú de la App",
                    tint = Color(0xCC00F0FF),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Menú desplegable holográfico
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(Color(0xFF091217))
                    .border(1.dp, Color(0x4D00F0FF), RoundedCornerShape(12.dp))
                    .width(260.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BlurOn, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Orbe Principal", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                        }
                    },
                    onClick = { showMenu = false }
                )

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Personalizar Asistente", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("Nombre, voz y frase de llamada", color = Color(0xFF80D8FF), fontSize = 10.sp)
                            }
                        }
                    },
                    onClick = {
                        showMenu = false
                        showCustomizationDialog = true
                    }
                )

                HorizontalDivider(color = Color(0x2200F0FF))

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Laptop, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Instalación en PC & Multiplataforma", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Windows, macOS, Linux y Android", color = Color(0xFF80D8FF), fontSize = 10.sp)
                            }
                        }
                    },
                    onClick = {
                        showMenu = false
                        onNavigateTo("desktop")
                    }
                )

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FindInPage, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Buscador de Archivos", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Búsqueda cruzada en este móvil y en PC", color = Color(0xFF80D8FF), fontSize = 10.sp)
                            }
                        }
                    },
                    onClick = {
                        showMenu = false
                        onNavigateTo("files")
                    }
                )

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Asistente Comercial", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Contestador automático y catálogo", color = Color(0xFF80D8FF), fontSize = 10.sp)
                            }
                        }
                    },
                    onClick = {
                        showMenu = false
                        onNavigateTo("business")
                    }
                )

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Crear y Editar Fotos / Videos", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Generación y edición con IA y Veo", color = Color(0xFF80D8FF), fontSize = 10.sp)
                            }
                        }
                    },
                    onClick = {
                        showMenu = false
                        onNavigateTo("multimodal")
                    }
                )

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Permisos y Control de Usos", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Privacidad, micrófono, archivos y red", color = Color(0xFF80D8FF), fontSize = 10.sp)
                            }
                        }
                    },
                    onClick = {
                        showMenu = false
                        onNavigateTo("permissions")
                    }
                )

                HorizontalDivider(color = Color(0x2200F0FF))

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Chat y Búsqueda Web", color = Color.White)
                        }
                    },
                    onClick = {
                        showMenu = false
                        onNavigateTo("chat")
                    }
                )

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Paso a Paso", color = Color.White)
                        }
                    },
                    onClick = {
                        showMenu = false
                        onNavigateTo("wizard")
                    }
                )

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Mis Proyectos", color = Color.White)
                        }
                    },
                    onClick = {
                        showMenu = false
                        onNavigateTo("projects")
                    }
                )

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Centro de Conocimiento", color = Color.White)
                        }
                    },
                    onClick = {
                        showMenu = false
                        onNavigateTo("knowledge")
                    }
                )
            }
        }

        // --- 5. DIÁLOGO DE PERSONALIZACIÓN COMPLETA ---
        if (showCustomizationDialog) {
            AssistantCustomizationDialog(
                settingsRepository = settingsRepository,
                apiClient = apiClient,
                audioPlayer = audioPlayer,
                onDismissRequest = { showCustomizationDialog = false }
            )
        }
    }
}
