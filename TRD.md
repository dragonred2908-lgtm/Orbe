# 🛠️ TRD: Documento de Requerimientos Técnicos (Technical Requirements Document)

**Proyecto:** IA Student (Orbe Cuántico)  
**Versión Técnica:** 1.0.0  
**Fecha:** Octubre 2026  
**Arquitectura:** Clean Architecture + MVVM + Compose Multiplatform  
**Lenguajes:** Kotlin 2.0+  
**Herramientas de Construcción:** Gradle (Kotlin DSL), Android Gradle Plugin 8.x

---

## 1. 🏗️ Arquitectura General del Sistema

El sistema implementa una arquitectura orientada a componentes desacoplados siguiendo los principios de **Clean Architecture** y el patrón **MVVM (Model-View-ViewModel)** con flujo unidireccional de datos (**Unidirectional Data Flow - UDF**).

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CAPA DE PRESENTACIÓN (UI)                      │
│   Jetpack Compose (Android) / Compose Multiplatform (Desktop)         │
│   - QuantumOrbCanvas (Shader / Simulación de partículas)               │
│   - ChatScreen / StepWizardScreen (Tutoría de 5 Pasos)                 │
│   - AssistantCustomizationDialog (Ajustes y Diagnóstico de API)        │
│   - FileSearchScreen / CommercialScreen / MediaStudioScreen           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Estado (StateFlow) / Eventos
┌───────────────────────────────────▼────────────────────────────────────┐
│                       CAPA DE NEGOCIO (VIEWMODELS)                     │
│   - AssistantViewModel / ChatViewModel / CommercialViewModel           │
│   - AudioRecorderHelper (Gestión de micrófono y RMS)                   │
│   - AudioPlayerHelper (Gestión de reproducción de síntesis de voz)    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Casos de Uso / Servicios
┌───────────────────────────────────▼────────────────────────────────────┐
│                    CAPA DE SEGURIDAD Y PROTECCIÓN                      │
│   - SecurityGuard (Sanitización de Prompts, Anti-Injection, PII)       │
│   - AudioPermissionService (Control de acceso a micrófono)             │
│   - Network Security Config (Strict TLS / No Cleartext)                │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Repositorios y Clientes
┌───────────────────────────────────▼────────────────────────────────────┐
│                        CAPA DE DATOS Y RED                            │
│   - GeminiApiClient (REST v1beta: generateContent, TTS, STT)          │
│   - LocalAssistantBrain (Fallback offline y lógica local)              │
│   - LocalFileSystemRepository (Búsqueda y navegación de archivos)      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 📦 Modularización del Proyecto

### 2.1 Módulo `:app` (Android Application)
- **Target SDK:** 36 (Android 16).
- **Min SDK:** 24 (Android 7.0 Nougat).
- **Librerías Clave:**
  - `androidx.compose.material3`: Sistema de diseño visual dinámico.
  - `androidx.lifecycle.viewmodel.compose`: Gestión del ciclo de vida y estado reactivo.
  - `kotlinx.coroutines`: Concurrencia y programación asíncrona no bloqueante.
  - `com.google.android.libraries.identity.googleid`: Compatibilidad con Credential Manager.

### 2.2 Módulo `:shared` (Compose Multiplatform / Desktop JVM)
- **Runtime:** JVM 17+.
- **Sistemas Operativos:** Windows (x64), macOS (Apple Silicon y x64), Linux (x64).
- **Librerías Clave:**
  - `org.jetbrains.compose`: UI declarativa multiplataforma.
  - `javax.sound.sampled`: Subsistema de audio nativo del JDK para captura de micrófono y diagnóstico de hardware de sonido.

---

## 3. 🌐 Especificación de Integración con Google Gemini API

### 3.1 Endpoints y Protocolo
- **Protocolo:** HTTPS / TLS 1.3 exclusivo.
- **Base URL:** `https://generativelanguage.googleapis.com/v1beta/`
- **Autenticación:** Cabecera HTTP `x-goog-api-key: {API_KEY}` o parámetro de consulta seguro.

### 3.2 Catálogo de Modelos y Casos de Uso
| Modelo | Identificador | Rol en la Aplicación |
| :--- | :--- | :--- |
| **Gemini 3.8 Flash** | `gemini-3.8-flash` | Modelo por defecto para conversación general, tutoría de AI Studio y búsqueda multimodal. |
| **Gemini 4 Argon** | `gemini-4-argon` | Razonamiento de alta complejidad, generación de código para Google Cloud y modo de pensamiento profundo. |
| **Gemini 3.1 Flash Lite** | `gemini-3.1-flash-lite` | Interacciones de ultrabaja latencia para respuestas rápidas de voz. |
| **Gemini TTS Preview** | `gemini-2.5-flash-preview-tts` | Síntesis de voz directa con modalidad de audio (`responseModalities: ["AUDIO"]`). |

### 3.3 Esquema de Síntesis de Voz (TTS)
```json
{
  "contents": [
    {
      "parts": [{ "text": "Hola, ¿en qué proyecto de Google AI Studio te gustaría trabajar hoy?" }]
    }
  ],
  "generationConfig": {
    "responseModalities": ["AUDIO"],
    "speechConfig": {
      "voiceConfig": {
        "prebuiltVoiceConfig": {
          "voiceName": "Puck"
        }
      }
    }
  }
}
```

