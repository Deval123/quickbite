package com.devalere.quickbite.restaurantservice.config;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@EnableCaching
public class RedisCacheConfig
{
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // ObjectMapper avec default typing : stocke @class dans le JSON Redis
        // pour désérialiser correctement en MenuItem au lieu de LinkedHashMap
        ObjectMapper objectMapper = JsonMapper.builder()
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class)
                                .build(),
                        DefaultTyping.NON_FINAL)
                .build();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJacksonJsonRedisSerializer(objectMapper)))
                .disableCachingNullValues()
                .prefixCacheNameWith("quickbite:");

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("menus", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("restaurants", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}
