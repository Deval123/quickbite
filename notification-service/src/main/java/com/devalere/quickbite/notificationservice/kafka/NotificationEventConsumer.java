package com.devalere.quickbite.notificationservice.kafka;

import com.devalere.quickbite.kafka.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer du Notification Service.
 * Écoute TOUS les topics pour envoyer des notifications au client.
 * Email, push, SMS selon le type d'event.
 */
@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

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
        log.info("[NOTIFICATION] Event recu: topic={}, type={}, orderId={}",
                record.topic(), eventType, record.key());

        // TODO: dispatcher selon le type d'event
        switch (eventType) {
        case "OrderCreatedEvent" ->
                log.info("Email: 'Votre commande {} a ete creee'", record.key());
        case "PaymentCompletedEvent" ->
                log.info("Email: 'Paiement confirme pour commande {}'", record.key());
        case "PaymentFailedEvent" ->
                log.info("Email: 'Echec paiement pour commande {}'", record.key());
        case "OrderConfirmedEvent" ->
                log.info("Push: 'Le restaurant prepare votre commande {}'", record.key());
        case "OrderReadyEvent" ->
                log.info("Push: 'Votre commande {} est prete !'", record.key());
        case "DeliveryAssignedEvent" ->
                log.info("Push: 'Un livreur arrive pour commande {}'", record.key());
        case "DeliveryCompletedEvent" ->
                log.info("Push: 'Commande {} livree ! Bon appetit !'", record.key());
        default ->
                log.warn("Event inconnu: {}", eventType);
        }
    }

    private String getEventType(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader("event-type");
        return header != null ? new String(header.value()) : "unknown";
    }
}
