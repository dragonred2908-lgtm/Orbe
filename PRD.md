# 📄 PRD: Documento de Requerimientos de Producto (Product Requirements Document)

**Proyecto:** IA Student (Orbe Cuántico)  
**Versión del Documento:** 1.0.0  
**Fecha de Publicación:** Octubre 2026  
**Estado:** Aprobado / En Producción  
**Plataformas:** Android (móvil, tablet, plegables) y Escritorio (Windows, macOS, Linux vía Compose Multiplatform)

---

## 1. 🌟 Resumen Ejecutivo y Visión del Producto

**IA Student** es una aplicación multiplataforma de asistencia inteligente de nueva generación. Combina una interfaz cinematográfica inmersiva basada en un **Orbe Cuántico 3D reactivo** con un motor de tutoría y asistencia especializado en **Google AI Studio** y los modelos fundacionales de la familia **Gemini**.

El objetivo principal es democratizar el aprendizaje, la experimentación y la construcción de aplicaciones de inteligencia artificial generativa, acompañando a usuarios desde un nivel principiante hasta niveles técnicos avanzados mediante una metodología estructurada de 5 pasos, interacción por voz bidireccional en tiempo real, gestión de archivos inteligente y automatización comercial.

---

## 2. 🎯 Objetivos Estratégicos y Alcance

### 2.1 Objetivos Clave
1. **Guiar con Rigor:** Proporcionar explicaciones precisas sin alucinaciones técnicas sobre las capacidades de Google AI Studio, Gemini API y Vertex AI.
2. **Experiencia de Voz Natural:** Ofrecer una experiencia de voz fluida de baja latencia con frase de activación configurable, captura PCM a 16 kHz y síntesis de voz neuronal (TTS).
3. **Privacidad y Soberanía de Datos:** Implementar una arquitectura *Local-First* donde el almacenamiento de proyectos, configuraciones y archivos permanece en el dispositivo del usuario sin telemetría invasiva.
4. **Ecosistema Multiplataforma:** Experiencia unificada entre Android (`:app`) y ordenadores de escritorio (`:shared`), permitiendo búsqueda remota de archivos y sincronización de flujos de trabajo.

### 2.2 Alcance del Sistema
- **En Alcance (MVP y Versión Actual):**
  - Orbe cuántico interactivo con física de partículas sensibles al audio y al tacto.
  - Asistente pedagógico para Google AI Studio (Paso 1 a Paso 5).
  - Reconocimiento de voz en tiempo real (STT) y síntesis de audio (TTS).
  - Banco de pruebas y diagnóstico interactivo de latencia para claves de Gemini API.
  - Búsqueda inteligente de archivos locales y remotos entre nodos vinculados.
  - Asistente comercial para respuestas automáticas a clientes con catálogo de productos.
  - Laboratorio multimedia de edición y generación visual.
  - Sistema de permisos granulares con activación/pausa instantánea.
- **Fuera de Alcance:**
  - Almacenamiento obligatorio en la nube con cuentas de terceros obligatorias (el sistema prioriza la privacidad local).
  - Ejecución de modelos LLM completos en dispositivos sin acelerador NPU (se delega a Gemini API o al cerebro local ligero).

---

## 3. 👥 Personas y Usuarios Objetivo

| Perfil | Descripción | Necesidad Principal |
| :--- | :--- | :--- |
| **Estudiante / Desarrollador Junior** | Desarrollador o estudiante interesado en crear apps con IA generativa. | Comprender prompts, parámetros (temperatura, top-p, tokens) y crear su primera app en AI Studio sin perderse en jerga técnica. |
| **Creador de Contenido / Diseñador** | Profesional multimedia que busca generar imágenes, guiones y activos visuales. | Acceso rápido al laboratorio multimedia, previsualización en vivo y edición multimodal asistida. |
| **Emprendedor / Negocio Local** | Dueño de comercio o profesional independiente. | Automatizar respuestas a clientes frecuentes mediante el asistente comercial con su propio catálogo. |
| **Usuario Avanzado Multidispositivo** | Usuario con teléfono Android y PC (Windows/Mac/Linux). | Búsqueda cruzada de archivos, asistencia por voz en manos libres y personalización profunda de modelos. |

---

## 4. 🧭 Casos de Uso Críticos (Critical User Journeys - CUJs)

### CUJ-01: Creación Guiada de una Aplicación en Google AI Studio
1. El usuario abre la app y activa el Asistente en modo Guía.
2. **Paso 1 (Comprensión):** El asistente formula preguntas concretas sobre el tipo de app, audiencia y necesidades de datos.
3. **Paso 2 (Conceptos):** Explica con analogías claras qué modelo de Gemini conviene usar (`gemini-3.8-flash` vs `gemini-4-argon`) y cómo calibrar la temperatura.
4. **Paso 3 (Opciones):** Presenta opciones concretas (Chatbot conversacional, flujo de pasos o integración API).
5. **Paso 4 (Construcción):** Entrega el system prompt optimizado y el código de conexión para la API.
6. **Paso 5 (Pruebas y Ajustes):** Guía al usuario en el panel de pruebas de AI Studio.

### CUJ-02: Asistencia por Voz con Frase de Activación en Manos Libres
1. El usuario pronuncia la frase de activación (ej. *"Oye Aura"* o *"IA Student"*).
2. El orbe cuántico reacciona visualmente aumentando su velocidad de rotación y deformación de partículas.
3. El usuario formula su pregunta en lenguaje natural.
4. El sistema captura el audio PCM a 16 kHz, lo procesa con la API de Gemini (o cerebro local si está offline), y emite la respuesta con síntesis de voz natural mientras las partículas del orbe vibran al compás del habla.

