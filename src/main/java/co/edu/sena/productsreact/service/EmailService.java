package co.edu.sena.productsreact.service;

// Se ejecuta una vez después de que Spring haya inyectado todas las dependencias (para verificar la configuración)
import jakarta.annotation.PostConstruct;
// Lombok: genera automáticamente el campo 'log' para registrar eventos (info, warn, error, debug)
import lombok.extern.slf4j.Slf4j;
// Spring: permite la inyección opcional de dependencias (si el bean no existe, no lanza error)
import org.springframework.beans.factory.annotation.Autowired;
// Spring: permite inyectar valores desde application.properties o variables de entorno
import org.springframework.beans.factory.annotation.Value;
// Spring Mail: cliente SMTP para enviar emails vía JavaMail (Gmail SMTP, Mailgun SMTP, etc.)
import org.springframework.mail.javamail.JavaMailSender;
// Spring Mail: helper que facilita la construcción de mensajes MIME (HTML, adjuntos, codificación UTF-8)
import org.springframework.mail.javamail.MimeMessageHelper;
// Spring: marca esta clase como un servicio de la capa de negocio gestionado por Spring
import org.springframework.stereotype.Service;

// Clases estándar de Java para hacer peticiones HTTP a APIs externas (Brevo, Resend, Gmail API)
import java.net.URI;                    // Representa la dirección URL del endpoint de la API
import java.net.http.HttpClient;        // Cliente HTTP nativo de Java 11+ para hacer peticiones sin librerías externas
import java.net.http.HttpRequest;       // Construye la petición HTTP con método, cabeceras y cuerpo
import java.net.http.HttpResponse;      // Contiene la respuesta HTTP con código de estado y cuerpo
import java.time.Duration;              // Define tiempos de espera máximos para las peticiones HTTP

// @Service: Spring crea un único bean de este servicio y lo inyecta donde sea necesario
@Service
// @Slf4j: Lombok genera el campo 'log' para escribir en los logs de la aplicación
@Slf4j
public class EmailService {

    // @Autowired(required = false): inyecta JavaMailSender si está configurado en application.properties;
    // si no hay configuración SMTP, este campo queda null (la app no falla al arrancar)
    @Autowired(required = false)
    private JavaMailSender mailSender;

    // URL del frontend (ej: https://dentvision.vercel.app); se usa para construir los enlaces en los emails
    // El valor por defecto "http://localhost:5173" sirve para desarrollo local
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // Brevo HTTP API (prioridad máxima — funciona en Render sin bloqueo de puertos)
    // Clave API de Brevo (antes SendinBlue); si está vacía, este canal de envío no se usa
    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    // Email remitente para los correos enviados vía Brevo o SMTP (el "De:" que verá el receptor)
    @Value("${app.mail.from:creyese23@gmail.com}")
    private String mailFrom;

    // Nombre visible del remitente que aparece en el cliente de correo del receptor (ej: "DentVision")
    @Value("${app.mail.from-name:DentVision}")
    private String mailFromName;

    // Resend (segunda opción)
    // Clave API de Resend (resend.com); si está vacía, este canal no se usa
    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    // Email remitente para envíos vía Resend; por defecto usa el dominio de pruebas de Resend
    @Value("${RESEND_FROM:onboarding@resend.dev}")
    private String resendFrom;

    // Gmail API (alternativa a SMTP cuando Render bloquea puertos)
    // Client ID de la aplicación OAuth2 registrada en Google Cloud Console
    @Value("${GMAIL_CLIENT_ID:}")
    private String gmailClientId;

    // Client Secret de la aplicación OAuth2 registrada en Google Cloud Console
    @Value("${GMAIL_CLIENT_SECRET:}")
    private String gmailClientSecret;

    // Refresh token OAuth2 de Gmail; permite obtener access tokens sin interacción del usuario
    // (se obtiene una sola vez y se rota automáticamente)
    @Value("${GMAIL_REFRESH_TOKEN:}")
    private String gmailRefreshToken;

    // SMTP — último recurso
    // Email del remitente configurado en spring.mail.username (para SMTP tradicional)
    @Value("${spring.mail.username:}")
    private String smtpUsername;

    // ── estado ──────────────────────────────────────────────────────────────

    // Verifica si la API de Brevo está disponible comprobando que la clave no esté vacía
    public boolean isBrevoConfigured() {
        return brevoApiKey != null && !brevoApiKey.isBlank();
    }

    // Verifica si la API de Resend está disponible comprobando que la clave no esté vacía
    public boolean isResendConfigured() {
        return resendApiKey != null && !resendApiKey.isBlank();
    }

    // Verifica si Gmail API está disponible comprobando que los tres credenciales OAuth2 estén presentes
    public boolean isGmailApiConfigured() {
        return gmailClientId != null && !gmailClientId.isBlank()
            && gmailClientSecret != null && !gmailClientSecret.isBlank()
            && gmailRefreshToken != null && !gmailRefreshToken.isBlank();
    }

