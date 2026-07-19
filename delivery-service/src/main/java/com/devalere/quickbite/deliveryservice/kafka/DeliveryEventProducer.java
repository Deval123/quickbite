package com.devalere.quickbite.deliveryservice.kafka;

import com.devalere.quickbite.events.DeliveryAssignedEvent;
import com.devalere.quickbite.events.DeliveryCompletedEvent;
import com.devalere.quickbite.kafka.KafkaTopics;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

/**
 * Producer d'events pour le Delivery Service.
 * Publie sur le topic "delivery-events".
 */
@Component
public class DeliveryEventProducer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DeliveryEventProducer(KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishDeliveryAssigned(String orderId, String driverId,
            String driverName, int estimatedMinutes) {
        var event = new DeliveryAssignedEvent(
                orderId,
                UUID.randomUUID().toString(),
                driverId,
                driverName,
                estimatedMinutes,
                Instant.now()
        );
        publish(KafkaTopics.DELIVERY_EVENTS, orderId, event, "DeliveryAssignedEvent");
    }

    public void publishDeliveryCompleted(String orderId, String driverId) {
        var event = new DeliveryCompletedEvent(
                orderId,
                UUID.randomUUID().toString(),
                driverId,
                Instant.now()
        );
        publish(KafkaTopics.DELIVERY_EVENTS, orderId, event, "DeliveryCompletedEvent");
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
