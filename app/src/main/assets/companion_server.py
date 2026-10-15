#!/usr/bin/env python3
"""
Companion Server Seguro para PC (Windows, macOS, Linux) — IA Student
Servicio de búsqueda y sincronización remota de archivos emparejado de forma segura.

Medidas de seguridad aplicadas:
  - Enlace por defecto restringido exclusivamente a 127.0.0.1 (localhost) o configurable por variable de entorno.
  - Autenticación obligatoria mediante Bearer Token (Authorization header) o header X-Companion-Token.
  - Validación y canonicalización estricta de rutas; prohibición de Directory Traversal ('..') y enlaces simbólicos externos.
  - Solo permite navegación en el HOME del usuario o subcarpetas autorizadas (Documents, Desktop, Downloads).
  - Bloqueo de directorios ocultos sensibles (.ssh, .gnupg, .aws, .env, etc.).
  - Límites estrictos: máximo 25 resultados devueltos por petición; solo metadatos, nunca contenido de archivos.
  - CORS restringido: lista blanca configurable, nunca wildcard '*'.
  - Servidor multi-hilo con ThreadingHTTPServer y registro estructurado de accesos y errores.
  - Cero credenciales embebidas en el código: generación automática de token criptográfico o uso de variable de entorno.
"""

import http.server
import json
import logging
import os
import secrets
import socket
import sys
from typing import List, Dict, Any, Optional

# Configuración de Logging
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s [%(threadName)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger("CompanionServer")

# Parámetros del Servidor
BIND_HOST = os.environ.get("COMPANION_BIND_HOST", "127.0.0.1").strip()
PORT = int(os.environ.get("COMPANION_PORT", "8080"))
MAX_RESULTS_LIMIT = 25

# Token de Autorización (Variable de entorno o generado criptográficamente en el host)
AUTH_TOKEN = os.environ.get("COMPANION_AUTH_TOKEN", "").strip()
if not AUTH_TOKEN:
    AUTH_TOKEN = secrets.token_hex(24)
    logger.info("Token de autorización único generado automáticamente para esta sesión.")

# Configuración de CORS Seguro
ALLOWED_ORIGINS = [
    origin.strip()
    for origin in os.environ.get("COMPANION_ALLOWED_ORIGINS", "http://localhost:8080,http://127.0.0.1:8080").split(",")
    if origin.strip()
]

# Rutas base permitidas (Home del usuario y subcarpetas explícitas)
USER_HOME = os.path.realpath(os.path.abspath(os.path.expanduser('~')))
ALLOWED_BASE_DIRS = [
    USER_HOME,
    os.path.join(USER_HOME, "Documents"),
    os.path.join(USER_HOME, "Desktop"),
    os.path.join(USER_HOME, "Downloads"),
    os.path.join(USER_HOME, "Projects")
]

# Directorios y archivos sensibles prohibidos
BLOCKED_PATTERNS = {
    ".ssh", ".gnupg", ".aws", ".config", ".env", ".git", ".password-store",
    "id_rsa", "id_ed25519", "known_hosts", "authorized_keys", ".bash_history",
    ".zsh_history", "credentials", "secrets.json", "keystore.jks"
}


def is_path_safe_and_allowed(target_path: str) -> bool:
    """Valida que la ruta solicitada sea canónica, esté dentro del HOME y no acceda a recursos prohibidos."""
    try:
        real_path = os.path.realpath(os.path.abspath(target_path))
        # Comprobar que comience por alguna de las carpetas base autorizadas
        is_under_allowed_base = any(
            real_path == base or real_path.startswith(base + os.sep)
            for base in ALLOWED_BASE_DIRS
        )
        if not is_under_allowed_base:
            return False

        # Comprobar que ningún componente del path sea un directorio/archivo bloqueado
        parts = real_path.split(os.sep)
        for part in parts:
            if part.lower() in BLOCKED_PATTERNS:
                return False

        return True
    except Exception as e:
        logger.warning(f"Error al validar ruta '{target_path}': {e}")
        return False


class HardenedCompanionHandler(http.server.SimpleHTTPRequestHandler):
    server_version = "IAStudent-Companion/2.0"
    sys_version = ""

    def log_message(self, format: str, *args: Any) -> None:
        logger.info(f"{self.client_address[0]} - {format % args}")

    def send_cors_headers(self) -> None:
        origin = self.headers.get("Origin", "")
        if origin and (origin in ALLOWED_ORIGINS or origin.startswith("http://localhost:") or origin.startswith("http://127.0.0.1:")):
            self.send_header("Access-Control-Allow-Origin", origin)
            self.send_header("Vary", "Origin")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Companion-Token")

    def is_authenticated(self) -> bool:
        """Verifica el token de autorización mediante comparación en tiempo constante."""
        auth_header = self.headers.get("Authorization", "").strip()
        custom_header = self.headers.get("X-Companion-Token", "").strip()

        token_to_check = ""
        if auth_header.startswith("Bearer "):
            token_to_check = auth_header[7:].strip()
        elif custom_header:
            token_to_check = custom_header

        if not token_to_check:
            return False

        return secrets.compare_digest(token_to_check, AUTH_TOKEN)

    def do_OPTIONS(self) -> None:
        self.send_response(200)
        self.send_cors_headers()
        self.end_headers()

    def do_GET(self) -> None:
        # Endpoint de salud y estado protegido
        if self.path == '/status':
            if not self.is_authenticated():
                self.send_error_response(401, "No autorizado. Token de emparejamiento inválido o ausente.")
                return

            info = {
                'status': 'online',
                'hostname': socket.gethostname(),
                'platform': sys.platform,
                'max_results': MAX_RESULTS_LIMIT,
                'bind_host': BIND_HOST
            }
            self.send_json_response(200, info)
        else:
            self.send_error_response(404, "Ruta no encontrada.")

    def do_POST(self) -> None:
        if self.path == '/search':
            if not self.is_authenticated():
                logger.warning(f"Intento de búsqueda no autorizado desde {self.client_address[0]}")
                self.send_error_response(401, "No autorizado. Token de emparejamiento inválido o ausente.")
                return

            content_length = int(self.headers.get('Content-Length', 0))
            if content_length <= 0 or content_length > 65536: # Máx 64KB de payload
                self.send_error_response(400, "Payload inválido o excede el tamaño máximo.")
                return

            try:
                post_data = self.rfile.read(content_length)
                req = json.loads(post_data.decode('utf-8'))
            except Exception as e:
                logger.warning(f"Error decodificando JSON: {e}")
                self.send_error_response(400, "JSON malformado.")
                return

            query = str(req.get('query', '')).strip().lower()
            requested_path = str(req.get('path', USER_HOME)).strip()

            # Validación y restricción de path
            if not is_path_safe_and_allowed(requested_path):
                logger.warning(f"Intento de acceso denegado a ruta no permitida: {requested_path}")
                self.send_error_response(403, "Acceso denegado: Ruta fuera de los directorios permitidos o contiene elementos restringidos.")
                return

            canonical_base = os.path.realpath(os.path.abspath(requested_path))
            if not os.path.isdir(canonical_base):
                self.send_error_response(404, "El directorio especificado no existe.")
                return

            results: List[Dict[str, Any]] = []
            max_limit = min(int(req.get('limit', MAX_RESULTS_LIMIT)), MAX_RESULTS_LIMIT)

            try:
                # Recorrer directorios de forma controlada
                for root, dirs, files in os.walk(canonical_base):
                    # Filtrar directorios ocultos o sensibles en caliente
                    dirs[:] = [d for d in dirs if not d.startswith('.') and d.lower() not in BLOCKED_PATTERNS]

                    for file in files:
                        if file.startswith('.') or file.lower() in BLOCKED_PATTERNS:
                            continue

                        if not query or query in file.lower():
                            full_path = os.path.join(root, file)
                            try:
                                # Comprobar que no sea enlace simbólico que apunte fuera
                                if os.path.islink(full_path):
                                    real_target = os.path.realpath(full_path)
                                    if not is_path_safe_and_allowed(real_target):
                                        continue

                                stat_info = os.stat(full_path)
                                size_kb = stat_info.st_size // 1024

                                # No devolvemos contenido de archivos, únicamente metadatos seguros
                                relative_path = os.path.relpath(full_path, USER_HOME)
                                results.append({
                                    'name': file,
                                    'relativePath': relative_path,
                                    'size': f"{size_kb} KB",
                                    'sizeBytes': stat_info.st_size,
                                    'type': os.path.splitext(file)[1].replace('.', '').upper() or 'FILE',
                                    'modifiedTimestamp': int(stat_info.st_mtime)
                                })
                            except (OSError, PermissionError):
                                continue

                        if len(results) >= max_limit:
                            break
                    if len(results) >= max_limit:
                        break

                response_payload = {
                    'status': 'ok',
                    'count': len(results),
                    'results': results
                }
                self.send_json_response(200, response_payload)
            except Exception as e:
                logger.error(f"Error durante la búsqueda: {e}")
                self.send_error_response(500, "Error interno durante la búsqueda de archivos.")
        else:
            self.send_error_response(404, "Endpoint no disponible.")

    def send_json_response(self, code: int, data: Dict[str, Any]) -> None:
        encoded = json.dumps(data, ensure_ascii=False).encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(encoded)))
        self.send_cors_headers()
        self.end_headers()
        self.wfile.write(encoded)

    def send_error_response(self, code: int, message: str) -> None:
        self.send_json_response(code, {'status': 'error', 'code': code, 'message': message})


