package com.asaf.appointment_service.service.customer;

import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.common.exception.ValidationException;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.entity.service.PackageTemplate;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.repository.customer.CustomerPackageRepository;
import com.asaf.appointment_service.service.service.PackageTemplateService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.util.List;
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
class PackageServiceTest {

    @Mock private CustomerService customerService;
    @Mock private ServiceDefinitionService serviceDefinitionService;
    @Mock private PackageTemplateService packageTemplateService;
    @Mock private PackageSessionService packageSessionService;
    @Mock private CustomerPackageRepository packageRepository;

    @InjectMocks
    private PackageService packageService;

    @Test
    void deductSession_whenValidPackage_shouldDecreaseRemainingCount() {
        CustomerPackage pkg = buildActivePackage(3);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        packageService.deductSession(1L);

        verify(packageRepository).save(pkg);
    }

    @Test
    void deductSession_whenExpired_shouldThrowBusinessException() {
        CustomerPackage pkg = buildExpiredPackage();
        when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> packageService.deductSession(1L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void deductSession_whenNoRemainingSessions_shouldThrowBusinessException() {
        CustomerPackage pkg = buildActivePackage(0);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> packageService.deductSession(1L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createFromTemplate_whenTemplateWrongBusiness_shouldThrowValidationException() {
        Customer customer = Customer.builder().id(1L).businessId(10L).build();
        PackageTemplate template = PackageTemplate.builder().id(5L).businessId(99L).serviceId(3L).build();

        when(customerService.findByIdOrThrow(1L)).thenReturn(customer);
        when(packageTemplateService.findByIdOrThrow(5L)).thenReturn(template);

        assertThatThrownBy(() -> packageService.createFromTemplate(1L, 5L))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void createFromTemplate_whenValid_shouldCreatePackageAndSessions() {
        Customer customer = Customer.builder().id(1L).businessId(10L).build();
        PackageTemplate template = PackageTemplate.builder()
            .id(5L).businessId(10L).serviceId(3L).name("Test Paket").totalSessions(5).build();
        ServiceDefinition service = ServiceDefinition.builder().id(3L).packageEligible(true).build();
        CustomerPackage savedPkg = buildActivePackage(5);

        when(customerService.findByIdOrThrow(1L)).thenReturn(customer);
        when(packageTemplateService.findByIdOrThrow(5L)).thenReturn(template);
        when(serviceDefinitionService.findByIdOrThrow(3L)).thenReturn(service);
        when(packageRepository.save(any())).thenReturn(savedPkg);

        packageService.createFromTemplate(1L, 5L);

        verify(packageSessionService).createSessionsForPackage(savedPkg);
    }

    @Test
    void expireOverduePackages_shouldMarkExpiredPackages() {
        CustomerPackage pkg1 = buildActivePackage(2);
        CustomerPackage pkg2 = buildActivePackage(1);
        when(packageRepository.findByActiveTrueAndExpiresAtBefore(any())).thenReturn(List.of(pkg1, pkg2));

        packageService.expireOverduePackages();

        verify(packageRepository).save(pkg1);
        verify(packageRepository).save(pkg2);
        assertThat(pkg1.isActive()).isFalse();
        assertThat(pkg2.isActive()).isFalse();
    }

    private CustomerPackage buildActivePackage(int remaining) {
        return CustomerPackage.builder()
            .id(1L).customerId(1L).businessId(10L).serviceId(3L)
            .active(true).totalSessions(remaining + 2).remainingSessions(remaining).build();
    }

    private CustomerPackage buildExpiredPackage() {
        return CustomerPackage.builder()
            .id(1L).customerId(1L).businessId(10L).serviceId(3L)
            .active(false).totalSessions(3).remainingSessions(0)
            .expiresAt(java.time.LocalDateTime.now().minusDays(1)).build();
    }
}