### CUJ-03: Diagnóstico y Validación de Conexión de Gemini API
1. El usuario accede a Ajustes del Asistente y pulsa en la sección de Integración de API.
2. Ingresa su clave API de Google AI Studio y presiona **"🧪 Probar Conexión"**.
3. El sistema valida el formato, enmascara la clave en la interfaz, envía una solicitud ligera de prueba y reporta el estado junto con la latencia en milisegundos.

---

## 5. 📋 Requerimientos Funcionales (Functional Requirements)

### FR-01: Orbe Cuántico y Renderizado Visual
- El sistema debe renderizar un orbe 3D volumétrico con simulación de partículas sobre un fondo espacial oscuro.
- Las partículas deben responder a:
  - Nivel de entrada de micrófono (RMS).
  - Estado del sistema (reposo, escuchando, pensando, hablando, error).
  - Toque y gestos en pantalla (rotación y deformación elástica).

### FR-02: Reconocimiento y Síntesis de Voz (STT y TTS)
- Soporte para frase de activación configurable por el usuario.
- Transcripción de audio continua con control de nivel de silencio (VAD).
- Modalidad de salida de audio (`responseModalities: ["AUDIO"]`) utilizando voces neuronales de Gemini (*Puck*, *Charon*, *Kore*, *Fenrir*, *Aoede*) con fallback a Text-to-Speech del sistema operativo.

### FR-03: Motor Experto de Google AI Studio
- Implementación de las reglas estrictas de honestidad técnica:
  - Prohibición estricta de alucinaciones sobre funciones inexistentes.
  - Declaración explícita cuando una función no está soportada en AI Studio, ofreciendo alternativas reales.
- Asesoramiento de parámetros de inferencia:
  - Temperatura (0.0 a 2.0).
  - Top-P y Top-K.
  - Safety Settings (bloqueo de contenido dañino).
  - System Instructions y Few-Shot Prompts.

### FR-04: Búsqueda Inteligente de Archivos (Local y Remota)
- Exploración de archivos locales en almacenamiento interno con filtrado por tipo (documentos, imágenes, audio, video).
- Búsqueda remota P2P entre dispositivos vinculados bajo el mismo identificador de usuario sin intermediarios no cifrados.

### FR-05: Asistente Comercial y Automatización
- Configuración de catálogo de productos/servicios con precios y descripciones.
- Definición de horarios de atención al cliente y respuestas fuera de horario.
- Simulador de conversación para verificar el comportamiento del agente antes de activarlo.

### FR-06: Laboratorio Multimedia
- Generación de imágenes y retoque visual mediante modelos multimodales.
- Previsualización interactiva con opciones para guardar en galería o compartir de forma segura vía `FileProvider`.

### FR-07: Gestión Granular de Permisos
- Panel de control de permisos (Micrófono, Archivos, Notificaciones, Red).
- Capacidad para pausar o revocar permisos temporalmente desde la interfaz de la aplicación sin salir a los ajustes del sistema.

---

## 6. 🛡️ Requerimientos No Funcionales (Non-Functional Requirements)

### NFR-01: Rendimiento y Latencia
- Tiempo de arranque en frío (Cold start) < 1.5 segundos en dispositivos modernos.
- Latencia de respuesta de voz < 1.200 ms con conexión estándar de banda ancha o 5G.
- Consumo de CPU en reposo (pantalla del orbe activa) < 8% de uso de CPU promedio.

### NFR-02: Seguridad y Protección de Datos
- **Cero Texto Plano:** Todo el tráfico HTTP no cifrado está bloqueado a nivel de sistema operativo (`network-security-config.xml`).
- **Enmascaramiento de PII:** Sanitización preventiva de prompts para anonimizar tarjetas de crédito, correos electrónicos y números de teléfono antes de enviarlos a APIs externas.
- **Protección contra Prompt Injection:** Detección automática de vectores de evasión y manipulación de instrucciones de sistema.
- **Protección de Claves API:** Claves almacenadas en entorno protegido (BuildConfig / SharedPreferences encriptadas), nunca en el código fuente ni registradas en logs de producción.

### NFR-03: Compatibilidad de Plataforma
- **Android:** Soporte desde Android 7.0 (API Level 24) hasta Android 16 (API Level 36).
- **Escritorio:** Java Runtime Environment (JRE) 17+ en Windows 10/11, macOS (Intel y Apple Silicon) y distribuciones modernas de Linux.

### NFR-04: Accesibilidad (a11y)
- Cumplimiento de Material 3 con áreas táctiles mínimas de 48 x 48 dp.
- Soporte para lectores de pantalla TalkBack en Android y lectores de accesibilidad de escritorio.
- Modo de alto contraste y etiquetas descriptivas (`contentDescription`) en todos los elementos interactivos.

---

## 7. 📊 Métricas de Éxito y KPIs

1. **Tasa de Éxito en Interacciones de Voz:** > 95% de comandos de voz transcritos y respondidos correctamente.
2. **Latencia Media de Ping API:** < 300 ms en pruebas de diagnóstico a los servidores de Gemini.
3. **Satisfacción en Tutoría de AI Studio:** Flujo de 5 pasos completado sin reportes de alucinación técnica.
4. **Incidencias de Seguridad:** 0 filtraciones de credenciales o claves API registradas en código o registros públicos.
