package com.devalere.quickbite.notificationservice.model;

import java.util.Map;

/**
 * Objet interne representant une notification a envoyer.
 */
public record Notification(
        String orderId,
        String userId,
        String eventType,
        NotificationChannel channel,
        NotificationPriority priority,
        Map<String, Object> templateData,
        boolean overrideQuietHours
) {}
