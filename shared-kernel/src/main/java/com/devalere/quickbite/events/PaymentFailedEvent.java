package com.devalere.quickbite.events;

import java.time.Instant;

/**
 * Émis quand le paiement échoue Consommé par : OrderService (annule la commande), NotificationService.
 *
 * @param orderId
 * @param paymentId
 * @param failureReason
 * @param failedAt
 */
public record PaymentFailedEvent(
        String orderId,
        String paymentId,
        String failureReason,
        Instant failedAt)
{
}
