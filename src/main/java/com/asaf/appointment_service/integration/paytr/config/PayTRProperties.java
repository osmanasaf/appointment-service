package com.asaf.appointment_service.integration.paytr.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "paytr")
public record PayTRProperties(
    String merchantId,
    String merchantKey,
    String merchantSalt,
    boolean testMode,
    String baseUrl,
    boolean recurringEnabled,
    Discount discount
) {

    public String baseUrl() {
        return baseUrl != null ? baseUrl : "https://www.paytr.com";
    }

    public boolean recurringEnabled() {
        return recurringEnabled;
    }

    public Discount discount() {
        return discount != null ? discount : new Discount(null, null);
    }

    public record Discount(BigDecimal quarterly, BigDecimal semiannual) {

        public BigDecimal quarterly() {
            return quarterly != null ? quarterly : new BigDecimal("0.05");
        }

        public BigDecimal semiannual() {
            return semiannual != null ? semiannual : new BigDecimal("0.10");
        }
    }
}
