package com.asaf.appointment_service.controller.appointment;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.dto.PageResponse;
import com.asaf.appointment_service.common.service.AuthenticatedBusinessService;
import com.asaf.appointment_service.dto.appointment.AppointmentResponse;
import com.asaf.appointment_service.dto.appointment.CreateAppointmentRequest;
import com.asaf.appointment_service.dto.appointment.StaffAppointmentRequest;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.mapper.appointment.AppointmentMapper;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthenticatedBusinessService authenticatedBusinessService;

    public AppointmentController(AppointmentService appointmentService,
                                  AuthenticatedBusinessService authenticatedBusinessService) {
        this.appointmentService = appointmentService;
        this.authenticatedBusinessService = authenticatedBusinessService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        Long businessId = authenticatedBusinessService.getCurrentBusinessId();
        Appointment appointment = appointmentService.createAppointment(businessId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu oluşturuldu"));
    }

    /**
     * Personel: Telefonla arayan veya yerinde gelen müşteri için randevu kaydı.
     * Müşteri adı + telefon ile bulunur/oluşturulur; source PHONE veya WALK_IN olmalı.
     * Tüm randevular (online, telefon, manuel) aynı takvimde senkron tutulur.
     */
    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createStaffAppointment(
            @Valid @RequestBody StaffAppointmentRequest request) {
        Long businessId = authenticatedBusinessService.getCurrentBusinessId();
        Appointment appointment = appointmentService.createStaffAppointment(businessId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu kaydedildi"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(AppointmentMapper.toResponse(appointment)));
    }

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> getAppointmentsByBusiness(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long businessId = authenticatedBusinessService.getCurrentBusinessId();
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        var pageable = PageRequest.of(page, safeSize, Sort.by("scheduledAt").descending());
        Page<Appointment> appointments;
        if (startDate != null && endDate != null) {
            appointments = appointmentService.findByDateRangePaged(businessId, startDate, endDate, pageable);
        } else {
            appointments = appointmentService.findByBusinessIdPaged(businessId, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(appointments, AppointmentMapper::toResponse)));
    }

    @GetMapping("/risky")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getRiskyAppointments() {
        Long businessId = authenticatedBusinessService.getCurrentBusinessId();
        List<Appointment> appointments = appointmentService.findRiskyAppointments(businessId);
        List<AppointmentResponse> responses = appointments.stream()
            .map(AppointmentMapper::toResponse)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<AppointmentResponse>> confirmAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.confirmAppointment(id);
        return ResponseEntity.ok(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu onaylandı"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Müşteri tarafından iptal edildi") String reason) {
        Appointment appointment = appointmentService.cancelAppointment(id, reason);
        return ResponseEntity.ok(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu iptal edildi"));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<AppointmentResponse>> completeAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.completeAppointment(id);
        return ResponseEntity.ok(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu tamamlandı"));
    }

    @PostMapping("/{id}/no-show")
    public ResponseEntity<ApiResponse<AppointmentResponse>> markAsNoShow(@PathVariable Long id) {
        Appointment appointment = appointmentService.markAsNoShow(id);
        return ResponseEntity.ok(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu no-show olarak işaretlendi"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Randevu silindi"));
    }
}
