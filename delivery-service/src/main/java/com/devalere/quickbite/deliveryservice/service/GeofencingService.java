package com.devalere.quickbite.deliveryservice.service;

import com.devalere.quickbite.deliveryservice.service.DriverLocationService.DriverWithDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Geofencing dynamique : expansion progressive du rayon de recherche
 * quand aucun livreur n'est disponible dans le rayon initial.
 * <p>
 * Paliers :
 * - 2 km (defaut)
 * - 5 km (si aucun resultat a 2 km)
 * - 10 km (maximum, si aucun resultat a 5 km)
 */
@Service
public class GeofencingService {

    private static final Logger log = LoggerFactory.getLogger(GeofencingService.class);

    /** Rayons d'expansion progressive en km */
    private static final double[] RADIUS_TIERS = {2.0, 5.0, 10.0};

    private final DriverLocationService locationService;

    public GeofencingService(DriverLocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * Cherche des livreurs en expandant le rayon progressivement.
     * S'arrete des qu'un palier retourne au moins un resultat.
     *
     * @param restaurantLat latitude du restaurant
     * @param restaurantLon longitude du restaurant
     * @return resultat de la recherche avec le rayon utilise
     */
    public GeofenceResult findDriversWithExpansion(
            double restaurantLat, double restaurantLon) {

        for (double radius : RADIUS_TIERS) {
            List<DriverWithDistance> drivers =
                    locationService.findDriversWithinRadius(
                            restaurantLat, restaurantLon, radius);

            if (!drivers.isEmpty()) {
                log.info("Geofencing: {} livreurs trouves dans {} km",
                        drivers.size(), radius);
                return new GeofenceResult(drivers, radius, false);
            }

            log.info("Geofencing: aucun livreur dans {} km, expansion...", radius);
        }

        // Aucun livreur meme a 10 km
        log.warn("Geofencing: aucun livreur disponible dans {} km",
                RADIUS_TIERS[RADIUS_TIERS.length - 1]);
        return new GeofenceResult(List.of(),
                RADIUS_TIERS[RADIUS_TIERS.length - 1], true);
    }

    /**
     * Resultat du geofencing.
     *
     * @param drivers         livreurs trouves
     * @param radiusUsedKm    rayon qui a produit le resultat
     * @param noDriversFound  true si aucun livreur dans le rayon max
     */
    public record GeofenceResult(
            List<DriverWithDistance> drivers,
            double radiusUsedKm,
            boolean noDriversFound) {
    }
}
