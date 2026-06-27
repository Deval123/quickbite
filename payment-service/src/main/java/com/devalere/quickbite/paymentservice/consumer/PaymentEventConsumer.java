package com.devalere.quickbite.paymentservice.consumer;


import com.devalere.quickbite.shared.security.KafkaSecurityHeaders;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "payment-service")
    public void onOrderCreated(ConsumerRecord<String, String> record) {
        // Reconstruire le contexte utilisateur depuis les headers
        String userId = KafkaSecurityHeaders.getUserId(record.headers());
        String roles = KafkaSecurityHeaders.getRoles(record.headers());

        System.out.println("Traitement paiement pour user: " + userId);
        System.out.println("Roles: " + roles);
        System.out.println("Order data: " + record.value());

        // Traiter le paiement...
    }
}