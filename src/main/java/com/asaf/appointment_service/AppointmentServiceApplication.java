package com.asaf.appointment_service;

import com.asaf.appointment_service.integration.email.EmailProperties;
import com.asaf.appointment_service.integration.paytr.config.PayTRProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({EmailProperties.class, PayTRProperties.class})
public class AppointmentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppointmentServiceApplication.class, args);
	}

}
