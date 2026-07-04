CREATE TABLE deliveries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID NOT NULL,
    driver_id           VARCHAR(255),
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    pickup_address      TEXT NOT NULL,
    delivery_address    TEXT NOT NULL,
    pickup_latitude     DOUBLE PRECISION,
    pickup_longitude    DOUBLE PRECISION,
    delivery_latitude   DOUBLE PRECISION,
    delivery_longitude  DOUBLE PRECISION,
    estimated_time      INT,
    actual_time         INT,
    picked_up_at        TIMESTAMP,
    delivered_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_deliveries_order_id ON deliveries(order_id);
CREATE INDEX idx_deliveries_driver_id ON deliveries(driver_id);
CREATE INDEX idx_deliveries_status ON deliveries(status);