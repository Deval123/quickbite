package com.devalere.quickbite.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Émis quand le paiement est confirmé
 * Consommé par : OrderService (met a jour le statut), NotificationService.
 * @param orderId
 * @param paymentId
 * @param amount
 * @param transactionRef
 * @param completeAt
 */
public record PaymentCompletedEvent(
        String orderId,
        String paymentId,
        BigDecimal amount,
        String transactionRef,
        Instant completeAt)
{
}
