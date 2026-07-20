package com.asaf.appointment_service.repository.review;

import com.asaf.appointment_service.entity.review.Review;
import com.asaf.appointment_service.entity.review.ReviewStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByAppointmentId(Long appointmentId);

    Optional<Review> findByAppointmentId(Long appointmentId);

    List<Review> findByBusinessIdAndStatusOrderByCreatedAtDesc(Long businessId, ReviewStatus status, Pageable pageable);

    List<Review> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.businessId = :businessId AND r.status = 'PUBLISHED'")
    Double findAverageRating(@Param("businessId") Long businessId);

    long countByBusinessIdAndStatus(Long businessId, ReviewStatus status);
}
