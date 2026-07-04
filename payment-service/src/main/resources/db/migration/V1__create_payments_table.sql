CREATE TABLE payments (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id            UUID NOT NULL,
  user_id             VARCHAR(255) NOT NULL,
  amount              DECIMAL(10, 2) NOT NULL,
  currency            VARCHAR(3) NOT NULL DEFAULT 'EUR',
  status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  stripe_payment_id   VARCHAR(255),
  idempotency_key     VARCHAR(255) NOT NULL UNIQUE,
  created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Idempotency key unique : empêche le double paiement.;
CREATE UNIQUE INDEX idx_payments_idempotency ON payments(idempotency_key);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);