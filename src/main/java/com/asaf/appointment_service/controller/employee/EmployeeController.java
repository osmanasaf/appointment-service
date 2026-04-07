package com.asaf.appointment_service.controller.employee;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.employee.CreateEmployeeRequest;
import com.asaf.appointment_service.dto.employee.EmployeeResponse;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.mapper.employee.EmployeeMapper;
import com.asaf.appointment_service.service.employee.EmployeeService;
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
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        Employee employee = employeeService.createEmployee(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(EmployeeMapper.toResponse(employee), "Çalışan oluşturuldu"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(@PathVariable Long id) {
        Employee employee = employeeService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toResponse(employee)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployeesByBusiness(
            @RequestParam Long businessId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<Employee> employees = activeOnly 
            ? employeeService.findActiveByBusinessId(businessId)
            : employeeService.findByBusinessId(businessId);
        
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toResponseList(employees)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<EmployeeResponse>> activateEmployee(@PathVariable Long id) {
        Employee employee = employeeService.activateEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toResponse(employee), "Çalışan aktifleştirildi"));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<EmployeeResponse>> deactivateEmployee(@PathVariable Long id) {
        Employee employee = employeeService.deactivateEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toResponse(employee), "Çalışan pasifleştirildi"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Çalışan silindi"));
    }
}
