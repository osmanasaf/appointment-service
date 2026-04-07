package com.asaf.appointment_service.common.service;

import com.asaf.appointment_service.service.business.BusinessService;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Base service for entities that belong to a Business.
 * Provides automatic business existence validation.
 * 
 * DRY Pattern: Centralizes businessService.findByIdOrThrow() calls.
 */
public abstract class BusinessAwareService<T, ID, R extends JpaRepository<T, ID>> 
    extends BaseService<T, ID, R> {

    protected final BusinessService businessService;

    protected BusinessAwareService(R repository, String entityName, BusinessService businessService) {
        super(repository, entityName);
        this.businessService = businessService;
    }

    /**
     * Validates that the business exists before performing operations.
     * Throws BusinessNotFoundException if not found.
     */
    protected void validateBusinessExists(Long businessId) {
        businessService.findByIdOrThrow(businessId);
    }
}