    // Verifica si el servidor SMTP de Spring Mail está configurado (mailSender no es null)
    public boolean isSmtpConfigured() {
        return mailSender != null;
    }

    // Retorna true si al menos uno de los cuatro canales de email está disponible
    // (Brevo, Resend, Gmail API o SMTP); si todos fallan, el servicio no puede enviar emails
    public boolean canSendEmail() {
        return isBrevoConfigured() || isResendConfigured() || isGmailApiConfigured() || isSmtpConfigured();
    }

    // @PostConstruct: Spring ejecuta este método automáticamente después de inyectar todas las dependencias
    // Registra en el log el estado de cada canal de email al iniciar la aplicación (diagnóstico de configuración)
    @PostConstruct
    public void logSmtpStatus() {
        log.info("[EmailService] brevo={} | resend={} | gmailApi={} | mailSender={} | mailFrom='{}' | canSend={}",
            isBrevoConfigured(), isResendConfigured(), isGmailApiConfigured(),
            mailSender != null ? "OK" : "NULL", mailFrom, canSendEmail());
    }

    // ── URLs ─────────────────────────────────────────────────────────────────

    // Construye la URL completa para restablecer contraseña; incluye el token como parámetro de consulta
    // Ejemplo: https://dentvision.vercel.app/reset-password?token=550e8400-e29b-41d4-a716-446655440000
    public String buildResetUrl(String token) {
        return frontendUrl + "/reset-password?token=" + token;
    }

    // Construye la URL completa para verificar el email del usuario; el token identifica la cuenta a verificar
    // Ejemplo: https://dentvision.vercel.app/verify-email?token=550e8400-e29b-41d4-a716-446655440000
    public String buildVerificationUrl(String token) {
        return frontendUrl + "/verify-email?token=" + token;
    }

    // ── Emails públicos ───────────────────────────────────────────────────────

    // Envía el email de verificación de cuenta al usuario recién registrado
    public void sendVerificationEmail(String toEmail, String token) {
        // Lanza excepción si ningún canal de email está configurado (evita llamadas inútiles)
        if (!canSendEmail()) throw new IllegalStateException("Email no configurado");
        // Construye el enlace de verificación con el token único del usuario
        String link = buildVerificationUrl(token);
        // Invoca el método privado 'send' que intentará cada canal en orden (Brevo → Resend → Gmail → SMTP)
        send(toEmail,
            "DentVision - Confirma tu cuenta",     // Asunto del email que aparece en la bandeja de entrada
            buildHtml(
                "Confirma tu cuenta en DentVision", // Preheader: texto pequeño de vista previa en el cliente de correo
                "¡Bienvenido a DentVision!",        // Encabezado principal visible en el cuerpo del email
                "Tu cuenta ha sido creada exitosamente. Haz clic en el botón para confirmarla:", // Párrafo descriptivo
                link, "Verificar mi cuenta",         // URL y texto del botón de acción principal
                "Este enlace expirará en 24 horas. Si no creaste esta cuenta, ignora este mensaje." // Nota al pie de seguridad
            ));
        // Registra en el log que el email fue enviado exitosamente
        log.info("Email de verificación enviado a {}", toEmail);
    }

    // Envía el email de recuperación de contraseña con el enlace de reset
    public void sendPasswordResetEmail(String toEmail, String token) {
        // Lanza excepción si ningún canal de email está configurado
        if (!canSendEmail()) throw new IllegalStateException("Email no configurado");
        // Construye el enlace de reset con el token único generado para esta solicitud
        String link = buildResetUrl(token);
        // Envía el email usando el canal disponible (Brevo → Resend → Gmail → SMTP)
        send(toEmail,
            "DentVision - Recuperación de contraseña", // Asunto del email
            buildHtml(
                "Recuperación de contraseña – DentVision",  // Preheader de vista previa
                "Restablece tu contraseña",                 // Encabezado del email
                "Recibimos una solicitud para restablecer la contraseña de tu cuenta DentVision. Haz clic en el botón:", // Cuerpo
                link, "Restablecer contraseña",             // Botón de acción con el enlace de reset
                "Este enlace expirará en 1 hora. Si no solicitaste este cambio, puedes ignorar este correo." // Nota de seguridad
            ));
        // Registra en el log el envío exitoso
        log.info("Email de recuperación enviado a {}", toEmail);
    }

    // ── Notificaciones de citas ───────────────────────────────────────────────

