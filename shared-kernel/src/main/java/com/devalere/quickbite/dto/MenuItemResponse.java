package com.devalere.quickbite.dto;

public record MenuItemResponse(
    String id,
    String name,
    String description,
    double price,
    String category,
    boolean available)
{
}
