package com.devalere.quickbite.events;

import java.time.Instant;

/**
 * Émis quand le restaurant confirme la commande.
 * Consommé par : OrderService, DeliveryService (pour se préparer), NotificationService.
 * @param orderId
 * @param restaurantId
 * @param estimatePreparationMinutes
 * @param confirmedAt
 */
public record OrderConfirmedEvent(
        String orderId,
        String restaurantId,
        int estimatePreparationMinutes,
        Instant confirmedAt)
{
}
