package com.example.ui.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AiStudioBlue
import com.example.ui.theme.GeminiAmber
import com.example.ui.theme.GeminiViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingConsentScreen(
    onAcceptConsent: () -> Unit,
    onDeclineConsent: () -> Unit,
    modifier: Modifier = Modifier
) {
    var acceptedTerms by remember { mutableStateOf(false) }
    var acceptedPrivacy by remember { mutableStateOf(false) }
    var viewingDocumentTitle by remember { mutableStateOf<String?>(null) }
    var viewingDocumentContent by remember { mutableStateOf<String?>(null) }

    val isReadyToContinue = acceptedTerms && acceptedPrivacy

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1117),
                        Color(0xFF161B22),
                        Color(0xFF0A0E14)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Logo o Distintivo de Seguridad
            Surface(
                color = Color(0x2200F0FF),
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Protección Legal",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "Consentimiento y Marco Legal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Para comenzar a utilizar el asistente multiplataforma y sus modelos de IA, por favor revisa y acepta los términos de servicio y políticas de privacidad.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8B949E),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            HorizontalDivider(color = Color(0x2200F0FF), modifier = Modifier.padding(vertical = 4.dp))

            // Tarjetas de Acceso a Documentos Legales
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F0FF)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Documentación Oficial Vigente (v1.0):",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.SemiBold
                    )

                    LegalDocRow(
                        icon = Icons.Default.Description,
                        title = "Términos y Condiciones de Uso",
                        subtitle = "Licencia Apache-2.0, uso responsable y exención de responsabilidad",
                        onClick = {
                            viewingDocumentTitle = "Términos y Condiciones de Uso (v1.0)"
                            viewingDocumentContent = LegalTexts.TERMS_SUMMARY
                        }
                    )

                    LegalDocRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Política de Privacidad y Metadatos",
                        subtitle = "Procesamiento local, retención mínima y estampado EXIF",
                        onClick = {
                            viewingDocumentTitle = "Política de Privacidad y Metadatos"
                            viewingDocumentContent = LegalTexts.PRIVACY_SUMMARY
                        }
                    )

                    LegalDocRow(
                        icon = Icons.Default.Copyright,
                        title = "Procedimiento DMCA de Retirada",
                        subtitle = "Protocolo para la protección de derechos de autor",
                        onClick = {
                            viewingDocumentTitle = "Procedimiento de Retirada DMCA"
                            viewingDocumentContent = LegalTexts.DMCA_SUMMARY
                        }
                    )
                }
            }

            // Casillas de Verificación de Consentimiento Obligatorio
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2128)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { acceptedTerms = !acceptedTerms }
                    ) {
                        Checkbox(
                            checked = acceptedTerms,
                            onCheckedChange = { acceptedTerms = it },
                            modifier = Modifier.testTag("checkbox_accept_terms")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "He leído y acepto los Términos y Condiciones de Uso bajo licencia Apache-2.0.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { acceptedPrivacy = !acceptedPrivacy }
                    ) {
                        Checkbox(
                            checked = acceptedPrivacy,
                            onCheckedChange = { acceptedPrivacy = it },
                            modifier = Modifier.testTag("checkbox_accept_privacy")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acepto la Política de Privacidad, el almacenamiento en dispositivo y la adjunción de metadatos de autoría en contenido multimedia.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            // Botones de Acción
            Button(
                onClick = onAcceptConsent,
                enabled = isReadyToContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("accept_consent_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color(0xFF0A0E14),
                    disabledContainerColor = Color(0xFF21262D),
                    disabledContentColor = Color(0xFF8B949E)
                )
            ) {
                Text(
                    text = if (isReadyToContinue) "Aceptar y Comenzar" else "Acepta ambas casillas para continuar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (isReadyToContinue) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            TextButton(
                onClick = onDeclineConsent,
                modifier = Modifier.testTag("decline_consent_button")
            ) {
                Text(
                    text = "Rechazar y Cerrar",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // Diálogo Modal para Visualizar Documento Completo
    if (viewingDocumentTitle != null && viewingDocumentContent != null) {
        AlertDialog(
            onDismissRequest = {
                viewingDocumentTitle = null
                viewingDocumentContent = null
            },
            title = {
                Text(
                    text = viewingDocumentTitle ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = viewingDocumentContent ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewingDocumentTitle = null
                        viewingDocumentContent = null
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun LegalDocRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00F0FF),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8B949E), fontSize = 11.sp)
        }
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = "Leer",
            tint = Color(0xFF8B949E),
            modifier = Modifier.size(16.dp)
        )
    }
}

object LegalTexts {
    val TERMS_SUMMARY = """
1. Licencia de Software: El software se distribuye bajo la Apache License 2.0.
2. Uso de IA: El contenido es generado mediante modelos Google Gemini y es responsabilidad del usuario validar su idoneidad.
3. Responsabilidad: Queda prohibido el uso de la aplicación para fines ilícitos, engañosos o infractores.
4. Claves de API: El usuario resguarda sus propias credenciales de acceso a Google Cloud / AI Studio.
5. Metadatos: Al exportar multimedia, se adjuntan campos EXIF de autoría y licencia para asegurar la procedencia.
Consulta el documento completo en el repositorio: LEGAL/TERMS.md
    """.trimIndent()

    val PRIVACY_SUMMARY = """
1. Datos Técnicos: Configuraciones del asistente y estado de consentimiento se guardan en el dispositivo mediante DataStore.
2. Micrófono y Audio: El audio se procesa en búferes locales exclusivamente para la detección de llamada o transcripción STT.
3. API de Gemini: Las peticiones multimodales se envían mediante conexiones cifradas HTTPS a Google. No se comparten datos con terceros.
4. Derechos del Usuario: Puedes revocar el consentimiento y eliminar todos los datos locales desde la sección 'Legal y Privacidad' de Ajustes.
Consulta el documento completo en el repositorio: LEGAL/PRIVACY.md
    """.trimIndent()

    val DMCA_SUMMARY = """
1. Protección de Derechos: Respetamos la propiedad intelectual y atendemos solicitudes DMCA de retirada.
2. Notificaciones: Los titulares pueden reportar infracciones con firma, ubicación del material y declaración de titularidad.
3. Contacto: Envía reclamaciones a través de los canales oficiales de soporte o un Issue etiquetado en el repositorio.
Consulta el documento completo en el repositorio: LEGAL/DMCA.md
    """.trimIndent()
}
