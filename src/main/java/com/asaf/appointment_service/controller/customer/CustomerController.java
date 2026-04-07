package com.asaf.appointment_service.controller.customer;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.customer.CreateCustomerRequest;
import com.asaf.appointment_service.dto.customer.CustomerResponse;
import com.asaf.appointment_service.dto.customer.PackageResponse;
import com.asaf.appointment_service.dto.customer.UpdateCustomerRequest;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.mapper.customer.CustomerMapper;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.encryption.EncryptionService;
import jakarta.validation.Valid;
import java.util.List;
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

    private final CustomerService customerService;
    private final EncryptionService encryptionService;

    public CustomerController(CustomerService customerService, EncryptionService encryptionService) {
        this.customerService = customerService;
        this.encryptionService = encryptionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.createCustomer(request);
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
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getCustomersByBusiness(
            @RequestParam Long businessId) {
        List<Customer> customers = customerService.findByBusinessId(businessId);
        return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toResponseList(customers, encryptionService)));
    }

    @GetMapping("/{id}/packages")
    public ResponseEntity<ApiResponse<List<PackageResponse>>> getCustomerPackages(@PathVariable Long id) {
        List<CustomerPackage> packages = customerService.findActivePackages(id);
        return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toPackageResponseList(packages)));
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
