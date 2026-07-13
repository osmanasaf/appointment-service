package com.asaf.appointment_service.integration.paytr;

import com.asaf.appointment_service.integration.paytr.config.PayTRProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PayTRHashServiceTest {

    private static final String MERCHANT_ID = "123456";
    private static final String MERCHANT_KEY = "testMerchantKey";
    private static final String MERCHANT_SALT = "testMerchantSalt";

    private PayTRHashService hashService;

    @BeforeEach
    void setUp() {
        PayTRProperties props = new PayTRProperties(
                MERCHANT_ID, MERCHANT_KEY, MERCHANT_SALT,
                true, "https://www.paytr.com", false, null
        );
        hashService = new PayTRHashService(props);
    }

    @Test
    void should_return_non_empty_hash_when_computeIFrameHash_called() {
        String hash = hashService.computeIFrameHash(
                "127.0.0.1", "TEST-OID-001", "test@test.com",
                69000, "card", 0, "TL", 1, 0
        );

        assertThat(hash).isNotBlank();
    }

    @Test
    void should_return_same_hash_for_same_inputs() {
        String hash1 = hashService.computeIFrameHash(
                "127.0.0.1", "TEST-OID-001", "test@test.com",
                69000, "card", 0, "TL", 1, 0
        );
        String hash2 = hashService.computeIFrameHash(
                "127.0.0.1", "TEST-OID-001", "test@test.com",
                69000, "card", 0, "TL", 1, 0
        );

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void should_return_different_hashes_for_different_amounts() {
        String hash1 = hashService.computeIFrameHash(
                "127.0.0.1", "TEST-OID-001", "test@test.com",
                69000, "card", 0, "TL", 1, 0
        );
        String hash2 = hashService.computeIFrameHash(
                "127.0.0.1", "TEST-OID-001", "test@test.com",
                119000, "card", 0, "TL", 1, 0
        );

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void should_return_true_when_verifyCallbackHash_with_matching_hash() {
        String merchantOid = "TEST-OID-001";
        String status = "success";
        String totalAmount = "69000";

        String validHash = hashService.computeHmacBase64ForTest(
                merchantOid + MERCHANT_SALT + status + totalAmount, MERCHANT_KEY
        );

        boolean result = hashService.verifyCallbackHash(merchantOid, status, totalAmount, validHash);

        assertThat(result).isTrue();
    }

    @Test
    void should_return_false_when_verifyCallbackHash_with_wrong_hash() {
        boolean result = hashService.verifyCallbackHash(
                "TEST-OID-001", "success", "69000", "invalid-hash"
        );

        assertThat(result).isFalse();
    }
}
