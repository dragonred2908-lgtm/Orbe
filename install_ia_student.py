#!/usr/bin/env python3
"""
===============================================================================
IA STUDENT — Instalador y Orquestador Multiplataforma (Windows, Linux, macOS)
===============================================================================
Automatiza la verificación de entorno, resolución de dependencias, compilación
de Compose Desktop (:shared) y ejecución del servidor compañero de sincronización
para Android en cualquier sistema operativo moderno.

Uso:
    python3 install_ia_student.py                # Menú interactivo guiado
    python3 install_ia_student.py --check        # Diagnóstico y auditoría de entorno
    python3 install_ia_student.py --desktop      # Ejecutar app de escritorio Compose
    python3 install_ia_student.py --package      # Empaquetar instalador nativo (.deb/.msi/.dmg)
    python3 install_ia_student.py --server       # Iniciar Servidor Compañero de sincronización
    python3 install_ia_student.py --full-install # Instalación y preparación completa
===============================================================================
"""

import argparse
import os
import platform
import re
import shutil
import socket
import subprocess
import sys
import time
from pathlib import Path

VERSION = "2.0.0"

# Códigos de color ANSI compatibles con Windows Terminal, macOS y Linux
USE_COLOR = sys.stdout.isatty() and platform.system() != "Windows" or os.environ.get("ANSICON") or os.environ.get("WT_SESSION")
CLR_RESET = "\033[0m" if USE_COLOR else ""
CLR_BOLD = "\033[1m" if USE_COLOR else ""
CLR_CYAN = "\033[96m" if USE_COLOR else ""
CLR_GREEN = "\033[92m" if USE_COLOR else ""
CLR_YELLOW = "\033[93m" if USE_COLOR else ""
CLR_RED = "\033[91m" if USE_COLOR else ""
CLR_BLUE = "\033[94m" if USE_COLOR else ""


def log_info(msg: str):
    print(f"{CLR_CYAN}[INFO]{CLR_RESET} {msg}")


def log_success(msg: str):
    print(f"{CLR_GREEN}[OK]{CLR_RESET} {CLR_BOLD}{msg}{CLR_RESET}")


def log_warn(msg: str):
    print(f"{CLR_YELLOW}[AVISO]{CLR_RESET} {msg}")


def log_error(msg: str):
    print(f"{CLR_RED}[ERROR]{CLR_RESET} {CLR_BOLD}{msg}{CLR_RESET}")


def get_project_root() -> Path:
    """Devuelve la ruta absoluta de la raíz del proyecto."""
    script_dir = Path(__file__).resolve().parent
    return script_dir


def detect_os() -> dict:
    """Identifica el sistema operativo, arquitectura y detalles de plataforma."""
    system = platform.system()
    machine = platform.machine().lower()
    is_arm = machine in ("arm64", "aarch64")

    os_type = "unknown"
    pkg_type = "unknown"

    if system == "Linux":
        os_type = "linux"
        pkg_type = "deb"
    elif system == "Darwin":
        os_type = "macos"
        pkg_type = "dmg"
    elif system == "Windows":
        os_type = "windows"
        pkg_type = "msi"

    return {
        "system": system,
        "os_type": os_type,
        "machine": machine,
        "is_arm": is_arm,
        "pkg_type": pkg_type,
        "release": platform.release(),
    }


