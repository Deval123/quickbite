package com.devalere.quickbite.deliveryservice.controller;

import com.devalere.quickbite.deliveryservice.dto.LocationUpdateRequest;
import com.devalere.quickbite.deliveryservice.kafka.DeliveryEventProducer;
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
 * Le controller publie un DeliveryLocationEvent sur Kafka,
 * qui sera consommé par la Notification Service pour pousser
 * la position au client via WebSocket.
 */
@RestController
@RequestMapping("/api/deliveries")
public class DeliveryLocationController {

    private static final Logger log = LoggerFactory.getLogger(DeliveryLocationController.class);

    private final DeliveryEventProducer producer;

    public DeliveryLocationController(DeliveryEventProducer producer) {
        this.producer = producer;
    }

    /**
     * Reçoit la position GPS du livreur et la publie sur Kafka.
     * Appelé toutes les TROIS secondes par l'app mobile.
     */
    @PostMapping("/location")
    public ResponseEntity<Map<String, String>> updateLocation(@RequestBody LocationUpdateRequest request) {
        log.info("Position recue: orderId={}, lat={}, lon={}",
                request.orderId(), request.latitude(), request.longitude());

        producer.publishDeliveryLocation(
                request.orderId(),
                request.latitude(),
                request.longitude()
        );

        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
