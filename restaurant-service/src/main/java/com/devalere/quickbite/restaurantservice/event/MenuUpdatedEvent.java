package com.devalere.quickbite.restaurantservice.event;

public record MenuUpdatedEvent(
        String type,
        String restaurantId,
        String timestamp
) { }
