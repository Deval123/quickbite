package com.devalere.quickbite.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Integration Event émis quand une commande est créee.
 * Consommé par : PaymentService, RestaurantService, NotificationService.
 * @param orderId
 * @param userId
 * @param restaurantId
 * @param items
 * @param totalAmount
 * @param deliveryAddress
 * @param createdAt
 */
public record OrderCreatedEvent(
        String orderId,
        String userId,
        String restaurantId,
        List<OrderItemData> items,
        BigDecimal totalAmount,
        String deliveryAddress,
        Instant createdAt)
{
}
