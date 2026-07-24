package com.devalere.quickbite.orderservice.saga;

import java.time.LocalDateTime;

import com.devalere.quickbite.orderservice.model.OrderStatus;
import com.devalere.quickbite.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vérifier les commandes bloquées en états intermediaires.
 * Si aucun event n'est react dans le délai, on annule.
 */
@Component
public class SagaTimeoutScheduler
{

    private static final Logger log = LoggerFactory.getLogger(SagaTimeoutScheduler.class);
    private static final int TIMEOUT_MINUTES = 5; // délai d'attente pour les étapes de la saga

    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator orderSagaOrchestrator;

    public SagaTimeoutScheduler(OrderRepository orderRepository, OrderSagaOrchestrator orderSagaOrchestrator)
    {
        this.orderRepository = orderRepository;
        this.orderSagaOrchestrator = orderSagaOrchestrator;
    }

    @Scheduled(fixedRate = 60_000) // toutes les 60 secondes
    public void checkTimeouts(){
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);


        // Commandes en PAYMENT_PENDING depuis trop longtemps
        var stuckOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PAYMENT_PENDING, cutoff);

        for (var order : stuckOrders) {
            orderSagaOrchestrator.handleTimeout(order);
        }

        if(!stuckOrders.isEmpty()){
            log.info("Timeout : {} commande (s) annulée(s)", stuckOrders.size());
        }
    }
}
