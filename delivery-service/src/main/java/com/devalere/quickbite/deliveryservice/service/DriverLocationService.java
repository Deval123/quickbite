package com.devalere.quickbite.deliveryservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Stocke et requete les positions GPS des livreurs via Redis GEO.
 * <p>
 * Commandes Redis utilisees :
 * - GEOADD drivers:active longitude latitude driverId
 * - GEORADIUS drivers:active lon lat radius km ASC
 * - GEODIST drivers:active driver1 restaurant km
 * <p>
 * Les positions expirent automatiquement si le livreur
 * ne met plus a jour sa position depuis 30 secondes.
 */
@Service
public class DriverLocationService {

    private static final Logger log = LoggerFactory.getLogger(DriverLocationService.class);

    /** Cle Redis GEO pour les livreurs actifs */
    private static final String GEO_KEY = "drivers:active";

    /** Cle Redis pour tracker le dernier heartbeat par livreur */
    private static final String HEARTBEAT_PREFIX = "driver:heartbeat:";

    /** Duree avant de considerer un livreur hors-ligne */
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);

    private final GeoOperations<String, String> geoOps;
    private final StringRedisTemplate redisTemplate;

    public DriverLocationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.geoOps = redisTemplate.opsForGeo();
    }

    /**
     * Met a jour la position GPS d'un livreur.
     * Appele toutes les 3 secondes par l'app mobile.
     *
     * @param driverId  identifiant du livreur
     * @param latitude  latitude GPS
     * @param longitude longitude GPS
     */
    public void updateDriverLocation(String driverId, double latitude, double longitude) {
        // GEOADD drivers:active longitude latitude driverId
        geoOps.add(GEO_KEY, new Point(longitude, latitude), driverId);

        // Rafraichir le heartbeat (TTL 30s)
        redisTemplate.opsForValue().set(
                HEARTBEAT_PREFIX + driverId,
                String.valueOf(System.currentTimeMillis()),
                HEARTBEAT_TTL
        );

        log.debug("Position MAJ: driver={}, lat={}, lon={}", driverId, latitude, longitude);
    }

    /**
     * Trouve les livreurs disponibles dans un rayon donne
     * autour d'un point (le restaurant).
     *
     * @param latitude  latitude du restaurant
     * @param longitude longitude du restaurant
     * @param radiusKm  rayon de recherche en km
     * @return liste des driverIds tries par distance croissante
     */
    public List<DriverWithDistance> findDriversWithinRadius(
            double latitude, double longitude, double radiusKm) {

        Distance radius = new Distance(radiusKm,
                RedisGeoCommands.DistanceUnit.KILOMETERS);
        Circle circle = new Circle(new Point(longitude, latitude), radius);

        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(50);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                geoOps.radius(GEO_KEY, circle, args);

        if (results == null) {
            return List.of();
        }

        return results.getContent().stream()
                .filter(r -> isDriverOnline(r.getContent().getName()))
                .map(r -> new DriverWithDistance(
                        r.getContent().getName(),
                        r.getDistance().getValue()
                ))
                .toList();
    }

    /**
     * Calcule la distance entre un livreur et un point (restaurant ou client).
     *
     * @return distance en kilometres, ou -1 si le livreur n'est pas trouve
     */
    public double getDistanceKm(String driverId, double latitude, double longitude) {
        // On cree un point temporaire pour calculer la distance
        String tempKey = "temp:" + System.nanoTime();
        geoOps.add(GEO_KEY, new Point(longitude, latitude), tempKey);

        Distance distance = geoOps.distance(
                GEO_KEY, driverId, tempKey,
                RedisGeoCommands.DistanceUnit.KILOMETERS);

        // Nettoyer le point temporaire
        geoOps.remove(GEO_KEY, tempKey);

        return distance != null ? distance.getValue() : -1;
    }

    /**
     * Verifie si le livreur est encore en ligne (heartbeat recent).
     */
    public boolean isDriverOnline(String driverId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(HEARTBEAT_PREFIX + driverId));
    }

    /**
     * Supprime un livreur du pool actif (fin de service).
     */
    public void removeDriver(String driverId) {
        geoOps.remove(GEO_KEY, driverId);
        redisTemplate.delete(HEARTBEAT_PREFIX + driverId);
        log.info("Livreur retire du pool: {}", driverId);
    }

    /**
     * Resultat d'une recherche : driverId + distance en km.
     */
    public record DriverWithDistance(String driverId, double distanceKm) {
    }
}
