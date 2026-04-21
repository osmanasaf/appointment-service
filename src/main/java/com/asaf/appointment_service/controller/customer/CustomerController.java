package com.asaf.appointment_service.controller.customer;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.dto.PageResponse;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.appointment.AppointmentResponse;
import com.asaf.appointment_service.dto.customer.CreateCustomerRequest;
import com.asaf.appointment_service.dto.customer.CustomerResponse;
import com.asaf.appointment_service.dto.customer.PackageResponse;
import com.asaf.appointment_service.dto.customer.UpdateCustomerRequest;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.mapper.appointment.AppointmentMapper;
import com.asaf.appointment_service.mapper.customer.CustomerMapper;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.encryption.EncryptionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CustomerService customerService;
    private final AppointmentService appointmentService;
    private final EncryptionService encryptionService;
    private final CurrentUserService currentUserService;

    public CustomerController(CustomerService customerService,
                               AppointmentService appointmentService,
                               EncryptionService encryptionService,
                               CurrentUserService currentUserService) {
        this.customerService = customerService;
        this.appointmentService = appointmentService;
        this.encryptionService = encryptionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        Long businessId = currentUserService.getCurrentBusinessId();
        Customer customer = customerService.createCustomer(businessId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(CustomerMapper.toResponse(customer, encryptionService), "Müşteri oluşturuldu"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(@PathVariable Long id) {
        Customer customer = customerService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toResponse(customer, encryptionService)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        Customer customer = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toResponse(customer, encryptionService), "Müşteri güncellendi"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> getCustomersByBusiness(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long businessId = currentUserService.getCurrentBusinessId();
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        var pageable = PageRequest.of(page, safeSize, Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.success(
            PageResponse.from(customerService.findByBusinessIdPaged(businessId, pageable),
                c -> CustomerMapper.toResponse(c, encryptionService))));
    }

    @GetMapping("/{id}/packages")
    public ResponseEntity<ApiResponse<List<PackageResponse>>> getCustomerPackages(@PathVariable Long id) {
        List<CustomerPackage> packages = customerService.findActivePackages(id);
        return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toPackageResponseList(packages)));
    }

    @GetMapping("/{id}/appointments")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> getCustomerAppointments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long businessId = currentUserService.getCurrentBusinessId();
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), safeSize);
        return ResponseEntity.ok(ApiResponse.success(
            PageResponse.from(
                appointmentService.findByBusinessIdAndCustomerIdPaged(businessId, id, pageable),
                AppointmentMapper::toResponse)));
    }

    @PostMapping("/{id}/blacklist")
    public ResponseEntity<ApiResponse<CustomerResponse>> blacklistCustomer(
            @PathVariable Long id,
            @RequestParam String reason) {
        Customer customer = customerService.blacklistCustomer(id, reason);
        return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toResponse(customer, encryptionService), "Müşteri kara listeye alındı"));
    }

    @PostMapping("/{id}/remove-blacklist")
    public ResponseEntity<ApiResponse<CustomerResponse>> removeFromBlacklist(@PathVariable Long id) {
        Customer customer = customerService.removeFromBlacklist(id);
        return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toResponse(customer, encryptionService), "Müşteri kara listeden çıkarıldı"));
    }

    @PostMapping("/{id}/clear-no-show-approval")
    public ResponseEntity<ApiResponse<CustomerResponse>> clearNoShowManualApproval(@PathVariable Long id) {
        customerService.clearNoShowManualApproval(id);
        Customer customer = customerService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toResponse(customer, encryptionService), "Onay zorunluluğu kaldırıldı"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        customerService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Müşteri silindi"));
    }
}
