package com.example.service;

import com.example.dto.OrderEventResponse;
import com.example.dto.OrderItemResponse;
import com.example.dto.OrderResponse;
import com.example.dto.OrderSummaryResponse;
import com.example.dto.PaymentResponse;
import com.example.entity.Customer;
import com.example.entity.CustomerOrder;
import com.example.repository.CustomerOrderRepository;
import com.example.repository.OrderEventRepository;
import com.example.repository.OrderItemRepository;
import com.example.repository.PaymentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderEventRepository eventRepository;
    private final PaymentRepository paymentRepository;

    public OrderService(
            CustomerOrderRepository orderRepository,
            OrderItemRepository itemRepository,
            OrderEventRepository eventRepository,
            PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.eventRepository = eventRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<OrderSummaryResponse> findAll(Customer customer) {
        return orderRepository.findAllByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(order -> new OrderSummaryResponse(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getServiceType(),
                        order.getStatus(),
                        order.getTotalAmount(),
                        order.getCurrency(),
                        order.getCreatedAt()))
                .toList();
    }

    public List<CustomerOrder> findOrders(Customer customer) {
        return orderRepository.findAllByCustomerIdOrderByCreatedAtDesc(customer.getId());
    }

    public OrderResponse findById(Customer customer, UUID orderId) {
        return toResponse(findOwnedOrder(customer, orderId));
    }

    public CustomerOrder findOwnedOrder(Customer customer, UUID orderId) {
        return orderRepository.findByIdAndCustomerId(orderId, customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private OrderResponse toResponse(CustomerOrder order) {
        List<OrderItemResponse> items = itemRepository.findAllByOrderId(order.getId()).stream()
                .map(item -> new OrderItemResponse(item.getId(), item.getName(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        List<OrderEventResponse> events = eventRepository.findAllByOrderIdOrderByOccurredAtAsc(order.getId()).stream()
                .map(event -> new OrderEventResponse(
                        event.getId(), event.getStatus(), event.getDescription(), event.getOccurredAt()))
                .toList();
        List<PaymentResponse> payments = paymentRepository.findAllByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                .map(payment -> new PaymentResponse(
                        payment.getId(),
                        payment.getStatus(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getProviderReference(),
                        payment.getCreatedAt(),
                        payment.getPaidAt()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getServiceType(),
                order.getStatus(),
                order.getLocation(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt(),
                items,
                events,
                payments);
    }
}
