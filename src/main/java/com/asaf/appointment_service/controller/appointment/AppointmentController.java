package com.asaf.appointment_service.controller.appointment;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.appointment.AppointmentResponse;
import com.asaf.appointment_service.dto.appointment.CreateAppointmentRequest;
import com.asaf.appointment_service.dto.appointment.StaffAppointmentRequest;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.mapper.appointment.AppointmentMapper;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
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

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        Appointment appointment = appointmentService.createAppointment(request);
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
        Appointment appointment = appointmentService.createStaffAppointment(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu kaydedildi"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(AppointmentMapper.toResponse(appointment)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsByBusiness(
            @RequestParam Long businessId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<Appointment> appointments;
        if (startDate != null && endDate != null) {
            appointments = appointmentService.findByDateRange(businessId, startDate, endDate);
        } else {
            appointments = appointmentService.findByBusinessId(businessId);
        }
        
        List<AppointmentResponse> responses = appointments.stream()
            .map(AppointmentMapper::toResponse)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/risky")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getRiskyAppointments(
            @RequestParam Long businessId) {
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
