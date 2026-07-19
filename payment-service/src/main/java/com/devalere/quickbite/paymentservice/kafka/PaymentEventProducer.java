package com.devalere.quickbite.paymentservice.kafka;

import com.devalere.quickbite.events.PaymentCompletedEvent;
import com.devalere.quickbite.events.PaymentFailedEvent;
import com.devalere.quickbite.kafka.KafkaTopics;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Producer d'events pour le Payment Service.
 * Publie sur le topic "payment-events".
 */
@Component
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventProducer(KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishPaymentCompleted(String orderId, BigDecimal amount) {
        var event = new PaymentCompletedEvent(
                orderId,
                UUID.randomUUID().toString(),
                amount,
                "txn_" + UUID.randomUUID().toString().substring(0, 8),
                Instant.now()
        );
        publish(KafkaTopics.PAYMENT_EVENTS, orderId, event, "PaymentCompletedEvent");
    }

    public void publishPaymentFailed(String orderId, String reason) {
        var event = new PaymentFailedEvent(
                orderId,
                UUID.randomUUID().toString(),
                reason,
                Instant.now()
        );
        publish(KafkaTopics.PAYMENT_EVENTS, orderId, event, "PaymentFailedEvent");
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
