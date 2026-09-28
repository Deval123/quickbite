package com.devalere.quickbite.notificationservice.config;

import com.devalere.quickbite.notificationservice.websocket.LocationBroadcastListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Configuration Redis Pub/Sub pour le broadcast multi-instances.
 * <p>
 * Problème : si la Notification Service tourne sur N instances,
 * un event Kafka est consommé par UNE seule instance.
 * Les clients connectés aux autres instances ne reçoivent rien.
 * <p>
 * Solution : l'instance qui reçoit l'évent Kafka publie dans
 * un channel Redis. Toutes les instances sont abonnées et
 * poussent le message à leurs clients WebSocket.
 */
@Configuration
public class RedisConfig {

    public static final String DELIVERY_LOCATION_CHANNEL = "delivery-location-broadcast";

    @Bean
    public ChannelTopic deliveryLocationTopic() {
        return new ChannelTopic(DELIVERY_LOCATION_CHANNEL);
    }

    @Bean
    public MessageListenerAdapter locationListenerAdapter(LocationBroadcastListener listener) {
        return new MessageListenerAdapter(listener, "onMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter locationListenerAdapter,
            ChannelTopic deliveryLocationTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(locationListenerAdapter, deliveryLocationTopic);
        return container;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
