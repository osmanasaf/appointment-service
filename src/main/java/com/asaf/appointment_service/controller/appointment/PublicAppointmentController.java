package com.asaf.appointment_service.controller.appointment;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.appointment.AppointmentResponse;
import com.asaf.appointment_service.dto.appointment.AvailableSlotResponse;
import com.asaf.appointment_service.dto.appointment.CreateAppointmentRequest;
import com.asaf.appointment_service.dto.appointment.PublicAppointmentOtpRequest;
import com.asaf.appointment_service.dto.appointment.PublicAppointmentRequest;
import com.asaf.appointment_service.dto.business.BusinessResponse;
import com.asaf.appointment_service.dto.employee.EmployeeResponse;
import com.asaf.appointment_service.dto.service.ServiceResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentSource;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.employee.EmployeeServiceCapability;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.mapper.appointment.AppointmentMapper;
import com.asaf.appointment_service.mapper.business.BusinessMapper;
import com.asaf.appointment_service.mapper.employee.EmployeeMapper;
import com.asaf.appointment_service.mapper.service.ServiceMapper;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import com.asaf.appointment_service.service.appointment.AvailabilityService;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.employee.EmployeeCapabilityService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.otp.PublicOtpService;
import com.asaf.appointment_service.service.review.ReviewService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicAppointmentController {

    private final BusinessService businessService;
    private final EmployeeService employeeService;
    private final EmployeeCapabilityService capabilityService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final CustomerService customerService;
    private final AppointmentService appointmentService;
    private final AvailabilityService availabilityService;
    private final PublicOtpService publicOtpService;
    private final ReviewService reviewService;

    public PublicAppointmentController(BusinessService businessService,
                                        EmployeeService employeeService,
                                        EmployeeCapabilityService capabilityService,
                                        ServiceDefinitionService serviceDefinitionService,
                                        CustomerService customerService,
                                        AppointmentService appointmentService,
                                        AvailabilityService availabilityService,
                                        PublicOtpService publicOtpService,
                                        ReviewService reviewService) {
        this.businessService = businessService;
        this.employeeService = employeeService;
        this.capabilityService = capabilityService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.customerService = customerService;
        this.appointmentService = appointmentService;
        this.availabilityService = availabilityService;
        this.publicOtpService = publicOtpService;
        this.reviewService = reviewService;
    }
    @GetMapping("/businesses/{slug}")
    public ResponseEntity<ApiResponse<BusinessResponse>> getBusinessBySlug(@PathVariable String slug) {
        Business business = businessService.findBySlug(slug);
        Double averageRating = reviewService.getAverageRating(business.getId());
        long reviewCount = reviewService.getPublishedCount(business.getId());
        return ResponseEntity.ok(
            ApiResponse.success(BusinessMapper.toResponse(business, averageRating, (int) reviewCount)));
    }
    @GetMapping("/businesses/{slug}/employees")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getActiveEmployees(@PathVariable String slug) {
        Business business = businessService.findBySlug(slug);
        List<Employee> employees = employeeService.findActiveByBusinessId(business.getId());
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toResponseList(employees)));
    }
    @GetMapping("/businesses/{slug}/services")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getActiveServices(@PathVariable String slug) {
        Business business = businessService.findBySlug(slug);
        List<ServiceDefinition> services = serviceDefinitionService.findActiveByBusinessId(business.getId());
        return ResponseEntity.ok(ApiResponse.success(ServiceMapper.toResponseList(services)));
    }
    @GetMapping("/businesses/{slug}/services/{serviceId}/employees")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployeesForService(
            @PathVariable String slug,
            @PathVariable Long serviceId) {
        Business business = businessService.findBySlug(slug);
        List<EmployeeServiceCapability> capabilities =
            capabilityService.getActiveByServiceAndBusiness(serviceId, business.getId());
        List<Employee> employees = capabilities.stream()
            .map(c -> employeeService.findByIdOrThrow(c.getEmployeeId()))
            .filter(Employee::isActive)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toResponseList(employees)));
    }
    @GetMapping("/businesses/{slug}/availability")
    public ResponseEntity<ApiResponse<List<AvailableSlotResponse>>> getAvailableSlotsBySlug(
            @PathVariable String slug,
            @RequestParam Long serviceId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Business business = businessService.findBySlug(slug);
        List<AvailableSlotResponse> slots =
            availabilityService.getAvailableSlots(business.getId(), employeeId, serviceId, date);
        return ResponseEntity.ok(ApiResponse.success(slots));
    }

    @GetMapping("/businesses/{slug}/availability/dates")
    public ResponseEntity<ApiResponse<List<LocalDate>>> getAvailableDatesBySlug(
            @PathVariable String slug,
            @RequestParam Long serviceId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "30") int daysAhead) {
        Business business = businessService.findBySlug(slug);
        List<LocalDate> dates =
            availabilityService.getAvailableDates(business.getId(), employeeId, serviceId, daysAhead);
        return ResponseEntity.ok(ApiResponse.success(dates));
    }

    @PostMapping("/businesses/{slug}/appointments/otp")
    public ResponseEntity<ApiResponse<Void>> requestPublicAppointmentOtp(
            @PathVariable String slug,
            @Valid @RequestBody PublicAppointmentOtpRequest request) {
        Business business = businessService.findBySlug(slug);
        publicOtpService.requestOtp(
            business.getId(),
            request.getPhoneCountryCode(),
            request.phoneNumber()
        );
        return ResponseEntity.ok(ApiResponse.success(null,
            "Doğrulama kodu WhatsApp ile gönderildi."));
    }

    @PostMapping("/businesses/{slug}/appointments")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createPublicAppointment(
            @PathVariable String slug,
            @Valid @RequestBody PublicAppointmentRequest request) {

        Business business = businessService.findBySlug(slug);

        publicOtpService.verifyAndConsume(
            business.getId(),
            request.getPhoneCountryCode(),
            request.phoneNumber(),
            request.otpCode()
        );

        Optional<Customer> existingCustomer = customerService.findByBusinessIdAndPhoneOptional(
            business.getId(),
            request.getPhoneCountryCode(),
            request.phoneNumber()
        );
        if (existingCustomer.isPresent()) {
            ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(request.serviceId());
            var endTime = request.scheduledAt().plusMinutes(service.totalDurationMinutes());
            if (appointmentService.hasActiveAppointmentAtSlot(
                existingCustomer.get().getId(), request.scheduledAt(), endTime)) {
                return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("DUPLICATE_APPOINTMENT",
                        "Bu telefon numarası ile bu saat için zaten bir randevunuz bulunmaktadır."));
            }
        }

        Customer customer = customerService.findOrCreateByPhone(
            business.getId(),
            request.customerName(),
            request.getPhoneCountryCode(),
            request.phoneNumber()
        );

        CreateAppointmentRequest appointmentRequest = new CreateAppointmentRequest(
            customer.getId(),
            request.employeeId(),
            request.serviceId(),
            request.scheduledAt(),
            request.notes(),
            null,
            AppointmentSource.ONLINE
        );

        Appointment appointment = appointmentService.createAppointment(business.getId(), appointmentRequest);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(AppointmentMapper.toResponse(appointment),
                "Randevu talebiniz alındı"));
    }
}
