package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_events")
public class TicketEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketStatus status;

    @Column(nullable = false, length = 1000)
    private String detail;

    @Column(name = "actor_reference", length = 255)
    private String actorReference;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected TicketEvent() {
    }

    public TicketEvent(SupportTicket ticket, String eventType, TicketStatus status,
                       String detail, String actorReference) {
        this.ticket = ticket;
        this.eventType = eventType;
        this.status = status;
        this.detail = detail;
        this.actorReference = actorReference;
        this.occurredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public SupportTicket getTicket() { return ticket; }
    public String getEventType() { return eventType; }
    public TicketStatus getStatus() { return status; }
    public String getDetail() { return detail; }
    public String getActorReference() { return actorReference; }
    public Instant getOccurredAt() { return occurredAt; }
}
