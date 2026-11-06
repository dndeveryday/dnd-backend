package com.example.service;

public final class PhoneNumberNormalizer {

    private PhoneNumberNormalizer() {
    }

    public static String toE164India(String phoneNumber) {
        String digits = phoneNumber.replaceAll("\\D", "");
        return digits.startsWith("91") && digits.length() == 12 ? "+" + digits : "+91" + digits;
    }
}