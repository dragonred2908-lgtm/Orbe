package com.example.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.VoiceAssistantState
import com.example.shared.stt.JvmVoiceAssistantController

@Composable
fun DesktopAssistantScreen(
    controller: JvmVoiceAssistantController = remember { JvmVoiceAssistantController() }
) {
    val session = controller.session
    val state by session.state.collectAsState()
    val config by session.config.collectAsState()
    val rmsLevel by session.rmsAudioLevel.collectAsState()
    val transcript by session.currentTranscript.collectAsState()
    val history by session.history.collectAsState()
    val isRecording by controller.isRecordingActive.collectAsState()
    val statusNotice by controller.statusNotice.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var wakePhraseInput by remember(config.wakePhrase) { mutableStateOf(config.wakePhrase) }

    val osName = remember { System.getProperty("os.name") ?: "Desktop OS" }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF030712),
            surface = Color(0xFF0B1329),
            primary = Color(0xFF00F0FF),
            secondary = Color(0xFF7000FF),
            tertiary = Color(0xFF00E676)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF030712)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🌌",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "IA Student",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00F0FF)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Voz Multiplataforma ($osName)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Text(
                                text = "Reconocimiento de voz Speech-to-Text en tiempo real con frase de activación",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Row {
                        OutlinedButton(
                            onClick = { showSettingsDialog = !showSettingsDialog },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (showSettingsDialog) "Ocultar Ajustes" else "⚙️ Configurar API / Modelo")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Panel de Ajustes expandible
                if (showSettingsDialog) {
                    DesktopSettingsPanel(
                        controller = controller,
                        onClose = { showSettingsDialog = false }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Barra de Frase de Activación Personalizada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Frase de Activación Personalizada (Wake Phrase):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = wakePhraseInput,
                                    onValueChange = { wakePhraseInput = it },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00F0FF),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.width(280.dp),
                                    placeholder = { Text("Ej: Oye Aura") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        controller.updateWakePhrase(wakePhraseInput)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Guardar Frase")
                                }
                            }
                        }

                        // Presets rápidos
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Sugerencias rápidas:",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Oye Aura", "IA Student", "Aura", "Computadora").forEach { preset ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (config.wakePhrase == preset) Color(0xFF0369A1) else Color(0xFF1E293B))
                                            .clickable {
                                                wakePhraseInput = preset
                                                controller.updateWakePhrase(preset)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = preset,
                                            fontSize = 11.sp,
                                            color = if (config.wakePhrase == preset) Color.White else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Área Central: Orbe de Voz + Estado + Control de Micrófono
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tarjeta Visualizadora de Voz
                    Card(
                        modifier = Modifier
                            .weight(1.1f)
                            .height(340.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Badge de Estado
                            val (badgeText, badgeColor) = when (state) {
                                is VoiceAssistantState.Idle -> Pair("INACTIVO", Color(0xFF64748B))
                                is VoiceAssistantState.ListeningForWakePhrase -> Pair("ESCUCHANDO: \"${config.wakePhrase}\"", Color(0xFF00F0FF))
                                is VoiceAssistantState.WakePhraseDetected -> Pair("FRASE DETECTADA: \"${config.wakePhrase}\"", Color(0xFFFFD700))
                                is VoiceAssistantState.RecordingQuery -> Pair("GRABANDO CONSULTA...", Color(0xFFFF0055))
                                is VoiceAssistantState.TranscribingSpeech -> Pair("TRANSCRIBIENDO AUDIO (STT)...", Color(0xFFB026FF))
                                is VoiceAssistantState.ProcessingWithAI -> Pair("CONSULTANDO GEMINI...", Color(0xFF818CF8))
                                is VoiceAssistantState.Responding -> Pair("RESPUESTA GENERADA", Color(0xFF00E676))
                                is VoiceAssistantState.Error -> Pair("ERROR EN ASISTENTE", Color(0xFFFF3366))
                            }

                            Box(
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                    .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }

                            // Orbe Cuántico
                            DesktopVoiceVisualizer(
                                state = state,
                                rmsLevel = rmsLevel
                            )

                            // Nivel de Audio RMS
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Nivel de Micrófono", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text("${(rmsLevel * 100).toInt()}%", fontSize = 10.sp, color = Color(0xFF38BDF8))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { rmsLevel },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (isRecording) Color(0xFFFF0055) else Color(0xFF00F0FF),
                                    trackColor = Color(0xFF1E293B)
                                )
                            }

                            // Botón de Micrófono
                            Button(
                                onClick = { controller.toggleListening() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecording) Color(0xFFE11D48) else Color(0xFF0284C7)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(
                                    text = if (isRecording) "⏹️ Detener y Procesar Audio" else "🎙️ Hablar por Micrófono (Presiona para iniciar)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Tarjeta de Transcripción y Mensajes Rápidos
                    Card(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(340.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Monitor de Reconocimiento de Voz en Tiempo Real",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = statusNotice ?: "Inactivo",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Caja de Transcripción Actual
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFF030712), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                if (transcript.isBlank()) {
                                    Text(
                                        text = "El texto reconocido por la API de Speech-to-Text aparecerá aquí cuando hables con la frase de activación (ej: \"${config.wakePhrase}, ¿cómo estás?\") o uses las pruebas rápidas.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569)
                                    )
                                } else {
                                    Column {
                                        Text(
                                            text = "Texto transcrito:",
                                            fontSize = 11.sp,
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = transcript,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Simulador de Voz y Pruebas Rápidas sin Micrófono
                            Text(
                                text = "Pruebas directas de activación por voz (Simulador sin micrófono):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        controller.simulateVoiceInput("${config.wakePhrase}, ¿qué es la computación cuántica?")
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("🧪 ¿Qué es cuántica?", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        controller.simulateVoiceInput("${config.wakePhrase}, explícame las leyes de Newton")
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("🧪 Leyes de Newton", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        controller.simulateVoiceInput("${config.wakePhrase}, genera un plan de estudio")
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("🧪 Plan de estudio", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Historial de Consultas y Respuestas
                Text(
                    text = "Historial de Respuestas del Asistente en Tiempo Real:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF0B1329), RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no hay consultas procesadas. Di \"${config.wakePhrase}\" o haz clic en un botón de prueba rápida arriba.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(history) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00E676))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Activado con: \"${item.wakePhraseUsed}\"",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }

                                        Text(
                                            text = "⏱️ ${item.executionTimeMs} ms",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "🗣️ Consulta: \"${item.transcribedQuery}\"",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF030712), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = item.assistantResponse,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            color = Color(0xFFE2E8F0)
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

@Composable
fun DesktopSettingsPanel(
    controller: JvmVoiceAssistantController,
    onClose: () -> Unit
) {
    val config = controller.session.config.collectAsState().value
    var apiKeyText by remember { mutableStateOf(config.geminiApiKey) }
    var selectedModel by remember { mutableStateOf(config.model) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️ Configuración del Motor de Voz e Inteligencia Artificial",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Cerrar", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "GEMINI_API_KEY (Google AI Studio):",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = apiKeyText,
                            onValueChange = { apiKeyText = it },
                            placeholder = { Text("Pega tu GEMINI_API_KEY") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                controller.updateApiKey(apiKeyText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Aplicar")
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Modelo de IA Generativa:",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("gemini-3.5-flash", "gemini-3.1-pro-preview").forEach { m ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selectedModel == m) Color(0xFF0284C7) else Color(0xFF334155))
                                    .clickable {
                                        selectedModel = m
                                        controller.session.updateConfig(config.copy(model = m))
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = m,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
