package com.devalere.quickbite.restaurantservice.search;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController
{
    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService)
    {
        this.searchService = searchService;
    }

    /**
     * Recherche de restaurants. GET
     * /api/search/restaurants?q=pizza&lat=48.8566&lon=2.3522&radius=3&cuisine=Italien&minRating=4.0
     *
     * @param q         requis (ex "pizza", "ramen", "italian")
     * @param lat       latitude du client (nullable)
     * @param lon       longitude du client (nullable)
     * @param radius    rayon de recherche en km (default 5)
     * @param cuisine   Filtre par type de cuisine (nullable)
     * @param minRating Note minimum (nullable)
     * @return
     */
    @GetMapping("/restaurants")
    public ResponseEntity<List<Map<String, Object>>> searchRestaurants(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false, defaultValue = "5") Double radius,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) Double minRating)
    {

        log.info("Search request: query={}, lat={}, lon={}, radiusKm={}, cuisine={}, minRating={}",
                q, lat, lon, radius, cuisine, minRating);

        List<SearchHit<RestaurantDocument>> hits = searchService.search(
                q, lat, lon, radius, cuisine, minRating);

        List<Map<String, Object>> results = hits.stream()
                .map(hit -> Map.<String, Object>of(
                        "score", hit.getScore(),
                        "restaurant", hit.getContent()
                ))
                .toList();

        return ResponseEntity.ok(results);
    }
}
