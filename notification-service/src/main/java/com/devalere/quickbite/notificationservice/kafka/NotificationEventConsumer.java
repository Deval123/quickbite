package com.devalere.quickbite.notificationservice.kafka;

import com.devalere.quickbite.kafka.KafkaTopics;
import com.devalere.quickbite.notificationservice.sse.SseEmitterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Consumer du Notification Service.
 * Ecoute TOUS les topics pour :
 * 1. Envoyer des notifications (email, push, SMS) - existant
 * 2. Pousser les changements de statut via SSE au client - NOUVEAU
 */
@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final SseEmitterRegistry sseRegistry;

    public NotificationEventConsumer(SseEmitterRegistry sseRegistry) {
        this.sseRegistry = sseRegistry;
    }

    @KafkaListener(
            topics = {
                    KafkaTopics.ORDER_EVENTS,
                    KafkaTopics.PAYMENT_EVENTS,
                    KafkaTopics.RESTAURANT_EVENTS,
                    KafkaTopics.DELIVERY_EVENTS
            },
            groupId = "notification-group"
    )
    public void onAnyEvent(ConsumerRecord<String, String> record) {
        String eventType = getEventType(record);
        String orderId = record.key();

        log.info("[NOTIFICATION] Event recu: topic={}, type={}, orderId={}",
                record.topic(), eventType, orderId);

        // 1. Notifications classiques (email, push, SMS)
        switch (eventType) {
        case "OrderCreatedEvent" ->
                log.info("Email: 'Votre commande {} a ete creee'", orderId);
        case "PaymentCompletedEvent" ->
                log.info("Email: 'Paiement confirme pour commande {}'", orderId);
        case "PaymentFailedEvent" ->
                log.info("Email: 'Echec paiement pour commande {}'", orderId);
        case "OrderConfirmedEvent" ->
                log.info("Push: 'Le restaurant prepare votre commande {}'", orderId);
        case "OrderReadyEvent" ->
                log.info("Push: 'Votre commande {} est prete !'", orderId);
        case "DeliveryAssignedEvent" ->
                log.info("Push: 'Un livreur arrive pour commande {}'", orderId);
        case "DeliveryCompletedEvent" ->
                log.info("Push: 'Commande {} livree ! Bon appetit !'", orderId);
        default ->
                log.warn("Event inconnu: {}", eventType);
        }

        // 2. Push SSE : envoyer le changement de statut au client en temps reel
        String status = mapEventToStatus(eventType);
        if (status != null && orderId != null) {
            Map<String, Object> sseData = Map.of(
                    "status", status,
                    "orderId", orderId,
                    "timestamp", Instant.now().toString()
            );
            sseRegistry.send(orderId, "STATUS_CHANGED", sseData);
            log.info("[SSE] Statut {} pousse pour orderId={}", status, orderId);
        }
    }

    /**
     * Mappe un event Kafka vers un statut de commande lisible.
     */
    private String mapEventToStatus(String eventType) {
        return switch (eventType) {
            case "OrderCreatedEvent" -> "CREATED";
            case "PaymentCompletedEvent" -> "PAYMENT_CONFIRMED";
            case "OrderConfirmedEvent" -> "CONFIRMED";
            case "OrderReadyEvent" -> "READY";
            case "DeliveryAssignedEvent" -> "PICKED_UP";
            case "DeliveryCompletedEvent" -> "DELIVERED";
            default -> null;
        };
    }

    private String getEventType(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader("event-type");
        return header != null ? new String(header.value()) : "unknown";
    }
}
