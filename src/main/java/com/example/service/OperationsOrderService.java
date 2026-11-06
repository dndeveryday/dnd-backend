package com.example.service;

import com.example.dto.OperationsOrderResponse;
import com.example.entity.CustomerOrder;
import com.example.entity.OrderStatus;
import com.example.repository.CustomerOrderRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class OperationsOrderService {

    private final CustomerOrderRepository orderRepository;

    public OperationsOrderService(CustomerOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Page<OperationsOrderResponse> searchOrders(
            String serviceType,
            OrderStatus status,
            Instant createdFrom,
            Instant createdTo,
            UUID customerId,
            String orderNumber,
            Pageable pageable) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "createdFrom must not be after createdTo");
        }

        String normalizedServiceType = normalize(serviceType);
        String orderNumberPattern = containsPattern(orderNumber);
        Specification<CustomerOrder> filters = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (normalizedServiceType != null) {
                predicates.add(builder.equal(builder.lower(root.get("serviceType")), normalizedServiceType));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (createdFrom != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            if (customerId != null) {
                predicates.add(builder.equal(root.get("customer").get("id"), customerId));
            }
            if (orderNumberPattern != null) {
                predicates.add(builder.like(builder.lower(root.get("orderNumber")), orderNumberPattern));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        return orderRepository.findAll(filters, pageable).map(this::toResponse);
    }

    private OperationsOrderResponse toResponse(CustomerOrder order) {
        return new OperationsOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getServiceType(),
                order.getStatus(),
                order.getCustomer().getId(),
                order.getCustomer().getDisplayName(),
                order.getLocation(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String containsPattern(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : "%" + normalized + "%";
    }
}