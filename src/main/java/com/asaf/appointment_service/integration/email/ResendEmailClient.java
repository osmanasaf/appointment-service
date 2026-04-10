package com.asaf.appointment_service.integration.email;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("prod")
public class ResendEmailClient implements EmailClient {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailClient.class);
    private static final String RESEND_BASE_URL = "https://api.resend.com";

    private final RestClient restClient;
    private final EmailProperties emailProperties;

    public ResendEmailClient(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
        this.restClient = RestClient.builder()
            .baseUrl(RESEND_BASE_URL)
            .defaultHeader("Authorization", "Bearer " + emailProperties.resend().apiKey())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Override
    public void sendVerificationEmail(String toEmail, String toName, String verificationToken) {
        String verificationUrl = emailProperties.baseUrl() + "/auth/verify-email?token=" + verificationToken;
        String html = buildVerificationHtml(toName, verificationUrl);
        send(toEmail, "Email Adresinizi Doğrulayın", html);
    }

    private void send(String to, String subject, String html) {
        var body = new ResendRequest(emailProperties.from(), List.of(to), subject, html);
        try {
            restClient.post()
                .uri("/emails")
                .body(body)
                .retrieve()
                .toBodilessEntity();
            log.info("Resend email gönderildi: to={}", to);
        } catch (Exception ex) {
            log.error("Resend email gönderilemedi: to={} error={}", to, ex.getMessage());
            throw new RuntimeException("Email gönderilemedi", ex);
        }
    }

    private String buildVerificationHtml(String name, String verificationUrl) {
        return """
            <!DOCTYPE html>
            <html lang="tr">
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:40px 0">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0"
                         style="background:#fff;border-radius:8px;padding:40px;box-shadow:0 2px 8px rgba(0,0,0,.08)">
                    <tr><td style="text-align:center;padding-bottom:32px">
                      <h2 style="color:#1a1a1a;margin:0">Randevu Sistemi</h2>
                    </td></tr>
                    <tr><td>
                      <p style="font-size:16px;color:#333">Merhaba <strong>%s</strong>,</p>
                      <p style="font-size:15px;color:#555;line-height:1.6">
                        Hesabınızı aktifleştirmek için aşağıdaki butona tıklayın.
                        Bu link <strong>24 saat</strong> geçerlidir.
                      </p>
                      <div style="text-align:center;margin:32px 0">
                        <a href="%s"
                           style="background:#6366f1;color:#fff;text-decoration:none;
                                  padding:14px 32px;border-radius:6px;font-size:16px;font-weight:600">
                          Email Adresimi Doğrula
                        </a>
                      </div>
                      <p style="font-size:13px;color:#999;word-break:break-all">
                        Buton çalışmıyorsa bu linki kopyalayıp tarayıcınıza yapıştırın:<br>%s
                      </p>
                    </td></tr>
                    <tr><td style="padding-top:32px;border-top:1px solid #eee;text-align:center">
                      <p style="font-size:12px;color:#aaa;margin:0">
                        Bu emaili siz istemediyseniz güvenle yok sayabilirsiniz.
                      </p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name, verificationUrl, verificationUrl);
    }

    private record ResendRequest(String from, List<String> to, String subject, String html) {}
}
