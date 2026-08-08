package com.devalere.quickbite.restaurantservice.controller;


import com.devalere.quickbite.restaurantservice.model.MenuItem;
import com.devalere.quickbite.restaurantservice.model.Restaurant;
import com.devalere.quickbite.restaurantservice.service.RestaurantService;
import com.devalere.quickbite.dto.MenuItemResponse;
import com.devalere.quickbite.dto.RestaurantResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable UUID id) {
        Restaurant restaurant = restaurantService.findById(id);
        return ResponseEntity.ok(toResponse(restaurant));
    }

    @GetMapping("/{id}/menu-items")
    public ResponseEntity<List<MenuItemResponse>> getMenuItems(@PathVariable UUID id) {
        // Utilise getMenu(String) qui passe par le cache Redis (@Cacheable)
        List<MenuItem> items = restaurantService.getMenu(id.toString());
        List<MenuItemResponse> response = items.stream()
                .map(this::toMenuItemResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/menu-items/{itemId}")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestBody MenuItem updated) {
        MenuItem item = restaurantService.updateMenuItem(id.toString(), itemId, updated);
        return ResponseEntity.ok(toMenuItemResponse(item));
    }

    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> getActiveRestaurants() {
        List<Restaurant> restaurants = restaurantService.findAllActive();
        List<RestaurantResponse> response = restaurants.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    private RestaurantResponse toResponse(Restaurant r) {
        return new RestaurantResponse(
                r.getId().toString(), r.getName(), r.getAddress(),
                r.getPhone(), r.isActive(), r.getCreatedAt()
        );
    }

    private MenuItemResponse toMenuItemResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId().toString(), item.getName(), item.getDescription(),
                item.getPrice().doubleValue(), item.getCategory(), item.isAvailable()
        );
    }
}