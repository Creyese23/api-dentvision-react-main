package co.edu.sena.productsreact.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public boolean isSmtpConfigured() {
        return mailSender != null && fromEmail != null && !fromEmail.isBlank();
    }

    public String buildResetUrl(String token) {
        return frontendUrl + "/reset-password?token=" + token;
    }

    public String buildVerificationUrl(String token) {
        return frontendUrl + "/verify-email?token=" + token;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        if (!isSmtpConfigured()) throw new IllegalStateException("SMTP no configurado");
        String link = buildVerificationUrl(token);
        String html = buildHtml(
            "Confirma tu cuenta en DentVision",
            "¡Bienvenido a DentVision!",
            "Tu cuenta ha sido creada exitosamente. Haz clic en el botón para confirmarla:",
            link, "Verificar mi cuenta",
            "Este enlace expirará en 24 horas. Si no creaste esta cuenta, ignora este mensaje."
        );
        send(toEmail, "DentVision - Confirma tu cuenta", html);
        log.info("Email de verificación enviado a {}", toEmail);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        if (!isSmtpConfigured()) throw new IllegalStateException("SMTP no configurado");
        String link = buildResetUrl(token);
        String html = buildHtml(
            "Recuperación de contraseña – DentVision",
            "Restablece tu contraseña",
            "Recibimos una solicitud para restablecer la contraseña de tu cuenta DentVision. Haz clic en el botón:",
            link, "Restablecer contraseña",
            "Este enlace expirará en 1 hora. Si no solicitaste este cambio, puedes ignorar este correo."
        );
        send(toEmail, "DentVision - Recuperación de contraseña", html);
        log.info("Email de recuperación enviado a {}", toEmail);
    }

    public void sendPasswordChangedEmail(String toEmail) {
        if (!isSmtpConfigured()) {
            log.warn("SMTP no configurado - notificación de cambio de contraseña no enviada a {}", toEmail);
            return;
        }
        String html = buildHtml(
            "Tu contraseña fue cambiada – DentVision",
            "Contraseña actualizada",
            "Tu contraseña de DentVision fue cambiada exitosamente. Si fuiste tú, no necesitas hacer nada.",
            frontendUrl + "/forgot-password", "No fui yo — Recuperar acceso",
            "Si no realizaste este cambio, restablece tu contraseña de inmediato usando el botón de arriba."
        );
        send(toEmail, "DentVision - Tu contraseña fue cambiada", html);
        log.info("Notificación de cambio de contraseña enviada a {}", toEmail);
    }

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
            + "<tr><td style='background:linear-gradient(135deg,#003560,#004d84,#0078d4);"
            + "padding:32px 40px;text-align:center;'>"
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

    private void send(String to, String subject, String html) {
        try {
            var msg = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(fromEmail, "DentVision");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }
}
