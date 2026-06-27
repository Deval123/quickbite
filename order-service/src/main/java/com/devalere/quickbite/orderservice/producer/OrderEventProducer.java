package com.devalere.quickbite.orderservice.producer;

import com.devalere.quickbite.shared.security.KafkaSecurityHeaders;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(String orderId, String payload) {
        // Extraire userId et roles du JWT courant
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaimAsString("sub");
        String roles = String.join(",", jwt.getClaimAsStringList("realm_access"));

        // Creer le record Kafka
        var record = new ProducerRecord<String, String>(
                "order-events",
                orderId,   // key = orderId (partitioning)
                payload    // value = event JSON
        );

        // Ajouter les headers de securite
        KafkaSecurityHeaders.addSecurityHeaders(
                record.headers(),
                userId,
                roles
        );

        kafkaTemplate.send(record);
    }
}
