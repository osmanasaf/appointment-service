package com.asaf.appointment_service.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtil {

    private static final int KURUS_SCALE = 2;
    private static final int KURUS_MULTIPLIER = 100;

    private MoneyUtil() {}

    /**
     * TL tutarını PayTR'nin beklediği kuruş cinsinden integer'a çevirir.
     * Örn: 690.00 TL → 69000
     */
    public static int toKurus(BigDecimal tlAmount) {
        if (tlAmount == null) {
            throw new IllegalArgumentException("Tutar null olamaz");
        }
        return tlAmount.setScale(KURUS_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(KURUS_MULTIPLIER))
                .intValueExact();
    }

    /**
     * Kuruş cinsinden integer'ı TL BigDecimal'e çevirir.
     * Örn: 69000 → 690.00
     */
    public static BigDecimal fromKurus(int kurusAmount) {
        return BigDecimal.valueOf(kurusAmount)
                .divide(BigDecimal.valueOf(KURUS_MULTIPLIER), KURUS_SCALE, RoundingMode.HALF_UP);
    }
}
