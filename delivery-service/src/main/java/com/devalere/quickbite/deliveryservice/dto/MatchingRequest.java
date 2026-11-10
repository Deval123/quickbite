package com.devalere.quickbite.deliveryservice.dto;

/**
 * Requete de matching livreur-commande (pour test via REST).
 */
public record MatchingRequest(
        String orderId,
        String restaurantId,
        double restaurantLat,
        double restaurantLon,
        double clientLat,
        double clientLon) {
}
