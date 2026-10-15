package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class PlatformType(val displayName: String, val icon: String) {
    ANDROID("Android", "📱"),
    WINDOWS("Windows 10/11", "🪟"),
    MACOS("macOS", "🍎"),
    LINUX("Linux", "🐧")
}

data class LinkedDevice(
    val id: String,
    val name: String,
    val platform: PlatformType,
    val ipAddress: String,
    val port: Int = 8080,
    val isConnected: Boolean = true,
    val defaultSearchPath: String,
    val lastSyncTime: String
)

data class RemoteFileItem(
    val name: String,
    val path: String,
    val size: String,
    val type: String,
    val deviceName: String,
    val platform: PlatformType,
    val lastModified: String
)

class CrossPlatformRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cross_platform_prefs", Context.MODE_PRIVATE)

    private val _linkedDevices = MutableStateFlow<List<LinkedDevice>>(loadLinkedDevices())
    val linkedDevices: StateFlow<List<LinkedDevice>> = _linkedDevices.asStateFlow()

    private val _syncCode = MutableStateFlow(loadSyncCode())
    val syncCode: StateFlow<String> = _syncCode.asStateFlow()

    private fun loadSyncCode(): String {
        var code = prefs.getString("sync_code", "")
        if (code.isNullOrBlank()) {
            code = "AI-" + (1000..9999).random().toString()
            prefs.edit().putString("sync_code", code).apply()
        }
        return code
    }

    private fun loadLinkedDevices(): List<LinkedDevice> {
        val raw = prefs.getString("linked_devices_json", null)
        if (raw.isNullOrBlank()) {
            // Dispositivos iniciales por defecto para facilitar la experiencia
            return listOf(
                LinkedDevice(
                    id = "dev_local",
                    name = "Móvil Android (Este Equipo)",
                    platform = PlatformType.ANDROID,
                    ipAddress = "127.0.0.1",
                    port = 8080,
                    isConnected = true,
                    defaultSearchPath = "/storage/emulated/0/Download",
                    lastSyncTime = "En línea"
                ),
                LinkedDevice(
                    id = "dev_pc_win",
                    name = "PC Principal (Windows)",
                    platform = PlatformType.WINDOWS,
                    ipAddress = "192.168.1.105",
                    port = 8080,
                    isConnected = true,
                    defaultSearchPath = "C:\\Users\\Usuario\\Documents",
                    lastSyncTime = "Hace 2 min"
                )
            )
        }
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<LinkedDevice>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    LinkedDevice(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        platform = PlatformType.valueOf(obj.getString("platform")),
                        ipAddress = obj.getString("ipAddress"),
                        port = obj.getInt("port"),
                        isConnected = obj.getBoolean("isConnected"),
                        defaultSearchPath = obj.getString("defaultSearchPath"),
                        lastSyncTime = obj.getString("lastSyncTime")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveDevices(list: List<LinkedDevice>) {
        val array = JSONArray()
        list.forEach { dev ->
            val obj = JSONObject().apply {
                put("id", dev.id)
                put("name", dev.name)
                put("platform", dev.platform.name)
                put("ipAddress", dev.ipAddress)
                put("port", dev.port)
                put("isConnected", dev.isConnected)
                put("defaultSearchPath", dev.defaultSearchPath)
                put("lastSyncTime", dev.lastSyncTime)
            }
            array.put(obj)
        }
        prefs.edit().putString("linked_devices_json", array.toString()).apply()
        _linkedDevices.value = list
    }

    fun addLinkedDevice(name: String, platform: PlatformType, ipAddress: String, path: String) {
        val newDev = LinkedDevice(
            id = "dev_" + System.currentTimeMillis(),
            name = name.ifBlank { "${platform.displayName} Sync Node" },
            platform = platform,
            ipAddress = ipAddress.ifBlank { "192.168.1.200" },
            port = 8080,
            isConnected = true,
            defaultSearchPath = path.ifBlank {
                when (platform) {
                    PlatformType.WINDOWS -> "C:\\Users\\Usuario\\Documents"
                    PlatformType.MACOS -> "/Users/usuario/Documents"
                    PlatformType.LINUX -> "/home/usuario/Documentos"
                    PlatformType.ANDROID -> "/storage/emulated/0/Download"
                }
            },
            lastSyncTime = "Recién vinculado"
        )
        saveDevices(_linkedDevices.value + newDev)
    }

    fun removeDevice(id: String) {
        saveDevices(_linkedDevices.value.filter { it.id != id })
    }

    fun toggleDeviceConnection(id: String) {
        val updated = _linkedDevices.value.map {
            if (it.id == id) it.copy(isConnected = !it.isConnected) else it
        }
        saveDevices(updated)
    }

    fun generateNewSyncCode(): String {
        val newCode = "AI-" + (1000..9999).random().toString()
        prefs.edit().putString("sync_code", newCode).apply()
        _syncCode.value = newCode
        return newCode
    }

    /**
     * Script compañero para PC (Windows .bat / PowerShell, macOS / Linux bash)
     * Permite ejecutar un servidor de sincronización y búsqueda de archivos con 1 solo comando.
     */
    fun getPythonDesktopCompanionScript(): String {
        return """
# Companion Server de Sincronizacion Remota para PC (Windows, Mac, Linux)
# Ejecuta este script en tu PC para permitir que el asistente busque archivos remotamente:
# python3 companion_server.py

import http.server
import socketserver
import os
import json
import socket

PORT = 8080
SYNC_CODE = "${_syncCode.value}"

class RemoteSearchHandler(http.server.SimpleHTTPRequestHandler):
    def do_POST(self):
        if self.path == '/search':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            req = json.loads(post_data.decode('utf-8'))
            query = req.get('query', '').lower()
            base_dir = req.get('path', os.path.expanduser('~'))
            
            results = []
            for root, dirs, files in os.walk(base_dir):
                for file in files:
                    if query in file.lower():
                        full_path = os.path.join(root, file)
                        results.append({
                            'name': file,
                            'path': full_path,
                            'size': f"{os.path.getsize(full_path) // 1024} KB",
                            'type': os.path.splitext(file)[1]
                        })
                if len(results) >= 25:
                    break
                    
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(json.dumps({'results': results}).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()

with socketserver.TCPServer(("", PORT), RemoteSearchHandler) as httpd:
    local_ip = socket.gethostbyname(socket.gethostname())
    print(f"=== NODO DE ESCRITORIO ACTIVO ===")
    print(f"IP Local: {local_ip}:{PORT}")
    print(f"Codigo de Sincronizacion: {SYNC_CODE}")
    print("El asistente en tu móvil ahora puede realizar búsquedas remotas en este equipo.")
    httpd.serve_forever()
        """.trimIndent()
    }
}
