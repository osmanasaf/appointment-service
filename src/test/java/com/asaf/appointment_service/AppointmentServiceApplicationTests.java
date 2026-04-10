package com.asaf.appointment_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
	"whatsapp.enabled=false",
	"whatsapp.provider=mock",
	"whatsapp.meta.phone-number-id=test",
	"whatsapp.meta.access-token=test",
	"whatsapp.meta.waba-id=test",
	"whatsapp.meta.app-secret=test",
	"whatsapp.meta.webhook-verify-token=test",
	"ratelimit.enabled=false"
})
class AppointmentServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
