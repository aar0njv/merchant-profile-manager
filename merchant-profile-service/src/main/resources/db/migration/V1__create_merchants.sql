CREATE TABLE merchants (
    id UUID NOT NULL,
    legal_name VARCHAR(200) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    business_type VARCHAR(30) NOT NULL,
    mcc  VARCHAR(4) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(20)  NOT NULL,
    address_line1 VARCHAR(200) NOT NULL,
    address_line2 VARCHAR(200),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(10)  NOT NULL,
    country VARCHAR(2) NOT NULL,
    status VARCHAR(20)NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_merchants PRIMARY KEY (id),
    CONSTRAINT uq_merchants_email UNIQUE (email)
);

CREATE INDEX idx_merchants_status_created_id
    ON merchants (status, created_at DESC, id ASC);

CREATE INDEX idx_merchants_created_id
    ON merchants (created_at DESC, id ASC);