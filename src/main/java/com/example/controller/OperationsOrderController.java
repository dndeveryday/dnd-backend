package com.example.controller;

import com.example.dto.OperationsOrderResponse;
import com.example.entity.OrderStatus;
import com.example.service.OperationsOrderService;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class OperationsOrderController {

    private static final int MAX_PAGE_INDEX = 10_000;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "createdAt", "updatedAt", "orderNumber", "serviceType", "status", "totalAmount");

    private final OperationsOrderService orderService;

    public OperationsOrderController(OperationsOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Search orders for authorized operations staff")
    public Page<OperationsOrderResponse> searchOrders(
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) List<String> sort) {
        if (page < 0 || page > MAX_PAGE_INDEX) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "page must be between 0 and " + MAX_PAGE_INDEX);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "size must be between 1 and " + MAX_PAGE_SIZE);
        }

        Sort sortOrder = parseSort(sort);
        PageRequest pageable = PageRequest.of(page, size, sortOrder);
        return orderService.searchOrders(serviceType, status, createdFrom, createdTo, customerId, orderNumber, pageable);
    }

    private Sort parseSort(List<String> sortParameters) {
        if (sortParameters == null || sortParameters.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String sortParameter : sortParameters) {
            String[] parts = sortParameter.split(",", -1);
            String field = parts[0].trim();
            if (parts.length > 2 || !SORTABLE_FIELDS.contains(field)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "sort must use an allowed field: " + String.join(", ", SORTABLE_FIELDS));
            }

            Sort.Direction direction = Sort.Direction.ASC;
            if (parts.length == 2) {
                try {
                    direction = Sort.Direction.fromString(parts[1].trim());
                } catch (IllegalArgumentException exception) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "sort direction must be asc or desc");
                }
            }
            orders.add(new Sort.Order(direction, field));
        }
        return Sort.by(orders);
    }
}