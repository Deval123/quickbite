package com.devalere.quickbite.dto;

public record OrderItemRequest(
        String menuItemId,
        int quantity)
{
}
