package com.devalere.quickbite.restaurantservice.service;


import com.devalere.quickbite.restaurantservice.model.MenuItem;
import com.devalere.quickbite.restaurantservice.model.Restaurant;
import com.devalere.quickbite.restaurantservice.repository.MenuItemRepository;
import com.devalere.quickbite.restaurantservice.repository.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public RestaurantService(RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public Restaurant findById(UUID id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found: " + id));
    }

    public List<Restaurant> findAllActive() {
        return restaurantRepository.findByActiveTrue();
    }

    public List<MenuItem> getMenuItems(UUID restaurantId) {
        // Vérifie que le restaurant existe
        findById(restaurantId);
        return menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId);
    }

    public List<MenuItem> getMenuItemsByIds(UUID restaurantId, List<UUID> itemIds) {
        return menuItemRepository.findByRestaurantIdAndIdIn(restaurantId, itemIds);
    }
}