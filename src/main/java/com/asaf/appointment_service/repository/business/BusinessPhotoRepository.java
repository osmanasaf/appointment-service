package com.asaf.appointment_service.repository.business;

import com.asaf.appointment_service.entity.business.BusinessPhoto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessPhotoRepository extends JpaRepository<BusinessPhoto, Long> {

    List<BusinessPhoto> findByBusinessIdOrderBySortOrderAsc(Long businessId);

    long countByBusinessId(Long businessId);
}
