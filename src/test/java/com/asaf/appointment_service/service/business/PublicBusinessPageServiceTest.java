package com.asaf.appointment_service.service.business;

import com.asaf.appointment_service.dto.business.PublicBusinessPageResponse;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import com.asaf.appointment_service.entity.business.BusinessPhoto;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import com.asaf.appointment_service.service.review.ReviewService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicBusinessPageServiceTest {

    private static final String SLUG = "kuafor-x";
    private static final Long BUSINESS_ID = 1L;

    @Mock
    private BusinessService businessService;
    @Mock
    private BusinessPhotoService photoService;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private ReviewService reviewService;
    @Mock
    private SubscriptionService subscriptionService;

    private PublicBusinessPageService pageService;

    private void setUp() {
        pageService = new PublicBusinessPageService(
            businessService, photoService, employeeService, reviewService, subscriptionService);
    }

    private Business business() {
        Business business = Business.builder()
            .id(BUSINESS_ID).name("Kuaför X").slug(SLUG).category(BusinessCategory.OTHER).build();
        return business;
    }

    private Employee employee(Long id, boolean showOnPublicPage) {
        Employee employee = Employee.builder()
            .id(id).businessId(BUSINESS_ID).name("Ayşe").showOnPublicPage(showOnPublicPage).build();
        return employee;
    }

    @Test
    void should_returnEmptyGalleryAndEmployees_when_miniWebsiteDisabled() {
        setUp();
        when(businessService.findBySlug(SLUG)).thenReturn(business());
        lenient().when(reviewService.getAverageRating(BUSINESS_ID)).thenReturn(null);
        lenient().when(reviewService.getPublishedCount(BUSINESS_ID)).thenReturn(0L);
        when(subscriptionService.isFeatureEnabled(BUSINESS_ID, PlanFeature.MINI_WEBSITE)).thenReturn(false);
        when(subscriptionService.isFeatureEnabled(BUSINESS_ID, PlanFeature.REMOVE_BRANDING)).thenReturn(false);

        PublicBusinessPageResponse response = pageService.getPageBySlug(SLUG);

        assertThat(response.miniWebsiteEnabled()).isFalse();
        assertThat(response.photos()).isEmpty();
        assertThat(response.employeeCards()).isEmpty();
        assertThat(response.showBranding()).isTrue();
        verify(photoService, never()).findByBusinessId(BUSINESS_ID);
        verify(employeeService, never()).findActiveByBusinessId(BUSINESS_ID);
    }

    @Test
    void should_returnGalleryAndVisibleEmployeesOnly_when_miniWebsiteEnabled() {
        setUp();
        when(businessService.findBySlug(SLUG)).thenReturn(business());
        lenient().when(reviewService.getAverageRating(BUSINESS_ID)).thenReturn(4.8);
        lenient().when(reviewService.getPublishedCount(BUSINESS_ID)).thenReturn(10L);
        when(subscriptionService.isFeatureEnabled(BUSINESS_ID, PlanFeature.MINI_WEBSITE)).thenReturn(true);
        when(subscriptionService.isFeatureEnabled(BUSINESS_ID, PlanFeature.REMOVE_BRANDING)).thenReturn(true);

        BusinessPhoto photo = BusinessPhoto.builder()
            .businessId(BUSINESS_ID).url("https://cdn.example.com/1.jpg").sortOrder(0).build();
        ReflectionTestUtils.setField(photo, "id", 1L);
        when(photoService.findByBusinessId(BUSINESS_ID)).thenReturn(List.of(photo));

        when(employeeService.findActiveByBusinessId(BUSINESS_ID))
            .thenReturn(List.of(employee(2L, true), employee(3L, false)));

        PublicBusinessPageResponse response = pageService.getPageBySlug(SLUG);

        assertThat(response.miniWebsiteEnabled()).isTrue();
        assertThat(response.photos()).hasSize(1);
        assertThat(response.employeeCards()).hasSize(1);
        assertThat(response.employeeCards().get(0).id()).isEqualTo(2L);
        assertThat(response.showBranding()).isFalse();
        assertThat(response.business().averageRating()).isEqualTo(4.8);
        assertThat(response.business().reviewCount()).isEqualTo(10);
    }
}
