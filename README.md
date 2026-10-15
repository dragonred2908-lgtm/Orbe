# IA Student (Orbe) 🌌

> **Asistente Inteligente Multiplataforma con visualización cuántica, integración multimodal y sincronización móvil-desktop.**

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20Windows%20%7C%20macOS%20%7C%20Linux-blue.svg)]()
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

---

## 📖 Descripción General del Proyecto

**IA Student (Orbe)** es una aplicación avanzada de asistencia personal y profesional impulsada por inteligencia artificial. Presenta una interfaz de usuario minimalista y cinematográfica dominada por un **Orbe Cuántico 3D** con partículas dinámicas en cian y verde azulado sobre fondo negro profundo. A través de un menú contextual sutil, el usuario accede a potentes herramientas de interacción por voz/texto, búsqueda de archivos local y remota, automatización comercial y laboratorio multimedia.

### 🎯 Objetivo Funcional de la App

1. **Orbe Cuántico Inmersivo:** Pantalla completa interactiva con física de partículas, oscilaciones cuánticas reactivas a la interacción y al estado de procesamiento.
2. **Asistencia de Tiempo Completo y Reconocimiento de Voz (Speech-to-Text):**
   - Activación mediante frase clave personalizada ("Oye Aura", "IA Student", "Aura", o configurable en caliente).
   - Motor de Speech-to-Text en tiempo real integrado en el módulo `:shared` compatible con Windows, Linux, macOS y Android.
   - Captura de audio nativa PCM/WAV (16kHz 16-bit) con visualización de nivel de audio (RMS) y Orbe Cuántico reactivo.
   - Procesamiento inmediato de consultas mediante Google Gemini API con respuestas estructuradas en tiempo real.
3. **Búsqueda Inteligente de Archivos (Móvil y PC):**
   - Búsqueda y visualización de archivos en el dispositivo local.
   - Búsqueda remota entre dispositivos vinculados bajo el mismo usuario (Android, Windows, macOS, Linux).
   - Acceso guiado para usuarios principiantes y explorador por rutas avanzadas.
4. **Modo Asistente Comercial:**
   - Auto-respuesta inteligente para clientes según catálogo de productos/servicios y horarios configurables.
   - Simulador de conversación para pruebas en vivo y registro de auditoría.
5. **Control Granular de Permisos:** Panel centralizado para conceder o pausar permisos (micrófono, archivos, red, automatización, cámara, etc.).
6. **Laboratorio de Creación y Edición Multimedia:** Generación y retoque de imágenes por IA, presets visuales y herramientas de video.
7. **Soporte Multiplataforma:** Núcleo Android nativo (`:app`) y módulo compartido (`:shared`) con soporte para Compose Desktop (Windows, macOS y Linux).

---

## 💻 Requisitos de Sistema

Para compilar y ejecutar este proyecto se requiere:

- **JDK:** Java Development Kit 11 o superior (recomendado JDK 17 o JDK 21).
- **Android Studio:** Ladybug / Meerkat o superior con Android SDK instalado (API Level 24 a 36).
- **Android SDK Build Tools & Platform Tools.**
- **Gradle:** Gradle Wrapper incluido (`./gradlew` en Linux/macOS o `gradlew.bat` en Windows).
- *(Opcional para Desktop)*: Soporte gráfico compatible con Skia / Compose Desktop en Windows, macOS o Linux.

---

## 🚀 Cómo Ejecutar la App

### 📱 Android

Compilar el APK de desarrollo:
```bash
./gradlew :app:assembleDebug
```

Instalar directamente en un emulador o dispositivo físico conectado por ADB:
```bash
./gradlew :app:installDebug
```

Ejecutar las pruebas unitarias y de Robolectric:
```bash
./gradlew :app:testDebugUnitTest
```

### 🖥️ Desktop (Windows, macOS, Linux)

Compilar el módulo compartido y la versión Desktop:
```bash
./gradlew :shared:build
```

Ejecutar la aplicación de escritorio en modo interactivo:
```bash
./gradlew :shared:run
```

Generar instaladores nativos para el sistema operativo actual:
```bash
# Distribución para el SO en ejecución (DEB en Linux, MSI en Windows, DMG en macOS):
./gradlew :shared:packageDistributionForCurrentOS
```

