CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         VARCHAR(255) NOT NULL,
    restaurant_id   UUID NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    total_amount    DECIMAL(10, 2) NOT NULL,
    delivery_address TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_restaurant_id ON orders (restaurant_id);
CREATE INDEX idx_orders_created_at ON orders (created_at DESC);