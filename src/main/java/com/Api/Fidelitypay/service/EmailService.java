package com.Api.Fidelitypay.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = multipart message
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "FidelityPay");
            helper.setTo(to);
            helper.setSubject("Réinitialisation de votre mot de passe FidelityPay");

            String htmlContent = "<!DOCTYPE html>"
                    + "<html lang=\"fr\">"
                    + "<head>"
                    + "<meta charset=\"UTF-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                    + "<style>"
                    + "body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f4f7fa; margin: 0; padding: 0; box-shadow: rgba(0, 0, 0, 0.24) 0px 3px 8px; }"
                    + ".email-container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05); }"
                    + ".header { background-color: #ffffff; padding: 30px; text-align: center; color: #ffffff; }"
                    + ".header h1 { margin: 0; font-size: 24px; font-weight: 600; color: #ffffff; }"
                    + ".body { padding: 40px 30px; color: #333333; line-height: 1.6; font-size: 16px; }"
                    + ".body h2 { color: #25224a; font-size: 20px; margin-top: 0; }"
                    + ".button-container { text-align: center; margin: 35px 0; }"
                    + ".button { background-color: #548580; color: #ffffff !important; text-decoration: none; padding: 14px 28px; border-radius: 6px; font-weight: 600; display: inline-block; font-size: 16px; }"
                    + ".footer { background-color: #f9fafb; padding: 20px 30px; text-align: center; color: #6b7280; font-size: 13px; border-top: 1px solid #e5e7eb; }"
                    + ".fallback-link { word-break: break-all; color: #548580; font-size: 13px; }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "<div class=\"email-container\">"
                    + "  <div class=\"header\">"
                    + "    <img src=\"cid:logoImage\" alt=\"FidelityPay Logo\" style=\"max-height: 100px;\" onerror=\"this.outerHTML='<h1>FidelityPay</h1>'\" />"
                    + "  </div>"
                    + "  <div class=\"body\">"
                    + "    <h2>Bonjour,</h2>"
                    + "    <p>Nous avons reçu une demande de réinitialisation de votre mot de passe pour votre compte <strong>FidelityPay</strong>.</p>"
                    + "    <p>Cliquez sur le bouton ci-dessous pour configurer un nouveau mot de passe :</p>"
                    + "    <div class=\"button-container\">"
                    + "      <a href=\"" + resetLink + "\" class=\"button\">Réinitialiser mon mot de passe</a>"
                    + "    </div>"
                    + "    <p>Ce lien est valide pendant <strong>24 heures</strong>.</p>"
                    + "    <p>Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail en toute sécurité. Votre compte restera protégé.</p>"
                    + "    <p style=\"margin-top: 30px;\">Cordialement,<br>L'équipe FidelityPay</p>"
                    + "  </div>"
                    + "  <div class=\"footer\">"
                    + "    <p>Si le bouton ne fonctionne pas, copiez-collez le lien suivant dans votre navigateur :</p>"
                    + "    <p class=\"fallback-link\">" + resetLink + "</p>"
                    + "    <p style=\"margin-top: 20px;\">&copy; 2026 FidelityPay. Tous droits réservés.</p>"
                    + "  </div>"
                    + "</div>"
                    + "</body>"
                    + "</html>";

            helper.setText(htmlContent, true); // true indicates html
            
            // Intégrer l'image directement dans l'e-mail
            ClassPathResource logoFile = new ClassPathResource("static/logo_fidelity.png");
            helper.addInline("logoImage", logoFile);

            mailSender.send(message);
            log.info("📧 Email de réinitialisation (HTML) envoyé avec succès à {}", to);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de réinitialisation (HTML) à {}: {}", to, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'e-mail de réinitialisation", e);
        }
    }
}
