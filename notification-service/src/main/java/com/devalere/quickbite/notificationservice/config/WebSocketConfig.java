package com.devalere.quickbite.notificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration WebSocket avec STOMP + SockJS.
 * <p>
 * - Endpoint : /ws/tracking (avec fallback SockJS). Prefix pour les destinations de broadcast : /topic
 * - Prefix pour les messages entrants depuis le client : /app
 * <p>
 * Le client s'abonne à /topic/delivery/{orderId} pour recevoir
 * la position GPS du livreur en temps reel.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Les messages broadcastes vers les clients commencent par /topic
        config.enableSimpleBroker("/topic");
        // Les messages envoyes PAR le client commencent par /app
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket avec fallback SockJS
        registry.addEndpoint("/ws/tracking")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
