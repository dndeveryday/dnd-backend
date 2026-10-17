package com.example.controller;

import com.example.dto.OrderResponse;
import com.example.dto.OrderSummaryResponse;
import com.example.entity.Customer;
import com.example.service.CustomerService;
import com.example.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CustomerService customerService;
    private final OrderService orderService;

    public OrderController(CustomerService customerService, OrderService orderService) {
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List the authenticated customer's orders")
    public List<OrderSummaryResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        Customer customer = customerService.requireCustomer(jwt);
        return orderService.findAll(customer);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an owned order with items, events, and payment status")
    public OrderResponse findById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        Customer customer = customerService.requireCustomer(jwt);
        return orderService.findById(customer, id);
    }
}
