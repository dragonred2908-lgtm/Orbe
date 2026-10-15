package com.example.ui.files

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.CrossPlatformRepository
import com.example.data.repository.FileSearchRepository
import com.example.data.repository.ManagedFileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSearchScreen(
    fileSearchRepo: FileSearchRepository,
    crossPlatformRepo: CrossPlatformRepository,
    onConsultAssistantWithFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val linkedDevices by crossPlatformRepo.linkedDevices.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var includeRemote by remember { mutableStateOf(true) }
    var selectedPathPreset by remember { mutableStateOf("Todo el equipo") }
    var customPathInput by remember { mutableStateOf("") }
    var showAdvancedPathEditor by remember { mutableStateOf(false) }

    var selectedFileForDetail by remember { mutableStateOf<ManagedFileItem?>(null) }

    val categories = listOf("Todos", "Documentos", "Imágenes", "Videos", "Audio", "Código")

    val searchResults = remember(searchQuery, selectedCategory, includeRemote, customPathInput, linkedDevices) {
        fileSearchRepo.searchLocalAndRemote(
            query = searchQuery,
            selectedCategory = selectedCategory,
            customPath = if (customPathInput.isNotBlank()) customPathInput else null,
            includeRemoteDevices = includeRemote,
            remoteDevices = linkedDevices
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. CABECERA Y RESUMEN ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00F0FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔍", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Buscador de Archivos Multi-Equipo",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Encuentra archivos en este móvil y en tus PC vinculadas (Windows, Mac, Linux)",
                                color = Color(0xFF8B949E),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buscador principal
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por nombre, extensión o palabra clave...", color = Color(0xFF6E7681), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00F0FF)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color(0xFF8B949E))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color(0xFF30363D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Switch de sincronización con PC
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌐", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Búsqueda Cruzada en PC Remota", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${linkedDevices.count { it.isConnected }} equipos activos en red", color = Color(0xFF8B949E), fontSize = 10.sp)
                            }
                        }
                        Switch(
                            checked = includeRemote,
                            onCheckedChange = { includeRemote = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00F0FF), checkedTrackColor = Color(0xFF00F0FF).copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }

        // --- 2. GUÍA DE RUTAS PARA PRINCIPIANTES & AVANZADOS ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📁 Ubicaciones y Rutas Guiadas",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        TextButton(onClick = { showAdvancedPathEditor = !showAdvancedPathEditor }) {
                            Text(
                                if (showAdvancedPathEditor) "Ocultar Ruta Manual" else "Ruta Personalizada",
                                color = Color(0xFF00F0FF),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Botones de 1 toque para principiantes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickPresets = listOf("Todo el equipo", "Descargas", "Documentos", "Fotos")
                        quickPresets.forEach { preset ->
                            val isSel = selectedPathPreset == preset
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedPathPreset = preset
                                    customPathInput = when (preset) {
                                        "Descargas" -> "/storage/emulated/0/Download"
                                        "Documentos" -> "/storage/emulated/0/Documents"
                                        "Fotos" -> "/storage/emulated/0/DCIM"
                                        else -> ""
                                    }
                                },
                                label = { Text(preset, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00F0FF).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFF00F0FF)
                                )
                            )
                        }
                    }

                    AnimatedVisibility(visible = showAdvancedPathEditor) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                "Ingresa o edita la ruta específica de tu PC o móvil (Windows: C:\\..., Mac/Linux: /home/...):",
                                color = Color(0xFF8B949E),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customPathInput,
                                onValueChange = { customPathInput = it },
                                placeholder = { Text("Ej: C:\\Users\\MiUsuario\\MiCarpeta", color = Color.Gray, fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00F0FF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // --- 3. CATEGORÍAS DE FILTRADO ---
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF00F0FF) else Color(0xFF21262D),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // --- 4. LISTA DE RESULTADOS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Resultados Encontrados (${searchResults.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (searchResults.isNotEmpty()) {
                    Text("Toca un archivo para ver detalles o consultar a la IA", color = Color(0xFF8B949E), fontSize = 10.sp)
                }
            }
        }

        if (searchResults.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📁", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No se encontraron archivos con ese criterio", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Prueba con otra palabra clave o selecciona 'Todo el equipo'", color = Color(0xFF8B949E), fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(searchResults) { fileItem ->
                FileResultItemCard(
                    item = fileItem,
                    onClick = { selectedFileForDetail = fileItem }
                )
            }
        }
    }

    // --- MODAL DE DETALLE DE ARCHIVO & ACCIÓN CON ASISTENTE IA ---
    selectedFileForDetail?.let { file ->
        AlertDialog(
            onDismissRequest = { selectedFileForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(getEmojiForExtension(file.extension), fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(file.name, color = Color.White, fontSize = 15.sp, maxLines = 1)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Ubicación:", file.locationType)
                    DetailRow("Tamaño:", file.sizeStr)
                    DetailRow("Formato:", file.extension)
                    DetailRow("Modificado:", file.lastModified)
                    DetailRow("Ruta completa:", file.path)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prompt = "Hola Asistente, encontré este archivo en mi equipo: '${file.name}' (${file.extension}, ${file.sizeStr}, ubicado en '${file.path}'). ¿Qué me recomiendas hacer con él o cómo puedes ayudarme a procesarlo?"
                        selectedFileForDetail = null
                        onConsultAssistantWithFile(prompt)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Consultar con el Asistente")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("FilePath", file.path))
                        Toast.makeText(context, "Ruta copiada al portapapeles", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copiar Ruta", color = Color(0xFF8B949E))
                }
            },
            containerColor = Color(0xFF161B22)
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFF8B949E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun FileResultItemCard(
    item: ManagedFileItem,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF21262D)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(getFormatBadgeColor(item.extension).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(getEmojiForExtension(item.extension), fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    item.locationType,
                    color = if (item.locationType.contains("PC") || item.locationType.contains("Mac") || item.locationType.contains("Linux")) Color(0xFF58A6FF) else Color(0xFF7EE787),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    item.path,
                    color = Color(0xFF8B949E),
                    fontSize = 9.sp,
                    maxLines = 1,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF21262D))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(item.sizeStr, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.lastModified, color = Color(0xFF8B949E), fontSize = 9.sp)
            }
        }
    }
}

fun getEmojiForExtension(ext: String): String {
    return when (ext.uppercase()) {
        "PDF" -> "📄"
        "DOC", "DOCX", "TXT", "MD" -> "📝"
        "XLS", "XLSX", "CSV" -> "📊"
        "PPT", "PPTX" -> "📽️"
        "PNG", "JPG", "JPEG", "WEBP", "GIF" -> "🖼️"
        "MP4", "MKV", "MOV" -> "🎬"
        "MP3", "WAV", "OGG", "FLAC" -> "🎵"
        "JSON", "KT", "PY", "JS", "HTML", "CSS" -> "💻"
        "ZIP", "RAR", "TAR.GZ" -> "📦"
        else -> "📁"
    }
}

fun getFormatBadgeColor(ext: String): Color {
    return when (ext.uppercase()) {
        "PDF" -> Color(0xFFE53935)
        "DOC", "DOCX", "TXT" -> Color(0xFF1E88E5)
        "XLS", "XLSX" -> Color(0xFF43A047)
        "PNG", "JPG", "JPEG" -> Color(0xFF8E24AA)
        "MP4", "MKV" -> Color(0xFFFB8C00)
        "MP3", "WAV" -> Color(0xFF00ACC1)
        else -> Color(0xFF00F0FF)
    }
}
