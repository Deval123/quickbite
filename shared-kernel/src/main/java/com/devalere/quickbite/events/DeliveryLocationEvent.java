package com.devalere.quickbite.events;

import java.time.Instant;

/**
 * Émis par le Delivery Service quand le livreur envoie sa position GPS.
 * Consomme par : NotificationService (push WebSocket vers le client).
 */
public record DeliveryLocationEvent(
        String orderId,
        String driverId,
        double latitude,
        double longitude,
        Instant timestamp)
{
}
