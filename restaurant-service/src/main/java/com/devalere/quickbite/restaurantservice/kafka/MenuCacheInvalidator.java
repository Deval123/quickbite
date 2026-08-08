package com.devalere.quickbite.restaurantservice.kafka;

import com.devalere.quickbite.restaurantservice.event.MenuUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * MenuUpdatedEvent pour invalider le cache redis.
 * Utile quand un AUTRE service ou instance modifie les données.
 */
@Component
public class MenuCacheInvalidator
{
    private static  final Logger log = LoggerFactory.getLogger(MenuCacheInvalidator.class);
    private final ObjectMapper objectMapper;

    public MenuCacheInvalidator(ObjectMapper objectMapper)
    {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "restaurant-events",
            groupId = "restaurant-cache-invalidator",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMenuUpdated(String message){
        try
        {
            MenuUpdatedEvent event = objectMapper.readValue(message, MenuUpdatedEvent.class);
            if("MenuUpdatedEvent".equals(event.type())){
                evictMenuCache(event.restaurantId());
                log.info("Cache invalide pour restaurant {} via Kafka event",
                        event.restaurantId());
            }
        } catch (Exception e){
            log.error("Erreur traitement MenuUpdatedEvent: {}", e.getMessage());
        }

    }

    @CacheEvict(value = "menus", key = "#restaurantId")
    public void evictMenuCache(String restaurantId){
        log.info("Eviction cache menu pour restaurant {}", restaurantId);
    }
}
