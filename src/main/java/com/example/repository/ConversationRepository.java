package com.example.repository;

import com.example.entity.Conversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findAllByCustomerIdOrderByUpdatedAtDesc(UUID customerId);
    Optional<Conversation> findByIdAndCustomerId(UUID id, UUID customerId);
}
