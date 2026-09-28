package com.devalere.quickbite.notificationservice.kafka;

import com.devalere.quickbite.kafka.KafkaTopics;
import com.devalere.quickbite.notificationservice.config.RedisConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer Kafka pour les events de position GPS du livreur.
 * <p>
 * Flux: Delivery Service -> Kafka (delivery-location-events)
 *        -> CE consumer -> Redis Pub/Sub -> toutes les instances
 *        -> WebSocket STOMP -> clients
 * <p>
 * On ne pousse PAS directement via SimpMessagingTemplate ici.
 * On passe par Redis Pub/Sub pour que TOUTES les instances
 * du Notification Service recoivent le message et puissent
 * pousser aux clients connectés à elles.
 */
@Component
public class DeliveryLocationConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryLocationConsumer.class);

    private final StringRedisTemplate redisTemplate;

    public DeliveryLocationConsumer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(
            topics = KafkaTopics.DELIVERY_LOCATION_EVENTS,
            groupId = "notification-location-group"
    )
    public void onDeliveryLocation(ConsumerRecord<String, String> record) {
        String orderId = record.key();
        String payload = record.value();

        log.info("[LOCATION] Position recue pour orderId={}", orderId);

        // Broadcast via Redis Pub/Sub à toutes les instances
        redisTemplate.convertAndSend(RedisConfig.DELIVERY_LOCATION_CHANNEL, payload);
    }
}
