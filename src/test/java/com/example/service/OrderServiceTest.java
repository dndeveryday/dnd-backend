package com.example.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.entity.Customer;
import com.example.repository.CustomerOrderRepository;
import com.example.repository.OrderEventRepository;
import com.example.repository.OrderItemRepository;
import com.example.repository.PaymentRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class OrderServiceTest {

    private CustomerOrderRepository orderRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(CustomerOrderRepository.class);
        orderService = new OrderService(
                orderRepository,
                mock(OrderItemRepository.class),
                mock(OrderEventRepository.class),
                mock(PaymentRepository.class));
    }

    @Test
    void doesNotReturnAnOrderThatDoesNotBelongToTheAuthenticatedCustomer() throws ReflectiveOperationException {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Customer customer = new Customer("test-subject", null, null);
        Field id = Customer.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(customer, customerId);
        when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(customer, orderId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(orderRepository).findByIdAndCustomerId(orderId, customerId);
    }
}
