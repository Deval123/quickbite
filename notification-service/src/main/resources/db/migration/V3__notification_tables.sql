-- V3__notification_tables.sql
-- Video 13 : Notifications multi-canal

-- Table de log des notifications envoyees
CREATE TABLE notification_log (
    id              BIGSERIAL PRIMARY KEY,
    order_id        VARCHAR(50) NOT NULL,
    user_id         VARCHAR(50) NOT NULL,
    channel         VARCHAR(20) NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    error_message   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_log_order ON notification_log(order_id);
CREATE INDEX idx_notif_log_user ON notification_log(user_id);
CREATE INDEX idx_notif_log_status ON notification_log(status);

-- Table des preferences de notification par utilisateur
CREATE TABLE user_notification_preference (
    id              BIGSERIAL PRIMARY KEY,
    user_id         VARCHAR(50) NOT NULL UNIQUE,
    push_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    quiet_start     TIME DEFAULT '22:00',
    quiet_end       TIME DEFAULT '08:00',
    fcm_token       VARCHAR(500),
    phone_number    VARCHAR(20),
    email           VARCHAR(255),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pref_user ON user_notification_preference(user_id);
