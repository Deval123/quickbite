package com.devalere.quickbite.dto;

import java.util.List;

// userId n'est pas un champ du body : il est toujours derive du JWT authentifie
// (voir OrderController.createOrder) pour eviter qu'un client puisse creer
// une commande au nom d'un autre utilisateur.
public record CreateOrderRequest(
        String restaurantId,
        List<OrderItemRequest> items,
        String deliveryAddress)
{
}
