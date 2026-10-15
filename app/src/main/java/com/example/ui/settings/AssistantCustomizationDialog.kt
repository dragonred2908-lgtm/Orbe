package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.api.GeminiApiClient
import com.example.data.audio.AudioPlayerHelper
import com.example.data.repository.AssistantSettings
import com.example.data.repository.AssistantSettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantCustomizationDialog(
    settingsRepository: AssistantSettingsRepository,
    apiClient: GeminiApiClient,
    audioPlayer: AudioPlayerHelper,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val currentSettings by settingsRepository.settings.collectAsState()

    var nameInput by remember(currentSettings) { mutableStateOf(currentSettings.assistantName) }
    var wakePhraseInput by remember(currentSettings) { mutableStateOf(currentSettings.wakePhrase) }
    var selectedVoice by remember(currentSettings) { mutableStateOf(currentSettings.voiceName) }
    var selectedPersonality by remember(currentSettings) { mutableStateOf(currentSettings.personality) }
    var greetingInput by remember(currentSettings) { mutableStateOf(currentSettings.customGreeting) }
    var matchContains by remember(currentSettings) { mutableStateOf(currentSettings.matchContains) }
    var autoListen by remember(currentSettings) { mutableStateOf(currentSettings.autoListenEnabled) }

    // Test phrase simulation
    var testInput by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestingVoice by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .testTag("assistant_customization_dialog"),
            color = Color(0xFF081116)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x2200F0FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Personalizar Asistente",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Define su nombre, voz y frase de llamada",
                                color = Color(0xFF80D8FF),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x1AFFFFFF))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0x3300F0FF),
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 1. Nombre del Asistente
                    Column {
                        Text(
                            "1. Nombre del Asistente",
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                // Suggest default wake phrase matching name if it starts with Oye/Hola
                                if (wakePhraseInput.startsWith("Oye ") || wakePhraseInput.startsWith("Hola ") || wakePhraseInput.isEmpty()) {
                                    wakePhraseInput = "Oye ${it.trim()}"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("assistant_name_input"),
                            singleLine = true,
                            placeholder = { Text("Ejemplo: Aura, Nova, Lumina...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0x6600F0FF),
                                focusedContainerColor = Color(0x1000F0FF),
                                unfocusedContainerColor = Color(0x0800F0FF)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sugerencias de nombres:", color = Color.LightGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistantSettingsRepository.PRESET_NAMES.take(5).forEach { preset ->
                                FilterChip(
                                    selected = nameInput.trim().equals(preset, ignoreCase = true),
                                    onClick = {
                                        nameInput = preset
                                        wakePhraseInput = "Oye $preset"
                                    },
                                    label = { Text(preset, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00F0FF),
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0x15FFFFFF),
                                        labelColor = Color(0xFFB0BEC5)
                                    )
                                )
                            }
                        }
                    }

                    // 2. Frase de Llamada (Wake Word)
                    Column {
                        Text(
                            "2. Frase de Llamada (Activación)",
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            "La frase que dirás para que el asistente despierte y se ponga a tu orden.",
                            color = Color(0xFF90A4AE),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = wakePhraseInput,
                            onValueChange = { wakePhraseInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wake_phrase_input"),
                            singleLine = true,
                            placeholder = { Text("Ej: Oye Aura, Activar Sistema, Hola...") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color(0xFF00F0FF)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0x6600F0FF),
                                focusedContainerColor = Color(0x1000F0FF),
                                unfocusedContainerColor = Color(0x0800F0FF)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val cleanName = if (nameInput.isNotBlank()) nameInput.trim() else "Asistente"
                            listOf("Oye $cleanName", "Hola $cleanName", "Activar $cleanName").forEach { phrase ->
                                FilterChip(
                                    selected = wakePhraseInput.trim().equals(phrase, ignoreCase = true),
                                    onClick = { wakePhraseInput = phrase },
                                    label = { Text(phrase, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00F0FF),
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0x15FFFFFF),
                                        labelColor = Color(0xFFB0BEC5)
                                    )
                                )
                            }
                        }
                    }

                    // 3. Probar la Frase de Llamada
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0x1500F0FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00F0FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Comprobar detección de frase",
                                    color = Color(0xFF00F0FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Escribe o prueba lo que dirías para comprobar si activa a tu asistente:",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = testInput,
                                    onValueChange = {
                                        testInput = it
                                        val normInput = it.lowercase().trim()
                                        val normWake = wakePhraseInput.lowercase().trim()
                                        testResult = if (normWake.isNotEmpty() && normInput.contains(normWake)) {
                                            "✅ ¡Frase detectada correctamente! Despertará al asistente."
                                        } else if (normInput.isEmpty()) {
                                            null
                                        } else {
                                            "❌ Aún no coincide con '$wakePhraseInput'"
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Escribe ej: '${wakePhraseInput}'", fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF00F0FF),
                                        unfocusedBorderColor = Color(0x4400F0FF)
                                    )
                                )
                            }

                            testResult?.let { msg ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = msg,
                                    color = if (msg.startsWith("✅")) Color(0xFF00E676) else Color(0xFFFF8A80),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 4. Voz de Respuesta (TTS de Gemini)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "3. Voz de Respuesta",
                                color = Color(0xFF00F0FF),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )

                            // Preview voice button
                            TextButton(
                                onClick = {
                                    if (!isTestingVoice) {
                                        isTestingVoice = true
                                        coroutineScope.launch {
                                            val previewText = "Hola, soy ${nameInput.ifBlank { "tu asistente" }}. Sistemas activos y a tu orden."
                                            val tts = apiClient.generateSpeech(previewText, voiceName = selectedVoice)
                                            tts.audioData?.let { bytes ->
                                                audioPlayer.playAudioBytes(bytes) {
                                                    isTestingVoice = false
                                                }
                                            } ?: run {
                                                isTestingVoice = false
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    if (isTestingVoice) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF00F0FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isTestingVoice) "Reproduciendo..." else "Escuchar Voz",
                                    color = Color(0xFF00F0FF),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistantSettingsRepository.PRESET_VOICES.forEach { voice ->
                                FilterChip(
                                    selected = selectedVoice == voice,
                                    onClick = { selectedVoice = voice },
                                    label = { Text(voice, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00F0FF),
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0x15FFFFFF),
                                        labelColor = Color(0xFFB0BEC5)
                                    )
                                )
                            }
                        }
                    }

                    // 5. Personalidad y Saludo
                    Column {
                        Text(
                            "4. Tono y Personalidad",
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        AssistantSettingsRepository.PRESET_PERSONALITIES.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedPersonality = p }
                                    .background(
                                        if (selectedPersonality == p) Color(0x2200F0FF) else Color(0x08FFFFFF)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPersonality == p,
                                    onClick = { selectedPersonality = p },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF00F0FF),
                                        unselectedColor = Color.Gray
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(p, color = Color.White, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // 6. Mensaje o Confirmación de Activación
                    Column {
                        Text(
                            "5. Saludo de Activación",
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = greetingInput,
                            onValueChange = { greetingInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0x6600F0FF),
                                focusedContainerColor = Color(0x1000F0FF),
                                unfocusedContainerColor = Color(0x0800F0FF)
                            )
                        )
                    }

                    // 7. Opciones adicionales
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Coincidencia flexible",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Activar aunque la frase esté dentro de una oración más larga",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = matchContains,
                            onCheckedChange = { matchContains = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00F0FF)
                            )
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0x3300F0FF),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600F0FF)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val finalName = if (nameInput.isNotBlank()) nameInput.trim() else "Aura"
                            val finalWake = if (wakePhraseInput.isNotBlank()) wakePhraseInput.trim() else "Oye $finalName"
                            settingsRepository.updateSettings(
                                AssistantSettings(
                                    assistantName = finalName,
                                    wakePhrase = finalWake,
                                    personality = selectedPersonality,
                                    voiceName = selectedVoice,
                                    autoListenEnabled = autoListen,
                                    customGreeting = greetingInput.ifBlank { "Sistemas listos y a su orden." },
                                    matchContains = matchContains
                                )
                            )
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_assistant_settings_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00F0FF),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
