package com.example.controller;

import com.example.dto.TicketRequest;
import com.example.dto.TicketResponse;
import com.example.entity.Customer;
import com.example.service.CustomerService;
import com.example.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final CustomerService customerService;
    private final TicketService ticketService;

    public TicketController(CustomerService customerService, TicketService ticketService) {
        this.customerService = customerService;
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an idempotent support ticket")
    public TicketResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TicketRequest request) {
        Customer customer = customerService.requireCustomer(jwt);
        return ticketService.create(customer, idempotencyKey, request);
    }

    @GetMapping
    @Operation(summary = "List the authenticated customer's tickets")
    public List<TicketResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return ticketService.findAll(customerService.requireCustomer(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an owned ticket with its event history")
    public TicketResponse findById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ticketService.findById(customerService.requireCustomer(jwt), id);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close one of the authenticated customer's tickets")
    public TicketResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        Customer customer = customerService.requireCustomer(jwt);
        return ticketService.close(customer, id);
    }
}
