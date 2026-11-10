package com.devalere.quickbite.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Deduplication des notifications via Redis SET NX.
 * Cle : notif:{userId}:{channel}:{eventType}:{orderId}
 * TTL : 5 minutes
 *
 * Si la cle existe deja -> doublon, on skip.
 * Si la cle n'existe pas -> on insere et on envoie.
 */
@Service
public class DeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);
    private static final Duration DEDUP_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public DeduplicationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * @return true si c'est un NOUVEAU event (pas de doublon), false si doublon
     */
    public boolean tryAcquire(String userId, String channel, String eventType, String orderId) {
        String key = "notif:" + userId + ":" + channel + ":" + eventType + ":" + orderId;

        // SET NX = Set if Not eXists. Atomique.
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", DEDUP_TTL);

        if (Boolean.TRUE.equals(acquired)) {
            log.debug("[DEDUP] Nouvelle notification : {}", key);
            return true;
        } else {
            log.info("[DEDUP] Doublon detecte, skip : {}", key);
            return false;
        }
    }
}
