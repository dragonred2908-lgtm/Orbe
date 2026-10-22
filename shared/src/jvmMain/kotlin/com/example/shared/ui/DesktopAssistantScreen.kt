package com.example.shared.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.flow.StateFlow

private enum class SidePanelKind(val title: String) {
    SETTINGS("⚙️ Ajustes del asistente"),
    HISTORY("🕘 Historial de conversaciones"),
    QUICK_TESTS("🧪 Pruebas rápidas")
}

/**
 * Permite abrir un panel lateral nada más arrancar, para diagnóstico o capturas:
 * `IA_STUDENT_UI_PANEL=settings|history|tests`.
 */
private fun initialPanelFromEnvironment(): SidePanelKind? {
    return when (System.getenv("IA_STUDENT_UI_PANEL")?.trim()?.lowercase()) {
        "settings", "ajustes" -> SidePanelKind.SETTINGS
        "history", "historial" -> SidePanelKind.HISTORY
        "tests", "pruebas" -> SidePanelKind.QUICK_TESTS
        else -> null
    }
}

/**
 * Pantalla principal del escritorio:
 *  - Orbe a PANTALLA COMPLETA (imagen/GIF del usuario o respaldo procedural).
 *  - Menú hamburguesa (☰) en la esquina superior derecha con el resto de funciones.
 *  - Respuesta del asistente siempre visible hasta la siguiente consulta.
 */
