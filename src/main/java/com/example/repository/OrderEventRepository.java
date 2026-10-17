package com.example.repository;

import com.example.entity.OrderEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventRepository extends JpaRepository<OrderEvent, UUID> {
    List<OrderEvent> findAllByOrderIdOrderByOccurredAtAsc(UUID orderId);
}
