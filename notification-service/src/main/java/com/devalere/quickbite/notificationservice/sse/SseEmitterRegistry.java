package com.devalere.quickbite.notificationservice.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry thread-safe pour les SseEmitter actifs.
 * <p>
 * Un orderId peut avoir plusieurs emitters (ex: le client
 * ouvre l'app sur son telephone ET sur son navigateur).
 * <p>
 * ConcurrentHashMap + CopyOnWriteArrayList pour la thread-safety
 * puisque les events Kafka arrivent sur des threads différents
 * des requêtes HTTP.
 */
@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Enregistre un nouveau SseEmitter pour une commande.
     */
    public void register(String orderId, SseEmitter emitter) {
        emitters.computeIfAbsent(orderId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SSE emitter enregistre pour orderId={}, total={}",
                orderId, emitters.get(orderId).size());
    }

    /**
     * Retire un SseEmitter (deconnexion, timeout, erreur).
     */
    public void remove(String orderId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(orderId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(orderId);
            }
        }
    }

    /**
     * Envoie un event SSE a tous les clients abonnés a cette commande.
     * Les emitters morts sont automatiquement retirés.
     */
    public void send(String orderId, String eventName, Object data) {
        List<SseEmitter> list = emitters.get(orderId);
        if (list == null || list.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.debug("SSE emitter mort pour orderId={}, suppression", orderId);
                deadEmitters.add(emitter);
            }
        }

        deadEmitters.forEach(e -> remove(orderId, e));
    }
}
