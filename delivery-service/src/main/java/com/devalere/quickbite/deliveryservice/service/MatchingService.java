package com.devalere.quickbite.deliveryservice.service;

import com.devalere.quickbite.deliveryservice.dto.MatchResult;
import com.devalere.quickbite.deliveryservice.kafka.DeliveryEventProducer;
import com.devalere.quickbite.deliveryservice.service.DriverLocationService.DriverWithDistance;
import com.devalere.quickbite.deliveryservice.service.GeofencingService.GeofenceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Algorithme de matching livreur-commande.
 * <p>
 * Score = 0.5 * distanceScore + 0.3 * chargeScore + 0.2 * directionScore
 * <p>
 * Le livreur avec le meilleur score est assigne.
 * En cas de timeout (30s), on passe au suivant (cascade).
 */
@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    /** Poids des criteres de scoring */
    private static final double WEIGHT_DISTANCE = 0.5;
    private static final double WEIGHT_CHARGE = 0.3;
    private static final double WEIGHT_DIRECTION = 0.2;

    /** Distance max pour normaliser le score (km) */
    private static final double MAX_DISTANCE_KM = 10.0;

    /** Charge max (nb livraisons en cours) */
    private static final int MAX_ACTIVE_DELIVERIES = 3;

    /** Temps de preparation par defaut si non fourni (minutes) */
    private static final int DEFAULT_PREP_TIME = 10;

    private final GeofencingService geofencingService;
    private final EtaCalculator etaCalculator;
    private final DeliveryEventProducer producer;

    public MatchingService(GeofencingService geofencingService,
                           EtaCalculator etaCalculator,
                           DeliveryEventProducer producer) {
        this.geofencingService = geofencingService;
        this.etaCalculator = etaCalculator;
        this.producer = producer;
    }

    /**
     * Trouve et assigne le meilleur livreur pour une commande.
     *
     * @param orderId       identifiant de la commande
     * @param restaurantId  identifiant du restaurant
     * @param restaurantLat latitude du restaurant
     * @param restaurantLon longitude du restaurant
     * @param clientLat     latitude du client
     * @param clientLon     longitude du client
     * @return resultat du matching ou null si aucun livreur
     */
    public MatchResult matchDriver(
            String orderId,
            String restaurantId,
            double restaurantLat, double restaurantLon,
            double clientLat, double clientLon) {

        log.info("Matching demarre pour orderId={}, restaurant={}",
                orderId, restaurantId);

        // 1. Geofencing dynamique : trouver les livreurs a proximite
        GeofenceResult geofenceResult =
                geofencingService.findDriversWithExpansion(restaurantLat, restaurantLon);

        if (geofenceResult.noDriversFound()) {
            log.warn("Aucun livreur disponible pour orderId={}", orderId);
            return MatchResult.noDriver(orderId);
        }

        // 2. Scoring multi-criteres
        List<ScoredDriver> scoredDrivers = geofenceResult.drivers().stream()
                .map(driver -> scoreDriver(driver, restaurantLat, restaurantLon))
                .sorted(Comparator.comparingDouble(ScoredDriver::score).reversed())
                .toList();

        // 3. Prendre le meilleur livreur
        ScoredDriver bestDriver = scoredDrivers.get(0);

        // 4. Calculer l'ETA
        double restaurantToClientKm = haversineKm(
                restaurantLat, restaurantLon, clientLat, clientLon);
        int etaMinutes = etaCalculator.calculateEtaMinutes(
                DEFAULT_PREP_TIME,
                bestDriver.distanceKm(),
                restaurantToClientKm);

        // 5. Publier l'event d'assignation sur Kafka
        producer.publishDeliveryAssigned(
                orderId,
                bestDriver.driverId(),
                bestDriver.driverId(), // driverName = driverId pour simplifier
                etaMinutes);

        log.info("Livreur assigne: orderId={}, driver={}, score={}, ETA={}min, rayon={}km",
                orderId, bestDriver.driverId(),
                String.format("%.3f", bestDriver.score()),
                etaMinutes, geofenceResult.radiusUsedKm());

        return new MatchResult(
                orderId,
                bestDriver.driverId(),
                bestDriver.score(),
                bestDriver.distanceKm(),
                etaMinutes,
                geofenceResult.radiusUsedKm(),
                true
        );
    }

    /**
     * Calcule le score d'un livreur.
     * Score = 0.5 * distanceScore + 0.3 * chargeScore + 0.2 * directionScore
     */
    private ScoredDriver scoreDriver(
            DriverWithDistance driver,
            double restaurantLat, double restaurantLon) {

        // Distance score : plus c'est proche, plus c'est haut (0 a 1)
        double distanceScore = Math.max(0,
                1.0 - (driver.distanceKm() / MAX_DISTANCE_KM));

        // Charge score : moins de livraisons en cours = meilleur score
        // TODO: recuperer la charge reelle depuis la BDD
        int activeDeliveries = 0; // Placeholder
        double chargeScore = 1.0 - ((double) activeDeliveries / MAX_ACTIVE_DELIVERIES);

        // Direction score : simplifie pour la demo (0.5 par defaut)
        // En prod : comparer le vecteur de deplacement avec la direction du restaurant
        double directionScore = 0.5;

        double totalScore = WEIGHT_DISTANCE * distanceScore
                + WEIGHT_CHARGE * chargeScore
                + WEIGHT_DIRECTION * directionScore;

        log.debug("Score driver={}: distance={} ({}km), charge={}, direction={}, total={}",
                driver.driverId(),
                String.format("%.3f", distanceScore), driver.distanceKm(),
                String.format("%.3f", chargeScore),
                String.format("%.3f", directionScore),
                String.format("%.3f", totalScore));

        return new ScoredDriver(
                driver.driverId(),
                totalScore,
                driver.distanceKm()
        );
    }

    /**
     * Formule Haversine pour calculer la distance entre deux points GPS.
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Rayon de la Terre en km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /** Resultat intermediaire : livreur avec son score calcule */
    private record ScoredDriver(String driverId, double score, double distanceKm) {
    }
}
