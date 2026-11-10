package com.devalere.quickbite.deliveryservice.controller;

import com.devalere.quickbite.deliveryservice.dto.LocationUpdateRequest;
import com.devalere.quickbite.deliveryservice.kafka.DeliveryEventProducer;
import com.devalere.quickbite.deliveryservice.service.DriverLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint pour recevoir la position GPS du livreur.
 * <p>
 * L'app mobile du livreur envoie sa position toutes les 3 secondes
 * via POST /api/deliveries/location.
 * <p>
 * Le controller :
 * 1. Stocke la position dans Redis GEO (Video 12 - matching)
 * 2. Publie un DeliveryLocationEvent sur Kafka (Video 11 - WebSocket)
 */
@RestController
@RequestMapping("/api/deliveries")
public class DeliveryLocationController {

    private static final Logger log = LoggerFactory.getLogger(DeliveryLocationController.class);

    private final DeliveryEventProducer producer;
    private final DriverLocationService locationService;

    public DeliveryLocationController(DeliveryEventProducer producer,
                                      DriverLocationService locationService) {
        this.producer = producer;
        this.locationService = locationService;
    }

    /**
     * Recoit la position GPS du livreur.
     * Appele toutes les TROIS secondes par l'app mobile.
     * <p>
     * 1. Stocke dans Redis GEO pour le matching (Video 12)
     * 2. Publie sur Kafka pour le WebSocket temps reel (Video 11)
     */
    @PostMapping("/location")
    public ResponseEntity<Map<String, String>> updateLocation(@RequestBody LocationUpdateRequest request) {
        log.info("Position recue: orderId={}, lat={}, lon={}",
                request.orderId(), request.latitude(), request.longitude());

        // Video 12 : stocker dans Redis GEO pour le matching
        locationService.updateDriverLocation(
                "driver-current", // TODO: extraire du JWT
                request.latitude(),
                request.longitude()
        );

        // Video 11 : publier sur Kafka pour WebSocket
        producer.publishDeliveryLocation(
                request.orderId(),
                request.latitude(),
                request.longitude()
        );

        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
