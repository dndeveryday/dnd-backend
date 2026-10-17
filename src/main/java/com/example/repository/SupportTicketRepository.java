package com.example.repository;

import com.example.entity.SupportTicket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
    List<SupportTicket> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    Optional<SupportTicket> findByIdAndCustomerId(UUID id, UUID customerId);
    Optional<SupportTicket> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);
}
