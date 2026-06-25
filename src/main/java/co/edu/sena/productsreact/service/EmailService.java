package co.edu.sena.productsreact.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // Brevo HTTP API (prioridad máxima — funciona en Render sin bloqueo de puertos)
    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    @Value("${app.mail.from:creyese23@gmail.com}")
    private String mailFrom;

    @Value("${app.mail.from-name:DentVision}")
    private String mailFromName;

    // Resend (segunda opción)
    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${RESEND_FROM:onboarding@resend.dev}")
    private String resendFrom;

    // SMTP — último recurso
    @Value("${spring.mail.username:}")
    private String smtpUsername;

    // ── estado ──────────────────────────────────────────────────────────────

    public boolean isBrevoConfigured() {
        return brevoApiKey != null && !brevoApiKey.isBlank();
    }

    public boolean isResendConfigured() {
        return resendApiKey != null && !resendApiKey.isBlank();
    }

    public boolean isSmtpConfigured() {
        return mailSender != null;
    }

    public boolean canSendEmail() {
        return isBrevoConfigured() || isResendConfigured() || isSmtpConfigured();
    }

    @PostConstruct
    public void logSmtpStatus() {
        log.info("[EmailService] brevo={} | resend={} | mailSender={} | mailFrom='{}' | canSend={}",
            isBrevoConfigured(), isResendConfigured(), mailSender != null ? "OK" : "NULL", mailFrom, canSendEmail());
    }

    // ── URLs ─────────────────────────────────────────────────────────────────

    public String buildResetUrl(String token) {
        return frontendUrl + "/reset-password?token=" + token;
    }

    public String buildVerificationUrl(String token) {
        return frontendUrl + "/verify-email?token=" + token;
    }

    // ── Emails públicos ───────────────────────────────────────────────────────

    public void sendVerificationEmail(String toEmail, String token) {
        if (!canSendEmail()) throw new IllegalStateException("Email no configurado");
        String link = buildVerificationUrl(token);
        send(toEmail,
            "DentVision - Confirma tu cuenta",
            buildHtml(
                "Confirma tu cuenta en DentVision",
                "¡Bienvenido a DentVision!",
                "Tu cuenta ha sido creada exitosamente. Haz clic en el botón para confirmarla:",
                link, "Verificar mi cuenta",
                "Este enlace expirará en 24 horas. Si no creaste esta cuenta, ignora este mensaje."
            ));
        log.info("Email de verificación enviado a {}", toEmail);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        if (!canSendEmail()) throw new IllegalStateException("Email no configurado");
        String link = buildResetUrl(token);
        send(toEmail,
            "DentVision - Recuperación de contraseña",
            buildHtml(
                "Recuperación de contraseña – DentVision",
                "Restablece tu contraseña",
                "Recibimos una solicitud para restablecer la contraseña de tu cuenta DentVision. Haz clic en el botón:",
                link, "Restablecer contraseña",
                "Este enlace expirará en 1 hora. Si no solicitaste este cambio, puedes ignorar este correo."
            ));
        log.info("Email de recuperación enviado a {}", toEmail);
    }

    // ── Notificaciones de citas ───────────────────────────────────────────────

    public void sendCitaConfirmada(String toEmail, String nombrePaciente,
                                   String motivo, String fechaHora, String odontologo) {
        if (!canSendEmail()) { log.warn("Email no configurado — notificación de cita no enviada"); return; }
        send(toEmail,
            "DentVision — Tu cita fue confirmada ✅",
            buildHtml(
                "Tu cita fue confirmada en DentVision",
                "¡Cita confirmada!",
                String.format("Hola %s, tu cita en DentVision ha sido <strong>confirmada</strong>.<br><br>"
                    + "<b>📋 Motivo:</b> %s<br>"
                    + "<b>📅 Fecha y hora:</b> %s<br>"
                    + "<b>👨‍⚕️ Odontólogo:</b> %s<br><br>"
                    + "Por favor llega 10 minutos antes de tu cita.",
                    nombrePaciente, motivo, fechaHora, odontologo),
                frontendUrl + "/paciente/citas", "Ver mis citas",
                "Si necesitas reagendar o cancelar, hazlo desde tu panel de paciente con al menos 24 horas de anticipación."
            ));
        log.info("Confirmación de cita enviada a {}", toEmail);
    }

    public void sendCitaCancelada(String toEmail, String nombrePaciente,
                                   String motivo, String fechaHora) {
        if (!canSendEmail()) { log.warn("Email no configurado — notificación de cancelación no enviada"); return; }
        send(toEmail,
            "DentVision — Tu cita fue cancelada",
            buildHtml(
                "Cancelación de cita en DentVision",
                "Cita cancelada",
                String.format("Hola %s, te informamos que tu cita en DentVision ha sido <strong>cancelada</strong>.<br><br>"
                    + "<b>📋 Motivo:</b> %s<br>"
                    + "<b>📅 Fecha y hora:</b> %s<br><br>"
                    + "Puedes agendar una nueva cita cuando lo necesites.",
                    nombrePaciente, motivo, fechaHora),
                frontendUrl + "/paciente/citas", "Agendar nueva cita",
                "Si crees que esto es un error, contáctanos respondiendo este correo."
            ));
        log.info("Notificación de cancelación enviada a {}", toEmail);
    }

    public void sendCitaRecordatorio(String toEmail, String nombrePaciente,
                                      String motivo, String fechaHora, String odontologo) {
        if (!canSendEmail()) { log.warn("Email no configurado — recordatorio no enviado"); return; }
        send(toEmail,
            "DentVision — Recordatorio de cita 📅",
            buildHtml(
                "Tienes una cita mañana en DentVision",
                "Recordatorio de cita",
                String.format("Hola %s, te recordamos que <strong>mañana tienes una cita</strong> en DentVision.<br><br>"
                    + "<b>📋 Motivo:</b> %s<br>"
                    + "<b>📅 Fecha y hora:</b> %s<br>"
                    + "<b>👨‍⚕️ Odontólogo:</b> %s<br><br>"
                    + "Recuerda traer tu documento de identidad.",
                    nombrePaciente, motivo, fechaHora, odontologo),
                frontendUrl + "/paciente/citas", "Ver detalles",
                "Para reagendar o cancelar hazlo con al menos 24 horas de anticipación."
            ));
        log.info("Recordatorio de cita enviado a {}", toEmail);
    }

    // ── Notificaciones de factura ─────────────────────────────────────────────

    public void sendFacturaGenerada(String toEmail, String nombrePaciente,
                                     Long facturaId, String total) {
        if (!canSendEmail()) { log.warn("Email no configurado — notificación de factura no enviada"); return; }
        send(toEmail,
            "DentVision — Nueva factura disponible 🧾",
            buildHtml(
                "Tienes una nueva factura en DentVision",
                "Factura generada",
                String.format("Hola %s, se ha generado una nueva factura por tus servicios en DentVision.<br><br>"
                    + "<b>📄 Factura #:</b> %s<br>"
                    + "<b>💰 Total:</b> %s COP<br><br>"
                    + "Puedes ver el detalle y realizar el pago desde tu panel.",
                    nombrePaciente, String.valueOf(facturaId).formatted("%06d", facturaId), total),
                frontendUrl + "/paciente/facturas", "Ver y pagar factura",
                "Si tienes preguntas sobre esta factura, contáctanos."
            ));
        log.info("Notificación de factura enviada a {}", toEmail);
    }

    public void sendPasswordChangedEmail(String toEmail) {
        if (!canSendEmail()) {
            log.warn("Email no configurado - notificación de cambio de contraseña no enviada a {}", toEmail);
            return;
        }
        send(toEmail,
            "DentVision - Tu contraseña fue cambiada",
            buildHtml(
                "Tu contraseña fue cambiada – DentVision",
                "Contraseña actualizada",
                "Tu contraseña de DentVision fue cambiada exitosamente. Si fuiste tú, no necesitas hacer nada más.",
                frontendUrl + "/forgot-password", "No fui yo — Recuperar acceso",
                "Si no realizaste este cambio, restablece tu contraseña de inmediato usando el botón de arriba."
            ));
        log.info("Notificación de cambio de contraseña enviada a {}", toEmail);
    }

    // ── Despacho: Brevo API → Resend → SMTP ─────────────────────────────────

    private void send(String to, String subject, String html) {
        if (isBrevoConfigured()) {
            try {
                sendViaBrevo(to, subject, html);
                return;
            } catch (Exception e) {
                log.warn("[EmailService] Brevo falló ({}), intentando siguiente canal...", e.getMessage());
            }
        }
        if (isResendConfigured()) {
            try {
                sendViaResend(to, subject, html);
                return;
            } catch (Exception e) {
                log.warn("[EmailService] Resend falló ({}), intentando SMTP...", e.getMessage());
            }
        }
        if (isSmtpConfigured()) {
            sendViaSmtp(to, subject, html);
            return;
        }
        throw new IllegalStateException("Ningún canal de email disponible (Brevo/Resend/SMTP fallaron o no están configurados)");
    }

    private void sendViaBrevo(String to, String subject, String html) {
        try {
            String body = String.format(
                "{\"sender\":{\"name\":\"%s\",\"email\":\"%s\"}," +
                "\"to\":[{\"email\":\"%s\"}]," +
                "\"subject\":\"%s\"," +
                "\"htmlContent\":%s}",
                mailFromName, mailFrom, to, subject, toJsonString(html)
            );
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(Duration.ofSeconds(15))
                .header("api-key", brevoApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Brevo API error " + response.statusCode() + ": " + response.body());
            }
            log.debug("Brevo API OK → {} ({})", to, response.statusCode());
        } catch (Exception e) {
            log.error("Error enviando email vía Brevo API a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }

    private void sendViaResend(String to, String subject, String html) {
        try {
            String body = String.format(
                "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":%s}",
                resendFrom, to, subject, toJsonString(html)
            );
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Resend API error " + response.statusCode() + ": " + response.body());
            }
            log.debug("Resend OK → {} ({})", to, response.statusCode());
        } catch (Exception e) {
            log.error("Error enviando email vía Resend a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }

    private void sendViaSmtp(String to, String subject, String html) {
        try {
            var msg = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(mailFrom, mailFromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.debug("SMTP OK → {}", to);
        } catch (Exception e) {
            log.error("Error enviando email vía SMTP a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }

    // ── Plantilla HTML ────────────────────────────────────────────────────────

    private String buildHtml(String preheader, String heading, String body,
                              String ctaUrl, String ctaText, String footer) {
        int year = java.time.Year.now().getValue();
        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'></head>"
            + "<body style='margin:0;padding:0;background:#f1f5f9;font-family:Segoe UI,Arial,sans-serif;'>"
            + "<span style='display:none;font-size:0;max-height:0;overflow:hidden;'>" + preheader + "</span>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f1f5f9;padding:40px 0;'>"
            + "<tr><td align='center'>"
            + "<table width='580' cellpadding='0' cellspacing='0' "
            + "style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 16px rgba(0,0,0,.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#003560,#004d84,#0078d4);padding:32px 40px;text-align:center;'>"
            + "<div style='font-size:28px;margin-bottom:8px;'>&#129463;</div>"
            + "<div style='color:#fff;font-size:22px;font-weight:800;'>DentVision</div>"
            + "<div style='color:rgba(255,255,255,0.7);font-size:13px;margin-top:4px;'>Cl&iacute;nica Odontol&oacute;gica</div>"
            + "</td></tr>"
            + "<tr><td style='padding:40px 40px 32px;'>"
            + "<h1 style='margin:0 0 16px;font-size:22px;font-weight:700;color:#0f172a;'>" + heading + "</h1>"
            + "<p style='margin:0 0 28px;font-size:15px;color:#475569;line-height:1.65;'>" + body + "</p>"
            + "<div style='text-align:center;margin-bottom:28px;'>"
            + "<a href='" + ctaUrl + "' style='display:inline-block;background:#004d84;color:#fff;"
            + "text-decoration:none;font-size:15px;font-weight:700;padding:14px 32px;border-radius:8px;'>"
            + ctaText + "</a></div>"
            + "<p style='margin:0;font-size:13px;color:#94a3b8;line-height:1.6;"
            + "border-top:1px solid #e2e8f0;padding-top:20px;'>" + footer + "</p>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;padding:20px 40px;text-align:center;border-top:1px solid #e2e8f0;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>&copy; " + year
            + " DentVision &middot; Todos los derechos reservados<br>"
            + "<a href='" + frontendUrl + "' style='color:#004d84;text-decoration:none;'>Ir a DentVision</a></p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    /** Convierte un string a un literal JSON string escapado (sin librerías externas). */
    private String toJsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }
}
