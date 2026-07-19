package com.devalere.quickbite.deliveryservice.kafka;

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
 */
@Component
public class DeliveryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final DeliveryEventProducer producer;

    public DeliveryEventConsumer(ObjectMapper objectMapper,
            DeliveryEventProducer producer) {
        this.objectMapper = objectMapper;
        this.producer = producer;
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

                // TODO: assigner un livreur disponible
                // producer.publishDeliveryAssigned(event.orderId(), driverId, driverName, 15);
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
