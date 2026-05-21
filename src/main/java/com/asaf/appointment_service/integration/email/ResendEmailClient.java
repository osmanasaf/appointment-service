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

    @Override
    public void sendEmployeeInvite(String toEmail, String employeeName, String businessName, String inviteToken) {
        String inviteUrl = emailProperties.baseUrl() + "/auth/accept-invite?token=" + inviteToken;
        String html = buildInviteHtml(employeeName, businessName, inviteUrl);
        send(toEmail, businessName + " - Çalışan Daveti", html);
    }

    @Override
    public void sendOtpEmail(String toEmail, String toName, String otpCode, OtpEmailType type) {
        String subject = switch (type) {
            case EMAIL_VERIFICATION -> "Randevum - Email Doğrulama Kodu";
            case PASSWORD_RESET -> "Randevum - Şifre Sıfırlama Kodu";
            case EMAIL_CHANGE -> "Randevum - Yeni Email Doğrulama Kodu";
            case PHONE_CHANGE -> "Randevum - Telefon Değişikliği Kodu";
        };
        String html = buildOtpHtml(toName, otpCode, type);
        send(toEmail, subject, html);
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
        String safeName = HtmlEmailLayout.escape(name);
        String inner = """
            <p style="margin:0 0 16px;">Merhaba <strong style="color:#0f172a;">%s</strong>,</p>
            <p style="margin:0 0 4px;color:#475569;">
              Randevum hesabınızı kullanmaya başlamak için e-posta adresinizi doğrulayın.
            </p>
            <p style="margin:0 0 24px;color:#475569;">
              Bu bağlantı <strong style="color:#0f172a;">24 saat</strong> boyunca geçerlidir.
            </p>
            %s
            %s
            """.formatted(
            safeName,
            HtmlEmailLayout.primaryButton(verificationUrl, "E-postamı doğrula"),
            HtmlEmailLayout.mutedLinkFallback(verificationUrl));
        return HtmlEmailLayout.document(
            "E-postanızı doğrulayın — Randevum",
            "Hesap doğrulama",
            "E-postanızı onaylayın",
            inner,
            "Bu e-postayı siz istemediyseniz güvenle yok sayabilirsiniz.");
    }

    private String buildOtpHtml(String name, String otpCode, OtpEmailType type) {
        String headline = switch (type) {
            case EMAIL_VERIFICATION -> "Email doğrulama kodu";
            case PASSWORD_RESET -> "Şifre sıfırlama kodu";
            case EMAIL_CHANGE -> "Yeni email doğrulama kodu";
            case PHONE_CHANGE -> "Telefon değişikliği kodu";
        };
        String message = switch (type) {
            case EMAIL_VERIFICATION -> "Hesabınızı doğrulamak için aşağıdaki tek kullanımlık kodu girin.";
            case PASSWORD_RESET -> "Şifrenizi yenilemek için aşağıdaki tek kullanımlık kodu girin.";
            case EMAIL_CHANGE -> "Yeni e-posta adresinizi doğrulamak için aşağıdaki tek kullanımlık kodu girin.";
            case PHONE_CHANGE -> "Telefon numarası değişikliğini onaylamak için aşağıdaki tek kullanımlık kodu girin.";
        };
        String kicker = switch (type) {
            case EMAIL_VERIFICATION, EMAIL_CHANGE, PHONE_CHANGE -> "Güvenlik kodu";
            case PASSWORD_RESET -> "Şifre sıfırlama";
        };
        String safeName = HtmlEmailLayout.escape(name);
        String safeCode = HtmlEmailLayout.escape(otpCode);
        String inner = """
            <p style="margin:0 0 16px;">Merhaba <strong style="color:#0f172a;">%s</strong>,</p>
            <p style="margin:0 0 22px;color:#475569;">%s</p>
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:0 0 20px;">
              <tr>
                <td align="center" style="padding:24px 18px;background-color:#eef2ff;border:1px solid #c7d2fe;border-radius:12px;">
                  <span style="font-family:Consolas,'Courier New',monospace;font-size:28px;font-weight:800;letter-spacing:0.2em;color:#4338ca;">%s</span>
                </td>
              </tr>
            </table>
            <p style="margin:0;text-align:center;font-size:13px;color:#64748b;">
              Bu kod <strong style="color:#0f172a;">5 dakika</strong> içinde geçerliliğini yitirecektir.
            </p>
            """.formatted(safeName, message, safeCode);
        return HtmlEmailLayout.document(
            headline + " — Randevum",
            kicker,
            headline,
            inner,
            "Bu kodu siz talep etmediyseniz bu e-postayı yok sayabilirsiniz.");
    }

    private String buildInviteHtml(String employeeName, String businessName, String inviteUrl) {
        String en = HtmlEmailLayout.escape(employeeName);
        String bn = HtmlEmailLayout.escape(businessName);
        String inner = """
            <p style="margin:0 0 16px;">Merhaba <strong style="color:#0f172a;">%s</strong>,</p>
            <p style="margin:0 0 6px;color:#475569;">
              <strong style="color:#0f172a;">%s</strong> sizi Randevum ekibine davet ediyor.
            </p>
            <p style="margin:0 0 24px;color:#475569;">Daveti kabul etmek için aşağıdaki düğmeyi kullanın.</p>
            %s
            %s
            """.formatted(
            en,
            bn,
            HtmlEmailLayout.primaryButton(inviteUrl, "Daveti kabul et"),
            HtmlEmailLayout.mutedLinkFallback(inviteUrl));
        return HtmlEmailLayout.document(
            businessName + " sizi ekibine davet ediyor",
            "Çalışan daveti",
            "Yeni bir davet",
            inner,
            "Bu daveti siz beklemediyseniz bu e-postayı yok sayabilirsiniz.");
    }

    private record ResendRequest(String from, List<String> to, String subject, String html) {}
}
