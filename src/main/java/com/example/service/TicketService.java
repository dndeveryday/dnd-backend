package com.example.service;

import com.example.dto.TicketEventResponse;
import com.example.dto.TicketRequest;
import com.example.dto.TicketResponse;
import com.example.entity.Conversation;
import com.example.entity.ConversationStatus;
import com.example.entity.Customer;
import com.example.entity.CustomerOrder;
import com.example.entity.SupportTicket;
import com.example.entity.TicketEvent;
import com.example.entity.TicketStatus;
import com.example.repository.ConversationRepository;
import com.example.repository.CustomerOrderRepository;
import com.example.repository.SupportTicketRepository;
import com.example.repository.TicketEventRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketEventRepository eventRepository;
    private final CustomerOrderRepository orderRepository;
    private final ConversationRepository conversationRepository;

    public TicketService(
            SupportTicketRepository ticketRepository,
            TicketEventRepository eventRepository,
            CustomerOrderRepository orderRepository,
            ConversationRepository conversationRepository) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.conversationRepository = conversationRepository;
    }

    public TicketResponse create(Customer customer, String idempotencyKey, TicketRequest request) {
        if (!StringUtils.hasText(idempotencyKey) || idempotencyKey.length() > 128) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Idempotency-Key must contain 1 to 128 characters");
        }
        return ticketRepository.findByCustomerIdAndIdempotencyKey(customer.getId(), idempotencyKey)
                .map(this::toResponse)
                .orElseGet(() -> createNew(customer, idempotencyKey, request));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> findAll(Customer customer) {
        return ticketRepository.findAllByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse findById(Customer customer, UUID id) {
        SupportTicket ticket = ticketRepository.findByIdAndCustomerId(id, customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        return toResponse(ticket);
    }

    public TicketResponse close(Customer customer, UUID id) {
        SupportTicket ticket = ticketRepository.findByIdAndCustomerId(id, customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (ticket.close()) {
            eventRepository.save(new TicketEvent(
                    ticket, "CLOSED", TicketStatus.CLOSED, "Ticket closed by customer", "customer"));
        }
        return toResponse(ticket);
    }

    private TicketResponse createNew(Customer customer, String idempotencyKey, TicketRequest request) {
        CustomerOrder order = request.orderId() == null
                ? null
                : orderRepository.findByIdAndCustomerId(request.orderId(), customer.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        Conversation conversation = request.conversationId() == null
                ? null
                : conversationRepository.findByIdAndCustomerId(request.conversationId(), customer.getId())
                        .orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        String category = request.category().trim();
        SupportTicket ticket = ticketRepository.save(new SupportTicket(
                customer,
                order,
                conversation,
                idempotencyKey,
                category,
                request.summary().trim(),
                request.description().trim()));
        eventRepository.save(new TicketEvent(
                ticket, "CREATED", TicketStatus.OPEN, "Support ticket created", "customer"));

        if (conversation != null && category.equalsIgnoreCase("human_support")) {
            conversation.setStatus(ConversationStatus.HUMAN);
        }
        return toResponse(ticket);
    }

    private TicketResponse toResponse(SupportTicket ticket) {
        List<TicketEventResponse> events = eventRepository
                .findAllByTicketIdOrderByOccurredAtAsc(ticket.getId()).stream()
                .map(event -> new TicketEventResponse(
                        event.getId(),
                        event.getEventType(),
                        event.getStatus(),
                        event.getDetail(),
                        event.getOccurredAt()))
                .toList();
        return new TicketResponse(
                ticket.getId(),
                ticket.getOrder() == null ? null : ticket.getOrder().getId(),
                ticket.getConversation() == null ? null : ticket.getConversation().getId(),
                ticket.getCategory(),
                ticket.getSummary(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                events);
    }
}
