package com.asaf.appointment_service.controller.appointment;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.appointment.AppointmentResponse;
import com.asaf.appointment_service.dto.appointment.PublicRescheduleRequest;
import com.asaf.appointment_service.dto.appointment.RescheduleContextResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.mapper.appointment.AppointmentMapper;
import com.asaf.appointment_service.service.appointment.RescheduleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/reschedule")
public class PublicRescheduleController {

    private final RescheduleService rescheduleService;

    public PublicRescheduleController(RescheduleService rescheduleService) {
        this.rescheduleService = rescheduleService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<RescheduleContextResponse>> getContext(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(rescheduleService.getContext(token)));
    }

    @PostMapping("/{token}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> reschedule(
            @PathVariable String token,
            @Valid @RequestBody PublicRescheduleRequest request) {
        Appointment appointment = rescheduleService.reschedule(token, request.employeeId(), request.scheduledAt());
        return ResponseEntity.ok(
            ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevunuz yeni saate taşındı."));
    }
}