@Composable
fun DesktopAssistantScreen(
    controller: JvmVoiceAssistantController = remember { JvmVoiceAssistantController() }
) {
    var menuExpanded by remember { mutableStateOf(false) }
    // IA_STUDENT_UI_PANEL=settings|history|tests abre un panel al arrancar (diagnóstico)
    var panel by remember { mutableStateOf(initialPanelFromEnvironment()) }
    var orbReloadKey by remember { mutableIntStateOf(0) }
    var ttsEnabled by remember { mutableStateOf(controller.isTtsEnabled()) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF0B1220),
            primary = Color(0xFF00F0FF),
            secondary = Color(0xFF7000FF),
            tertiary = Color(0xFF00E676)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1) Orbe a pantalla completa
            OrbHero(
                stateFlow = controller.session.state,
                rmsFlow = controller.session.rmsAudioLevel,
                modifier = Modifier.fillMaxSize(),
                reloadKey = orbReloadKey
            )

            // 2) Barra superior: marca + menú hamburguesa
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "IA STUDENT",
                        color = Color(0xFF00F0FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    ModeLabel(controller)
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Text("☰", fontSize = 26.sp, color = Color.White)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(Color(0xFF0B1220))
                    ) {
                        MenuItem("⚙️  Ajustes del asistente") {
                            menuExpanded = false
                            panel = SidePanelKind.SETTINGS
                        }
                        MenuItem("🕘  Historial") {
                            menuExpanded = false
                            panel = SidePanelKind.HISTORY
                        }
                        MenuItem("🧪  Pruebas rápidas") {
                            menuExpanded = false
                            panel = SidePanelKind.QUICK_TESTS
                        }
                        HorizontalDivider(color = Color(0xFF1E293B))
                        MenuItem("🖼️  Elegir imagen del orbe…") {
                            menuExpanded = false
                            OrbImageLoader.pickAndInstallOrb { orbReloadKey++ }
                        }
                        MenuItem(if (ttsEnabled) "🔇  Silenciar voz" else "🔊  Activar voz") {
                            menuExpanded = false
                            ttsEnabled = !ttsEnabled
                            controller.setTtsEnabled(ttsEnabled)
                        }
                    }
                }
            }

            // 3) Contenido inferior: estado, respuesta persistente, micrófono, texto
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusBadge(controller.session.state)
                Spacer(modifier = Modifier.height(10.dp))
                TranscriptLine(controller.session.currentTranscript)
                AnswerCard(controller.session.history)
                Spacer(modifier = Modifier.height(14.dp))
                MicControls(controller)
                Spacer(modifier = Modifier.height(10.dp))
                TextQueryRow(onSend = { controller.submitTextQuery(it) })
            }

            // 4) Panel lateral derecho (ajustes / historial / pruebas)
            panel?.let { kind ->
                SidePanel(
                    kind = kind,
                    controller = controller,
                    onClose = { panel = null },
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {    DropdownMenuItem(
        text = { Text(label, color = Color(0xFFE2E8F0), fontSize = 13.sp) },
        onClick = onClick
    )
}

/** Etiqueta del modo (offline u online) bajo la marca. */
@Composable
private fun ModeLabel(controller: JvmVoiceAssistantController) {
    val config by controller.session.config.collectAsState()
    val onlineConfigured = config.geminiApiKey.isNotBlank()
    Text(
        text = if (onlineConfigured) "asistente · Gemini en línea" else "asistente · modo sin conexión",
        color = Color(0xFF64748B),
        fontSize = 10.sp
    )
}

@Composable
private fun StatusBadge(stateFlow: StateFlow<VoiceAssistantState>) {
    val state by stateFlow.collectAsState()
    val (badgeText, badgeColor) = when (state) {
        is VoiceAssistantState.Idle -> Pair("INACTIVO", Color(0xFF64748B))
        is VoiceAssistantState.ListeningForWakePhrase -> Pair("ESCUCHANDO", Color(0xFF00F0FF))
        is VoiceAssistantState.WakePhraseDetected -> Pair("FRASE DETECTADA", Color(0xFFFFD700))
        is VoiceAssistantState.RecordingQuery -> Pair("GRABANDO CONSULTA…", Color(0xFFFF0055))
        is VoiceAssistantState.TranscribingSpeech -> Pair("TRANSCRIBIENDO…", Color(0xFFB026FF))
        is VoiceAssistantState.ProcessingWithAI -> Pair("PENSANDO…", Color(0xFF818CF8))
        is VoiceAssistantState.Responding -> Pair("RESPUESTA LISTA", Color(0xFF00E676))
        is VoiceAssistantState.Error -> Pair("ERROR", Color(0xFFFF3366))
    }

    Surface(
        color = badgeColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
    ) {
        Text(
            text = badgeText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun TranscriptLine(transcriptFlow: StateFlow<String>) {
    val transcript by transcriptFlow.collectAsState()
    if (transcript.isNotBlank()) {
        Text(
            text = "🎧  \"$transcript\"",
            color = Color(0xFF7DD3FC),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

/** Última respuesta del asistente: permanece visible hasta la siguiente consulta. */
@Composable
private fun AnswerCard(historyFlow: StateFlow<List<com.example.shared.model.VoiceConversationItem>>) {
    val history by historyFlow.collectAsState()
    val last = history.firstOrNull() ?: return

    Surface(
        color = Color(0xE60B1220),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0x26FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 230.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Tú: ${last.transcribedQuery}",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = last.assistantResponse,
                color = Color(0xFFE2E8F0),
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun MicControls(controller: JvmVoiceAssistantController) {
    val isRecording by controller.isRecordingActive.collectAsState()
    val rms by controller.session.rmsAudioLevel.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            if (isRecording) {
                val ring = (92 + (rms * 34)).dp
                Box(
                    modifier = Modifier
                        .size(ring)
                        .clip(CircleShape)
                        .background(Color(0x33FF0055))
                )
            }
            Button(
                onClick = { controller.toggleListening() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFFE11D48) else Color(0xFF0E9FB8)
                ),
                modifier = Modifier.size(78.dp)
            ) {
                Text(if (isRecording) "⏹" else "🎙", fontSize = 30.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isRecording) "Escuchando… pulsa para detener"
            else "Pulsa el micrófono y di \"Oye Aura…\"",
            color = Color(0xFF64748B),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun TextQueryRow(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(max = 560.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Escribe una consulta (sin micrófono)…", color = Color(0xFF475569), fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0E9FB8),
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFE2E8F0)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E9FB8)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Text("Enviar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SidePanel(
    kind: SidePanelKind,
    controller: JvmVoiceAssistantController,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xF20A1220),
        modifier = modifier
            .fillMaxHeight()
            .width(440.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(kind.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Text("✕", fontSize = 20.sp, color = Color(0xFF94A3B8))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            when (kind) {
                SidePanelKind.SETTINGS -> SettingsContent(controller)
                SidePanelKind.HISTORY -> HistoryContent(controller)
                SidePanelKind.QUICK_TESTS -> QuickTestsContent(controller)
            }
        }
    }
}

@Composable
private fun SettingsContent(controller: JvmVoiceAssistantController) {
    val config by controller.session.config.collectAsState()
    var apiKey by remember { mutableStateOf(config.geminiApiKey) }
    var wakePhrase by remember(config.wakePhrase) { mutableStateOf(config.wakePhrase) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "GEMINI_API_KEY (opcional: sin ella funciona el modo offline)",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            placeholder = { Text("Pega tu GEMINI_API_KEY", color = Color(0xFF475569), fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00F0FF),
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFE2E8F0)
            )
        )
        Button(
            onClick = { controller.updateApiKey(apiKey) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E9FB8)),
            shape = RoundedCornerShape(8.dp)
        ) { Text("Aplicar clave") }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Modelo de IA generativa", fontSize = 11.sp, color = Color(0xFF94A3B8))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("gemini-3.8-flash", "gemini-3.1-pro-preview").forEach { model ->
                val selected = config.model == model
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Color(0xFF0E9FB8) else Color(0xFF1E293B))
                        .clickable {
                            controller.session.updateConfig(config.copy(model = model))
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(model, fontSize = 11.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Frase de activación", fontSize = 11.sp, color = Color(0xFF94A3B8))
        OutlinedTextField(
            value = wakePhrase,
            onValueChange = { wakePhrase = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00F0FF),
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFE2E8F0)
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Oye Aura", "IA Student", "Aura").forEach { preset ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .clickable {
                            wakePhrase = preset
                            controller.updateWakePhrase(preset)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(preset, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
        }
        Button(
            onClick = { controller.updateWakePhrase(wakePhrase) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E9FB8)),
            shape = RoundedCornerShape(8.dp)
        ) { Text("Guardar frase") }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "💡 El modo sin conexión usa Vosk (modelo local de español) y las voces del sistema. " +
                "No requiere internet ni API key. Sin clave, la IA responde comandos locales.",
            fontSize = 10.sp,
            color = Color(0xFF64748B),
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun HistoryContent(controller: JvmVoiceAssistantController) {
    val history by controller.session.history.collectAsState()

    if (history.isEmpty()) {
        Text(
            "Aún no hay consultas. Di \"Oye Aura…\" o usa las pruebas rápidas.",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(history) { item ->
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "🗣️ ${item.transcribedQuery}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        item.assistantResponse,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickTestsContent(controller: JvmVoiceAssistantController) {
    val config by controller.session.config.collectAsState()
    val wake = config.wakePhrase

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Simula la voz sin micrófono para probar el modo offline:",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
        listOf(
            "¿Qué hora es?" to "$wake, ¿qué hora es?",
            "¿Cuánto es 25 por 4?" to "$wake, ¿cuánto es 25 por 4?",
            "¿Quién eres?" to "$wake, ¿quién eres?",
            "Ayuda (comandos)" to "$wake, ayuda"
        ).forEach { (label, spoken) ->
            Button(
                onClick = { controller.simulateVoiceInput(spoken) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(label, fontSize = 12.sp, color = Color(0xFFE2E8F0))
            }
        }
    }
}
