package com.example.service;

import com.example.dto.CustomerRegistrationRequest;
import com.example.dto.CustomerResponse;
import com.example.entity.Customer;
import com.example.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerRegistrationService {

    private final CustomerRepository customerRepository;
    private final KeycloakProvisioningService keycloakProvisioningService;
    private final boolean allowUnverifiedPhone;

    public CustomerRegistrationService(
            CustomerRepository customerRepository,
            KeycloakProvisioningService keycloakProvisioningService,
            @Value("${app.registration.allow-unverified-phone:false}") boolean allowUnverifiedPhone) {
        this.customerRepository = customerRepository;
        this.keycloakProvisioningService = keycloakProvisioningService;
        this.allowUnverifiedPhone = allowUnverifiedPhone;
    }

    @Transactional
    public CustomerResponse register(CustomerRegistrationRequest request) {
        if (!allowUnverifiedPhone) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Phone verification is required before registration");
        }
        String phoneNumber = PhoneNumberNormalizer.toE164India(request.phoneNumber());
        if (customerRepository.existsByPhoneNumber(phoneNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this phone number already exists");
        }

        String keycloakUserId = keycloakProvisioningService.createCustomer(request, phoneNumber);
        try {
            Customer customer = customerRepository.saveAndFlush(new Customer(
                    keycloakUserId,
                    request.email().trim(),
                    request.firstName().trim() + " " + request.lastName().trim(),
                    phoneNumber));
            return new CustomerResponse(
                    customer.getId(),
                    customer.getEmail(),
                    customer.getDisplayName(),
                    customer.getPhoneNumber(),
                    customer.getCreatedAt());
        } catch (RuntimeException exception) {
            try {
                keycloakProvisioningService.deleteCustomer(keycloakUserId);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }
}