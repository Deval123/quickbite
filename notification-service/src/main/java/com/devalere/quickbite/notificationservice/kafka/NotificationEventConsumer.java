package com.devalere.quickbite.notificationservice.kafka;

import com.devalere.quickbite.kafka.KafkaTopics;
import com.devalere.quickbite.notificationservice.service.NotificationRouter;
import com.devalere.quickbite.notificationservice.sse.SseEmitterRegistry;
import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Consumer du Notification Service.
 * Ecoute TOUS les topics pour :
 * 1. Router vers le systeme de notification multi-canal (NOUVEAU Video 13)
 * 2. Pousser les changements de statut via SSE au client (Video 11)
 */
@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationRouter notificationRouter;
    private final SseEmitterRegistry sseRegistry;
    private final ObjectMapper objectMapper;

    public NotificationEventConsumer(NotificationRouter notificationRouter,
                                     SseEmitterRegistry sseRegistry,
                                     ObjectMapper objectMapper) {
        this.notificationRouter = notificationRouter;
        this.sseRegistry = sseRegistry;
        this.objectMapper = objectMapper;
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

        // 1. Extraire les donnees de l'event
        Map<String, Object> eventData = parseEventData(record.value());
        eventData.put("orderId", orderId);
        eventData.put("eventType", eventType);

        String userId = (String) eventData.getOrDefault("userId", "unknown");

        // 2. Router vers le systeme de notification multi-canal
        try {
            notificationRouter.route(eventType, orderId, userId, eventData);
        } catch (Exception e) {
            log.error("[NOTIFICATION] Erreur routing notification : {}", e.getMessage(), e);
        }

        // 3. Push SSE : envoyer le changement de statut au client en temps reel
        String status = mapEventToStatus(eventType);
        if (status != null && orderId != null) {
            Map<String, Object> sseData = Map.of(
                    "status", status,
                    "orderId", orderId,
                    "timestamp", Instant.now().toString()
            );
            sseRegistry.send(orderId, "STATUS_CHANGED", sseData);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseEventData(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("[NOTIFICATION] Impossible de parser le JSON de l'event : {}",
                    e.getMessage());
            return new HashMap<>();
        }
    }

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
