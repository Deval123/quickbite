package com.devalere.quickbite.restaurantservice.search;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface RestaurantSearchRepository extends ElasticsearchRepository<RestaurantDocument, String>
{
    List<RestaurantDocument> findByNameContaining(String name);
    List<RestaurantDocument> findByCuisineType(String cuisineType);
    List<RestaurantDocument> findByActiveTrue();
}
