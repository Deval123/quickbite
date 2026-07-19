package com.devalere.quickbite.restaurantservice.kafka;

import com.devalere.quickbite.events.OrderConfirmedEvent;
import com.devalere.quickbite.events.OrderReadyEvent;
import com.devalere.quickbite.kafka.KafkaTopics;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Producer d'events pour le Restaurant Service.
 * Publie sur le topic "restaurant-events".
 */
@Component
public class RestaurantEventProducer {

    private static final Logger log = LoggerFactory.getLogger(RestaurantEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public RestaurantEventProducer(KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishOrderConfirmed(String orderId, String restaurantId,
            int estimatedPrepTimeMinutes) {
        var event = new OrderConfirmedEvent(
                orderId, restaurantId, estimatedPrepTimeMinutes, Instant.now()
        );
        publish(KafkaTopics.RESTAURANT_EVENTS, orderId, event, "OrderConfirmedEvent");
    }

    public void publishOrderReady(String orderId, String restaurantId) {
        var event = new OrderReadyEvent(orderId, restaurantId, Instant.now());
        publish(KafkaTopics.RESTAURANT_EVENTS, orderId, event, "OrderReadyEvent");
    }

    private void publish(String topic, String key, Object event, String eventType) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            var record = new ProducerRecord<>(topic, key, payload);
            record.headers().add("event-type", eventType.getBytes());

            kafkaTemplate.send(record)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Echec envoi {}: {}", eventType, ex.getMessage());
                        } else {
                            log.info("{} envoye, partition={} offset={}",
                                    eventType,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (JacksonException e) {
            log.error("Erreur serialisation event: {}", e.getMessage());
            throw new RuntimeException("Impossible de serialiser l'event", e);
        }
    }
}