### 3.4 Esquema de Transcripción de Audio (STT)
```json
{
  "contents": [
    {
      "parts": [
        { "text": "Transcribe fielmente este audio sin comentarios adicionales:" },
        {
          "inlineData": {
            "mimeType": "audio/wav",
            "data": "<BASE64_PCM_WAV_BYTES>"
          }
        }
      ]
    }
  ]
}
```

### 3.5 Diagnóstico de Latencia y Ping Test
El cliente implementa la función `testApiConnection(customKey: String?): ApiTestResult`, la cual:
1. Registra la marca de tiempo inicial en milisegundos (`t0 = currentTimeMillis()`).
2. Realiza una petición mínima de ping al modelo Flash.
3. Evalúa la respuesta en tiempo real y calcula `latencia = t1 - t0`.
4. Devuelve un resultado estructurado con el estado booleano, mensaje amigable y métrica de latencia en milisegundos.

---

## 4. 🎙️ Subsistema de Audio y Control de Permisos

### 4.1 Abstracción de Permisos Multiplataforma
```kotlin
interface AudioPermissionService {
    val status: StateFlow<AudioPermissionStatus>
    fun hasPermission(): Boolean
    fun requestPermission(onResult: (Boolean) -> Unit)
}

enum class AudioPermissionStatus {
    GRANTED,
    DENIED,
    NOT_REQUESTED
}
```

### 4.2 Pipeline de Captura de Audio
1. **Muestreo:** 16.000 Hz (16 kHz), 16 bits por muestra, canal Mono (PCM).
2. **Cálculo de Amplitud (RMS):** Se calcula el valor cuadrático medio de cada paquete de audio entrante y se expone a la UI para alimentar la física cuántica del orbe.
3. **Control de Flujo:** Cancelación automática de hilos de grabación cuando el usuario suelta el control de voz o transcurre un período de silencio superior al umbral configurado.

---

## 5. 🔒 Especificación de Seguridad, Protección y Ofuscación

### 5.1 Módulo `SecurityGuard`
- **Detección de Prompt Injection:** Evalúa entradas de usuario contra patrones conocidos de manipulación de instrucciones (ej. *"ignore previous instructions"*, *"system prompt:"*, *"DAN mode"*, etiquetas `<|im_start|>`).
- **Anonimización Preventiva de PII:** Identifica y sustituye datos sensibles antes de transmitirlos a APIs externas:
  - Tarjetas de crédito -> `[TARJETA_PROTEGIDA]`
  - Correos electrónicos -> `[EMAIL_PROTEGIDO]`
  - Teléfonos -> `[TELÉFONO_PROTEGIDO]`
  - Claves API filtradas accidentalmente en prompts -> `[API_KEY_PROTEGIDA]`
- **Validación y Enmascaramiento de Claves:** Valida el formato de claves de Google AI Studio (`^AIza[0-9A-Za-z\-_]{35,}$`) y genera máscaras visuales de seguridad (`AIza••••••••••••1234`).

### 5.2 Configuración de Red (`network_security_config.xml`)
- `cleartextTrafficPermitted="false"` activado a nivel global en la aplicación.
- Restricción estricta de tráfico a dominios verificados de Google Cloud y Google AI Studio (`generativelanguage.googleapis.com`).

### 5.3 Reglas de Ofuscación y Protección R8/ProGuard (`proguard-rules.pro`)
- Eliminación de invocaciones a `android.util.Log.v` y `Log.d` en compilaciones de producción (`release`) mediante `-assumenosideeffects`.
- Preservación de serialización y modelos DTO (`com.example.data.model.**`).
- Protección de interfaces criptográficas y de seguridad (`com.example.security.**`).

---

## 6. 💾 Persistencia y Almacenamiento Local

- **Directorio Privado:** Los datos de catálogo, historial de conversaciones y ajustes se almacenan en el directorio privado de la aplicación (`Context.getFilesDir()`).
- **Política de Backup:** `android:allowBackup="false"` configurado en el manifiesto para evitar que herramientas externas extraigan información sensible a través de copias de seguridad de depuración.
- **Acceso a Archivos Externos:** El explorador de archivos utiliza el `FileProvider` con autoridades restringidas (`${applicationId}.fileprovider`) para compartir archivos de forma temporal sin exponer rutas absolutas del almacenamiento.

---

## 7. 🧪 Estrategia de Pruebas Automatizadas

1. **Pruebas Unitarias de Android (`:app:testDebugUnitTest`):**
   - Ejecutadas en la JVM local mediante **Robolectric** con `@Config(sdk = [35])`.
   - Cobertura de validación de permisos de audio, ciclo de vida de grabación, modelos de datos de Gemini y funciones de sanitización de `SecurityGuard`.
2. **Pruebas de Escritorio (`:shared:jvmTest`):**
   - Validación del servicio de permisos de escritorio (`JvmAudioPermissionServiceTest`) y comprobación de soporte de líneas de entrada de audio en Java Sound API.
