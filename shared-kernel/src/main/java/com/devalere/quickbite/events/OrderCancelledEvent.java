package com.devalere.quickbite.events;

import java.time.Instant;

/**
 * Emis quand une commande est annulee (par un client ou timeout).
 * Consommé par : PaymentService (remboursement), NotificationService.
 * @param orderId
 * @param userId
 * @param reason
 * @param cancelledAt
 */
public record OrderCancelledEvent(
        String orderId,
        String userId,
        String reason,
        Instant cancelledAt)
{
}
