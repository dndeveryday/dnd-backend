package com.example.repository;

import com.example.entity.CustomerOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
    List<CustomerOrder> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    Optional<CustomerOrder> findByIdAndCustomerId(UUID id, UUID customerId);
    Optional<CustomerOrder> findByOrderNumberAndCustomerId(String orderNumber, UUID customerId);
}
