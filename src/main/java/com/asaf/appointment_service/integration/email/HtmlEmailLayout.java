package com.asaf.appointment_service.integration.email;

import java.time.Year;
import org.springframework.web.util.HtmlUtils;

public final class HtmlEmailLayout {

    private static final String SITE_URL = "https://randevum.pro";

    private HtmlEmailLayout() {
    }

    public static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(raw);
    }

    public static String document(
            String preheaderText,
            String kicker,
            String headline,
            String bodyHtml,
            String footerNotice) {
        String pre = escape(preheaderText);
        String k = escape(kicker);
        String h = escape(headline);
        String foot = footerNotice == null || footerNotice.isBlank()
            ? "Bu e-postayı siz talep etmediyseniz yok sayabilirsiniz."
            : escape(footerNotice);
        int year = Year.now().getValue();
        return """
            <!DOCTYPE html>
            <html lang="tr">
            <head>
            <meta charset="UTF-8">
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f1f5f9;">
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f1f5f9;padding:28px 12px;">
              <tr>
                <td align="center">
                  <div style="display:none;font-size:1px;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden;">%s</div>
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;border-collapse:separate;border-spacing:0;width:100%%;">
                    <tr>
                      <td bgcolor="#4f46e5" style="background-color:#4f46e5;background-image:linear-gradient(135deg,#4f46e5 0%%,#7c3aed 100%%);border-radius:14px 14px 0 0;padding:26px 24px 24px;text-align:center;">
                        <p style="margin:0 0 8px;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;font-size:11px;font-weight:600;letter-spacing:0.14em;text-transform:uppercase;color:rgba(255,255,255,0.9);">%s</p>
                        <h1 style="margin:0;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;font-size:24px;font-weight:800;line-height:1.25;color:#ffffff;letter-spacing:-0.02em;">%s</h1>
                      </td>
                    </tr>
                    <tr>
                      <td bgcolor="#ffffff" style="background-color:#ffffff;border-left:1px solid #e2e8f0;border-right:1px solid #e2e8f0;padding:32px 24px 8px;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;font-size:15px;line-height:1.65;color:#334155;">
                        %s
                      </td>
                    </tr>
                    <tr>
                      <td bgcolor="#f8fafc" style="background-color:#f8fafc;border:1px solid #e2e8f0;border-top:0;border-radius:0 0 14px 14px;padding:20px 24px 24px;text-align:center;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;font-size:12px;line-height:1.55;color:#94a3b8;">
                        <p style="margin:0 0 10px;">%s</p>
                        <p style="margin:0;"><span style="color:#cbd5e1;">&copy; %d Randevum</span> <span style="color:#cbd5e1;">&middot;</span> <a href="%s" style="color:#6366f1;text-decoration:none;font-weight:600;">randevum.pro</a></p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
            </body>
            </html>
            """.formatted(h, pre, k, h, bodyHtml, foot, year, SITE_URL);
    }

    public static String primaryButton(String href, String label) {
        return """
            <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center" style="margin:28px auto 8px;">
              <tr>
                <td bgcolor="#4f46e5" style="border-radius:11px;background-color:#4f46e5;background-image:linear-gradient(135deg,#4f46e5 0%%,#6366f1 100%%);box-shadow:0 4px 14px rgba(79,70,229,0.35);">
                  <a href="%s" target="_blank" rel="noopener noreferrer" style="display:inline-block;padding:15px 32px;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;border-radius:11px;">%s</a>
                </td>
              </tr>
            </table>
            """.formatted(href, escape(label));
    }

    public static String mutedLinkFallback(String url) {
        return """
            <p style="margin:20px 0 0;font-size:12px;line-height:1.5;color:#94a3b8;">
              Buton çalışmıyorsa aşağıdaki bağlantıyı kopyalayıp tarayıcınıza yapıştırın:
            </p>
            <p style="margin:8px 0 0;word-break:break-all;font-size:12px;line-height:1.45;">
              <a href="%s" style="color:#6366f1;text-decoration:underline;">%s</a>
            </p>
            """.formatted(url, escape(url));
    }
}
