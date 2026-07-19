package com.devalere.quickbite.events;

import java.math.BigDecimal;

/**
 * Sous-objet partagé dans les events de commande.
 * Contient les données d'un item commande.
 * @param menuItemId
 * @param menuItemName
 * @param quantity
 * @param unitPrice
 */
public record OrderItemData(
        String menuItemId,
        String menuItemName,
        int quantity,
        BigDecimal unitPrice
)
{
}
