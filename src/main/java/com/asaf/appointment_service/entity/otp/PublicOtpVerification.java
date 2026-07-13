package com.asaf.appointment_service.entity.otp;

import com.asaf.appointment_service.common.entity.BaseEntity;
import com.asaf.appointment_service.entity.common.OtpCredential;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "public_otp_verification",
    uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "phone_normalized"})
)
public class PublicOtpVerification extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "phone_normalized", nullable = false, length = 20)
    private String phoneNormalized;

    @Embedded
    private OtpCredential otpCredential = new OtpCredential();

    protected PublicOtpVerification() {}

    public PublicOtpVerification(Long businessId, String phoneNormalized) {
        this.businessId = businessId;
        this.phoneNormalized = phoneNormalized;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getPhoneNormalized() {
        return phoneNormalized;
    }

    public OtpCredential getOtpCredential() {
        return otpCredential;
    }
}
