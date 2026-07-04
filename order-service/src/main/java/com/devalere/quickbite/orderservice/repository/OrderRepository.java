package com.devalere.quickbite.orderservice.repository;


import com.devalere.quickbite.orderservice.MODEL.Order;
import com.devalere.quickbite.orderservice.MODEL.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Trouver les commandes d'un utilisateur.
     * Un client ne voit que SES commandes.
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Trouver les commandes par statut.
     * Utilise par le dashboard restaurant.
     */
    List<Order> findByRestaurantIdAndStatus(UUID restaurantId, OrderStatus status);

    /**
     * Trouver les commandes en cours d'un utilisateur.
     */
    List<Order> findByUserIdAndStatusIn(String userId, List<OrderStatus> statuses);
}