package com.asaf.appointment_service.controller.waitlist;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.waitlist.AddWaitlistEntryRequest;
import com.asaf.appointment_service.dto.waitlist.WaitlistEntryResponse;
import com.asaf.appointment_service.entity.waitlist.WaitlistEntry;
import com.asaf.appointment_service.entity.waitlist.WaitlistStatus;
import com.asaf.appointment_service.mapper.waitlist.WaitlistMapper;
import com.asaf.appointment_service.service.waitlist.WaitlistService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final CurrentUserService currentUserService;

    public WaitlistController(WaitlistService waitlistService, CurrentUserService currentUserService) {
        this.waitlistService = waitlistService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WaitlistEntryResponse>>> list(
            @RequestParam(required = false) WaitlistStatus status) {
        Long businessId = currentUserService.getCurrentBusinessId();
        List<WaitlistEntry> entries = waitlistService.listByBusiness(businessId, status);
        return ResponseEntity.ok(ApiResponse.success(entries.stream().map(WaitlistMapper::toResponse).toList()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WaitlistEntryResponse>> add(@Valid @RequestBody AddWaitlistEntryRequest request) {
        Long businessId = currentUserService.getCurrentBusinessId();
        WaitlistEntry entry = waitlistService.addManually(
            businessId, request.customerId(), request.serviceId(),
            request.employeeId(), request.preferredFrom(), request.preferredUntil());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(WaitlistMapper.toResponse(entry), "Bekleme listesine eklendi"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        Long businessId = currentUserService.getCurrentBusinessId();
        waitlistService.cancelEntry(businessId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Bekleme listesi kaydı iptal edildi"));
    }
}
