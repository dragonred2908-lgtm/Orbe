# Política de Privacidad

**Última actualización:** 18 de octubre de 2026  
**Versión:** 1.0.0

Esta Política de Privacidad describe cómo **IA Student** (en adelante, "la Aplicación") recopila, procesa, almacena y protege la información de los usuarios tanto en dispositivos móviles (Android) como en sistemas de escritorio (JVM Desktop).

Nos tomamos muy en serio la privacidad y el principio de minimización de datos. La arquitectura de la Aplicación está diseñada para priorizar el procesamiento y almacenamiento **local en el dispositivo**.

---

## 1. Tipos de Datos que la Aplicación Procesa

### A. Datos Técnicos y de Configuración (Almacenamiento Local)
- **Preferencias del Asistente:** Nombre asignado al asistente (ej. Aura), frase de llamada personalizada ("Oye Aura"), voz seleccionada, nivel de temperatura y parámetros de inferencia. Estos datos se guardan estrictamente en el dispositivo mediante SharedPreferences y Preferences DataStore.
- **Registro de Consentimiento:** Claves de aceptación de términos (`accepted_terms_v1`), marca de tiempo (*timestamp*) de consentimiento y versión legal aceptada.

### B. Credenciales y Claves de API
- **Clave de API (`GEMINI_API_KEY`):** Si el usuario proporciona su propia clave de Google AI Studio, esta se almacena de forma segura en memoria durante la sesión o en la configuración protegida del sistema. La clave **nunca** se envía a servidores de analítica de terceros; se transmite exclusivamente a los endpoints oficiales de Google (`https://generativelanguage.googleapis.com`) a través de conexiones cifradas TLS/HTTPS.

### C. Contenido Multimedia y Metadatos
- **Entradas de Voz y Micrófono:** El audio capturado por el micrófono se procesa localmente en búferes de memoria PCM/WAV exclusivamente para detectar la frase de llamada (*wake phrase*) o transcribir consultas habladas mediante la API de Speech-to-Text. El audio nunca se graba de forma continua ni se vende a intermediarios.
- **Imágenes y Archivos:** Las imágenes seleccionadas para prompts multimodales o edición se procesan de forma transitoria.
- **Metadatos de Exportación (EXIF / XMP / JSON Sidecar):** Cuando el usuario decide exportar una imagen o audio generado, la aplicación puede adjuntar metadatos de atribución (autoría declarada, prompt utilizado, licencia Apache-2.0, fecha de creación) con fines de trazabilidad y derechos de autor.

---

## 2. Finalidad del Tratamiento de los Datos

Los datos procesados se utilizan exclusivamente para:
1. Proveer las funciones del asistente conversacional, transcripción de voz y herramientas del laboratorio multimodal.
2. Validar que el usuario ha otorgado su consentimiento informado previo al uso de la aplicación.
3. Permitir la búsqueda local de archivos y la sincronización opcional con el servidor compañero de escritorio en la red local.

---

## 3. Transferencias a Terceros

- **Google Gemini API:** Para generar respuestas de texto, visión, música o video, las solicitudes son procesadas por los servidores de Google AI Studio conforme a los términos de servicio de la API de Google Gemini.
- **No Comercialización:** No vendemos, alquilamos ni compartimos datos personales o telemetría del usuario con agencias de publicidad o recopiladores de datos de terceros.

---

## 4. Retención y Eliminación de Datos

- Todos los proyectos, historial de conversaciones y preferencias creadas por el usuario residen localmente en las bases de datos SQLite/Room y DataStore del dispositivo.
- **Derecho al Olvido y Borrado Local:** El usuario puede en cualquier momento:
  1. Restablecer o revocar su consentimiento desde los Ajustes de la Aplicación (**Sección Legal y Privacidad**).
  2. Eliminar proyectos y conversaciones individuales desde la interfaz.
  3. Desinstalar la aplicación o limpiar los datos del sistema, lo cual elimina de manera permanente toda la información almacenada en el dispositivo.

---

## 5. Contacto y Soporte de Privacidad

Para consultas relacionadas con la privacidad, el ejercicio de derechos ARCO o aclaraciones sobre el manejo de metadatos, el usuario puede abrir una incidencia en el repositorio oficial o comunicarse con el equipo responsable a través de los canales de soporte indicados en `CONTRIBUTING.md`.
