package com.devalere.quickbite.deliveryservice.dto;

/**
 * Resultat du matching livreur-commande.
 */
public record MatchResult(
        String orderId,
        String driverId,
        double score,
        double distanceKm,
        int etaMinutes,
        double radiusUsedKm,
        boolean matched) {

    /** Factory pour le cas "aucun livreur disponible" */
    public static MatchResult noDriver(String orderId) {
        return new MatchResult(orderId, null, 0, 0, 0, 10.0, false);
    }
}