---

---

## 🔑 Configuración Segura de Claves (GEMINI_API_KEY)

> 🛡️ **Buenas Prácticas de Seguridad:** No edites manualmente `.env` ni expongas claves en commits de Git.

### 1. Desde la Interfaz de Usuario (Recomendado)
- **En la Aplicación de Escritorio (`:shared`):**  
  Haz clic en el botón **`⚙️ Configurar API / Modelo`** en la esquina superior derecha de la ventana. Pega tu clave de Google Gemini directamente en el campo seguro y presiona **"Aplicar"**. La clave se almacena en memoria de forma protegida para tu sesión de trabajo sin persistir en texto plano en el repositorio.
- **En la Aplicación Móvil Android (`:app`):**  
  Abre la aplicación y accede a los ajustes del asistente para introducir o actualizar tu clave. Alternativamente, en el entorno de desarrollo de **Google AI Studio**, utiliza el panel lateral seguro de **Secrets** para inyectar automáticamente `GEMINI_API_KEY`.

### 2. Para Entornos de Integración Continua (CI/CD)
El archivo `.env.example` sirve como plantilla segura con marcadores de posición (`MY_GEMINI_API_KEY`). El archivo `.env` real está estrictamente ignorado por `.gitignore`.

---

## 🔒 Servidor Compañero de Escritorio (`companion_server.py`) [Opcional]

El proyecto incluye un servicio opcional (`app/src/main/assets/companion_server.py`) que permite emparejar tu PC con el asistente móvil para realizar búsquedas remotas de archivos entre dispositivos autorizados.

### Medidas de Seguridad y Endurecimiento:
- **Enlace Local por Defecto:** Escucha exclusivamente en `127.0.0.1` (localhost). Para permitir acceso desde tu red local segura, puedes definir la variable de entorno `COMPANION_BIND_HOST=0.0.0.0`.
- **Autenticación Obligatoria:** Requiere un token criptográfico generado automáticamente al iniciar el servidor o configurado con `COMPANION_AUTH_TOKEN`. Cada petición debe incluir el encabezado `Authorization: Bearer <TOKEN>` o `X-Companion-Token: <TOKEN>`.
- **Prevención de Directory Traversal:** Las búsquedas están estrictamente confinadas al directorio del usuario (`~`) o subcarpetas explícitas (`Documents`, `Desktop`, `Downloads`). Se bloquean accesos a carpetas y archivos sensibles (`.ssh`, `.gnupg`, `.aws`, `.env`, llaves privadas, etc.).
- **Privacidad de Archivos:** El servicio **nunca** transmite el contenido de los archivos; únicamente devuelve metadatos (nombre, extensión, tamaño en KB, fecha de modificación) con un límite máximo de 25 resultados.
- **CORS Restringido:** Se prohíbe el uso de `Access-Control-Allow-Origin: *`. Se valida el origen o se restringe a clientes autorizados.
- **Multi-hilo sin Bloqueo:** Implementado sobre `ThreadingHTTPServer` con registro estructurado de accesos y errores.

### Cómo Iniciar y Emparejar el Servidor:
```bash
# Iniciar el servidor (se generará un token único mostrado en consola):
python3 app/src/main/assets/companion_server.py

# Opcional: configurar host y token fijos para tu red privada:
COMPANION_BIND_HOST=0.0.0.0 COMPANION_AUTH_TOKEN="mi_token_secreto_personal" python3 app/src/main/assets/companion_server.py
```
Copia el **Token de Autorización** mostrado en tu terminal e introdúcelo en la pantalla de *Instalación en PC & Búsqueda Remota* de tu app.

---

## 📜 Licencia

Este proyecto está distribuido bajo los términos de la **Apache License 2.0**.  
Para más información, consulta el archivo [LICENSE](LICENSE).

---

## 🎨 Aviso Sobre Recursos e Imágenes (Assets)

Los recursos visuales ubicados en `app/src/main/res/drawable/` fueron generados para la identidad visual de la aplicación. Para mayor detalle sobre dependencias, atribuciones y revisión de licencias de terceros, consulta [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

