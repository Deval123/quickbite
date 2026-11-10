package com.devalere.quickbite.notificationservice.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "user_notification_preference")
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "push_enabled")
    private boolean pushEnabled = true;

    @Column(name = "sms_enabled")
    private boolean smsEnabled = true;

    @Column(name = "email_enabled")
    private boolean emailEnabled = true;

    @Column(name = "quiet_start")
    private LocalTime quietStart = LocalTime.of(22, 0);

    @Column(name = "quiet_end")
    private LocalTime quietEnd = LocalTime.of(8, 0);

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column
    private String email;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public String getUserId() { return userId; }
    public boolean isPushEnabled() { return pushEnabled; }
    public boolean isSmsEnabled() { return smsEnabled; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public LocalTime getQuietStart() { return quietStart; }
    public LocalTime getQuietEnd() { return quietEnd; }
    public String getFcmToken() { return fcmToken; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }

    public boolean isChannelEnabled(NotificationChannel channel) {
        return switch (channel) {
            case PUSH -> pushEnabled;
            case SMS -> smsEnabled;
            case EMAIL -> emailEnabled;
        };
    }
}
