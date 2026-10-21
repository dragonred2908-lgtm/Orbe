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
     * Script compañero para PC (Windows .bat / PowerShell, macOS / Linux bash).
     * Endurecido con las mismas protecciones que el servidor oficial empaquetado
     * en `app/src/main/assets/companion_server.py`:
     *   - Enlace por defecto SOLO en 127.0.0.1 (la LAN requiere variables de entorno explícitas).
     *   - Autenticación obligatoria por Bearer Token (generado criptográficamente al iniciar).
     *   - Validación/canonicalización de rutas y bloqueo de secretos sensibles.
     *   - Solo metadatos, máximo 25 resultados, CORS sin wildcard.
     */
    fun getPythonDesktopCompanionScript(): String {
        return """
# Companion Server Seguro de Sincronizacion Remota para PC (Windows, Mac, Linux)
# Ejecutalo en tu PC con: python3 companion_server.py
#
# Seguridad: escucha en 127.0.0.1 (localhost). No expone tu equipo en la red.
# El TOKEN se genera aleatoriamente en cada inicio y es necesario para cada peticion.

import http.server
import os
import secrets
import socket
import sys

PORT = 8080
SYNC_CODE = "${_syncCode.value}"
# De 25 a 25 resultados como maximo; nunca se devuelve el contenido de los archivos.
MAX_RESULTS_LIMIT = 25
AUTH_TOKEN = os.environ.get("COMPANION_AUTH_TOKEN", "").strip() or secrets.token_hex(24)

USER_HOME = os.path.realpath(os.path.abspath(os.path.expanduser('~')))
BLOCKED_PATTERNS = {
    ".ssh", ".gnupg", ".aws", ".config", ".env", ".git", ".password-store",
    "id_rsa", "id_ed25519", "known_hosts", "authorized_keys", ".bash_history",
    ".zsh_history", "credentials", "secrets.json", "keystore.jks"
}


def is_path_safe(target_path):
    real_path = os.path.realpath(os.path.abspath(target_path))
    if not (real_path == USER_HOME or real_path.startswith(USER_HOME + os.sep)):
        return False
    for part in real_path.split(os.sep):
        if part.lower() in BLOCKED_PATTERNS:
            return False
    return True


class SecureHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        print("[companion] " + (fmt % args))

    def _send_json(self, code, payload):
        import json
        encoded = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def _authenticated(self):
        header_auth = (self.headers.get("Authorization") or "").strip()
        header_custom = (self.headers.get("X-Companion-Token") or "").strip()
        token = ""
        if header_auth.startswith("Bearer "):
            token = header_auth[7:].strip()
        elif header_custom:
            token = header_custom
        return bool(token) and secrets.compare_digest(token, AUTH_TOKEN)

    def do_GET(self):
        if self.path == "/status":
            if not self._authenticated():
                return self._send_json(401, {"status": "error", "message": "Token invalido o ausente"})
            import json, platform as plat
            info = json.dumps({
                "status": "online",
                "hostname": socket.gethostname(),
                "platform": plat.system(),
                "max_results": MAX_RESULTS_LIMIT
            }, ensure_ascii=False).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(info)))
            self.end_headers()
            self.wfile.write(info)
            return
        self._send_json(404, {"status": "error", "message": "Ruta no encontrada"})

    def do_POST(self):
        import json
        if self.path != "/search":
            return self._send_json(404, {"status": "error", "message": "Endpoint no disponible"})
        if not self._authenticated():
            return self._send_json(401, {"status": "error", "message": "Token invalido o ausente"})
        try:
            length = int(self.headers.get("Content-Length", 0))
            if length <= 0 or length > 65536:
                return self._send_json(400, {"status": "error", "message": "Payload invalido"})
            req = json.loads(self.rfile.read(length).decode("utf-8"))
        except Exception:
            return self._send_json(400, {"status": "error", "message": "JSON malformado"})

        base_dir = os.path.realpath(os.path.abspath(str(req.get("path") or USER_HOME)))
        if not is_path_safe(base_dir) or not os.path.isdir(base_dir):
            return self._send_json(403, {"status": "error", "message": "Ruta fuera de los directorios permitidos"})

        query = str(req.get("query", "")).strip().lower()
        results = []
        try:
            for root, dirs, files in os.walk(base_dir):
                dirs[:] = [d for d in dirs if not d.startswith(".") and d.lower() not in BLOCKED_PATTERNS]
                if len(results) >= MAX_RESULTS_LIMIT:
                    break
                for file in files:
                    if len(results) >= MAX_RESULTS_LIMIT:
                        break
                    if file.startswith(".") or file.lower() in BLOCKED_PATTERNS:
                        continue
                    if query and query not in file.lower():
                        continue
                    full_path = os.path.join(root, file)
                    if os.path.islink(full_path):
                        if not is_path_safe(os.path.realpath(full_path)):
                            continue
                    try:
                        stat_info = os.stat(full_path)
                        results.append({
                            "name": file,
                            "relativePath": os.path.relpath(full_path, USER_HOME),
                            "sizeBytes": stat_info.st_size,
                            "type": os.path.splitext(file)[1].replace(".", "").upper() or "FILE",
                            "modifiedTimestamp": int(stat_info.st_mtime)
                        })
                    except OSError:
                        continue
        except Exception:
            return self._send_json(500, {"status": "error", "message": "Error interno durante la busqueda"})
        self._send_json(200, {"status": "ok", "count": len(results), "results": results})


if __name__ == "__main__":
    print("=" * 68)
    print("  IA STUDENT - COMPANION SERVER SEGURO")
    print("=" * 68)
    print("  Enlace:                 127.0.0.1:" + str(PORT) + " (restringido a localhost)")
    print("  Carpeta base permitida: " + USER_HOME)
    print("  Codigo de sincronizacion: " + SYNC_CODE)
    print("  TOKEN de autorizacion (requerido en la app movil):")
    print("  " + AUTH_TOKEN)
    print("=" * 68)
    print("  Define COMPANION_AUTH_TOKEN antes de ejecutar para fijar un token.")
    print("=" * 68)
    try:
        httpd = http.server.ThreadingHTTPServer(("127.0.0.1", PORT), SecureHandler)
        print("Servidor iniciado en http://127.0.0.1:" + str(PORT))
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("Servidor detenido por el usuario.")
    except Exception as exc:
        print("Fallo al iniciar el servidor: " + str(exc))
        sys.exit(1)
        """.trimIndent()
    }
}