    // Envía notificación al paciente cuando su cita médica ha sido confirmada por el odontólogo
    public void sendCitaConfirmada(String toEmail, String nombrePaciente,
                                   String motivo, String fechaHora, String odontologo) {
        // Si no hay canal de email disponible, registra advertencia y retorna sin lanzar excepción
        // (las notificaciones de citas no deben bloquear el flujo principal de la aplicación)
        if (!canSendEmail()) { log.warn("Email no configurado — notificación de cita no enviada"); return; }
        send(toEmail,
            "DentVision — Tu cita fue confirmada ✅",  // Asunto con emoji para destacar en la bandeja
            buildHtml(
                "Tu cita fue confirmada en DentVision",  // Preheader de vista previa
                "¡Cita confirmada!",                     // Encabezado del email
                // Cuerpo del email con los detalles de la cita formateados con HTML
                String.format("Hola %s, tu cita en DentVision ha sido <strong>confirmada</strong>.<br><br>"
                    + "<b>📋 Motivo:</b> %s<br>"           // Motivo de la consulta
                    + "<b>📅 Fecha y hora:</b> %s<br>"      // Fecha y hora exacta de la cita
                    + "<b>👨‍⚕️ Odontólogo:</b> %s<br><br>"    // Nombre del odontólogo asignado
                    + "Por favor llega 10 minutos antes de tu cita.",
                    nombrePaciente, motivo, fechaHora, odontologo), // Valores reemplazados en el template
                frontendUrl + "/paciente/citas", "Ver mis citas",   // Botón para ver todas las citas del paciente
                "Si necesitas reagendar o cancelar, hazlo desde tu panel de paciente con al menos 24 horas de anticipación."
            ));
        log.info("Confirmación de cita enviada a {}", toEmail);
    }

    // Envía notificación al paciente cuando su cita ha sido cancelada
    public void sendCitaCancelada(String toEmail, String nombrePaciente,
                                   String motivo, String fechaHora) {
        // Si no hay canal disponible, registra advertencia y sale sin error
        if (!canSendEmail()) { log.warn("Email no configurado — notificación de cancelación no enviada"); return; }
        send(toEmail,
            "DentVision — Tu cita fue cancelada",  // Asunto informativo sin emoji de urgencia
            buildHtml(
                "Cancelación de cita en DentVision", // Preheader de vista previa
                "Cita cancelada",                    // Encabezado del email
                // Cuerpo con los detalles de la cita cancelada
                String.format("Hola %s, te informamos que tu cita en DentVision ha sido <strong>cancelada</strong>.<br><br>"
                    + "<b>📋 Motivo:</b> %s<br>"    // Motivo original de la cita (no el motivo de cancelación)
                    + "<b>📅 Fecha y hora:</b> %s<br><br>"
                    + "Puedes agendar una nueva cita cuando lo necesites.",
                    nombrePaciente, motivo, fechaHora),
                frontendUrl + "/paciente/citas", "Agendar nueva cita", // Botón para agendar otra cita
                "Si crees que esto es un error, contáctanos respondiendo este correo."
            ));
        log.info("Notificación de cancelación enviada a {}", toEmail);
    }

    // Envía un recordatorio automático al paciente 24 horas antes de su cita
    public void sendCitaRecordatorio(String toEmail, String nombrePaciente,
                                      String motivo, String fechaHora, String odontologo) {
        // Si no hay canal disponible, registra advertencia y sale sin lanzar excepción
        if (!canSendEmail()) { log.warn("Email no configurado — recordatorio no enviado"); return; }
        send(toEmail,
            "DentVision — Recordatorio de cita 📅", // Asunto con emoji de calendario
            buildHtml(
                "Tienes una cita mañana en DentVision", // Preheader de vista previa
                "Recordatorio de cita",                  // Encabezado del email
                // Cuerpo con todos los detalles de la cita próxima
                String.format("Hola %s, te recordamos que <strong>mañana tienes una cita</strong> en DentVision.<br><br>"
                    + "<b>📋 Motivo:</b> %s<br>"
                    + "<b>📅 Fecha y hora:</b> %s<br>"
                    + "<b>👨‍⚕️ Odontólogo:</b> %s<br><br>"
                    + "Recuerda traer tu documento de identidad.", // Instrucción importante para la cita
                    nombrePaciente, motivo, fechaHora, odontologo),
                frontendUrl + "/paciente/citas", "Ver detalles", // Botón para ver el detalle de la cita
                "Para reagendar o cancelar hazlo con al menos 24 horas de anticipación."
            ));
        log.info("Recordatorio de cita enviado a {}", toEmail);
    }

    // ── Notificaciones de factura ─────────────────────────────────────────────

