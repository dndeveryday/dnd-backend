package com.example.controller;

import com.example.dto.CustomerResponse;
import com.example.dto.CustomerRegistrationRequest;
import com.example.service.CustomerRegistrationService;
import com.example.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerRegistrationService registrationService;

    public CustomerController(CustomerService customerService, CustomerRegistrationService registrationService) {
        this.customerService = customerService;
        this.registrationService = registrationService;
    }

    @PostMapping("/registration")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a customer with phone and password")
    public CustomerResponse register(@Valid @RequestBody CustomerRegistrationRequest request) {
        return registrationService.register(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated customer's profile")
    public CustomerResponse getCurrentCustomer(@AuthenticationPrincipal Jwt jwt) {
        return customerService.getProfile(jwt);
    }
}