def get_local_ip() -> str:
    """Detecta la IP local de la interfaz de red para facilitar el emparejamiento."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


if __name__ == '__main__':
    local_ip = get_local_ip()

    print("=" * 68)
    print("  🌌 IA STUDENT — COMPANION SERVER SEGURO (PC / MAC / LINUX)")
    print("=" * 68)
    print(f"  Sistema Operativo:    {sys.platform.upper()}")
    print(f"  Enlace (Bind Host):    {BIND_HOST}:{PORT} (Restringido)")
    print(f"  IP Local de Red:       {local_ip}")
    print(f"  Carpeta Base Permitida: {USER_HOME}")
    print("-" * 68)
    print("  🔐 TOKEN DE AUTORIZACIÓN (Requerido para emparejar con el móvil):")
    print(f"  {AUTH_TOKEN}")
    print("-" * 68)
    print("  📋 INSTRUCCIONES DE EMPAREJAMIENTO:")
    print("  1. Abre tu aplicación IA Student en tu dispositivo Android o Desktop.")
    print("  2. Accede a 'Instalación en PC & Búsqueda Remota'.")
    print(f"  3. Introduce la IP del equipo ({local_ip} o {BIND_HOST}) y el Token de arriba.")
    print("  4. Las solicitudes están protegidas con autenticación Bearer y límites.")
    print("=" * 68)

    # Uso de ThreadingHTTPServer para no bloquear peticiones concurrentes
    try:
        httpd = http.server.ThreadingHTTPServer((BIND_HOST, PORT), HardenedCompanionHandler)
        logger.info(f"Servidor iniciado y escuchando en http://{BIND_HOST}:{PORT}")
        httpd.serve_forever()
    except KeyboardInterrupt:
        logger.info("Servidor detenido por el usuario.")
    except Exception as e:
        logger.error(f"Fallo al iniciar el servidor: {e}")
        sys.exit(1)
