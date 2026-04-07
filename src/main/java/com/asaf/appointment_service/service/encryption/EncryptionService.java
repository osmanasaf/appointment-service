package com.asaf.appointment_service.service.encryption;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AES-GCM encryption for sensitive fields (e.g. national ID).
 * Stored value is Base64(IV + ciphertext). Key from app.encryption.secret-key (32 bytes for AES-256).
 */
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_LENGTH_BYTES = 32;

    private final byte[] keyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(@Value("${app.encryption.secret-key:ChangeMeInProduction32Bytes!!}") String secretKey) {
        byte[] raw = secretKey.getBytes(StandardCharsets.UTF_8);
        if (raw.length != KEY_LENGTH_BYTES) {
            this.keyBytes = new byte[KEY_LENGTH_BYTES];
            int toCopy = Math.min(raw.length, KEY_LENGTH_BYTES);
            System.arraycopy(raw, 0, keyBytes, 0, toCopy);
            if (toCopy < KEY_LENGTH_BYTES) {
                for (int i = toCopy; i < KEY_LENGTH_BYTES; i++) {
                    keyBytes[i] = (byte) 0;
                }
            }
        } else {
            this.keyBytes = raw;
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);
            if (combined.length < GCM_IV_LENGTH) {
                return null;
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns masked value for display (e.g. *******3456 for last 4 digits).
     * Returns null if encrypted value is null or decryption fails.
     */
    public String maskLastFour(String encryptedBase64) {
        String decrypted = decrypt(encryptedBase64);
        if (decrypted == null || decrypted.length() < 4) {
            return null;
        }
        String last4 = decrypted.substring(decrypted.length() - 4);
        return "*******" + last4;
    }
}
