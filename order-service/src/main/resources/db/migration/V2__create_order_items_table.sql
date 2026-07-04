CREATE TABLE order_items (
     id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     order_id        UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
     menu_item_id    UUID NOT NULL,
     menu_item_name  VARCHAR(255) NOT NULL,
     quantity        INT NOT NULL CHECK (quantity > 0),
     unit_price      DECIMAL(10, 2) NOT NULL,
     created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);