    // Notifica al paciente que se generó una nueva factura a su nombre
    public void sendFacturaGenerada(String toEmail, String nombrePaciente,
                                     Long facturaId, String total) {
        // Sale silenciosamente si no hay canal de email disponible
        if (!canSendEmail()) { log.warn("Email no configurado — notificación de factura no enviada"); return; }
        send(toEmail,
            "DentVision — Nueva factura disponible 🧾", // Asunto con emoji de factura
            buildHtml(
                "Tienes una nueva factura en DentVision", // Preheader de vista previa
                "Factura generada",                       // Encabezado del email
                // Cuerpo con el número de factura formateado a 6 dígitos y el total a pagar
                String.format("Hola %s, se ha generado una nueva factura por tus servicios en DentVision.<br><br>"
                    + "<b>📄 Factura #:</b> %s<br>"     // Número de factura con formato de 6 dígitos (ej: 000042)
                    + "<b>💰 Total:</b> %s COP<br><br>"  // Total en pesos colombianos
                    + "Puedes ver el detalle y realizar el pago desde tu panel.",
                    nombrePaciente, String.valueOf(facturaId).formatted("%06d", facturaId), total),
                frontendUrl + "/paciente/facturas", "Ver y pagar factura", // Botón para ir al módulo de pagos
                "Si tienes preguntas sobre esta factura, contáctanos."
            ));
        log.info("Notificación de factura enviada a {}", toEmail);
    }

    // ── Notificaciones de procedimientos ─────────────────────────────────────

    // Notifica al paciente cuando el estado de un procedimiento odontológico cambia
    public void sendProcedimientoActualizado(String toEmail, String nombrePaciente,
                                              String nombreProcedimiento, String estado) {
        // Sale silenciosamente si no hay canal disponible
        if (!canSendEmail()) { log.warn("Email no configurado — notificación de procedimiento no enviada"); return; }
        // Convierte el estado interno (ej: "COMPLETADO") a un texto legible con emoji para el email
        String estadoLabel = switch (estado) {
            case "COMPLETADO" -> "✅ Completado";  // Procedimiento finalizado exitosamente
            case "CANCELADO"  -> "❌ Cancelado";   // Procedimiento que no se realizará
            default           -> "⏳ En proceso";  // Procedimiento aún en curso (PENDIENTE, EN_PROCESO, etc.)
        };
        send(toEmail,
            "DentVision — Actualización de procedimiento", // Asunto genérico para cualquier estado
            buildHtml(
                "Actualización de procedimiento en DentVision", // Preheader de vista previa
                "Procedimiento actualizado",                     // Encabezado del email
                // Cuerpo con el nombre del procedimiento y su nuevo estado
                String.format("Hola %s, tu procedimiento <strong>%s</strong> ha sido actualizado.<br><br>"
                    + "<b>📋 Estado actual:</b> %s<br><br>"    // Estado formateado con emoji y texto legible
                    + "Puedes ver el detalle completo en tu historial clínico.",
                    nombrePaciente, nombreProcedimiento, estadoLabel),
                frontendUrl + "/paciente/historial", "Ver historial clínico", // Botón al historial del paciente
                "Si tienes preguntas, contacta a tu odontólogo tratante."
            ));
        log.info("Notificación de procedimiento enviada a {}", toEmail);
    }

    // Notifica al paciente cuando se registra un pago en su factura (total o parcial)
    public void sendPagoConfirmado(String toEmail, String nombrePaciente,
                                    Long facturaId, String valor, String metodoPago, boolean facturaCompleta) {
        // Sale silenciosamente si no hay canal de email disponible
        if (!canSendEmail()) { log.warn("Email no configurado — notificación de pago no enviada"); return; }
        // Personaliza el asunto según si el pago cubrió la totalidad de la factura o fue un abono parcial
        String titulo = facturaCompleta ? "DentVision — Pago completado ✅" : "DentVision — Pago registrado 💰";
        // Personaliza el encabezado del email según el tipo de pago
        String heading = facturaCompleta ? "¡Factura pagada completamente!" : "Pago registrado";
        // Construye el cuerpo del email con información diferente según si fue pago total o abono
        String cuerpo = facturaCompleta
            // Mensaje para pago total: confirma que la factura quedó saldada
            ? String.format("Hola %s, tu factura <strong>#%06d</strong> ha sido <strong>pagada en su totalidad</strong>.<br><br>"
                + "<b>💰 Valor pagado:</b> %s COP<br>"    // Monto total pagado en pesos colombianos
                + "<b>💳 Método:</b> %s<br><br>"           // Método de pago usado (efectivo, tarjeta, transferencia, etc.)
                + "Puedes descargar tu factura desde el panel de pagos.",
                nombrePaciente, facturaId, valor, metodoPago)
            // Mensaje para abono parcial: informa que aún queda saldo pendiente
            : String.format("Hola %s, hemos registrado un abono en tu factura <strong>#%06d</strong>.<br><br>"
                + "<b>💰 Valor abonado:</b> %s COP<br>"   // Monto del abono registrado
                + "<b>💳 Método:</b> %s<br><br>"           // Método de pago del abono
                + "El saldo restante aún está pendiente.", // Aviso de que la factura no está completamente pagada
                nombrePaciente, facturaId, valor, metodoPago);
        // Envía el email con el cuerpo construido dinámicamente según el tipo de pago
        send(toEmail, titulo,
            buildHtml(titulo, heading, cuerpo,
                frontendUrl + "/paciente/facturas", "Ver mis facturas", // Botón al módulo de facturas del paciente
                "Si tienes dudas sobre este pago, contáctanos respondiendo este correo."));
        log.info("Notificación de pago enviada a {}", toEmail);
    }

