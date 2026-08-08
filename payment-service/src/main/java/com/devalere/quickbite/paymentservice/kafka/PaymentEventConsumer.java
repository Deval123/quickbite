package com.devalere.quickbite.paymentservice.kafka;

import com.devalere.quickbite.events.OrderCreatedEvent;
import com.devalere.quickbite.kafka.KafkaTopics;
import com.devalere.quickbite.paymentservice.service.PaymentService;
import com.devalere.quickbite.shared.security.KafkaSecurityHeaders;
import tools.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer du Payment Service.
 * Ecoute order-events pour déclencher le paiement.
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final PaymentEventProducer producer;
    private final PaymentService paymentService;

    public PaymentEventConsumer(ObjectMapper objectMapper,
            PaymentEventProducer producer, PaymentService paymentService) {
        this.objectMapper = objectMapper;
        this.producer = producer;
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "payment-group")
    public void onOrderEvent(ConsumerRecord<String, String> record) {
        String eventType = getEventType(record);
        String userId = KafkaSecurityHeaders.getUserId(record.headers());
        log.info("Payment recu event: {} pour orderId={}, user={}",
                eventType, record.key(), userId);

        try {
            if ("OrderCreatedEvent".equals(eventType)) {
                var event = objectMapper.readValue(record.value(), OrderCreatedEvent.class);
                log.info("Déclenchement paiement pour commande {} : {} EUR",
                        event.orderId(), event.totalAmount());

                // Créer le PaymentIntent Stripe (pré-autorisation)
                paymentService.createPaymentIntent(event);
            }
        } catch (Exception e) {
            log.error("Erreur traitement order event: {}", e.getMessage());
        }
    }

    private String getEventType(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader("event-type");
        return header != null ? new String(header.value()) : "unknown";
    }
}
