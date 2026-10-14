package com.example.ui.orb

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.api.GeminiApiClient
import com.example.data.audio.AudioPlayerHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun JarvisOrbScreen(
    apiClient: GeminiApiClient,
    audioPlayer: AudioPlayerHelper,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var orbState by remember { mutableStateOf(OrbState.REPOSO) }
    var showMenu by remember { mutableStateOf(false) }

    fun activateOrbVoiceCycle() {
        if (orbState != OrbState.REPOSO) return

        coroutineScope.launch {
            // 1. Fase de Escucha
            orbState = OrbState.ESCUCHA
            delay(2800)

            // 2. Fase de Procesamiento con Gemini & Obsidian
            orbState = OrbState.PROCESAMIENTO
            val queryPrompt = "Genera una confirmación breve y cortés en una sola oración como J.A.R.V.I.S. indicando que los sistemas están operativos y las notas sincronizadas."
            val res = apiClient.generateContent(
                prompt = queryPrompt,
                model = "gemini-3.8-flash"
            )

            // 3. Fase de Respuesta por voz
            val ttsRes = apiClient.generateSpeech(res.text, voiceName = "Kore")
            if (ttsRes.audioData != null) {
                audioPlayer.playAudioBytes(ttsRes.audioData) {
                    orbState = OrbState.REPOSO
                }
            } else {
                delay(2500)
                orbState = OrbState.REPOSO
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B0E))
            .testTag("jarvis_pure_orb_screen")
    ) {
        val isWide = maxWidth > 650.dp
        val orbSize = if (isWide) 360.dp else 290.dp

        // --- ÚNICO ELEMENTO CENTRAL: EL ORBE CUÁNTICO 3D ---
        Box(
            modifier = Modifier
                .fillMaxSize(),
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
                        .alpha(if (orbState == OrbState.PROCESAMIENTO) 0.35f else 0.20f)
                )

                // Simulación viva y matemática de ondas y partículas
                CyanOrbCanvas(
                    state = orbState,
                    size = orbSize,
                    onClick = { activateOrbVoiceCycle() }
                )
            }
        }

        // --- SUTIL HAMBURGUESA EN LA ESQUINA SUPERIOR DERECHA ---
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

            // Menú desplegable elegante estilo holográfico
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(Color(0xFF091217))
                    .border(1.dp, Color(0x4D00F0FF), RoundedCornerShape(12.dp))
                    .width(240.dp)
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

                HorizontalDivider(color = Color(0x2200F0FF))

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Chat IA Student", color = Color.White)
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
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Studio Lab (Multimodal)", color = Color.White)
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
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Centro de Conocimiento", color = Color.White)
                        }
                    },
                    onClick = {
                        showMenu = false
                        onNavigateTo("knowledge")
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
            }
        }
    }
}