    // Notifica al usuario que su contraseña fue cambiada exitosamente (o permite recuperar acceso si no fue él)
    public void sendPasswordChangedEmail(String toEmail) {
        if (!canSendEmail()) {
            // No lanza excepción; el cambio de contraseña ya ocurrió, solo falla la notificación
            log.warn("Email no configurado - notificación de cambio de contraseña no enviada a {}", toEmail);
            return;
        }
        send(toEmail,
            "DentVision - Tu contraseña fue cambiada", // Asunto de alerta de seguridad
            buildHtml(
                "Tu contraseña fue cambiada – DentVision", // Preheader de vista previa
                "Contraseña actualizada",                   // Encabezado del email
                // Cuerpo informativo: si fue el usuario, no necesita hacer nada; si no fue él, debe actuar
                "Tu contraseña de DentVision fue cambiada exitosamente. Si fuiste tú, no necesitas hacer nada más.",
                frontendUrl + "/forgot-password", "No fui yo — Recuperar acceso", // Botón de recuperación de emergencia
                "Si no realizaste este cambio, restablece tu contraseña de inmediato usando el botón de arriba."
            ));
        log.info("Notificación de cambio de contraseña enviada a {}", toEmail);
    }

    // ── Despacho: Brevo API → Resend → Gmail API → SMTP ─────────────────────────────────
    // Método central de envío: implementa la lógica de fallback entre los cuatro canales disponibles
    // El orden de prioridad es: Brevo (más confiable en Render) → Resend → Gmail API → SMTP (último recurso)
    private void send(String to, String subject, String html) {
        // Intenta primero con Brevo (API HTTP, no usa puertos SMTP que pueden estar bloqueados en servidores cloud)
        if (isBrevoConfigured()) {
            try { sendViaBrevo(to, subject, html); return; } // Si tiene éxito, retorna inmediatamente sin probar otros
            catch (Exception e) { log.warn("[EmailService] Brevo falló: {}", e.getMessage()); } // Si falla, prueba el siguiente
        }
        // Segundo intento: Resend API (similar a Brevo, usa HTTP en lugar de SMTP)
        if (isResendConfigured()) {
            try { sendViaResend(to, subject, html); return; } // Si tiene éxito, retorna
            catch (Exception e) { log.warn("[EmailService] Resend falló: {}", e.getMessage()); }
        }
        // Tercer intento: Gmail API usando OAuth2 (alternativa cuando el SMTP de Gmail está bloqueado)
        if (isGmailApiConfigured()) {
            try { sendViaGmailApi(to, subject, html); return; } // Si tiene éxito, retorna
            catch (Exception e) { log.warn("[EmailService] Gmail API falló: {}", e.getMessage()); }
        }
        // Último recurso: envío SMTP tradicional vía JavaMailSender de Spring
        if (isSmtpConfigured()) {
            try { sendViaSmtp(to, subject, html); return; } // Si tiene éxito, retorna
            catch (Exception e) { log.warn("[EmailService] SMTP falló: {}", e.getMessage()); }
        }
        // Si todos los canales fallan, lanza excepción para que el llamador pueda manejarlo
        throw new IllegalStateException("Ningún canal de email disponible");
    }

