CREATE TABLE users (
   id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   username    VARCHAR(100) NOT NULL UNIQUE,
   email       VARCHAR(255) NOT NULL UNIQUE,
   first_name  VARCHAR(100),
   last_name   VARCHAR(100),
   phone       VARCHAR(20),
   role        VARCHAR(50) NOT NULL DEFAULT 'CLIENT',
   keycloak_id VARCHAR(255) UNIQUE,
   active      BOOLEAN NOT NULL DEFAULT true,
   created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
   updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_keycloak_id ON users(keycloak_id);