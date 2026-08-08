package com.devalere.quickbite.restaurantservice.service;


import com.devalere.quickbite.restaurantservice.model.MenuItem;
import com.devalere.quickbite.restaurantservice.model.Restaurant;
import com.devalere.quickbite.restaurantservice.repository.MenuItemRepository;
import com.devalere.quickbite.restaurantservice.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RestaurantService {
    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public RestaurantService(RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Récupérer le menu d'un restaurant.
     * Cache-Aside : Redis d'abord, PostgreSQL si miss.
     * sync = true : protection contre le cache stampede.
     * @param restaurantId l'id du restaurant.
     * @return une liste de menu item.
     */
    @Cacheable(value = "menus", key = "#restaurantId", sync = true)
    public List<MenuItem> getMenu(String restaurantId)
    {
        log.info("CACHE MISS - Chargement menu depuis PostgreSQL pour restaurant {}", restaurantId);
        UUID restaurantUUID = UUID.fromString(restaurantId);
        return menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantUUID);
    }

    /**
     * Récupérer les infos d'un restaurant.
     * @param restaurantId l'id du restaurant.
     * @return un restaurant
     */
    @Cacheable(value = "restaurants", key = "#restaurantId", sync = true)
    public Restaurant getRestaurant(UUID restaurantId)
    {
        log.info("CACHE MISS - Chargement restaurant depuis PostgreSQL pour restaurant {}", restaurantId);
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found: " + restaurantId));
    }

    /**
     * Modifier un item menu.
     * CacheEvict : invalide le cache du menu après modification.
     * Publie un MenuUpdatedEvent pour que les autres services invalident aussi.
     * @param restaurantId l'id du restaurant.
     * @param itemId l'id de l'item.
     * @param updated le menu à modifier
     * @return un item menu.
     */
    @CacheEvict(value = "menus", key = "#restaurantId")
    @Transactional
    public MenuItem updateMenuItem(String restaurantId, UUID itemId, MenuItem updated){
        MenuItem menuItem = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        menuItem.setName(updated.getName());
        menuItem.setDescription(updated.getDescription());
        menuItem.setPrice(updated.getPrice());
        menuItem.setAvailable(updated.isAvailable());

        MenuItem savedItem = menuItemRepository.save(menuItem);

        // Publier un événement pour invalidation distribuée.

        publishMenuUpdatedEvent(restaurantId);
        log.info("Menu item modifie: {} pour restaurant {}", itemId, restaurantId);

        return savedItem;
    }

    /**
     * Publier un MenuUpdatedEvent sur Kafka.
     * Tous les services qui cachent ce menu recevront l'évent.
     * @param restaurantId l'id du restaurant.
     */
    private void publishMenuUpdatedEvent(String restaurantId)
    {
        String event = """
                {"type":"MenuUpdatedEvent","restaurantId":"%s","timestamp":"%s"}
                """.formatted(restaurantId, Instant.now());

        kafkaTemplate.send("restaurant-event", restaurantId, event);
        log.info("MenuUpdatedEvent publié pour restaurant {}", restaurantId);
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