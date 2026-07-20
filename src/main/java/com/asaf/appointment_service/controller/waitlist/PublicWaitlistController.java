package com.asaf.appointment_service.controller.waitlist;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.appointment.AppointmentResponse;
import com.asaf.appointment_service.dto.waitlist.PublicWaitlistRequest;
import com.asaf.appointment_service.dto.waitlist.WaitlistEntryResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.waitlist.WaitlistEntry;
import com.asaf.appointment_service.mapper.appointment.AppointmentMapper;
import com.asaf.appointment_service.mapper.waitlist.WaitlistMapper;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.otp.PublicOtpService;
import com.asaf.appointment_service.service.waitlist.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicWaitlistController {

    private final WaitlistService waitlistService;
    private final PublicOtpService publicOtpService;
    private final BusinessService businessService;

    public PublicWaitlistController(WaitlistService waitlistService,
                                    PublicOtpService publicOtpService,
                                    BusinessService businessService) {
        this.waitlistService = waitlistService;
        this.publicOtpService = publicOtpService;
        this.businessService = businessService;
    }

    @PostMapping("/businesses/{slug}/waitlist")
    public ResponseEntity<ApiResponse<WaitlistEntryResponse>> register(
            @PathVariable String slug,
            @Valid @RequestBody PublicWaitlistRequest request) {
        Business business = businessService.findBySlug(slug);

        publicOtpService.verifyAndConsume(
            business.getId(),
            request.getPhoneCountryCode(),
            request.phoneNumber(),
            request.otpCode()
        );

        WaitlistEntry entry = waitlistService.registerPublic(
            business.getId(), request.serviceId(), request.employeeId(),
            request.preferredFrom(), request.preferredUntil(),
            request.customerName(), request.getPhoneCountryCode(), request.phoneNumber());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(WaitlistMapper.toResponse(entry), "Bekleme listesine eklendiniz"));
    }

    @PostMapping("/waitlist-offer/{token}/accept")
    public ResponseEntity<ApiResponse<AppointmentResponse>> acceptOffer(@PathVariable String token) {
        Appointment appointment = waitlistService.acceptOffer(token);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevunuz oluşturuldu"));
    }
}
