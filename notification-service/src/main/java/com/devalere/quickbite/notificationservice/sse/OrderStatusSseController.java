package com.devalere.quickbite.notificationservice.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Controller SSE pour le suivi du statut de commande.
 * <p>
 * Le client fait GET /api/orders/{orderId}/stream
 * et reçoit un flux SSE (text/event-stream) avec les changements
 * de statut : CONFIRMED, PREPARING, READY, PICKED_UP, DELIVERING, DELIVERED.
 * <p>
 * La connexion reste ouverte. Si elle est coupee, le navigateur
 * EventSource reconnecte automatiquement.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderStatusSseController {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusSseController.class);

    private final SseEmitterRegistry emitterRegistry;

    public OrderStatusSseController(SseEmitterRegistry emitterRegistry) {
        this.emitterRegistry = emitterRegistry;
    }

    /**
     * Ouvre un flux SSE pour une commande.
     * Le client s'abonne et reçoit les events de changement de statut.
     * <p>
     * Timeout : 30 minutes (durée max d'une livraison).
     * Keepalive : un commentaire vide toutes les 30 secondes
     * pour éviter que les proxies ne coupent la connexion.
     */
    @GetMapping(value = "/{orderId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrderStatus(@PathVariable String orderId) {
        // Timeout 30 minutes
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitterRegistry.register(orderId, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE connexion fermee pour orderId={}", orderId);
            emitterRegistry.remove(orderId, emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE timeout pour orderId={}", orderId);
            emitterRegistry.remove(orderId, emitter);
        });

        emitter.onError(e -> {
            log.warn("SSE erreur pour orderId={}: {}", orderId, e.getMessage());
            emitterRegistry.remove(orderId, emitter);
        });

        log.info("SSE connexion ouverte pour orderId={}", orderId);

        return emitter;
    }
}
