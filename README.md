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

#### 🐍 Método Recomendado: Instalador Universal Python (Automatizado)
El proyecto incluye un orquestador inteligente en Python (`install_ia_student.py`) que detecta automáticamente tu sistema operativo, verifica la presencia de Java 17+, configura `.env`, compila el proyecto y lanza el entorno sin configuraciones manuales:

```bash
# En Windows (PowerShell o CMD):
python install_ia_student.py

# En Linux o macOS (Terminal):
python3 install_ia_student.py
```

Comandos directos por línea de comandos:
```bash
# 1. Diagnóstico completo de dependencias y red LAN:
python3 install_ia_student.py --check

# 2. Ejecutar la app de escritorio nativa:
python3 install_ia_student.py --desktop

# 3. Generar el instalador nativo (.deb en Linux, .msi en Windows, .dmg en macOS):
python3 install_ia_student.py --package

# 4. Iniciar el Servidor Compañero para vincular y buscar archivos desde Android:
python3 install_ia_student.py --server

# 5. Instalación y preparación completa de una sola vez:
python3 install_ia_student.py --full-install
```

#### ⚙️ Método Tradicional con Gradle
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

## 🎙️ Modo de Voz SIN Conexión (sin GEMINI_API_KEY)

El asistente de voz de escritorio funciona **de forma totalmente offline**: no necesita
clave de API de Gemini ni internet en uso normal. La frase de activación
**"Oye Aura"**, la transcripción y las respuestas básicas se ejecutan 100 % en local.

### Cómo funciona

