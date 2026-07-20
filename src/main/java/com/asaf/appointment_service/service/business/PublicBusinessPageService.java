package com.asaf.appointment_service.service.business;

import com.asaf.appointment_service.dto.business.BusinessPhotoResponse;
import com.asaf.appointment_service.dto.business.BusinessResponse;
import com.asaf.appointment_service.dto.business.PublicBusinessPageResponse;
import com.asaf.appointment_service.dto.employee.EmployeeResponse;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.mapper.business.BusinessMapper;
import com.asaf.appointment_service.mapper.business.BusinessPhotoMapper;
import com.asaf.appointment_service.mapper.employee.EmployeeMapper;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import com.asaf.appointment_service.service.review.ReviewService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * `/b/:slug` mini web sitesi için zenginleştirilmiş public sayfa verisini derler.
 * Galeri/çalışan kartları yalnızca MINI_WEBSITE planında dönerdir; SOLO'da temel
 * BusinessResponse ile aynı sade görünüm korunur (boş liste, showBranding=true).
 */
@Service
public class PublicBusinessPageService {

    private final BusinessService businessService;
    private final BusinessPhotoService photoService;
    private final EmployeeService employeeService;
    private final ReviewService reviewService;
    private final SubscriptionService subscriptionService;

    public PublicBusinessPageService(BusinessService businessService, BusinessPhotoService photoService,
                                      EmployeeService employeeService, ReviewService reviewService,
                                      SubscriptionService subscriptionService) {
        this.businessService = businessService;
        this.photoService = photoService;
        this.employeeService = employeeService;
        this.reviewService = reviewService;
        this.subscriptionService = subscriptionService;
    }

    @Transactional(readOnly = true)
    public PublicBusinessPageResponse getPageBySlug(String slug) {
        Business business = businessService.findBySlug(slug);
        Long businessId = business.getId();

        Double averageRating = reviewService.getAverageRating(businessId);
        long reviewCount = reviewService.getPublishedCount(businessId);
        BusinessResponse businessResponse = BusinessMapper.toResponse(business, averageRating, (int) reviewCount);

        boolean miniWebsiteEnabled = subscriptionService.isFeatureEnabled(businessId, PlanFeature.MINI_WEBSITE);
        boolean showBranding = !subscriptionService.isFeatureEnabled(businessId, PlanFeature.REMOVE_BRANDING);

        List<BusinessPhotoResponse> photos = miniWebsiteEnabled
            ? BusinessPhotoMapper.toResponseList(photoService.findByBusinessId(businessId))
            : List.of();
        List<EmployeeResponse> employeeCards = miniWebsiteEnabled
            ? employeeService.findActiveByBusinessId(businessId).stream()
                .filter(Employee::isShowOnPublicPage)
                .map(EmployeeMapper::toResponse)
                .toList()
            : List.of();

        return new PublicBusinessPageResponse(businessResponse, miniWebsiteEnabled, photos, employeeCards, showBranding);
    }
}
