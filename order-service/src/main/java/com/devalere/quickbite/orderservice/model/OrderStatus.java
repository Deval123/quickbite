package com.devalere.quickbite.orderservice.model;

public enum OrderStatus
{
    CREATED,
    PAYMENT_PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    PICKED_UP,
    DELIVERED,
    COMPLETED,
    CANCELLED
}
