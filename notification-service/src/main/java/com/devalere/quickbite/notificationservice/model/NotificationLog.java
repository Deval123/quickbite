package com.devalere.quickbite.notificationservice.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String status; // SENT, FAILED, SKIPPED_DEDUP, SKIPPED_PREFERENCE

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public NotificationLog() {}

    public NotificationLog(String orderId, String userId,
                           NotificationChannel channel, String eventType,
                           String status, String errorMessage) {
        this.orderId = orderId;
        this.userId = userId;
        this.channel = channel;
        this.eventType = eventType;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public NotificationChannel getChannel() { return channel; }
    public String getEventType() { return eventType; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
