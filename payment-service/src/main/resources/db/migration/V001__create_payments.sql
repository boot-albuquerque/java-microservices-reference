CREATE TABLE payments (
    id               UUID         NOT NULL PRIMARY KEY,
    amount           NUMERIC(15, 2) NOT NULL,
    currency         VARCHAR(3)   NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    idempotency_key  UUID         NOT NULL UNIQUE,
    payer_id         UUID         NOT NULL,
    payee_id         UUID         NOT NULL,
    external_reference VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_payments_payer_id ON payments (payer_id);
CREATE INDEX idx_payments_status   ON payments (status);
