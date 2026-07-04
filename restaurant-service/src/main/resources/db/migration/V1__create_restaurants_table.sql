CREATE TABLE restaurants (
     id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     owner_id        VARCHAR(255) NOT NULL,
     name            VARCHAR(255) NOT NULL,
     description     TEXT,
     cuisine_type    VARCHAR(100),
     address         TEXT NOT NULL,
     phone           VARCHAR(20),
     latitude        DOUBLE PRECISION,
     longitude       DOUBLE PRECISION,
     opening_time    TIME,
     closing_time    TIME,
     active          BOOLEAN NOT NULL DEFAULT true,
     avg_rating      DECIMAL(2, 1) DEFAULT 0.0,
     created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
     updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_restaurants_owner_id ON restaurants(owner_id);
CREATE INDEX idx_restaurants_cuisine ON restaurants(cuisine_type);
CREATE INDEX idx_restaurants_active ON restaurants(active);