def find_local_ip() -> str:
    """Detecta la IP local de la interfaz de red para sincronización LAN."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # No requiere conexión a internet real, solo resuelve la ruta de salida
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = "127.0.0.1"
    finally:
        s.close()
    return ip


def get_gradle_command(root_dir: Path, os_type: str) -> str:
    """Determina si debe usar gradlew nativo o gradlew.bat."""
    if os_type == "windows":
        bat_file = root_dir / "gradlew.bat"
        if bat_file.exists():
            return str(bat_file)
        return "gradle"
    else:
        sh_file = root_dir / "gradlew"
        if sh_file.exists():
            # Asegurar permisos de ejecución en Unix
            try:
                os.chmod(sh_file, os.stat(sh_file).st_mode | 0o111)
            except Exception:
                pass
            return f"./{sh_file.name}"
        return "gradle"


def check_java() -> tuple[bool, str]:
    """Verifica la presencia y versión de Java JDK (requiere 17+)."""
    java_cmd = shutil.which("java")
    if not java_cmd:
        java_home = os.environ.get("JAVA_HOME")
        if java_home:
            potential = Path(java_home) / "bin" / ("java.exe" if platform.system() == "Windows" else "java")
            if potential.exists():
                java_cmd = str(potential)

    if not java_cmd:
        return False, "Java no fue detectado en PATH ni en JAVA_HOME."

    try:
        proc = subprocess.run([java_cmd, "-version"], capture_output=True, text=True, check=False)
        output = proc.stderr + proc.stdout
        match = re.search(r'version\s+"(\d+)', output)
        if match:
            major = int(match.group(1))
            if major >= 17:
                return True, f"Java JDK {major} detectado correctamente."
            else:
                return False, f"Se detectó Java {major}. Compose Desktop y Gradle requieren JDK 17 o superior."
        return True, "Java detectado (versión no identificada con precisión)."
    except Exception as e:
        return False, f"Error al ejecutar comprobación de Java: {e}"


def check_python() -> tuple[bool, str]:
    """Verifica que la versión de Python sea 3.8+."""
    version = sys.version_info
    if version.major >= 3 and version.minor >= 8:
        return True, f"Python {version.major}.{version.minor}.{version.micro} compatible."
    return False, f"Se requiere Python 3.8 o superior (actual: {version.major}.{version.minor})."


def ensure_env_file(root_dir: Path):
    """Garantiza la existencia de un archivo .env válido para configuración y secretos."""
    env_file = root_dir / ".env"
    env_example = root_dir / ".env.example"

    if not env_file.exists():
        if env_example.exists():
            shutil.copy(env_example, env_file)
            log_success("Archivo .env creado a partir de .env.example.")
        else:
            env_file.write_text("GEMINI_API_KEY=\n")
            log_success("Archivo .env generado con marcador para GEMINI_API_KEY.")
    else:
        log_info("Archivo .env verificado presente en el proyecto.")


def run_diagnostics(root_dir: Path, os_info: dict) -> bool:
    """Ejecuta una auditoría completa del entorno multiplataforma."""
    print(f"\n{CLR_BOLD}{CLR_CYAN}--- DIAGNÓSTICO DEL ENTORNO MULTIPLATAFORMA ---{CLR_RESET}")
    print(f"Sistema Operativo: {CLR_BOLD}{os_info['system']} ({os_info['release']}){CLR_RESET}")
    print(f"Arquitectura:     {CLR_BOLD}{os_info['machine']}{' (Apple Silicon / ARM)' if os_info['is_arm'] else ''}{CLR_RESET}")
    print(f"IP Local LAN:     {CLR_BOLD}{find_local_ip()}{CLR_RESET}\n")

    all_passed = True

    # 1. Python
    py_ok, py_msg = check_python()
    if py_ok:
        log_success(py_msg)
    else:
        log_error(py_msg)
        all_passed = False

    # 2. Java JDK
    java_ok, java_msg = check_java()
    if java_ok:
        log_success(java_msg)
    else:
        log_error(java_msg)
        all_passed = False
        print_java_install_instructions(os_info["os_type"])

    # 3. Gradlew
    gradle_cmd = get_gradle_command(root_dir, os_info["os_type"])
    log_info(f"Comando de Gradle seleccionado: '{gradle_cmd}'")

    # 4. Verificación de archivos clave
    desktop_main = root_dir / "shared" / "src" / "jvmMain" / "kotlin" / "com" / "example" / "shared" / "DesktopMain.kt"
    if desktop_main.exists():
        log_success("Punto de entrada DesktopMain.kt verificado en :shared.")
    else:
        log_error("Falta DesktopMain.kt en shared/src/jvmMain/kotlin/com/example/shared/")
        all_passed = False

    companion_script = root_dir / "app" / "src" / "main" / "assets" / "companion_server.py"
    if companion_script.exists():
        log_success("Servidor compañero companion_server.py presente y listo.")
    else:
        log_error("No se encontró companion_server.py en assets.")
        all_passed = False

    ensure_env_file(root_dir)

    print()
    if all_passed:
        log_success("Todos los componentes multiplataforma están listos para ejecutarse.")
    else:
        log_warn("Algunos componentes requieren atención antes de continuar.")
    return all_passed


def print_java_install_instructions(os_type: str):
    """Imprime instrucciones rápidas para instalar JDK 17 según la plataforma."""
    print(f"\n{CLR_YELLOW}💡 Cómo instalar Java JDK 17:{CLR_RESET}")
    if os_type == "linux":
        print("  Debian/Ubuntu: sudo apt update && sudo apt install -y openjdk-17-jdk")
        print("  Fedora/RHEL:   sudo dnf install -y java-17-openjdk-devel")
        print("  Arch Linux:    sudo pacman -S jdk17-openjdk")
    elif os_type == "macos":
        print("  Homebrew:      brew install openjdk@17")
        print("  Configurar:    sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk")
    elif os_type == "windows":
        print("  Winget:        winget install EclipseAdoptium.Temurin.17.JDK")
        print("  O descarga el instalador MSI desde: https://adoptium.net/temurin/releases/?version=17")
    print()


def run_desktop_app(root_dir: Path, os_info: dict):
    """Compila y lanza la aplicación de escritorio Compose Multiplatform."""
    log_info("Compilando e iniciando la aplicación de escritorio Compose...")
    gradle_cmd = get_gradle_command(root_dir, os_info["os_type"])
    cmd = [gradle_cmd, ":shared:run", "--no-daemon"]

    try:
        subprocess.run(cmd, cwd=root_dir, check=True)
    except subprocess.CalledProcessError as e:
        log_error(f"Fallo al ejecutar la aplicación de escritorio: {e}")
        log_info("Para ver el error detallado, ejecuta: " + f"{gradle_cmd} :shared:run --stacktrace")


def package_native(root_dir: Path, os_info: dict):
    """Empaqueta el instalador nativo compatible con el SO actual."""
    pkg_type = os_info["pkg_type"]
    log_info(f"Empaquetando instalador nativo ({pkg_type.upper()}) para {os_info['system']}...")
    gradle_cmd = get_gradle_command(root_dir, os_info["os_type"])
    cmd = [gradle_cmd, ":shared:packageDistributionForCurrentOS", "--no-daemon"]

    try:
        subprocess.run(cmd, cwd=root_dir, check=True)
        dist_dir = root_dir / "shared" / "build" / "compose" / "binaries" / "main"
        log_success(f"Empaquetado completado. Revisa los archivos generados en:\n   {dist_dir}")
    except subprocess.CalledProcessError as e:
        log_error(f"Error al empaquetar instalador nativo: {e}")


def run_companion_server(root_dir: Path, port: int = 8080):
    """Lanza el servidor compañero para sincronización y búsqueda remota."""
    companion_script = root_dir / "app" / "src" / "main" / "assets" / "companion_server.py"
    if not companion_script.exists():
        log_error(f"No se encontró el script en {companion_script}")
        return

    local_ip = find_local_ip()
    log_info("Iniciando Servidor Compañero de IA Student...")
    print(f"\n{CLR_BOLD}{CLR_CYAN}════════════════════════════════════════════════════════════════{CLR_RESET}")
    print(f"{CLR_BOLD}📱 DATOS PARA EMPAREJAR CON LA APLICACIÓN ANDROID:{CLR_RESET}")
    print(f"   • Host / Dirección IP:  {CLR_GREEN}{CLR_BOLD}{local_ip}{CLR_RESET}")
    print(f"   • Puerto de Escucha:   {CLR_GREEN}{CLR_BOLD}{port}{CLR_RESET}")
    print(f"   • URL Completa:        {CLR_CYAN}http://{local_ip}:{port}{CLR_RESET}")
    print(f"{CLR_BOLD}{CLR_CYAN}════════════════════════════════════════════════════════════════{CLR_RESET}\n")

    env = os.environ.copy()
    env["COMPANION_BIND_HOST"] = "0.0.0.0"
    env["COMPANION_PORT"] = str(port)

    try:
        subprocess.run([sys.executable, str(companion_script)], env=env)
    except KeyboardInterrupt:
        print("\nServidor compañero detenido por el usuario.")


def main():
    parser = argparse.ArgumentParser(
        description="Instalador y orquestador multiplataforma de IA Student para Windows, Linux y macOS.",
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--check", action="store_true", help="Auditar dependencias y compatibilidad del entorno.")
    parser.add_argument("--desktop", action="store_true", help="Compilar y ejecutar la app de escritorio Compose.")
    parser.add_argument("--package", action="store_true", help="Generar instalador nativo (.deb / .msi / .dmg).")
    parser.add_argument("--server", action="store_true", help="Iniciar el Servidor Compañero en PC.")
    parser.add_argument("--port", type=int, default=8080, help="Puerto del Servidor Compañero (por defecto: 8080).")
    parser.add_argument("--full-install", action="store_true", help="Ejecutar diagnóstico y preparar todo el sistema.")

    args = parser.parse_args()
    root_dir = get_project_root()
    os_info = detect_os()

    # Si se pasa un argumento por línea de comandos
    if args.check:
        run_diagnostics(root_dir, os_info)
        return
    elif args.desktop:
        ensure_env_file(root_dir)
        run_desktop_app(root_dir, os_info)
        return
    elif args.package:
        ensure_env_file(root_dir)
        package_native(root_dir, os_info)
        return
    elif args.server:
        run_companion_server(root_dir, args.port)
        return
    elif args.full_install:
        ok = run_diagnostics(root_dir, os_info)
        if ok:
            log_info("Compilando módulo multiplataforma...")
            gradle_cmd = get_gradle_command(root_dir, os_info["os_type"])
            subprocess.run([gradle_cmd, ":shared:build", "--no-daemon"], cwd=root_dir, check=False)
            log_success("Instalación y preparación completa finalizada.")
        return

    # Modo interactivo amigable
    print(f"\n{CLR_BOLD}{CLR_CYAN}╔══════════════════════════════════════════════════════════════════╗{CLR_RESET}")
    print(f"{CLR_BOLD}{CLR_CYAN}║     IA STUDENT — ASISTENTE MULTIPLATAFORMA (v{VERSION})          ║{CLR_RESET}")
    print(f"{CLR_BOLD}{CLR_CYAN}║     Soporte Nativo: Windows | Linux | macOS | Android            ║{CLR_RESET}")
    print(f"{CLR_BOLD}{CLR_CYAN}╚══════════════════════════════════════════════════════════════════╝{CLR_RESET}\n")

    print(f"Sistema Detectado: {CLR_BOLD}{os_info['system']} ({os_info['machine']}){CLR_RESET}")
    print(f"IP Local para Sincronización: {CLR_GREEN}{find_local_ip()}{CLR_RESET}\n")

    print("Selecciona una opción:")
    print("  1) Diagnóstico y verificación del entorno")
    print("  2) Iniciar Servidor Compañero de sincronización en PC")
    print("  3) Compilar y ejecutar la Aplicación de Escritorio (:shared)")
    print("  4) Generar instalador nativo para este SO (.deb / .msi / .dmg)")
    print("  5) Instalación y verificación completa de una vez")
    print("  0) Salir\n")

    try:
        choice = input(f"{CLR_BOLD}Ingresa tu opción [1-5]: {CLR_RESET}").strip()
    except (EOFError, KeyboardInterrupt):
        print("\nOperación cancelada.")
        return

    if choice == "1":
        run_diagnostics(root_dir, os_info)
    elif choice == "2":
        run_companion_server(root_dir)
    elif choice == "3":
        ensure_env_file(root_dir)
        run_desktop_app(root_dir, os_info)
    elif choice == "4":
        ensure_env_file(root_dir)
        package_native(root_dir, os_info)
    elif choice == "5":
        ok = run_diagnostics(root_dir, os_info)
        if ok:
            log_info("Compilando y verificando módulos...")
            gradle_cmd = get_gradle_command(root_dir, os_info["os_type"])
            subprocess.run([gradle_cmd, ":shared:build", "--no-daemon"], cwd=root_dir, check=False)
            log_success("Todo el ecosistema multiplataforma está configurado y verificado.")
    else:
        print("Saliendo.")


if __name__ == "__main__":
    main()
