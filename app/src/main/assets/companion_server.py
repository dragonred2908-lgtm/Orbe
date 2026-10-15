#!/usr/bin/env python3
"""
Companion Server para PC (Windows, macOS, Linux)
Permite al Asistente en tu móvil buscar y sincronizar archivos con tu ordenador.

Uso:
  python3 companion_server.py
"""

import http.server
import socketserver
import os
import json
import socket
import sys

PORT = 8080

class RemoteSearchHandler(http.server.SimpleHTTPRequestHandler):
    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

    def do_POST(self):
        if self.path == '/search':
            content_length = int(self.headers.get('Content-Length', 0))
            post_data = self.rfile.read(content_length)
            try:
                req = json.loads(post_data.decode('utf-8'))
            except Exception:
                req = {}

            query = req.get('query', '').lower()
            base_dir = req.get('path', os.path.expanduser('~'))

            results = []
            if os.path.exists(base_dir) and os.path.isdir(base_dir):
                for root, dirs, files in os.walk(base_dir):
                    for file in files:
                        if not query or query in file.lower():
                            full_path = os.path.join(root, file)
                            try:
                                size_kb = os.path.getsize(full_path) // 1024
                                results.append({
                                    'name': file,
                                    'path': full_path,
                                    'size': f"{size_kb} KB",
                                    'type': os.path.splitext(file)[1].replace('.', '').upper()
                                })
                            except Exception:
                                pass
                    if len(results) >= 50:
                        break

            response_data = json.dumps({'status': 'ok', 'results': results}).encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(response_data)
        else:
            self.send_response(404)
            self.end_headers()

    def do_GET(self):
        if self.path == '/status':
            info = {
                'status': 'online',
                'hostname': socket.gethostname(),
                'platform': sys.platform,
                'home_dir': os.path.expanduser('~')
            }
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(json.dumps(info).encode('utf-8'))
        else:
            super().do_GET()

if __name__ == '__main__':
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
        s.close()
    except Exception:
        local_ip = "127.0.0.1"

    print("=" * 60)
    print(f"  ASISTENTE IA - NODO COMPAÑERO DE ESCRITORIO (PC / MAC / LINUX)")
    print("=" * 60)
    print(f"  Sistema: {sys.platform.upper()}")
    print(f"  Dirección IP Local: {local_ip}:{PORT}")
    print(f"  Carpeta Raíz: {os.path.expanduser('~')}")
    print("-" * 60)
    print(f"  1. Abre tu Asistente en tu móvil Android.")
    print(f"  2. Ve a Menú > 'Instalación en PC & Multiplataforma'.")
    print(f"  3. Agrega este equipo con la IP {local_ip}.")
    print(f"  ¡Listo! Ya puedes realizar búsquedas remotas en ambos equipos.")
    print("=" * 60)

    with socketserver.TCPServer(("", PORT), RemoteSearchHandler) as httpd:
        httpd.serve_forever()
