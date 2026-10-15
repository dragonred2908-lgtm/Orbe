# Política de Privacidad y Tratamiento de Datos (Privacy Policy)

**Última actualización:** Octubre 2026  
**Proyecto:** IA Student (Orbe)

---

## 📌 Declaración Previa a la Distribución Pública

> ⚠️ **Aviso Importante:** Este documento describe los principios técnicos y el tratamiento de datos implementado en el código fuente de esta aplicación. **Antes de publicar o distribuir la aplicación en Google Play Store u otras plataformas comerciales, se publicará una versión legal definitiva y formal con los datos de contacto y jurisdicción de la entidad responsable.**

---

## 📊 Datos que Pueden Procesarse

La aplicación **IA Student (Orbe)** está diseñada bajo el principio de minimización de datos y soberanía del usuario:

1. **Interacciones de Texto y Consultas:**
   - Mensajes y preguntas ingresadas por el usuario en el chat o activadas mediante comandos.
   - Estos datos se envían a través de la API segura de **Google Gemini** para generar respuestas contextuales.
2. **Interacciones de Voz y Audio:**
   - La aplicación requiere acceso al micrófono exclusivamente para la detección de la frase de llamada (wake-word) y transcripción de comandos por voz cuando el usuario lo habilita.
   - El audio no se graba de forma continua ni se almacena permanentemente en servidores externos sin consentimiento explícito.
3. **Exploración de Archivos (Local y Remota):**
   - El explorador de archivos lee únicamente los metadatos y rutas del sistema local o de los nodos vinculados autorizados por el usuario.
   - Ningún archivo personal o documento se sube automáticamente a servidores en la nube sin la orden directa del usuario.
4. **Respuestas Automáticas y Catálogo Comercial:**
   - La información de productos, horarios y reglas de respuesta comercial se almacena localmente en el dispositivo (utilizando SQLite / Room / SharedPreferences seguras).
5. **Servicios de Google Firebase y App Check:**
   - Si se activa la sincronización en la nube, se emplean servicios seguros de Firebase bajo reglas de seguridad estrictas (Zero-Trust Security Rules) y verificación de integridad mediante Firebase App Check.

---

## 🔒 Uso de APIs de Terceros (Google Gemini & Firebase)

- Las llamadas a los modelos de inteligencia artificial se procesan bajo los términos de privacidad y protección de datos provistos por las APIs de Google Cloud y Google AI Studio.
- No se comercializan ni transfieren datos personales de los usuarios a terceros con fines publicitarios no declarados.

---

## 🛡️ Control y Derechos del Usuario

El usuario tiene el control total sobre sus datos en cualquier momento desde el menú de la aplicación:
- **Gestión de Permisos Granulares:** Desde la pantalla de *Permisos y Usos*, el usuario puede revocar en un solo toque el acceso al micrófono, archivos, red, automatización o cámara.
- **Borrado de Datos Locales:** El usuario puede limpiar el historial de conversaciones, catálogo comercial o desvincular dispositivos conectados en cualquier momento.

Para consultas relativas a la privacidad del proyecto, comuníquese a través del canal establecido en [SECURITY.md](SECURITY.md).
