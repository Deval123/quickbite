package com.devalere.quickbite.deliveryservice.controller;

import com.devalere.quickbite.deliveryservice.dto.MatchResult;
import com.devalere.quickbite.deliveryservice.dto.MatchingRequest;
import com.devalere.quickbite.deliveryservice.service.DriverLocationService;
import com.devalere.quickbite.deliveryservice.service.MatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints pour tester le matching livreur-commande (Video 12).
 * <p>
 * Flux de test :
 * 1. Simuler les positions de livreurs via POST /api/deliveries/drivers/location
 * 2. Lancer un matching via POST /api/deliveries/match
 * 3. Verifier les livreurs proches via GET /api/deliveries/drivers/nearby
 */
@RestController
@RequestMapping("/api/deliveries")
public class MatchingController {

    private static final Logger log = LoggerFactory.getLogger(MatchingController.class);

    private final MatchingService matchingService;
    private final DriverLocationService locationService;

    public MatchingController(MatchingService matchingService,
                              DriverLocationService locationService) {
        this.matchingService = matchingService;
        this.locationService = locationService;
    }

    /**
     * Simule la position d'un livreur (pour test sans app mobile).
     * <p>
     * Exemple :
     * curl -X POST http://localhost:8088/api/deliveries/drivers/location \
     *   -H "Content-Type: application/json" \
     *   -d '{"driverId":"driver1","latitude":48.8566,"longitude":2.3522}'
     */
    @PostMapping("/drivers/location")
    public ResponseEntity<Map<String, String>> simulateDriverLocation(
            @RequestBody DriverLocationRequest request) {

        locationService.updateDriverLocation(
                request.driverId(), request.latitude(), request.longitude());

        return ResponseEntity.ok(Map.of(
                "status", "stored",
                "driver", request.driverId(),
                "lat", String.valueOf(request.latitude()),
                "lon", String.valueOf(request.longitude())
        ));
    }

    /**
     * Cherche les livreurs proches d'un point.
     * <p>
     * Exemple :
     * curl "http://localhost:8088/api/deliveries/drivers/nearby?lat=48.8566&lon=2.3522&radius=5"
     */
    @GetMapping("/drivers/nearby")
    public ResponseEntity<List<DriverLocationService.DriverWithDistance>> findNearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "2") double radius) {

        var drivers = locationService.findDriversWithinRadius(lat, lon, radius);
        return ResponseEntity.ok(drivers);
    }

    /**
     * Lance le matching pour une commande.
     * <p>
     * Exemple :
     * curl -X POST http://localhost:8088/api/deliveries/match \
     *   -H "Content-Type: application/json" \
     *   -d '{"orderId":"ORDER-001","restaurantId":"REST-001",
     *        "restaurantLat":48.8566,"restaurantLon":2.3522,
     *        "clientLat":48.8606,"clientLon":2.3376}'
     */
    @PostMapping("/match")
    public ResponseEntity<MatchResult> matchDriver(
            @RequestBody MatchingRequest request) {

        MatchResult result = matchingService.matchDriver(
                request.orderId(),
                request.restaurantId(),
                request.restaurantLat(), request.restaurantLon(),
                request.clientLat(), request.clientLon());

        if (!result.matched()) {
            return ResponseEntity.status(404).body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * DTO pour simuler la position d'un livreur.
     */
    public record DriverLocationRequest(
            String driverId, double latitude, double longitude) {
    }
}
