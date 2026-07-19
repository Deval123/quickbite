package com.devalere.quickbite.orderservice.kafka;

import com.devalere.quickbite.events.OrderCreatedEvent;
import com.devalere.quickbite.events.OrderCancelledEvent;
import com.devalere.quickbite.kafka.KafkaTopics;
import com.devalere.quickbite.shared.security.KafkaSecurityHeaders;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Producer d'events pour le Order Service.
 * Publie sur le topic "order-events" avec orderId comme cle de partition.
 */
@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        publish(KafkaTopics.ORDER_EVENTS, event.orderId(), event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        publish(KafkaTopics.ORDER_EVENTS, event.orderId(), event);
    }

    private void publish(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            var record = new ProducerRecord<>(topic, key, payload);

            // Propager le contexte securite dans les headers Kafka
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                String userId = jwt.getClaimAsString("sub");
                String roles = String.join(",",
                        jwt.getClaimAsStringList("realm_access") != null
                                ? jwt.getClaimAsStringList("realm_access")
                                : java.util.List.of());
                KafkaSecurityHeaders.addSecurityHeaders(record.headers(), userId, roles);
            }

            // Ajouter le type d'event dans un header (pour le consumer)
            record.headers().add("event-type",
                    event.getClass().getSimpleName().getBytes());

            kafkaTemplate.send(record)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Echec envoi event {} sur {}: {}",
                                    event.getClass().getSimpleName(), topic, ex.getMessage());
                        } else {
                            log.info("Event {} envoye sur {} partition {} offset {}",
                                    event.getClass().getSimpleName(),
                                    topic,
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
