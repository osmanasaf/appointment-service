package com.asaf.appointment_service.service.waitlist;

import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.dto.appointment.CreateAppointmentRequest;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentSource;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.entity.waitlist.WaitlistEntry;
import com.asaf.appointment_service.entity.waitlist.WaitlistStatus;
import com.asaf.appointment_service.exception.waitlist.WaitlistEntryNotFoundException;
import com.asaf.appointment_service.repository.waitlist.WaitlistEntryRepository;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import com.asaf.appointment_service.service.appointment.AppointmentValidationService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import com.asaf.appointment_service.service.token.PublicActionTokenService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WaitlistService {

    private final WaitlistEntryRepository repository;
    private final CustomerService customerService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final SubscriptionService subscriptionService;
    private final AppointmentValidationService validationService;
    private final AppointmentService appointmentService;
    private final PublicActionTokenService tokenService;

    public WaitlistService(WaitlistEntryRepository repository,
                           CustomerService customerService,
                           ServiceDefinitionService serviceDefinitionService,
                           SubscriptionService subscriptionService,
                           AppointmentValidationService validationService,
                           AppointmentService appointmentService,
                           PublicActionTokenService tokenService) {
        this.repository = repository;
        this.customerService = customerService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.subscriptionService = subscriptionService;
        this.validationService = validationService;
        this.appointmentService = appointmentService;
        this.tokenService = tokenService;
    }

    @Transactional
    public WaitlistEntry registerPublic(Long businessId, Long serviceId, Long employeeId,
                                        LocalDate preferredFrom, LocalDate preferredUntil,
                                        String customerName, String phoneCountryCode, String phoneNumber) {
        subscriptionService.requireFeature(businessId, PlanFeature.WAITLIST);
        serviceDefinitionService.findByIdOrThrow(serviceId);
        if (employeeId != null) {
            validationService.validateCapability(employeeId, serviceId);
        }
        Customer customer = customerService.findOrCreateByPhone(businessId, customerName, phoneCountryCode, phoneNumber);

        WaitlistEntry entry = WaitlistEntry.builder()
            .businessId(businessId)
            .customerId(customer.getId())
            .serviceId(serviceId)
            .employeeId(employeeId)
            .preferredFrom(preferredFrom)
            .preferredUntil(preferredUntil)
            .build();
        return repository.save(entry);
    }

    @Transactional
    public WaitlistEntry addManually(Long businessId, Long customerId, Long serviceId, Long employeeId,
                                     LocalDate preferredFrom, LocalDate preferredUntil) {
        subscriptionService.requireFeature(businessId, PlanFeature.WAITLIST);
        customerService.findByIdOrThrow(customerId);
        serviceDefinitionService.findByIdOrThrow(serviceId);
        if (employeeId != null) {
            validationService.validateCapability(employeeId, serviceId);
        }

        WaitlistEntry entry = WaitlistEntry.builder()
            .businessId(businessId)
            .customerId(customerId)
            .serviceId(serviceId)
            .employeeId(employeeId)
            .preferredFrom(preferredFrom)
            .preferredUntil(preferredUntil)
            .build();
        return repository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntry> listByBusiness(Long businessId, WaitlistStatus statusFilter) {
        subscriptionService.requireFeature(businessId, PlanFeature.WAITLIST);
        WaitlistStatus effectiveStatus = statusFilter != null ? statusFilter : WaitlistStatus.WAITING;
        return repository.findByBusinessIdAndStatusOrderByCreatedAtAsc(businessId, effectiveStatus);
    }

    @Transactional
    public void cancelEntry(Long businessId, Long entryId) {
        subscriptionService.requireFeature(businessId, PlanFeature.WAITLIST);
        WaitlistEntry entry = findByIdOrThrow(entryId);
        if (!entry.getBusinessId().equals(businessId)) {
            throw new WaitlistEntryNotFoundException(entryId);
        }
        entry.cancel();
        repository.save(entry);
    }

    @Transactional
    public Appointment acceptOffer(String token) {
        PublicActionToken actionToken = tokenService.validate(token, PublicActionTokenType.WAITLIST_OFFER);
        WaitlistEntry entry = findByIdOrThrow(actionToken.getSubjectId());

        if (entry.getStatus() != WaitlistStatus.NOTIFIED) {
            throw new BusinessException("WAITLIST_OFFER_NOT_ACTIVE",
                "Bu teklif artık geçerli değil.");
        }

        Long employeeId = entry.getOfferedEmployeeId();
        // capability + slot müsaitliği + gün limiti createAppointment içinde zaten doğrulanıyor
        CreateAppointmentRequest appointmentRequest = new CreateAppointmentRequest(
            entry.getCustomerId(), employeeId, entry.getServiceId(),
            entry.getOfferedScheduledAt(), null, null, AppointmentSource.ONLINE);
        Appointment appointment = appointmentService.createAppointment(entry.getBusinessId(), appointmentRequest);

        entry.markConverted();
        repository.save(entry);
        tokenService.consume(actionToken);

        return appointment;
    }

    public WaitlistEntry findByIdOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new WaitlistEntryNotFoundException(id));
    }
}
