package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNumberNormalizerTest {

    @Test
    void normalizesTenDigitIndianNumbers() {
        assertThat(PhoneNumberNormalizer.toE164India("9000000001")).isEqualTo("+919000000001");
        assertThat(PhoneNumberNormalizer.toE164India("+919000000001")).isEqualTo("+919000000001");
    }
}