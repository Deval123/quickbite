package com.devalere.quickbite.deliveryservice.service;

import org.springframework.stereotype.Component;

/**
 * Calcule l'ETA (Estimated Time of Arrival) d'une livraison.
 * <p>
 * ETA = prepTime + pickupTime + deliveryTime + marge 20%
 * <p>
 * Vitesse moyenne scooter en ville : 20 km/h
 * Marge de securite : +20% (mieux vaut surprendre en avance)
 */
@Component
public class EtaCalculator {

    /** Vitesse moyenne d'un scooter en ville (km/h) */
    private static final double AVERAGE_SPEED_KMH = 20.0;

    /** Marge de securite sur l'ETA (+20%) */
    private static final double SAFETY_MARGIN = 1.20;

    /**
     * Calcule l'ETA total en minutes.
     *
     * @param prepTimeMinutes     temps de preparation restant (fourni par le restaurant)
     * @param driverToRestaurantKm distance livreur -> restaurant en km
     * @param restaurantToClientKm distance restaurant -> client en km
     * @return ETA en minutes (arrondi superieur)
     */
    public int calculateEtaMinutes(
            int prepTimeMinutes,
            double driverToRestaurantKm,
            double restaurantToClientKm) {

        // pickupTime = distance / vitesse * 60 (conversion heures -> minutes)
        double pickupTimeMinutes = (driverToRestaurantKm / AVERAGE_SPEED_KMH) * 60;

        // deliveryTime = distance / vitesse * 60
        double deliveryTimeMinutes = (restaurantToClientKm / AVERAGE_SPEED_KMH) * 60;

        // ETA = prepTime + pickupTime + deliveryTime
        double rawEta = prepTimeMinutes + pickupTimeMinutes + deliveryTimeMinutes;

        // Ajouter la marge de securite de 20%
        double etaWithMargin = rawEta * SAFETY_MARGIN;

        return (int) Math.ceil(etaWithMargin);
    }

    /**
     * Calcule le temps de trajet en minutes pour une distance donnee.
     */
    public double travelTimeMinutes(double distanceKm) {
        return (distanceKm / AVERAGE_SPEED_KMH) * 60;
    }
}
