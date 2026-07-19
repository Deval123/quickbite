package com.devalere.quickbite.events;

import java.time.Instant;

/**
 * Émis quand la commande est termonée.
 * Consommé par : OrderService (clôturer la commande, NotificationService.
 * @param deliveryId
 * @param driverId
 * @param deliveredAt
 */
public record DeliveryCompletedEvent(
        String orderId,
        String deliveryId,
        String driverId,
       Instant deliveredAt)
{
}
