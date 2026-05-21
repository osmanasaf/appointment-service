package com.asaf.appointment_service.controller.appointment;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.dto.PageResponse;
import com.asaf.appointment_service.common.exception.UnauthorizedException;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.appointment.AppointmentResponse;
import com.asaf.appointment_service.dto.appointment.CreateAppointmentRequest;
import com.asaf.appointment_service.dto.appointment.StaffAppointmentRequest;
import com.asaf.appointment_service.dto.appointment.UpdateStaffAppointmentRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final CurrentUserService currentUserService;

    public AppointmentController(AppointmentService appointmentService,
                                  CurrentUserService currentUserService) {
        this.appointmentService = appointmentService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        Long businessId = currentUserService.getCurrentBusinessId();
        
        // Çalışan sadece kendi employeeId'si ile randevu oluşturabilir
        if (currentUserService.isEmployee()) {
            Long currentEmployeeId = currentUserService.getCurrentEmployeeId();
            if (currentEmployeeId == null) {
                throw new UnauthorizedException("Çalışan kaydınız bulunamadı");
            }
            if (!currentEmployeeId.equals(request.employeeId())) {
                throw new UnauthorizedException("Sadece kendinize randevu oluşturabilirsiniz");
            }
        }
        
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
        Long businessId = currentUserService.getCurrentBusinessId();
        
        // Çalışan sadece kendi employeeId'si ile randevu oluşturabilir
        if (currentUserService.isEmployee()) {
            Long currentEmployeeId = currentUserService.getCurrentEmployeeId();
            if (currentEmployeeId == null) {
                throw new UnauthorizedException("Çalışan kaydınız bulunamadı");
            }
            if (!currentEmployeeId.equals(request.employeeId())) {
                throw new UnauthorizedException("Sadece kendinize randevu oluşturabilirsiniz");
            }
        }
        
        Appointment appointment = appointmentService.createStaffAppointment(businessId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu kaydedildi"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStaffAppointment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffAppointmentRequest request) {
        Long businessId = currentUserService.getCurrentBusinessId();
        if (currentUserService.isEmployee()) {
            Long currentEmployeeId = currentUserService.getCurrentEmployeeId();
            if (currentEmployeeId == null) {
                throw new UnauthorizedException("Çalışan kaydınız bulunamadı");
            }
            if (!currentEmployeeId.equals(request.employeeId())) {
                throw new UnauthorizedException("Sadece kendinize atanmış randevuları bu şekilde güncelleyebilirsiniz");
            }
        }
        Appointment existing = appointmentService.findByIdOrThrow(id);
        validateEmployeeAccess(existing);
        Appointment appointment = appointmentService.updateStaffAppointment(businessId, id, request);
        return ResponseEntity.ok(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu güncellendi"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.findByIdOrThrow(id);
        
        // Çalışan sadece kendi randevusunu görebilir
        if (currentUserService.isEmployee()) {
            Long currentEmployeeId = currentUserService.getCurrentEmployeeId();
            if (currentEmployeeId == null || !currentEmployeeId.equals(appointment.getEmployeeId())) {
                throw new UnauthorizedException("Bu randevuyu görme yetkiniz yok");
            }
        }
        
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
        Long businessId = currentUserService.getCurrentBusinessId();
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        var pageable = PageRequest.of(page, safeSize, Sort.by("scheduledAt").descending());
        Page<Appointment> appointments;
        
        // Çalışan sadece kendi randevularını görebilir
        if (currentUserService.isEmployee()) {
            Long currentEmployeeId = currentUserService.getCurrentEmployeeId();
            if (currentEmployeeId == null) {
                throw new UnauthorizedException("Çalışan kaydınız bulunamadı");
            }
            if (startDate != null && endDate != null) {
                appointments = appointmentService.findByEmployeeIdAndDateRangePaged(
                    businessId, currentEmployeeId, startDate, endDate, pageable);
            } else {
                appointments = appointmentService.findByBusinessIdAndEmployeeIdPaged(
                    businessId, currentEmployeeId, pageable);
            }
        } else {
            // İşletme sahibi veya admin tüm randevuları görebilir
            if (startDate != null && endDate != null) {
                appointments = appointmentService.findByDateRangePaged(businessId, startDate, endDate, pageable);
            } else {
                appointments = appointmentService.findByBusinessIdPaged(businessId, pageable);
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(appointments, AppointmentMapper::toResponse)));
    }

    @GetMapping("/risky")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getRiskyAppointments() {
        // Risky randevular sadece işletme sahibi/admin için
        currentUserService.requireBusinessOwnerOrAdmin();
        
        Long businessId = currentUserService.getCurrentBusinessId();
        List<Appointment> appointments = appointmentService.findRiskyAppointments(businessId);
        List<AppointmentResponse> responses = appointments.stream()
            .map(AppointmentMapper::toResponse)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<AppointmentResponse>> confirmAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.findByIdOrThrow(id);
        validateEmployeeAccess(appointment);
        
        appointment = appointmentService.confirmAppointment(id);
        return ResponseEntity.ok(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu onaylandı"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Müşteri tarafından iptal edildi") String reason) {
        Appointment appointment = appointmentService.findByIdOrThrow(id);
        validateEmployeeAccess(appointment);
        
        appointment = appointmentService.cancelAppointment(id, reason);
        return ResponseEntity.ok(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu iptal edildi"));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<AppointmentResponse>> completeAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.findByIdOrThrow(id);
        validateEmployeeAccess(appointment);
        
        appointment = appointmentService.completeAppointment(id);
        return ResponseEntity.ok(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu tamamlandı"));
    }

    @PostMapping("/{id}/no-show")
    public ResponseEntity<ApiResponse<AppointmentResponse>> markAsNoShow(@PathVariable Long id) {
        Appointment appointment = appointmentService.findByIdOrThrow(id);
        validateEmployeeAccess(appointment);
        
        appointment = appointmentService.markAsNoShow(id);
        return ResponseEntity.ok(ApiResponse.success(AppointmentMapper.toResponse(appointment), "Randevu no-show olarak işaretlendi"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.findByIdOrThrow(id);
        validateEmployeeAccess(appointment);
        
        appointmentService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Randevu silindi"));
    }
    
    /**
     * Çalışan sadece kendi randevusuna erişebilir kontrolü
     */
    private void validateEmployeeAccess(Appointment appointment) {
        if (currentUserService.isEmployee()) {
            Long currentEmployeeId = currentUserService.getCurrentEmployeeId();
            if (currentEmployeeId == null || !currentEmployeeId.equals(appointment.getEmployeeId())) {
                throw new UnauthorizedException("Bu randevuya erişim yetkiniz yok");
            }
        }
    }
}
