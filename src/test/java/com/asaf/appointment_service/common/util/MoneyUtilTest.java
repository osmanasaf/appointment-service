package com.asaf.appointment_service.common.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyUtilTest {

    @Test
    void should_convert_690_tl_to_69000_kurus() {
        int result = MoneyUtil.toKurus(new BigDecimal("690.00"));
        assertThat(result).isEqualTo(69000);
    }

    @Test
    void should_convert_1190_tl_to_119000_kurus() {
        int result = MoneyUtil.toKurus(new BigDecimal("1190.00"));
        assertThat(result).isEqualTo(119000);
    }

    @Test
    void should_convert_1990_tl_to_199000_kurus() {
        int result = MoneyUtil.toKurus(new BigDecimal("1990.00"));
        assertThat(result).isEqualTo(199000);
    }

    @Test
    void should_throw_when_amount_is_null() {
        assertThatThrownBy(() -> MoneyUtil.toKurus(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_convert_69000_kurus_to_690_tl() {
        BigDecimal result = MoneyUtil.fromKurus(69000);
        assertThat(result).isEqualByComparingTo(new BigDecimal("690.00"));
    }

    @Test
    void should_roundtrip_correctly() {
        BigDecimal original = new BigDecimal("1190.00");
        int kurus = MoneyUtil.toKurus(original);
        BigDecimal back = MoneyUtil.fromKurus(kurus);
        assertThat(back).isEqualByComparingTo(original);
    }
}
