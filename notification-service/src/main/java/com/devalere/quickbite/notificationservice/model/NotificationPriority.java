package com.devalere.quickbite.notificationservice.model;

public enum NotificationPriority {
    HIGH,       // Override quiet hours, fallback SMS si push echoue
    NORMAL,     // Respecte quiet hours, pas de fallback
    LOW         // Peut etre delayed, batch possible
}
