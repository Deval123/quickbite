package com.devalere.quickbite.dto;

import java.time.LocalDateTime;

public record RestaurantResponse(
    String id,
    String name,
    String address,
    String phone,
    boolean active,
    LocalDateTime createAt)
{
}
