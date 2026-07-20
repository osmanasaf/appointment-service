package com.asaf.appointment_service.service.calendar;

import com.asaf.appointment_service.common.util.IcsGenerator;
import com.asaf.appointment_service.common.util.IcsGenerator.IcsEvent;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import com.asaf.appointment_service.service.token.PublicActionTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Randevu için tekrar tekrar indirilebilir .ics linkini yönetir ve içeriği üretir.
 */
@Service
public class IcsService {

    private final PublicActionTokenService tokenService;
    private final AppointmentService appointmentService;
    private final BusinessService businessService;
    private final ServiceDefinitionService serviceDefinitionService;

    public IcsService(PublicActionTokenService tokenService,
                      AppointmentService appointmentService,
                      BusinessService businessService,
                      ServiceDefinitionService serviceDefinitionService) {
        this.tokenService = tokenService;
        this.appointmentService = appointmentService;
        this.businessService = businessService;
        this.serviceDefinitionService = serviceDefinitionService;
    }

    @Transactional
    public String getOrIssueCalendarLink(String baseUrl, Appointment appointment) {
        PublicActionToken token = tokenService.getOrIssueCalendarIcsToken(appointment.getBusinessId(), appointment.getId());
        return baseUrl + "/api/v1/public/appointments/" + token.getToken() + "/calendar.ics";
    }

    @Transactional(readOnly = true)
    public String generateIcsForToken(String token) {
        PublicActionToken actionToken = tokenService.validate(token, PublicActionTokenType.CALENDAR_ICS);
        Appointment appointment = appointmentService.findByIdOrThrow(actionToken.getSubjectId());
        Business business = businessService.findByIdOrThrow(appointment.getBusinessId());
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(appointment.getServiceId());

        IcsEvent event = new IcsEvent(
            appointment.getId(),
            appointment.getScheduledAt(),
            appointment.getEndAt(),
            service.getName() + " - " + business.getName(),
            business.getAddress(),
            appointment.getNotes(),
            appointment.getStatus() == AppointmentStatus.CANCELLED
        );
        return IcsGenerator.generate(event);
    }
}
