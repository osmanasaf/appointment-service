package com.asaf.appointment_service.service.business;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;
import com.asaf.appointment_service.dto.business.AddBusinessPhotoRequest;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessPhoto;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.exception.plan.PlanFeatureNotAvailableException;
import com.asaf.appointment_service.exception.plan.PlanLimitExceededException;
import com.asaf.appointment_service.repository.business.BusinessPhotoRepository;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessPhotoServiceTest {

    private static final Long BUSINESS_ID = 1L;

    @Mock
    private BusinessPhotoRepository repository;
    @Mock
    private BusinessService businessService;
    @Mock
    private SubscriptionService subscriptionService;

    private BusinessPhotoService photoService;

    private void setUp() {
        photoService = new BusinessPhotoService(repository, businessService, subscriptionService);
        lenient().when(businessService.findByIdOrThrow(BUSINESS_ID)).thenReturn(mock(Business.class));
    }

    private BusinessPhoto photo(Long id, int sortOrder) {
        BusinessPhoto photo = BusinessPhoto.builder()
            .businessId(BUSINESS_ID).url("https://cdn.example.com/" + id + ".jpg").sortOrder(sortOrder).build();
        ReflectionTestUtils.setField(photo, "id", id);
        return photo;
    }

    @Test
    void should_throwPlanFeatureNotAvailable_when_miniWebsiteDisabled() {
        setUp();
        doThrow(new PlanFeatureNotAvailableException(PlanFeature.MINI_WEBSITE))
            .when(subscriptionService).requireFeature(BUSINESS_ID, PlanFeature.MINI_WEBSITE);

        assertThatThrownBy(() -> photoService.addPhoto(BUSINESS_ID, new AddBusinessPhotoRequest("url", null)))
            .isInstanceOf(PlanFeatureNotAvailableException.class);
    }

    @Test
    void should_addPhotoWithNextSortOrder_when_underLimit() {
        setUp();
        when(repository.countByBusinessId(BUSINESS_ID)).thenReturn(2L);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BusinessPhoto saved = photoService.addPhoto(BUSINESS_ID,
            new AddBusinessPhotoRequest("https://cdn.example.com/3.jpg", "Salon"));

        assertThat(saved.getSortOrder()).isEqualTo(2);
        assertThat(saved.getUrl()).isEqualTo("https://cdn.example.com/3.jpg");
    }

    @Test
    void should_throwPlanLimitExceeded_when_atMaxPhotos() {
        setUp();
        when(repository.countByBusinessId(BUSINESS_ID)).thenReturn(12L);

        assertThatThrownBy(() -> photoService.addPhoto(BUSINESS_ID, new AddBusinessPhotoRequest("url", null)))
            .isInstanceOf(PlanLimitExceededException.class);
    }

    @Test
    void should_returnPhotosOrderedBySortOrder_when_findByBusinessIdCalled() {
        setUp();
        List<BusinessPhoto> photos = List.of(photo(1L, 0), photo(2L, 1));
        when(repository.findByBusinessIdOrderBySortOrderAsc(BUSINESS_ID)).thenReturn(photos);

        List<BusinessPhoto> result = photoService.findByBusinessId(BUSINESS_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    void should_deletePhoto_when_ownedByBusiness() {
        setUp();
        BusinessPhoto photo = photo(5L, 0);
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(photo));

        photoService.deletePhoto(BUSINESS_ID, 5L);

        verify(repository).delete(photo);
    }

    @Test
    void should_throwNotFound_when_photoBelongsToDifferentBusiness() {
        setUp();
        BusinessPhoto photo = photo(5L, 0);
        ReflectionTestUtils.setField(photo, "businessId", 999L);
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(photo));

        assertThatThrownBy(() -> photoService.deletePhoto(BUSINESS_ID, 5L))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
