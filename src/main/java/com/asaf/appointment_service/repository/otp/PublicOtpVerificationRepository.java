package com.asaf.appointment_service.repository.otp;

import com.asaf.appointment_service.entity.otp.PublicOtpVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicOtpVerificationRepository extends JpaRepository<PublicOtpVerification, Long> {

    Optional<PublicOtpVerification> findByBusinessIdAndPhoneNormalized(Long businessId, String phoneNormalized);
}
