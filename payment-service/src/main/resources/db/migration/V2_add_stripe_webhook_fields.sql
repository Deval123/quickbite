-- Ajouter les champs pour le suivi stripe et la deduplication webhook.
ALTER TABLE payments ADD COLUMN stripe_payment_intent_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN stripe_event_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN failure_reason VARCHAR(255);

-- Index pour la recherche par PaymentIntent ID (webhook lookup)
CREATE UNIQUE INDEX idx_payments_stripe_pi ON payments(stripe_payment_intent_id);

-- Index pour la déduplication des webhooks.
CREATE UNIQUE INDEX idx_payments_stripe_event ON payments(stripe_event_id);

