package com.asaf.appointment_service.integration.whatsapp.config;

import com.asaf.appointment_service.integration.whatsapp.MetaCloudWhatsAppClient;
import com.asaf.appointment_service.integration.whatsapp.MockWhatsAppClient;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClientInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
@EnableConfigurationProperties(WhatsAppProperties.class)
public class WhatsAppConfig {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppConfig.class);

    @Bean
    public WhatsAppClientInterface whatsAppClient(WhatsAppProperties properties, RestClient whatsAppRestClient) {
        if (!properties.enabled()) {
            log.info("WhatsApp entegrasyonu devre dışı, MockWhatsAppClient kullanılıyor");
            return new MockWhatsAppClient();
        }

        String provider = properties.provider();
        if ("meta-cloud".equalsIgnoreCase(provider)) {
            log.info("WhatsApp entegrasyonu aktif, MetaCloudWhatsAppClient kullanılıyor");
            return new MetaCloudWhatsAppClient(properties, whatsAppRestClient);
        }

        log.info("WhatsApp provider '{}' tanınmıyor, MockWhatsAppClient kullanılıyor", provider);
        return new MockWhatsAppClient();
    }

    @Bean
    public RestClient whatsAppRestClient(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        converter.setSupportedMediaTypes(List.of(
            MediaType.APPLICATION_JSON,
            MediaType.TEXT_PLAIN,
            new MediaType("text", "javascript"),
            new MediaType("application", "javascript")
        ));

        return RestClient.builder()
            .messageConverters(converters -> {
                converters.clear();
                converters.add(converter);
            })
            .build();
    }
}
