package com.example.repository;

import com.example.entity.TicketEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketEventRepository extends JpaRepository<TicketEvent, UUID> {
    List<TicketEvent> findAllByTicketIdOrderByOccurredAtAsc(UUID ticketId);
}
