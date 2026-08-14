package com.devalere.quickbite.restaurantservice.search;

import java.util.List;

import com.devalere.quickbite.restaurantservice.model.Restaurant;
import com.devalere.quickbite.restaurantservice.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RestaurantSearchInitializer
{
    private static final Logger log = LoggerFactory.getLogger(RestaurantSearchInitializer.class);

    private final SearchService searchService;
    private final RestaurantRepository restaurantRepository;
    private final SearchIndexer searchIndexer;

    public RestaurantSearchInitializer(SearchService searchService, RestaurantRepository restaurantRepository, SearchIndexer searchIndexer)
    {

        this.searchService = searchService;
        this.restaurantRepository = restaurantRepository;
        this.searchIndexer = searchIndexer;
    }

    /**
     * Au démarrage, indexe tous les restaurants existants dans elasticsearch. Permet d'avoir un index à jour même si
     * des restaurants ont été cées avant l'ajout d'Elasticsearch.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void indexAllRestaurants()
    {
        log.info("=== Bootstrap Elasticsearch : indexation de tous les restaurants ===");

        List<Restaurant> restaurants = restaurantRepository.findAll();

        List<RestaurantDocument> documents = restaurants.stream()
                .map(searchIndexer::toDocument)
                .toList();
        searchService.indexAll(documents);

        log.info("=== Bootstrap termine : {} restaurants indexes ===", documents.size());
    }
}
