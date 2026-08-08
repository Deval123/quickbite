package com.devalere.quickbite.paymentservice.model;

public enum PaymentStatus {
    PENDING,            // PaymentIntent crée, en attente de confirmation
    REQUIRES_CAPTURE,   // Pre-autorise, en attente de capture
    PROCESSING,         // Paiement en cours de traitement par Stripe
    SUCCEEDED,          // Paiement réussi
    FAILED,             // Paiement échoue
    CANCELLED,          // PaymentIntent annule (pre-auth relâchée)
    REFUNDED            // Rembourse (total ou partiel)
}