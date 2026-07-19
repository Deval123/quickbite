package com.devalere.quickbite.orderservice.kafka;

import com.devalere.quickbite.events.PaymentCompletedEvent;
import com.devalere.quickbite.events.PaymentFailedEvent;
import com.devalere.quickbite.events.OrderConfirmedEvent;
import com.devalere.quickbite.events.DeliveryCompletedEvent;
import com.devalere.quickbite.kafka.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumer du Order Service.
 * Ecoute payment-events, restaurant-events et delivery-events
 * pour mettre a jour le statut de la commande.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ObjectMapper objectMapper;

    public OrderEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "order-group")
    public void onPaymentEvent(ConsumerRecord<String, String> record) {
        String eventType = getEventType(record);
        log.info("Order recu event paiement: {} pour orderId={}", eventType, record.key());

        try {
            if ("PaymentCompletedEvent".equals(eventType)) {
                var event = objectMapper.readValue(record.value(), PaymentCompletedEvent.class);
                // TODO: orderService.updateStatus(event.orderId(), OrderStatus.PAYMENT_CONFIRMED);
                log.info("Commande {} : paiement confirme ({})", event.orderId(), event.transactionRef());
            } else if ("PaymentFailedEvent".equals(eventType)) {
                var event = objectMapper.readValue(record.value(), PaymentFailedEvent.class);
                // TODO: orderService.updateStatus(event.orderId(), OrderStatus.CANCELLED);
                log.info("Commande {} : paiement echoue ({})", event.orderId(), event.failureReason());
            }
        } catch (Exception e) {
            log.error("Erreur traitement payment event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopics.RESTAURANT_EVENTS, groupId = "order-group")
    public void onRestaurantEvent(ConsumerRecord<String, String> record) {
        String eventType = getEventType(record);
        log.info("Order recu event restaurant: {} pour orderId={}", eventType, record.key());

        try {
            if ("OrderConfirmedEvent".equals(eventType)) {
                var event = objectMapper.readValue(record.value(), OrderConfirmedEvent.class);
                // TODO: orderService.updateStatus(event.orderId(), OrderStatus.CONFIRMED);
                log.info("Commande {} : confirmee par restaurant, prep ~{} min",
                        event.orderId(), event.estimatePreparationMinutes());
            }
        } catch (Exception e) {
            log.error("Erreur traitement restaurant event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopics.DELIVERY_EVENTS, groupId = "order-group")
    public void onDeliveryEvent(ConsumerRecord<String, String> record) {
        String eventType = getEventType(record);
        log.info("Order recu event livraison: {} pour orderId={}", eventType, record.key());

        try {
            if ("DeliveryCompletedEvent".equals(eventType)) {
                var event = objectMapper.readValue(record.value(), DeliveryCompletedEvent.class);
                // TODO: orderService.updateStatus(event.orderId(), OrderStatus.DELIVERED);
                log.info("Commande {} : livree par driver {}", event.orderId(), event.driverId());
            }
        } catch (Exception e) {
            log.error("Erreur traitement delivery event: {}", e.getMessage());
        }
    }

    private String getEventType(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader("event-type");
        return header != null ? new String(header.value()) : "unknown";
    }
}
