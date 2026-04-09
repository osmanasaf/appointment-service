package com.asaf.appointment_service.service.business;

import com.asaf.appointment_service.common.service.BaseService;
import com.asaf.appointment_service.common.util.SlugGenerator;
import com.asaf.appointment_service.dto.business.CreateBusinessRequest;
import com.asaf.appointment_service.dto.business.UpdateBusinessRequest;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessWorkingHours;
import com.asaf.appointment_service.exception.business.BusinessNotFoundException;
import com.asaf.appointment_service.mapper.business.BusinessMapper;
import com.asaf.appointment_service.repository.business.BusinessRepository;
import com.asaf.appointment_service.repository.business.BusinessWorkingHoursRepository;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessService extends BaseService<Business, Long, BusinessRepository> {

    private final BusinessWorkingHoursRepository workingHoursRepository;
    private final SlugGenerator slugGenerator;

    public BusinessService(BusinessRepository repository, 
                          BusinessWorkingHoursRepository workingHoursRepository,
                          SlugGenerator slugGenerator) {
        super(repository, "İşletme");
        this.workingHoursRepository = workingHoursRepository;
        this.slugGenerator = slugGenerator;
    }

    @Transactional
    public Business createBusiness(CreateBusinessRequest request) {
        String slug = slugGenerator.generate(request.name(), repository::existsBySlug);
        Business business = BusinessMapper.toEntity(request, slug);
        Business saved = repository.save(business);
        createDefaultWorkingHours(saved.getId());
        return saved;
    }

    @Transactional
    public Business updateBusiness(Long id, UpdateBusinessRequest request) {
        Business business = findByIdOrThrow(id);
        
        business.updateProfile(
            Objects.requireNonNullElse(request.name(), business.getName()),
            Objects.requireNonNullElse(request.email(), business.getEmail()),
            Objects.requireNonNullElse(request.address(), business.getAddress()),
            Objects.requireNonNullElse(request.description(), business.getDescription()),
            Objects.requireNonNullElse(request.logoUrl(), business.getLogoUrl())
        );

        business.updateCategory(request.category());

        business.updateSettings(
            Objects.requireNonNullElse(request.autoConfirm(), business.isAutoConfirm()),
            Objects.requireNonNullElse(request.reminderHoursBefore(), business.getReminderHoursBefore()),
            Objects.requireNonNullElse(request.sameDayBookingAllowed(), business.isSameDayBookingAllowed()),
            Objects.requireNonNullElse(request.maxAdvanceBookingDays(), business.getMaxAdvanceBookingDays()),
            request.maxAppointmentsPerCustomerPerDay(),
            request.whatsappSendPrice()
        );
        
        return repository.save(business);
    }

    @Transactional(readOnly = true)
    public Business findBySlug(String slug) {
        return repository.findBySlug(slug)
            .orElseThrow(() -> new BusinessNotFoundException(slug));
    }

    @Transactional(readOnly = true)
    public List<BusinessWorkingHours> getWorkingHours(Long businessId) {
        findByIdOrThrow(businessId);
        return workingHoursRepository.findByBusinessIdOrderByDayOfWeek(businessId);
    }

    @Transactional(readOnly = true)
    public List<Business> findAllByIds(List<Long> ids) {
        return repository.findAllById(ids);
    }

    @Override
    public Business findByIdOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new BusinessNotFoundException(id));
    }

    private void createDefaultWorkingHours(Long businessId) {
        for (DayOfWeek day : DayOfWeek.values()) {
            BusinessWorkingHours hours = BusinessWorkingHours.defaultHours(businessId, day);
            workingHoursRepository.save(hours);
        }
    }
}