| Componente | Motor offline | Requisitos |
| :--- | :--- | :--- |
| **Reconocimiento de voz (STT)** | [Vosk](https://alphacephei.com/) con modelo pequeño de español (`vosk-model-small-es-0.42`, ~38 MB) | Micrófono. El modelo se descarga **una sola vez** a `~/.ia-student/models/` y luego opera sin red. |
| **Respuestas del asistente** | `OfflineAssistantBrain` (Kotlin puro, sin red): hora, fecha, saludos, identidad, ayuda y cálculos básicos | Ninguno |
| **Síntesis de voz (TTS)** | Voz nativa del sistema: Windows SAPI (`System.Speech`), macOS `say`, Linux `espeak-ng`/`espeak`/`spd-say` | Ninguno |

Si más tarde configuras tu `GEMINI_API_KEY` (botón de ajustes o archivo `.env`),
el asistente **mejora automáticamente a modo en línea** (Gemini) y usa el offline
como respaldo si la red falla. No hay que cambiar nada.

### Comandos que entiende en modo offline

- *"Oye Aura, ¿qué hora es?"*
- *"Oye Aura, ¿qué fecha es hoy?"*
- *"Oye Aura, ¿quién eres?"*
- *"Oye Aura, ¿qué puedes hacer?"* (lista de comandos)
- *"Oye Aura, ¿cuánto es 25 por 4?"* (suma, resta, multiplicación, división, potencia)
- Saludos y despedidas

### Primera ejecución (descarga del modelo)

Al primer uso del micrófono sin API key, la app descarga el modelo de Vosk
(~38 MB) una sola vez. Después ya nunca más necesita internet para escuchar.
Si prefieres prepararlo a mano (p. ej. sin red en el equipo):

```bash
# Descargar y extraer el modelo a la ruta que usa la app:
#   Windows: %USERPROFILE%\.ia-student\models\
#   Linux/macOS: ~/.ia-student/models/
curl -L -o vosk-model-small-es-0.42.zip https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip
# (descomprimir dentro de la carpeta .ia-student/models/)
```

### 🖼️ Interfaz: orbe a pantalla completa y menú ☰

La pantalla principal del escritorio es **el orbe a pantalla completa**, con una
**barra superior mínima** y un **menú hamburguesa (☰) en la esquina superior
derecha** desde el que se accede a todo lo demás:

| Opción del menú ☰ | Qué hace |
| :--- | :--- |
| **⚙️ Ajustes del asistente** | `GEMINI_API_KEY`, modelo de IA, frase de activación y presets |
| **🕘 Historial** | Todas las consultas y respuestas de la sesión |
| **🧪 Pruebas rápidas** | Simula la voz (hora, cálculo, identidad, ayuda) sin micrófono |
| **🖼️ Elegir imagen del orbe…** | Abre el selector de archivos y usa **tu propio GIF/PNG/JPG** como orbe |
| **🔇 Silenciar / 🔊 Activar voz** | Enciende o apaga la síntesis de voz |

Tu imagen del orbe **no necesita recompilar la app**: se carga desde disco al
arrancar. Colócala en:

```
Windows:       %USERPROFILE%\.ia-student\orb.gif   (o .png / .jpg / .jpeg)
Linux / macOS: ~/.ia-student/orb.gif
```

También puedes elegirla desde el menú ☰ → *Elegir imagen del orbe…*, que copia
el archivo a esa carpeta automáticamente. La app ya incluye el diseño del orbe
del proyecto en `shared/src/jvmMain/resources/orb_ia.gif` (406×720, 23
fotogramas, ~2 s en bucle); si no hay imagen del usuario se usa ese GIF
empaquetado. Como último recurso se dibuja el orbe cuántico procedural. Los GIF
se reproducen en bucle y se reducen a 720 px y 72 fotogramas como máximo para no
saturar PCs con renderizado por software.

Para diagnósticos o capturas de soporte, `IA_STUDENT_UI_PANEL=settings`,
`history` o `tests` abre ese panel lateral al arrancar.

### 🗣️ Precisión del reconocimiento (gramática restringida)

El modelo pequeño de Vosk confundía palabras parecidas. Ahora el reconocimiento
está **restringido por gramática** (`Recognizer.setGrammar`) al vocabulario real
del asistente: frase de activación, comandos, números y operadores. Con el mismo
modelo de 38 MB la transcripción de comandos mejora de forma notable.

Además, el cerebro offline entiende **números escritos en palabras**, porque el
modelo suele devolver *"veinticinco"* en lugar de *"25"*:

- *"Oye Aura, ¿cuánto es veinticinco por cuatro?"* → **100**
- *"Oye Aura, ¿cuánto es treinta y dos más ocho?"* → **40**
- *"Oye Aura, ¿cuánto es ciento veinte entre cuatro?"* → **30**

### 🔊 Voz más natural (no robótica)

La síntesis usa **la mejor voz en español instalada en el sistema**, elegida
automáticamente (prioridad: *Helena → Sabina → Laura → Pablo → Raúl → cualquier
voz `es-*`*). Antes se usaba la voz inglesa por defecto (*Zira*), que pronunciaba
el español con fonética inglesa: esa era la causa principal del sonido robótico.

- El texto viaja a la voz en **Base64 UTF-8**, así los acentos y signos `¿¡` se
  pronuncian correctamente en Windows.
- Cada respuesta **cancela la locución anterior**, evitando respuestas encoladas.
- Si tu Windows solo tiene voces inglesas, instala el paquete de idioma
  *Español (España)* → *Voz* en **Configuración → Hora e idioma → Idioma**.

### 📖 Tiempo para leer la respuesta

La última respuesta queda **fija en pantalla hasta la siguiente consulta** y el
asistente tarda **8 segundos** en volver a esperar la frase de activación (antes
1,2 s), de modo que siempre da tiempo a leer y a oír la respuesta completa.

---

## 🖥️ PCs Antiguas (sin DirectX 12 / GPU integrada antigua)

En equipos con GPUs antiguas (p. ej. **Intel HD Graphics** sin DirectX 12 ni
contexto OpenGL utilizable), Compose Desktop puede arrancar con la **ventana en
negro** porque el renderizador por defecto (Skia/DirectX-OpenGL) no puede
inicializarse.

Para forzar el **renderizado por software** (CPU, sin GPU) añade la variable de
entorno **antes de abrir la app**:

```powershell
# Windows (PowerShell — efectivo para la sesión actual):
$env:IA_STUDENT_SOFTWARE_RENDER = "1"
./gradlew :shared:run
```

```powershell
# Windows (permanente, a nivel de usuario):
setx IA_STUDENT_SOFTWARE_RENDER 1
```

```bash
# Linux / macOS:
export IA_STUDENT_SOFTWARE_RENDER=1
./gradlew :shared:run
```

También puedes pasarlo como propiedad del JVM al empaquetar la app o al ejecutarla:
`-Dia.softwareRender=true` (equivalente), o fijar directamente
`-Dskiko.renderApi=SOFTWARE`. La app respeta cualquier valor que ya hayas fijado
en `skiko.renderApi` y no lo sobreescribe.

> 💡 El renderizado por software es más lento en animaciones pero garantiza que
> la interfaz sea visible y usable en cualquier equipo, sin GPU compatible.

---

---

## 🤖 Catálogo de Modelos Google Gemini Utilizados

La aplicación aprovecha las capacidades multimodales de la familia de modelos de **Google Gemini**:

| Modelo | Capacidad / Tarea | Módulos |
| :--- | :--- | :--- |
| **`gemini-3.8-flash`** | Texto general, asistente conversacional y respuestas con Google Search/Maps Grounding | `:app` y `:shared` |
| **`gemini-3.1-pro-preview`** | Razonamiento profundo, código, matemáticas y lógica de proyectos | `:app` y `:shared` |
| **`gemini-2.5-flash`** | Reconocimiento de voz (Speech-to-Text) multimodal sobre búfer WAV/PCM | `:app` y `:shared` |
| **`gemini-2.5-flash-preview-tts`** | Síntesis de voz (Text-to-Speech) con voces personalizadas (ej. Kore) | `:app` |
| **`gemini-nano-banana-2.1`** | Generación y retoque de imágenes por prompt en proporciones 1:1, 16:9 y 9:16 | `:app` |
| **`gemini-omni-1.1-flash`** | Generación de video cinemático (Interactions API, MP4 inline, 9:16/16:9) | `:app` |
| **`lyria-3-clip-preview`** | Generación de pistas musicales y clips de audio instrumental | `:app` |

---

## 🔑 Configuración Segura de Claves (GEMINI_API_KEY)

> 🛡️ **Buenas Prácticas de Seguridad:** No edites manualmente `.env` ni expongas claves en commits de Git.

### 1. Desde la Interfaz de Usuario (Recomendado)
- **En la Aplicación de Escritorio (`:shared`):**  
  Haz clic en el botón **`⚙️ Configurar API / Modelo`** en la esquina superior derecha de la ventana. Pega tu clave de Google Gemini directamente en el campo seguro y presiona **"Aplicar"**. La clave se almacena en memoria de forma protegida para tu sesión de trabajo sin persistir en texto plano en el repositorio.
- **En la Aplicación Móvil Android (`:app`):**  
  Abre la aplicación y accede a los ajustes del asistente para introducir o actualizar tu clave. Alternativamente, en el entorno de desarrollo de **Google AI Studio**, utiliza el panel lateral seguro de **Secrets** para inyectar automáticamente `GEMINI_API_KEY`.

### 2. Para Entornos de Integración Continua (GitHub Actions CI)
Para compilar y ejecutar tests en GitHub Actions con acceso opcional a las APIs:
1. En tu repositorio de GitHub, ve a **Settings > Secrets and variables > Actions**.
2. Haz clic en **New repository secret**.
3. Nombre: `GEMINI_API_KEY`.
4. Valor: Pega tu clave de API de Google AI Studio.
5. El flujo de CI (`.github/workflows/ci.yml`) inyecta automáticamente este secreto en el archivo `.env` antes de la ejecución de Gradle, asegurando que el secreto nunca se imprima en la consola ni quede registrado en el código fuente.

### 3. Manejo Seguro en `BuildConfig.GEMINI_API_KEY`
- El plugin **Secrets Gradle Plugin** está configurado para mapear `.env` con fallback seguro a `.env.example`:
  ```kotlin
  secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
    ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  }
  ```
- El código en `GeminiApiClient.kt` y `JvmGeminiSttEngine.kt` valida de forma proactiva si la clave obtenida es el marcador por defecto (`"MY_GEMINI_API_KEY"`). Si es así, no realiza peticiones vacías a la API y en su lugar guía amigablemente al usuario para ingresar su clave mediante la interfaz gráfica.
- Todas las credenciales se transmiten mediante encabezados HTTP seguros (`x-goog-api-key`), evitando su exposición en URLs, proxies o logs del sistema.

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

## ⚖️ Marco Legal, Privacidad y Consentimiento

Para asegurar la transparencia, el cumplimiento regulatorio y la protección del usuario, el proyecto cuenta con documentación legal formalizada:

- 📜 **[Términos y Condiciones de Uso](LEGAL/TERMS.md):** Condiciones de uso, responsabilidades del usuario, licencias y exenciones de responsabilidad.
- 🔒 **[Política de Privacidad](LEGAL/PRIVACY.md):** Tipos de datos procesados, almacenamiento local, minimización de telemetría y derechos de eliminación.
- 🛡️ **[Procedimiento de Retirada DMCA](LEGAL/DMCA.md):** Mecanismo para notificación y resolución de reclamaciones sobre derechos de autor.

---

## 🎨 Aviso Sobre Recursos e Imágenes (Assets)

Los recursos visuales ubicados en `app/src/main/res/drawable/` fueron generados para la identidad visual de la aplicación. Para mayor detalle sobre dependencias, atribuciones y revisión de licencias de terceros, consulta [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

