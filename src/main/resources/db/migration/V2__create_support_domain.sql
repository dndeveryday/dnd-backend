CREATE TABLE customer_profiles (
    id UUID PRIMARY KEY,
    auth_subject VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(320),
    display_name VARCHAR(160),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE customer_orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer_profiles (id),
    order_number VARCHAR(64) NOT NULL UNIQUE,
    service_type VARCHAR(80) NOT NULL,
    status VARCHAR(32) NOT NULL,
    location VARCHAR(500),
    total_amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_customer_orders_customer_created
    ON customer_orders (customer_id, created_at DESC);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_orders (id),
    name VARCHAR(160) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0)
);

CREATE TABLE order_events (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_orders (id),
    status VARCHAR(32) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_order_events_order_occurred
    ON order_events (order_id, occurred_at);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_orders (id),
    status VARCHAR(32) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL,
    provider_reference VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_payments_order_created ON payments (order_id, created_at DESC);

CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer_profiles (id),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_conversations_customer_updated
    ON conversations (customer_id, updated_at DESC);

CREATE TABLE conversation_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations (id),
    sender VARCHAR(24) NOT NULL,
    content VARCHAR(5000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_conversation_messages_conversation_created
    ON conversation_messages (conversation_id, created_at);

CREATE TABLE support_tickets (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer_profiles (id),
    order_id UUID REFERENCES customer_orders (id),
    conversation_id UUID REFERENCES conversations (id),
    idempotency_key VARCHAR(128) NOT NULL,
    category VARCHAR(80) NOT NULL,
    summary VARCHAR(240) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_support_tickets_customer_idempotency UNIQUE (customer_id, idempotency_key)
);
CREATE INDEX idx_support_tickets_customer_created
    ON support_tickets (customer_id, created_at DESC);

CREATE TABLE ticket_events (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES support_tickets (id),
    event_type VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    detail VARCHAR(1000) NOT NULL,
    actor_reference VARCHAR(255),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_ticket_events_ticket_occurred
    ON ticket_events (ticket_id, occurred_at);
