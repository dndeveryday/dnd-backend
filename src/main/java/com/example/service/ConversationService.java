package com.example.service;

import com.example.dto.ConversationResponse;
import com.example.dto.MessageResponse;
import com.example.entity.Conversation;
import com.example.entity.ConversationMessage;
import com.example.entity.ConversationStatus;
import com.example.entity.Customer;
import com.example.entity.MessageSender;
import com.example.repository.ConversationMessageRepository;
import com.example.repository.ConversationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final AssistantService assistantService;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            AssistantService assistantService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.assistantService = assistantService;
    }

    public ConversationResponse create(Customer customer) {
        Conversation conversation = conversationRepository.save(new Conversation(customer));
        return toResponse(conversation, List.of());
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> findAll(Customer customer) {
        return conversationRepository.findAllByCustomerIdOrderByUpdatedAtDesc(customer.getId()).stream()
                .map(conversation -> toResponse(conversation, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse findById(Customer customer, UUID id) {
        Conversation conversation = findOwned(customer, id);
        return toResponse(conversation, findMessages(conversation));
    }

    public ConversationResponse sendMessage(Customer customer, UUID id, String content) {
        Conversation conversation = findOwned(customer, id);
        String trimmedContent = content.trim();
        messageRepository.save(new ConversationMessage(conversation, MessageSender.CUSTOMER, trimmedContent));

        if (conversation.getStatus() == ConversationStatus.BOT) {
            String reply = assistantService.reply(customer, trimmedContent);
            messageRepository.save(new ConversationMessage(conversation, MessageSender.ASSISTANT, reply));
        }

        return toResponse(conversation, findMessages(conversation));
    }

    private Conversation findOwned(Customer customer, UUID id) {
        return conversationRepository.findByIdAndCustomerId(id, customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private List<MessageResponse> findMessages(Conversation conversation) {
        return messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(message -> new MessageResponse(
                        message.getId(), message.getSender(), message.getContent(), message.getCreatedAt()))
                .toList();
    }

    private ConversationResponse toResponse(Conversation conversation, List<MessageResponse> messages) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getStatus(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages);
    }
}
