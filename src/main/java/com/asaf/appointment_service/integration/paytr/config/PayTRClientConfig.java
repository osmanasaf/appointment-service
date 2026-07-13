package com.asaf.appointment_service.integration.paytr.config;

import com.asaf.appointment_service.integration.paytr.MockPayTRClient;
import com.asaf.appointment_service.integration.paytr.PayTRClient;
import com.asaf.appointment_service.integration.paytr.PayTRHashService;
import com.asaf.appointment_service.integration.paytr.PayTRHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayTRClientConfig {

    private static final Logger log = LoggerFactory.getLogger(PayTRClientConfig.class);

    @Bean
    public PayTRClient payTRClient(PayTRProperties properties,
                                   PayTRHashService hashService,
                                   ObjectMapper objectMapper) {
        boolean isMock = properties.testMode()
                || properties.merchantId() == null
                || properties.merchantId().isBlank();

        if (isMock) {
            log.info("PayTR Mock client aktif (test_mode={}, merchantId={})",
                    properties.testMode(),
                    properties.merchantId() == null || properties.merchantId().isBlank() ? "boş" : "set");
            return new MockPayTRClient();
        }

        log.info("PayTR HTTP client aktif");
        return new PayTRHttpClient(properties, hashService, objectMapper);
    }
}
