package com.asaf.appointment_service.service.customer;

import com.asaf.appointment_service.common.constants.PackageErrorMessages;
import com.asaf.appointment_service.common.service.BusinessAwareService;
import com.asaf.appointment_service.common.util.PhoneNumberUtil;
import com.asaf.appointment_service.dto.customer.CreateCustomerRequest;
import com.asaf.appointment_service.dto.customer.UpdateCustomerRequest;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.exception.customer.CustomerNotFoundException;
import com.asaf.appointment_service.mapper.customer.CustomerMapper;
import com.asaf.appointment_service.repository.customer.CustomerPackageRepository;
import com.asaf.appointment_service.repository.customer.CustomerRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.encryption.EncryptionService;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService extends BusinessAwareService<Customer, Long, CustomerRepository> {

    private static final int MIN_NO_SHOW_RISK_THRESHOLD = 2;

    private final CustomerPackageRepository packageRepository;
    private final EncryptionService encryptionService;

    public CustomerService(CustomerRepository repository,
                          BusinessService businessService,
                          CustomerPackageRepository packageRepository,
                          EncryptionService encryptionService) {
        super(repository, "Müşteri", businessService);
        this.packageRepository = packageRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public Customer createCustomer(Long businessId, CreateCustomerRequest request) {
        validateBusinessExists(businessId);
        Customer customer = CustomerMapper.toEntity(businessId, request);
        if (request.nationalId() != null && !request.nationalId().isBlank()) {
            customer.setTcEncrypted(encryptionService.encrypt(request.nationalId()));
        }
        return repository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = findByIdOrThrow(id);
        String tcEncrypted = customer.getTcEncrypted();
        if (request.nationalId() != null) {
            tcEncrypted = request.nationalId().isBlank()
                ? null
                : encryptionService.encrypt(request.nationalId());
        }
        customer.updateProfile(
            request.name(),
            request.email(),
            request.notes(),
            request.address(),
            tcEncrypted
        );
        return repository.save(customer);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findByBusinessIdAndPhoneOptional(Long businessId, String phoneCountryCode, String phoneNumber) {
        validateBusinessExists(businessId);
        String normalized = PhoneNumberUtil.normalizeDigitsOnly(phoneNumber);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return repository.findByBusinessIdAndPhoneNumber(businessId, normalized);
    }

    @Transactional
    public Customer findOrCreateByPhone(Long businessId, String name, String phoneCountryCode, String phoneNumber) {
        String normalized = PhoneNumberUtil.normalizeDigitsOnly(phoneNumber);
        Optional<Customer> existing = repository.findByBusinessIdAndPhoneNumber(businessId, normalized);
        if (existing.isPresent()) {
            return existing.get();
        }

        Customer customer = Customer.builder()
            .businessId(businessId)
            .name(name)
            .phone(phoneCountryCode != null ? phoneCountryCode : "+90", normalized)
            .build();
        return repository.save(customer);
    }

    @Transactional(readOnly = true)
    public List<Customer> findByBusinessId(Long businessId) {
        validateBusinessExists(businessId);
        return repository.findByBusinessIdOrderByNameAsc(businessId);
    }

    @Transactional(readOnly = true)
    public Page<Customer> findByBusinessIdPaged(Long businessId, Pageable pageable) {
        validateBusinessExists(businessId);
        return repository.findByBusinessIdOrderByNameAsc(businessId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Customer> findRiskyCustomers(Long businessId) {
        validateBusinessExists(businessId);
        return repository.findByBusinessIdAndNoShowCountGreaterThanEqual(businessId, MIN_NO_SHOW_RISK_THRESHOLD);
    }

    @Transactional(readOnly = true)
    public List<CustomerPackage> findActivePackages(Long customerId) {
        findByIdOrThrow(customerId);
        return packageRepository.findByCustomerIdAndActiveTrue(customerId);
    }

    @Transactional
    public void recordNoShow(Long customerId) {
        Customer customer = findByIdOrThrow(customerId);
        customer.incrementNoShow();
        customer.setNoShowForcesManualApproval(true);
        repository.save(customer);
    }

    @Transactional
    public void clearNoShowManualApproval(Long customerId) {
        Customer customer = findByIdOrThrow(customerId);
        customer.setNoShowForcesManualApproval(false);
        repository.save(customer);
    }

    @Transactional
    public Customer blacklistCustomer(Long customerId, String reason) {
        Customer customer = findByIdOrThrow(customerId);
        customer.blacklist(reason);
        return repository.save(customer);
    }

    @Transactional
    public Customer removeFromBlacklist(Long customerId) {
        Customer customer = findByIdOrThrow(customerId);
        customer.removeFromBlacklist();
        return repository.save(customer);
    }

    @Override
    public Customer findByIdOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Customer> findAllByIds(List<Long> ids) {
        return repository.findAllById(ids);
    }
}
