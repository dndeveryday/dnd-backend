package com.example.repository;

import com.example.entity.CustomerOrder;
import com.example.entity.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID>, JpaSpecificationExecutor<CustomerOrder> {
    List<CustomerOrder> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    Optional<CustomerOrder> findByIdAndCustomerId(UUID id, UUID customerId);
    Optional<CustomerOrder> findByOrderNumberAndCustomerId(String orderNumber, UUID customerId);
}
