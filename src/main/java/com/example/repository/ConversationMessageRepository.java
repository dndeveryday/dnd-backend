package com.example.repository;

import com.example.entity.ConversationMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
    List<ConversationMessage> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
