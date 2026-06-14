package co.edu.sena.productsreact.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public String buildResetUrl(String token) {
        return frontendUrl + "/reset-password?token=" + token;
    }

    public String buildVerificationUrl(String token) {
        return frontendUrl + "/verify-email?token=" + token;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("SMTP no configurado");
        }
        String link = buildVerificationUrl(token);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("DentVision - Confirma tu cuenta");
        message.setText(
            "¡Bienvenido a DentVision!\n\n" +
            "Tu cuenta ha sido creada exitosamente. Para confirmarla, haz clic en el enlace:\n\n" +
            link + "\n\n" +
            "Este enlace expirará en 24 horas.\n\n" +
            "Si no creaste esta cuenta, ignora este mensaje.\n\n" +
            "— Equipo DentVision"
        );
        mailSender.send(message);
        log.info("Email de verificación enviado a {}", toEmail);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("SMTP no configurado");
        }

        String resetLink = buildResetUrl(token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("DentVision - Recuperación de contraseña");
        message.setText(
            "Hola,\n\n" +
            "Recibimos una solicitud para restablecer la contraseña de tu cuenta DentVision.\n\n" +
            "Haz clic en el siguiente enlace para crear una nueva contraseña:\n" +
            resetLink + "\n\n" +
            "Este enlace expirará en 1 hora.\n\n" +
            "Si no solicitaste este cambio, puedes ignorar este correo.\n\n" +
            "— Equipo DentVision"
        );
        mailSender.send(message);
        log.info("Email de recuperación enviado a {}", toEmail);
    }
}
