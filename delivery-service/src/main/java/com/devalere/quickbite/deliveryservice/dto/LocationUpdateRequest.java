package com.devalere.quickbite.deliveryservice.dto;

/**
 * DTO pour la mise à jour de position GPS du livreur.
 * Envoyé par l'app mobile du livreur toutes les 3 secondes.
 */
public record LocationUpdateRequest(
        String orderId,
        double latitude,
        double longitude)
{
}
