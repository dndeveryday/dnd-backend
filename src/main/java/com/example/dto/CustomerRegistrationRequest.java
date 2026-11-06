package com.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRegistrationRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 79) String lastName,
        @NotBlank
        @Pattern(regexp = "^(?:\\+91)?[6-9][0-9]{9}$", message = "must be a valid 10-digit Indian mobile number")
        String phoneNumber,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 128) String password) {
}