package com.devalere.quickbite.restaurantservice.kafka;

import com.devalere.quickbite.events.OrderCreatedEvent;
import com.devalere.quickbite.kafka.KafkaTopics;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumer du Restaurant Service.
 * Ecoute order-events pour recevoir les nouvelles commandes.
 */
@Component
public class RestaurantEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RestaurantEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final RestaurantEventProducer producer;

    public RestaurantEventConsumer(ObjectMapper objectMapper, RestaurantEventProducer producer) {
        this.objectMapper = objectMapper;
        this.producer = producer;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "restaurant-group")
    public void onOrderEvent(ConsumerRecord<String, String> record) {
        String eventType = getEventType(record);
        log.info("Restaurant recu event {} pour orderId={}", eventType, record.key());

        try {
            if ("OrderCreatedEvent".equals(eventType)) {
                var event = objectMapper.readValue(record.value(), OrderCreatedEvent.class);
                log.info("Nouvelle commande {} : pour restaurant {} : {} items",
                        event.orderId(), event.restaurantId(), event.items().size());

                // TODO: verifier les items, estimer le temps de preparation
                // Pour l'instant, on confirme automatiquement
                // producer.publishOrderConfirmed(event.orderId(), event.restaurantId(), 15);
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
