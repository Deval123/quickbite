package com.devalere.quickbite.events;

import java.time.Instant;

/**
 * Émis quand un livreur est assigné a la commande.
 * Consommé par : OrderService, NotificationService (prévenir le client).
 * @param orderId
 * @param deliveryId
 * @param driverId
 * @param driverName
 * @param estimateDeliveryMinutes
 * @param assignedAt
 */
public record DeliveryAssignedEvent(
        String orderId,
        String deliveryId,
        String driverId,
        String driverName,
        int estimateDeliveryMinutes,
        Instant assignedAt)
{
}
