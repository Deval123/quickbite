package com.devalere.quickbite.restaurantservice.search;

import java.util.List;
import java.util.UUID;

import com.devalere.quickbite.restaurantservice.model.MenuItem;
import com.devalere.quickbite.restaurantservice.model.Restaurant;
import com.devalere.quickbite.restaurantservice.repository.MenuItemRepository;
import com.devalere.quickbite.restaurantservice.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class SearchIndexer
{
    private static final Logger log = LoggerFactory.getLogger(SearchIndexer.class);

    private final SearchService searchService;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final ObjectMapper objectMapper;

    public SearchIndexer(SearchService searchService, MenuItemRepository menuItemRepository, RestaurantRepository restaurantRepository, ObjectMapper objectMapper)
    {
        this.searchService = searchService;
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Écoute les events RestaurantUpdatedEvent sur le topic restaurant-event.
     * Re-indexe le restaurant dans elasticsearch.
     *
     *
     * @param message message Kafka
     */
    @KafkaListener(topics = "restaurant-event", groupId = "search-indexer-group")
    public void onRestaurantEvent(String message)
    {
        try
        {
            JsonNode jsonNode = objectMapper.readTree(message);
            String restaurantId = jsonNode.get("restaurantId").asText();

            log.info("Received restaurant event for {}, re-indexing...", restaurantId);

            Restaurant restaurant = restaurantRepository.findById(UUID.fromString(restaurantId)).orElse(null);

            if (restaurant == null)
            {
                log.warn("Restaurant not found for id {}, removing from index", restaurantId);
                searchService.deleFromIndex(restaurantId);
                return;
            }

            searchService.indexRestaurant(toDocument(restaurant));
            log.info("Re-indexed restaurant {} in Elasticsearch", restaurantId);
        }
        catch (Exception e)
        {
            log.error("Error processing restaurant event: {}", e.getMessage(), e);
        }
    }

    /**
     * Convertit un restaurant JPA en document RestaurantDocument) ElasticSearch.
     *
     * @param restaurant restaurant JPA
     * @return document ElasticSearch
     */
    public RestaurantDocument toDocument(Restaurant restaurant)
    {
        RestaurantDocument document = new RestaurantDocument();
        document.setId(restaurant.getId().toString());
        document.setName(restaurant.getName());
        document.setDescription(restaurant.getDescription());
        document.setCuisineType(restaurant.getCuisineType());
        document.setAddress(restaurant.getAddress());
        document.setPhone(restaurant.getPhone());
        document.setLocation(new GeoPoint(restaurant.getLatitude(), restaurant.getLongitude()));
        document.setAvgRating(restaurant.getAvgRating());
        document.setActive(restaurant.isActive());

        // Geo point
        if (restaurant.getLatitude() != null && restaurant.getLongitude() != null)
        {
            document.setLocation(new GeoPoint(restaurant.getLatitude(), restaurant.getLongitude()));

            List<MenuItem> items = menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurant.getId());

            List<RestaurantDocument.MenuItemDoc> menuItemDocs = items.stream()
                    .map(item -> new RestaurantDocument.MenuItemDoc(
                            item.getName(),
                            item.getPrice().doubleValue(),
                            item.getCategory(),
                            item.isAvailable()
                    )).toList();
            document.setMenuItems(menuItemDocs);
        }

        return document;
    }
}
