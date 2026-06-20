package co.edu.sena.productsreact.service;

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

    // Resend (API key — prioridad sobre SMTP)
    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${RESEND_FROM:onboarding@resend.dev}")
    private String resendFrom;

    // SMTP (Gmail u otro)
    @Value("${spring.mail.username:}")
    private String smtpUsername;

    // ── estado ──────────────────────────────────────────────────────────────

    public boolean isResendConfigured() {
        return resendApiKey != null && !resendApiKey.isBlank();
    }

    public boolean isSmtpConfigured() {
        return mailSender != null && smtpUsername != null && !smtpUsername.isBlank();
    }

    public boolean canSendEmail() {
        return isResendConfigured() || isSmtpConfigured();
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

    // ── Despacho: Resend tiene prioridad sobre SMTP ──────────────────────────

    private void send(String to, String subject, String html) {
        if (isResendConfigured()) {
            sendViaResend(to, subject, html);
        } else {
            sendViaSmtp(to, subject, html);
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
            helper.setFrom(smtpUsername, "DentVision");
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
