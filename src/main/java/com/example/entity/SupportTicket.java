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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_tickets", uniqueConstraints = @UniqueConstraint(
        name = "uq_support_tickets_customer_idempotency",
        columnNames = {"customer_id", "idempotency_key"}))
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 240)
    private String summary;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SupportTicket() {
    }

    public SupportTicket(Customer customer, CustomerOrder order, Conversation conversation,
                         String idempotencyKey, String category, String summary, String description) {
        this.customer = customer;
        this.order = order;
        this.conversation = conversation;
        this.idempotencyKey = idempotencyKey;
        this.category = category;
        this.summary = summary;
        this.description = description;
        this.status = TicketStatus.OPEN;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Customer getCustomer() { return customer; }
    public CustomerOrder getOrder() { return order; }
    public Conversation getConversation() { return conversation; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getCategory() { return category; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public TicketStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean close() {
        if (status == TicketStatus.CLOSED) {
            return false;
        }
        status = TicketStatus.CLOSED;
        return true;
    }
}
