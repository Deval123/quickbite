package com.devalere.quickbite.orderservice.saga;

import com.devalere.quickbite.events.*;
import com.devalere.quickbite.orderservice.kafka.OrderEventProducer;
import com.devalere.quickbite.orderservice.model.Order;
import com.devalere.quickbite.orderservice.model.OrderStatus;
import com.devalere.quickbite.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Saga Orchestrator pour le flux de commande.
 * Coordonne les étapes : Payment → Restaurant → Delivery.
 * Gere les compensations en cas d'échec.
 */
@Component
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    public OrderSagaOrchestrator(OrderRepository orderRepository,
            OrderEventProducer orderEventProducer) {
        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
    }

    // ===== STEP 1 : Paiement =====

    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        var order = findOrder(event.orderId());
        if (order == null || order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            log.warn("Ignoring PaymentCompleted for order {} (status={})",
                    event.orderId(), order != null ? order.getStatus() : "NOT_FOUND");
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Saga step 1 OK : commande {} paiement confirme -> CONFIRMED", order.getId());
        // RestaurantService reçoit deja OrderCreatedEvent via Kafka
        // Il va confirmer ou refuser via OrderConfirmedEvent
    }

    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        var order = findOrder(event.orderId());
        if (order == null) return;

        log.warn("Saga compensation : paiement echoue pour commande {} : {}",
                event.orderId(), event.failureReason());
        cancelOrder(order, "Paiement echoue : " + event.failureReason());
    }

    // ===== STEP 2 : Restaurant =====

    @Transactional
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        var order = findOrder(event.orderId());
        if (order == null || order.getStatus() != OrderStatus.CONFIRMED) {
            log.warn("Ignoring OrderConfirmed for order {} (status={})",
                    event.orderId(), order != null ? order.getStatus() : "NOT_FOUND");
            return;
        }

        order.setStatus(OrderStatus.PREPARING);
        orderRepository.save(order);
        log.info("Saga step 2 OK : commande {} confirmee par restaurant, prep ~{} min -> PREPARING",
                order.getId(), event.estimatePreparationMinutes());
    }

    @Transactional
    public void onOrderReady(OrderReadyEvent event) {
        var order = findOrder(event.orderId());
        if (order == null || order.getStatus() != OrderStatus.PREPARING) return;

        order.setStatus(OrderStatus.READY);
        orderRepository.save(order);
        log.info("Saga step 2 complete : commande {} prete -> READY", order.getId());
        // DeliveryService reçoit OrderReadyEvent via restaurant-events
    }

    // ===== STEP 3 : Livraison =====

    @Transactional
    public void onDeliveryAssigned(DeliveryAssignedEvent event) {
        var order = findOrder(event.orderId());
        if (order == null || order.getStatus() != OrderStatus.READY) return;

        order.setStatus(OrderStatus.PICKED_UP);
        orderRepository.save(order);
        log.info("Saga step 3 : commande {} en cours de livraison par {} -> PICKED_UP",
                order.getId(), event.driverName());
    }

    @Transactional
    public void onDeliveryCompleted(DeliveryCompletedEvent event) {
        var order = findOrder(event.orderId());
        if (order == null || order.getStatus() != OrderStatus.PICKED_UP) return;

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
        log.info("Saga COMPLETE : commande {} livrée -> DELIVERED", order.getId());
    }

    // ===== Compensation =====

    @Transactional
    public void cancelOrder(Order order, String reason) {
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Publier l'évent d'annulation pour déclencher les compensations
        var cancelEvent = new OrderCancelledEvent(
                order.getId().toString(),
                order.getUserId(),
                reason,
                Instant.now()
        );
        orderEventProducer.publishOrderCancelled(cancelEvent);

        log.info("Commande {} annulée (était {}) : {}. Compensation déclenchée.",
                order.getId(), previousStatus, reason);
    }

    // ===== Timeout =====

    @Transactional
    public void handleTimeout(Order order) {
        log.warn("Timeout : commande {} bloquée en {} depuis trop longtemps",
                order.getId(), order.getStatus());
        cancelOrder(order, "Timeout : pas de réponse dans le délai imparti");
    }

    private Order findOrder(String orderId) {
        try {
            return orderRepository.findById(UUID.fromString(orderId)).orElse(null);
        } catch (IllegalArgumentException e) {
            log.error("orderId invalide : {}", orderId);
            return null;
        }
    }
}