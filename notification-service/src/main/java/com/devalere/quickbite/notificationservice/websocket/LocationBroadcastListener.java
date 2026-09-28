package com.devalere.quickbite.notificationservice.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Listener Redis Pub/Sub.
 * Reçoit les positions GPS broadcastées par Redis
 * et les pousse via WebSocket STOMP aux clients abonnés.
 * <p>
 * Chaque instance du Notification Service reçoit le message Redis
 * et pousse aux clients connectés à CETTE instance.
 * C'est le pattern broadcast multi-instances.
 */
@Component
public class LocationBroadcastListener {

    private static final Logger log = LoggerFactory.getLogger(LocationBroadcastListener.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public LocationBroadcastListener(SimpMessagingTemplate messagingTemplate,
                                     ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Appelé par Redis quand un message arrive sur le channel delivery-location-broadcast.
     * Parse le JSON pour extraire orderId, puis broadcast sur /topic/delivery/{orderId}.
     */
    public void onMessage(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String orderId = json.get("orderId").asText();

            // Push via STOMP vers tous les clients abonnes a cette commande
            messagingTemplate.convertAndSend("/topic/delivery/" + orderId, message);

            log.debug("WebSocket push pour orderId={}: lat={}, lon={}",
                    orderId,
                    json.get("latitude").asDouble(),
                    json.get("longitude").asDouble());

        } catch (Exception e) {
            log.error("Erreur broadcast WebSocket: {}", e.getMessage(), e);
        }
    }
}
