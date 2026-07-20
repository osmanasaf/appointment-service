package com.asaf.appointment_service.service.business;

import com.asaf.appointment_service.common.util.SlugGenerator;
import com.asaf.appointment_service.dto.business.UpdateBusinessRequest;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import com.asaf.appointment_service.repository.business.BusinessRepository;
import com.asaf.appointment_service.repository.business.BusinessWorkingHoursRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessServiceGoogleReviewUrlTest {

    @Mock
    private BusinessRepository repository;
    @Mock
    private BusinessWorkingHoursRepository workingHoursRepository;
    @Mock
    private SlugGenerator slugGenerator;

    @InjectMocks
    private BusinessService businessService;

    @Test
    void should_updateGoogleReviewUrl_when_requestProvidesIt() {
        Business business = Business.builder()
            .id(1L).name("Kuaför X").slug("kuafor-x").category(BusinessCategory.OTHER)
            .email("info@kuaforx.com").address("Adres").description("Açıklama").logoUrl("logo.png").build();
        when(repository.findById(1L)).thenReturn(Optional.of(business));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateBusinessRequest request = new UpdateBusinessRequest(
            null, null, null, null, null, null, null, null, null, null, null, null,
            "https://g.page/r/kuafor-x/review", null, null, null, null, null);

        Business updated = businessService.updateBusiness(1L, request);

        assertThat(updated.getGoogleReviewUrl()).isEqualTo("https://g.page/r/kuafor-x/review");
    }

    @Test
    void should_keepExistingGoogleReviewUrl_when_requestOmitsIt() {
        Business business = Business.builder()
            .id(1L).name("Kuaför X").slug("kuafor-x").category(BusinessCategory.OTHER)
            .email("info@kuaforx.com").address("Adres").description("Açıklama").logoUrl("logo.png")
            .googleReviewUrl("https://g.page/existing").build();
        when(repository.findById(1L)).thenReturn(Optional.of(business));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateBusinessRequest request = new UpdateBusinessRequest(
            null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null);

        Business updated = businessService.updateBusiness(1L, request);

        assertThat(updated.getGoogleReviewUrl()).isEqualTo("https://g.page/existing");
    }
}
