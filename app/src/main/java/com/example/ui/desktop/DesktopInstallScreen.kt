package com.example.ui.desktop

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
import com.example.data.repository.LinkedDevice
import com.example.data.repository.PlatformType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopInstallScreen(
    crossPlatformRepo: CrossPlatformRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val linkedDevices by crossPlatformRepo.linkedDevices.collectAsState()
    val syncCode by crossPlatformRepo.syncCode.collectAsState()

    var selectedPlatform by remember { mutableStateOf(PlatformType.WINDOWS) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var showScriptDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. BANNER MULTIPLATAFORMA ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00F0FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💻", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Instalación en PC y Multi-Dispositivo",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                "Usa el asistente en Windows, Mac, Linux y Android con sincronización remota",
                                color = Color(0xFF8B949E),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Código de sincronización único
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0A2E3B))
                            .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "TU CÓDIGO DE SINCRONIZACIÓN",
                                color = Color(0xFF80D8FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                syncCode,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("SyncCode", syncCode))
                                    Toast.makeText(context, "Código copiado: $syncCode", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar Código", tint = Color(0xFF00F0FF))
                            }
                            IconButton(
                                onClick = {
                                    crossPlatformRepo.generateNewSyncCode()
                                    Toast.makeText(context, "Nuevo código generado", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Nuevo Código", tint = Color(0xFF8B949E))
                            }
                        }
                    }
                }
            }
        }

        // --- 2. SELECTOR DE PLATAFORMA ---
        item {
            Text(
                "Guía de Instalación por Sistema Operativo",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlatformType.values().forEach { platform ->
                    val isSelected = selectedPlatform == platform
                    Button(
                        onClick = { selectedPlatform = platform },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF00F0FF) else Color(0xFF21262D),
                            contentColor = if (isSelected) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(platform.icon, fontSize = 18.sp)
                            Text(
                                platform.displayName.split(" ").first(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // --- 3. DETALLE DE PASOS SEGÚN PLATAFORMA SELECCIONADA ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "${selectedPlatform.icon} Cómo instalar y ejecutar en ${selectedPlatform.displayName}",
                        color = Color(0xFF80D8FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    when (selectedPlatform) {
                        PlatformType.WINDOWS -> {
                            InstallationStepItem("1", "Instalador Nativo / Compose Desktop: Ejecuta en PowerShell o CMD: 'python install_ia_student.py --desktop' o genera tu instalador '.msi' con '--package'.")
                            InstallationStepItem("2", "Sincronizador Automático de Archivos: Corre 'python install_ia_student.py --server' para indexar y vincular tus carpetas con este teléfono.")
                            InstallationStepItem("3", "Acceso Web Inmediato: Abre la URL compartida de esta app en Chrome o Edge e instala como PWA en tu PC.")
                            InstallationStepItem("4", "Emulador / WSA: Abre el APK directamente con Windows Subsystem for Android o emulador.")
                        }
                        PlatformType.MACOS -> {
                            InstallationStepItem("1", "App Nativa Compose (Apple Silicon / Intel): Ejecuta en Terminal 'python3 install_ia_student.py --desktop' o genera la imagen nativa '.dmg' con '--package'.")
                            InstallationStepItem("2", "Sincronizador macOS: Corre 'python3 install_ia_student.py --server' para buscar remotamente en tus carpetas Documentos y Descargas.")
                            InstallationStepItem("3", "Acceso Web PWA: Abre la URL del asistente en Safari o Chrome y selecciona 'Compartir' > 'Agregar al Dock'.")
                        }
                        PlatformType.LINUX -> {
                            InstallationStepItem("1", "App Nativa de Escritorio Compose: Ejecuta en terminal 'python3 install_ia_student.py --desktop' o genera el paquete nativo '.deb' con '--package'.")
                            InstallationStepItem("2", "Sincronizador Linux: Corre 'python3 install_ia_student.py --server' para habilitar la búsqueda remota de archivos.")
                            InstallationStepItem("3", "Método Waydroid / APK: Instala el paquete APK directamente en Waydroid sobre Ubuntu, Debian, Fedora o Arch.")
                        }
                        PlatformType.ANDROID -> {
                            InstallationStepItem("1", "Instalación Directa: Instala el paquete APK en tu teléfono o tablet Android.")
                            InstallationStepItem("2", "Asistente de Pantalla Completa: Toca el Orbe o usa tu frase personalizada para activarlo en cualquier momento.")
                            InstallationStepItem("3", "Vinculación con PC: Conecta este móvil a tu ordenador con el código de sincronización para buscar en ambos.")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val pyCmd = if (selectedPlatform == PlatformType.WINDOWS) "python install_ia_student.py --full-install" else "python3 install_ia_student.py --full-install"
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("PythonCommand", pyCmd))
                                Toast.makeText(context, "Comando copiado: $pyCmd", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00F0FF)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copiar Comando Python", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val url = "https://ais-pre-hwxxjf3hmyoce2sldcus2y-446617345142.us-east1.run.app"
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("AppURL", url))
                                Toast.makeText(context, "Enlace web copiado para abrir en tu PC", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copiar Link PC", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showScriptDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF238636)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF238636)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver / Copiar Script Seguro del Servidor", fontSize = 12.sp)
                    }
                }
            }
        }

        // --- 4. DISPOSITIVOS VINCULADOS & BUSQUEDA REMOTA ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Equipos Vinculados (${linkedDevices.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                TextButton(
                    onClick = { showAddDeviceDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00F0FF))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Vincular PC / Equipo", fontSize = 12.sp)
                }
            }
        }

        items(linkedDevices) { device ->
            DeviceItemCard(
                device = device,
                onToggleConnection = { crossPlatformRepo.toggleDeviceConnection(device.id) },
                onRemove = { crossPlatformRepo.removeDevice(device.id) }
            )
        }
    }

    // --- DIALOG: VER Y COPIAR SCRIPT COMPANION PARA PC ---
    if (showScriptDialog) {
        val script = crossPlatformRepo.getPythonDesktopCompanionScript()
        AlertDialog(
            onDismissRequest = { showScriptDialog = false },
            title = {
                Text("Companion Server para PC (Windows, Mac, Linux)", color = Color.White, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        "Este script seguro corre en 127.0.0.1 (localhost), requiere un token Bearer y solo devuelve metadatos. Ejecútalo en tu PC con 'python3 companion_server.py':",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D1117))
                            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            script,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF7EE787)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("PythonScript", script))
                        Toast.makeText(context, "Script copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        showScriptDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black)
                ) {
                    Text("Copiar Script")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScriptDialog = false }) {
                    Text("Cerrar", color = Color(0xFF8B949E))
                }
            },
            containerColor = Color(0xFF161B22)
        )
    }

    // --- DIALOG: VINCULAR NUEVO EQUIPO / PC ---
    if (showAddDeviceDialog) {
        var devName by remember { mutableStateOf("") }
        var devPlatform by remember { mutableStateOf(PlatformType.WINDOWS) }
        var devIp by remember { mutableStateOf("192.168.1.150") }
        var devPath by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDeviceDialog = false },
            title = { Text("Vincular Nuevo Equipo / PC", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = devName,
                        onValueChange = { devName = it },
                        label = { Text("Nombre del Equipo (ej: Laptop Trabajo)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Sistema Operativo:", color = Color(0xFF8B949E), fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlatformType.values().forEach { p ->
                            FilterChip(
                                selected = devPlatform == p,
                                onClick = { devPlatform = p },
                                label = { Text(p.icon + " " + p.displayName.split(" ").first(), fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = devIp,
                        onValueChange = { devIp = it },
                        label = { Text("Dirección IP local (ej: 192.168.1.100)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = devPath,
                        onValueChange = { devPath = it },
                        label = { Text("Ruta de búsqueda (dejar vacío para defecto)") },
                        placeholder = {
                            Text(
                                if (devPlatform == PlatformType.WINDOWS) "C:\\Users\\...\\Documents" else "/home/.../Documentos",
                                color = Color.Gray
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        crossPlatformRepo.addLinkedDevice(devName, devPlatform, devIp, devPath)
                        Toast.makeText(context, "Equipo vinculado con éxito", Toast.LENGTH_SHORT).show()
                        showAddDeviceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black)
                ) {
                    Text("Vincular")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDeviceDialog = false }) {
                    Text("Cancelar", color = Color(0xFF8B949E))
                }
            },
            containerColor = Color(0xFF161B22)
        )
    }
}

@Composable
fun InstallationStepItem(step: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF00F0FF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(step, color = Color(0xFF00F0FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = Color(0xFFC9D1D9), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
fun DeviceItemCard(
    device: LinkedDevice,
    onToggleConnection: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (device.isConnected) Color(0xFF238636) else Color(0xFF30363D)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(device.platform.icon, fontSize = 26.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            device.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (device.isConnected) Color(0xFF238636) else Color(0xFF6E7681))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (device.isConnected) "Conectado" else "Pausado",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        "${device.platform.displayName} • ${device.ipAddress}:${device.port}",
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp
                    )
                    Text(
                        "Ruta: ${device.defaultSearchPath}",
                        color = Color(0xFF58A6FF),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }

            Row {
                IconButton(onClick = onToggleConnection) {
                    Icon(
                        if (device.isConnected) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = "Alternar Conexión",
                        tint = if (device.isConnected) Color(0xFFE3B341) else Color(0xFF238636)
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = Color(0xFFF85149))
                }
            }
        }
    }
}
