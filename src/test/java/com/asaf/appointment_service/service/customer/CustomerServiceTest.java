package com.asaf.appointment_service.service.customer;

import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.exception.customer.CustomerNotFoundException;
import com.asaf.appointment_service.repository.customer.CustomerPackageRepository;
import com.asaf.appointment_service.repository.customer.CustomerRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.encryption.EncryptionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository repository;
    @Mock private BusinessService businessService;
    @Mock private CustomerPackageRepository packageRepository;
    @Mock private EncryptionService encryptionService;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void findByIdOrThrow_whenNotFound_shouldThrowCustomerNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findByIdOrThrow(99L))
            .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void blacklistCustomer_shouldSetBlacklistedFlag() {
        Customer customer = buildCustomer();
        when(repository.findById(1L)).thenReturn(Optional.of(customer));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Customer result = customerService.blacklistCustomer(1L, "spam");

        assertThat(result.isBlacklisted()).isTrue();
    }

    @Test
    void removeFromBlacklist_shouldClearBlacklistedFlag() {
        Customer customer = buildCustomer();
        customer.blacklist("spam");
        when(repository.findById(1L)).thenReturn(Optional.of(customer));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Customer result = customerService.removeFromBlacklist(1L);

        assertThat(result.isBlacklisted()).isFalse();
    }

    @Test
    void recordNoShow_shouldIncrementCountAndSetManualApproval() {
        Customer customer = buildCustomer();
        when(repository.findById(1L)).thenReturn(Optional.of(customer));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        customerService.recordNoShow(1L);

        assertThat(customer.getNoShowCount()).isEqualTo(1);
        assertThat(customer.isNoShowForcesManualApproval()).isTrue();
        verify(repository).save(customer);
    }

    @Test
    void clearNoShowManualApproval_shouldResetFlag() {
        Customer customer = buildCustomer();
        customer.setNoShowForcesManualApproval(true);
        when(repository.findById(1L)).thenReturn(Optional.of(customer));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        customerService.clearNoShowManualApproval(1L);

        assertThat(customer.isNoShowForcesManualApproval()).isFalse();
    }

    private Customer buildCustomer() {
        return Customer.builder()
            .id(1L).businessId(10L).name("Test Müşteri").build();
    }
}