    // Envía el email usando la API HTTP de Brevo (https://api.brevo.com/v3/smtp/email)
    // Brevo es preferido en entornos cloud como Render porque usa puerto 443 (HTTPS), nunca bloqueado
    private void sendViaBrevo(String to, String subject, String html) {
        try {
            // Construye el cuerpo JSON de la petición con el formato requerido por la API de Brevo
            String body = String.format(
                "{\"sender\":{\"name\":\"%s\",\"email\":\"%s\"}," + // Objeto con nombre y email del remitente
                "\"to\":[{\"email\":\"%s\"}]," +                    // Array de destinatarios (solo uno en este caso)
                "\"subject\":\"%s\"," +                             // Asunto del email
                "\"htmlContent\":%s}",                              // Contenido HTML del email (escapado como JSON string)
                mailFromName, mailFrom, to, subject, toJsonString(html) // Escapa el HTML para incluirlo en JSON sin romper la estructura
            );
            // Construye la petición HTTP POST a la API de Brevo
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email")) // Endpoint oficial de envío de emails de Brevo
                .timeout(Duration.ofSeconds(15))                         // Tiempo máximo de espera: 15 segundos
                .header("api-key", brevoApiKey)                          // Cabecera de autenticación con la API key de Brevo
                .header("Content-Type", "application/json")              // Indica que el cuerpo es JSON
                .POST(HttpRequest.BodyPublishers.ofString(body))         // Método POST con el JSON construido arriba
                .build();
            // Ejecuta la petición HTTP de forma síncrona y espera la respuesta
            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString()); // Convierte el cuerpo de respuesta a String
            // Verifica si la API devolvió un error (código 400 o superior)
            if (response.statusCode() >= 400) {
                // Lanza excepción con el código y mensaje de error de Brevo para que el fallback lo capture
                throw new RuntimeException("Brevo API error " + response.statusCode() + ": " + response.body());
            }
            // Registra el envío exitoso en modo debug (solo visible si el nivel de log es DEBUG)
            log.debug("Brevo API OK → {} ({})", to, response.statusCode());
        } catch (Exception e) {
            // Registra el error y relanza como RuntimeException para que el método 'send' intente el siguiente canal
            log.error("Error enviando email vía Brevo API a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }

    // Envía el email usando la API HTTP de Resend (https://api.resend.com/emails)
    // Resend es el segundo canal de fallback; también usa HTTPS para evitar bloqueos de puertos SMTP
    private void sendViaResend(String to, String subject, String html) {
        try {
            // Construye el JSON con el formato requerido por la API de Resend
            String body = String.format(
                "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":%s}",
                resendFrom, to, subject, toJsonString(html) // Usa el email remitente de Resend y escapa el HTML
            );
            // Construye la petición HTTP POST a la API de Resend
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails")) // Endpoint oficial de envío de Resend
                .timeout(Duration.ofSeconds(15))                   // Tiempo máximo de espera: 15 segundos
                .header("Authorization", "Bearer " + resendApiKey) // Autenticación con el patrón Bearer Token de Resend
                .header("Content-Type", "application/json")        // Indica que el cuerpo es JSON
                .POST(HttpRequest.BodyPublishers.ofString(body))   // Método POST con el JSON del email
                .build();

            // Ejecuta la petición HTTP de forma síncrona
            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

            // Verifica si Resend respondió con un error
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Resend API error " + response.statusCode() + ": " + response.body());
            }
            // Registra el envío exitoso en modo debug
            log.debug("Resend OK → {} ({})", to, response.statusCode());
        } catch (Exception e) {
            // Registra el error y relanza para que el método 'send' pruebe el siguiente canal
            log.error("Error enviando email vía Resend a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }

    // Envía el email usando Gmail API con autenticación OAuth2
    // Este método hace DOS peticiones HTTP: primero obtiene un access token, luego envía el email
    // Se usa como tercer canal de fallback cuando Brevo y Resend no están disponibles
    private void sendViaGmailApi(String to, String subject, String html) {
        try {
            // PASO 1: Obtener un access token temporal usando el refresh token de OAuth2
            // El refresh token es permanente; el access token dura ~1 hora y debe renovarse
            String tokenBody = "client_id=" + gmailClientId           // ID de la aplicación OAuth2 en Google Cloud
                + "&client_secret=" + gmailClientSecret               // Secreto de la aplicación OAuth2
                + "&refresh_token=" + gmailRefreshToken               // Token de renovación obtenido en la configuración inicial
                + "&grant_type=refresh_token";                        // Tipo de concesión OAuth2 para renovar access tokens

            // Construye la petición HTTP POST al endpoint de tokens de Google
            HttpRequest tokenReq = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token")) // Endpoint de OAuth2 de Google para obtener tokens
                .timeout(Duration.ofSeconds(10))                        // Tiempo máximo para obtener el token: 10 segundos
                .header("Content-Type", "application/x-www-form-urlencoded") // Formato de los datos del formulario
                .POST(HttpRequest.BodyPublishers.ofString(tokenBody))   // Envía las credenciales OAuth2
                .build();

            // Ejecuta la petición para obtener el access token
            HttpResponse<String> tokenResp = HttpClient.newHttpClient()
                .send(tokenReq, HttpResponse.BodyHandlers.ofString());

            // Registra la respuesta completa para diagnóstico (útil para depurar problemas de OAuth2)
            log.info("[GmailAPI] Token response status={} body={}", tokenResp.statusCode(), tokenResp.body());

            // Verifica si Google rechazó la solicitud de token (credenciales inválidas, token revocado, etc.)
            if (tokenResp.statusCode() >= 400) {
                throw new RuntimeException("Error obteniendo access token: " + tokenResp.body());
            }

            // Extrae el campo "access_token" del JSON de respuesta de Google
            String accessToken = extractJson(tokenResp.body(), "access_token");
            // Si no se encontró el access_token en la respuesta, algo salió mal
            if (accessToken == null) throw new RuntimeException("access_token no encontrado. Respuesta: " + tokenResp.body());

            // PASO 2: Construir el email en formato RFC 2822 (estándar de emails de internet)
            // Gmail API requiere el email completo en formato RFC 2822 codificado en Base64
            String rawEmail = "From: " + mailFromName + " <" + mailFrom + ">\r\n" // Cabecera From con nombre y email
                + "To: " + to + "\r\n"                                             // Cabecera con el destinatario
                // Asunto codificado en Base64 UTF-8 usando el estándar MIME para soportar tildes y caracteres especiales
                + "Subject: =?UTF-8?B?" + java.util.Base64.getEncoder().encodeToString(subject.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "?=\r\n"
                + "MIME-Version: 1.0\r\n"                        // Versión MIME requerida para emails con HTML
                + "Content-Type: text/html; charset=UTF-8\r\n"  // Indica que el cuerpo es HTML en UTF-8
                + "Content-Transfer-Encoding: base64\r\n"        // El cuerpo está codificado en Base64
                + "\r\n"                                         // Línea en blanco que separa cabeceras del cuerpo
                // Cuerpo HTML del email codificado en Base64 con MIME encoder (líneas de 76 caracteres)
                + java.util.Base64.getMimeEncoder().encodeToString(html.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Codifica el email completo en Base64 URL-safe (requerido por Gmail API para el campo "raw")
            String encodedEmail = java.util.Base64.getUrlEncoder().encodeToString(rawEmail.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // PASO 3: Enviar el email codificado via Gmail API
            // El campo "raw" contiene el email completo en formato RFC 2822 codificado en Base64 URL-safe
            String gmailBody = "{\"raw\":\"" + encodedEmail + "\"}";

            // Construye la petición HTTP POST a la Gmail API para enviar el mensaje
            HttpRequest sendReq = HttpRequest.newBuilder()
                .uri(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")) // Endpoint de envío de Gmail API
                .timeout(Duration.ofSeconds(15))                    // Tiempo máximo para enviar el email: 15 segundos
                .header("Authorization", "Bearer " + accessToken)   // Autenticación con el access token obtenido en el paso 1
                .header("Content-Type", "application/json")         // El cuerpo de la petición es JSON
                .POST(HttpRequest.BodyPublishers.ofString(gmailBody)) // JSON con el email codificado en Base64
                .build();

            // Ejecuta la petición de envío
            HttpResponse<String> sendResp = HttpClient.newHttpClient()
                .send(sendReq, HttpResponse.BodyHandlers.ofString());

            // Verifica si Gmail API reportó un error al enviar (cuota excedida, email inválido, etc.)
            if (sendResp.statusCode() >= 400) {
                throw new RuntimeException("Gmail API error " + sendResp.statusCode() + ": " + sendResp.body());
            }
            // Registra el envío exitoso en modo debug
            log.debug("Gmail API OK → {} ({})", to, sendResp.statusCode());
        } catch (Exception e) {
            // Registra el error y relanza para que el método 'send' pruebe el canal SMTP como último recurso
            log.error("Error enviando email vía Gmail API a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }

    // Extrae el valor de un campo de un JSON simple sin usar librerías externas (Jackson, Gson, etc.)
    // Busca el patrón "key":"value" o "key": "value" y retorna el valor como String
    private String extractJson(String json, String key) {
        // Acepta "key":"value" y "key": "value" (JSON con o sin espacio después de los dos puntos)
        for (String search : new String[]{"\"" + key + "\":\"", "\"" + key + "\": \""}) {
            // Busca la posición donde aparece la clave en el JSON
            int start = json.indexOf(search);
            if (start >= 0) {
                // Mueve el índice al inicio del valor (después de la comilla de apertura)
                start += search.length();
                // Busca la comilla de cierre del valor
                int end = json.indexOf("\"", start);
                // Si encontró tanto inicio como fin, extrae y retorna el valor
                if (end >= 0) return json.substring(start, end);
            }
        }
        // Si no encontró el campo, retorna null para que el llamador pueda manejarlo
        return null;
    }

    // Envía el email usando SMTP tradicional vía JavaMailSender de Spring (último recurso)
    // Requiere configuración completa en application.properties (spring.mail.host, port, username, password)
    private void sendViaSmtp(String to, String subject, String html) {
        try {
            // Crea un nuevo mensaje MIME vacío usando el cliente SMTP configurado en Spring
            var msg = mailSender.createMimeMessage();
            // MimeMessageHelper facilita configurar el mensaje con soporte para HTML y codificación UTF-8
            // El segundo parámetro 'false' indica que no hay archivos adjuntos (email simple)
            var helper = new MimeMessageHelper(msg, false, "UTF-8");
            // Configura el remitente con nombre visible y dirección de email
            helper.setFrom(mailFrom, mailFromName);
            // Configura el destinatario del email
            helper.setTo(to);
            // Configura el asunto del email
            helper.setSubject(subject);
            // Configura el cuerpo del email; 'true' indica que el contenido es HTML (no texto plano)
            helper.setText(html, true);
            // Envía el mensaje usando el servidor SMTP configurado (puede tardar varios segundos)
            mailSender.send(msg);
            // Registra el envío exitoso por SMTP en modo debug
            log.debug("SMTP OK → {}", to);
        } catch (Exception e) {
            // Registra el error SMTP y relanza para que el método 'send' lo notifique como fallo definitivo
            log.error("Error enviando email vía SMTP a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }

    // ── Plantilla HTML ────────────────────────────────────────────────────────
    // Genera el HTML completo del email con diseño profesional y responsivo
    // Todos los emails de DentVision usan esta misma plantilla base para consistencia visual
    private String buildHtml(String preheader, String heading, String body,
                              String ctaUrl, String ctaText, String footer) {
        // Obtiene el año actual para el pie de página de copyright (se actualiza automáticamente cada año)
        int year = java.time.Year.now().getValue();
        // Construye el HTML del email como una sola cadena concatenada (compatible con todos los clientes de correo)
        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'></head>" // Responsivo en móviles
            + "<body style='margin:0;padding:0;background:#f1f5f9;font-family:Segoe UI,Arial,sans-serif;'>"
            // Texto de preheader: oculto visualmente pero visible en la vista previa del cliente de correo (Gmail, Outlook)
            + "<span style='display:none;font-size:0;max-height:0;overflow:hidden;'>" + preheader + "</span>"
            // Tabla exterior: fondo gris claro, padding vertical de 40px para centrar el contenido
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f1f5f9;padding:40px 0;'>"
            + "<tr><td align='center'>"
            // Tabla interior: ancho fijo de 580px, fondo blanco, bordes redondeados y sombra sutil
            + "<table width='580' cellpadding='0' cellspacing='0' "
            + "style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 16px rgba(0,0,0,.08);'>"
            // Cabecera del email: gradiente azul corporativo de DentVision con logo y nombre de la clínica
            + "<tr><td style='background:linear-gradient(135deg,#003560,#004d84,#0078d4);padding:32px 40px;text-align:center;'>"
            + "<div style='font-size:28px;margin-bottom:8px;'>&#129463;</div>" // Emoji de diente como logo
            + "<div style='color:#fff;font-size:22px;font-weight:800;'>DentVision</div>" // Nombre de la clínica en blanco
            + "<div style='color:rgba(255,255,255,0.7);font-size:13px;margin-top:4px;'>Cl&iacute;nica Odontol&oacute;gica</div>" // Subtítulo en color semitransparente
            + "</td></tr>"
            // Cuerpo principal del email: padding generoso para legibilidad
            + "<tr><td style='padding:40px 40px 32px;'>"
            // Encabezado principal del email (ej: "¡Bienvenido a DentVision!", "Cita confirmada")
            + "<h1 style='margin:0 0 16px;font-size:22px;font-weight:700;color:#0f172a;'>" + heading + "</h1>"
            // Párrafo con el contenido principal del email (puede contener HTML como <strong>, <br>, etc.)
            + "<p style='margin:0 0 28px;font-size:15px;color:#475569;line-height:1.65;'>" + body + "</p>"
            // Botón de llamada a la acción (CTA): centrado, azul corporativo, texto blanco en negrita
            + "<div style='text-align:center;margin-bottom:28px;'>"
            + "<a href='" + ctaUrl + "' style='display:inline-block;background:#004d84;color:#fff;"
            + "text-decoration:none;font-size:15px;font-weight:700;padding:14px 32px;border-radius:8px;'>"
            + ctaText + "</a></div>" // Texto del botón (ej: "Verificar mi cuenta", "Ver mis citas")
            // Nota al pie dentro del cuerpo: separada por línea horizontal, texto pequeño y gris
            + "<p style='margin:0;font-size:13px;color:#94a3b8;line-height:1.6;"
            + "border-top:1px solid #e2e8f0;padding-top:20px;'>" + footer + "</p>"
            + "</td></tr>"
            // Pie de página del email: fondo gris muy claro, copyright y enlace al sitio
            + "<tr><td style='background:#f8fafc;padding:20px 40px;text-align:center;border-top:1px solid #e2e8f0;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>&copy; " + year // Año dinámico del copyright
            + " DentVision &middot; Todos los derechos reservados<br>"
            + "<a href='" + frontendUrl + "' style='color:#004d84;text-decoration:none;'>Ir a DentVision</a></p>" // Enlace al frontend
            + "</td></tr></table></td></tr></table></body></html>";
    }

    /** Convierte un string a un literal JSON string escapado (sin librerías externas).
     *  Reemplaza caracteres especiales que romperían la estructura JSON:
     *  \\ → \\\\ (barra invertida), \" → \\\" (comilla), \n → \\n (nueva línea), etc.
     */
    private String toJsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")  // Escapa barras invertidas primero (debe ser la primera sustitución)
                       .replace("\"", "\\\"")  // Escapa comillas dobles para no romper el JSON
                       .replace("\n", "\\n")   // Convierte saltos de línea a secuencia de escape JSON
                       .replace("\r", "\\r")   // Convierte retornos de carro a secuencia de escape JSON
                       .replace("\t", "\\t") + "\""; // Convierte tabulaciones a secuencia de escape JSON
    }
}
