package com.devalere.quickbite.events;

import java.time.Instant;

/**
 * Émis quand le plat est prêt à être récupéré.
 * Consommé par : DeliveryService (assigner un livreur), OrderService, NotificationService.
 * @param orderId
 * @param restaurantId
 * @param readyAt
 */
public record OrderReadyEvent(
        String orderId,
        String restaurantId,
        Instant readyAt)
{
}
