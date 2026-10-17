package com.example.controller;

import com.example.dto.CustomerResponse;
import com.example.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated customer's profile")
    public CustomerResponse getCurrentCustomer(@AuthenticationPrincipal Jwt jwt) {
        return customerService.getProfile(jwt);
    }
}
