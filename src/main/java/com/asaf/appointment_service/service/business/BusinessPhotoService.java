package com.asaf.appointment_service.service.business;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;
import com.asaf.appointment_service.dto.business.AddBusinessPhotoRequest;
import com.asaf.appointment_service.entity.business.BusinessPhoto;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.exception.plan.PlanLimitExceededException;
import com.asaf.appointment_service.repository.business.BusinessPhotoRepository;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessPhotoService {

    private static final int MAX_PHOTOS_PER_BUSINESS = 12;

    private final BusinessPhotoRepository repository;
    private final BusinessService businessService;
    private final SubscriptionService subscriptionService;

    public BusinessPhotoService(BusinessPhotoRepository repository, BusinessService businessService,
                                 SubscriptionService subscriptionService) {
        this.repository = repository;
        this.businessService = businessService;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public BusinessPhoto addPhoto(Long businessId, AddBusinessPhotoRequest request) {
        businessService.findByIdOrThrow(businessId);
        subscriptionService.requireFeature(businessId, PlanFeature.MINI_WEBSITE);

        long existingCount = repository.countByBusinessId(businessId);
        if (existingCount >= MAX_PHOTOS_PER_BUSINESS) {
            throw new PlanLimitExceededException("PHOTO_LIMIT_EXCEEDED",
                "En fazla %d fotoğraf ekleyebilirsiniz.".formatted(MAX_PHOTOS_PER_BUSINESS));
        }

        BusinessPhoto photo = BusinessPhoto.builder()
            .businessId(businessId)
            .url(request.url())
            .caption(request.caption())
            .sortOrder((int) existingCount)
            .build();
        return repository.save(photo);
    }

    @Transactional(readOnly = true)
    public List<BusinessPhoto> findByBusinessId(Long businessId) {
        return repository.findByBusinessIdOrderBySortOrderAsc(businessId);
    }

    @Transactional
    public void deletePhoto(Long businessId, Long photoId) {
        BusinessPhoto photo = repository.findById(photoId)
            .orElseThrow(() -> new EntityNotFoundException("Fotoğraf", String.valueOf(photoId)));
        if (!photo.getBusinessId().equals(businessId)) {
            throw new EntityNotFoundException("Fotoğraf", String.valueOf(photoId));
        }
        repository.delete(photo);
    }
}
