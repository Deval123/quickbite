package com.devalere.quickbite.deliveryservice.kafka;

import com.devalere.quickbite.deliveryservice.dto.MatchResult;
import com.devalere.quickbite.deliveryservice.service.MatchingService;
import com.devalere.quickbite.events.OrderReadyEvent;
import com.devalere.quickbite.kafka.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumer du Delivery Service.
 * Ecoute restaurant-events pour assigner un livreur quand le plat est pret.
 * <p>
 * Video 12 : utilise le MatchingService pour trouver le meilleur livreur
 * via Redis GEO + scoring multi-criteres.
 */
@Component
public class DeliveryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final MatchingService matchingService;

    public DeliveryEventConsumer(ObjectMapper objectMapper,
            MatchingService matchingService) {
        this.objectMapper = objectMapper;
        this.matchingService = matchingService;
    }

    @KafkaListener(topics = KafkaTopics.RESTAURANT_EVENTS, groupId = "delivery-group")
    public void onRestaurantEvent(ConsumerRecord<String, String> record) {
        String eventType = getEventType(record);
        log.info("Delivery recu event: {} pour orderId={}", eventType, record.key());

        try {
            if ("OrderReadyEvent".equals(eventType)) {
                var event = objectMapper.readValue(record.value(), OrderReadyEvent.class);
                log.info("Plat pret pour commande {} chez restaurant {}. Recherche livreur...",
                        event.orderId(), event.restaurantId());

                // Video 12 : matching automatique via Redis GEO + scoring
                // Coordonnees de test (Paris centre) - en prod, recuperees de la BDD
                MatchResult result = matchingService.matchDriver(
                        event.orderId(),
                        event.restaurantId(),
                        48.8566, 2.3522,  // restaurant (placeholder)
                        48.8606, 2.3376   // client (placeholder)
                );

                if (result.matched()) {
                    log.info("Livreur {} assigne a commande {}, ETA={}min",
                            result.driverId(), event.orderId(), result.etaMinutes());
                } else {
                    log.warn("Aucun livreur disponible pour commande {}", event.orderId());
                }
            }
        } catch (Exception e) {
            log.error("Erreur traitement restaurant event: {}", e.getMessage());
        }
    }

    private String getEventType(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader("event-type");
        return header != null ? new String(header.value()) : "unknown";
    }
}
