package com.example.controller;

import com.example.dto.ConversationResponse;
import com.example.dto.MessageRequest;
import com.example.entity.Customer;
import com.example.service.ConversationService;
import com.example.service.CustomerService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final CustomerService customerService;
    private final ConversationService conversationService;

    public ConversationController(CustomerService customerService, ConversationService conversationService) {
        this.customerService = customerService;
        this.conversationService = conversationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a support conversation")
    public ConversationResponse create(@AuthenticationPrincipal Jwt jwt) {
        return conversationService.create(customerService.requireCustomer(jwt));
    }

    @GetMapping
    @Operation(summary = "List the authenticated customer's conversations")
    public List<ConversationResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return conversationService.findAll(customerService.requireCustomer(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an owned conversation with its messages")
    public ConversationResponse findById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return conversationService.findById(customerService.requireCustomer(jwt), id);
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Send a message to the deterministic assistant")
    public ConversationResponse sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody MessageRequest request) {
        Customer customer = customerService.requireCustomer(jwt);
        return conversationService.sendMessage(customer, id, request.content());
    }
}
