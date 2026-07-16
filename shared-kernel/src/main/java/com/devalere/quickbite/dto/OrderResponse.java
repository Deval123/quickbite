package com.devalere.quickbite.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    String id,
    String restaurantId,
    String userId,
    String status,
    BigDecimal totalAmount,
    List<OrderItemResponse> items,
    String deliveryAddress,
    LocalDateTime createdAt)
{
    public record OrderItemResponse(
            String menuItemId,
            String menuItemName,
            int quantity,
            BigDecimal unitPrice)
    {

    }

}
