package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.example.entity.CustomerOrder;
import com.example.repository.CustomerOrderRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

class OperationsOrderServiceTest {

    private CustomerOrderRepository orderRepository;
    private OperationsOrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(CustomerOrderRepository.class);
        orderService = new OperationsOrderService(orderRepository);
    }

    @Test
    void trimsTextFiltersAndPassesAllFiltersToTheRepository() {
        Instant createdFrom = Instant.parse("2026-01-01T00:00:00Z");
        Instant createdTo = Instant.parse("2026-01-31T23:59:59Z");
        UUID customerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 25);
        when(orderRepository.findAll(anySpecification(), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Page<?> result = orderService.searchOrders(
            " Food ",
            com.example.entity.OrderStatus.DELIVERED,
            createdFrom,
            createdTo,
            customerId,
            " ORD-10 ",
            pageable);

        assertThat(result).isEmpty();
        verify(orderRepository).findAll(anySpecification(), eq(pageable));
    }

    @Test
    void rejectsAnInvertedDateRangeBeforeQuerying() {
        Pageable pageable = PageRequest.of(0, 25);

        assertThatThrownBy(() -> orderService.searchOrders(
                null,
                null,
                Instant.parse("2026-02-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                null,
                pageable))
                .isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(orderRepository);
    }

    @SuppressWarnings("unchecked")
    private Specification<CustomerOrder> anySpecification() {
        return (Specification<CustomerOrder>) any(Specification.class);
    }
}