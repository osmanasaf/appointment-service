package com.asaf.appointment_service.controller.employee;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.employee.CreateLeaveRequest;
import com.asaf.appointment_service.dto.employee.LeaveConflictPreviewResponse;
import com.asaf.appointment_service.dto.employee.LeaveResponse;
import com.asaf.appointment_service.dto.employee.ResolveAndCreateLeaveRequest;
import com.asaf.appointment_service.dto.employee.ResolveLeaveConflictRequest;
import com.asaf.appointment_service.service.employee.EmployeeLeaveService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/leaves")
public class EmployeeLeaveController {

    private final EmployeeLeaveService leaveService;

    public EmployeeLeaveController(EmployeeLeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> listLeaves(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.listLeaves(employeeId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LeaveResponse>> createLeave(
            @PathVariable Long employeeId,
            @Valid @RequestBody CreateLeaveRequest request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(leaveService.createLeave(employeeId, request)));
    }

    @PostMapping("/conflict-preview")
    public ResponseEntity<ApiResponse<LeaveConflictPreviewResponse>> previewConflicts(
            @PathVariable Long employeeId,
            @Valid @RequestBody CreateLeaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.previewConflicts(employeeId, request)));
    }

    @PostMapping("/resolve-and-create")
    public ResponseEntity<ApiResponse<LeaveResponse>> resolveAndCreate(
            @PathVariable Long employeeId,
            @Valid @RequestBody ResolveAndCreateLeaveRequest request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                leaveService.resolveConflictsAndCreateLeave(employeeId, request.leave(), request.resolve())));
    }

    @DeleteMapping("/{leaveId}")
    public ResponseEntity<ApiResponse<Void>> cancelLeave(
            @PathVariable Long employeeId,
            @PathVariable Long leaveId) {
        leaveService.cancelLeave(employeeId, leaveId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
