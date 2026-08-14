package com.devalere.quickbite.restaurantservice.search;

import java.util.List;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

@Service
public class SearchService{

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final ElasticsearchOperations esOperations;
    private final RestaurantSearchRepository searchRepository;

    public SearchService(ElasticsearchOperations esOperations, RestaurantSearchRepository searchRepository) {
        this.esOperations = esOperations;
        this.searchRepository = searchRepository;
    }
    /**
     * Recherche combinée : full-text + geo + filtres.
     *
     * @param query       texte de recherche (ex "pizza", "ramen", "italian")
     * @param lat latitude du client (nullable)
     * @param lon longitude du client (nullable)
     * @param radiusKm rayon de recherche en km (default 5)
     * @param cuisine  filtre par type de cuisine (nullable)
     * @param minRating note minimum (nullable)
     * @return liste de restaurants triés par pertinence.
     */
    public List<SearchHit<RestaurantDocument>> search(
            String query,
            Double lat,
            Double lon,
            Double radiusKm,
            String cuisine,
            Double minRating)
    {

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 1. Full-text search avec fuzzy matching (top-level + nested menuItems).
        if (query != null && !query.isBlank()) {
            Query textQuery = Query.of(q -> q.multiMatch(mm -> mm
                    .query(query)
                    .fields("name^3", "description^2", "cuisineType", "address")
                    .fuzziness("AUTO")
                    .type(TextQueryType.BestFields)
            ));

            Query menuQuery = Query.of(q -> q.nested(n -> n
                    .path("menuItems")
                    .query(nq -> nq.match(mt -> mt
                            .field("menuItems.name")
                            .query(query)
                            .fuzziness("AUTO")
                    ))
            ));

            BoolQuery innerBool = BoolQuery.of(b -> b
                    .should(textQuery)
                    .should(menuQuery)
            );

            boolBuilder.must(m -> m.bool(innerBool));
        }

        // 2. Filtre : uniquement les restaurants actifs.
            boolBuilder.filter(f -> f.term(t -> t.field("active").value(true)));

            // 3. Filtre geo-distance
            if (lat != null && lon != null) {
                double radius = (radiusKm != null) ? radiusKm : 5.0;
                boolBuilder.filter(f -> f.geoDistance(g -> g
                        .field("location")
                        .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                        .distance(radius + "km")
                ));
            }

            // 4. Filtre par cuisine
        if (cuisine != null && !cuisine.isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t
                    .field("cuisineType")
                    .value(cuisine)
            ));
        }

        // 5. Filtre par note minimale
        if (minRating != null) {
            boolBuilder.filter(f -> f.range(r -> r
                    .number(n -> n
                            .field("avgRating")
                            .gte(minRating)
                    )
            ));
        }

        BoolQuery boolQuery = boolBuilder.build();
        log.info("ES Query: {}", Query.of(q -> q.bool(boolQuery)));

        var queryBuilder = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery))
                .withMaxResults(20);

        if (lat != null && lon != null) {
            queryBuilder.withSort(s -> s.geoDistance(g -> g
                    .field("location")
                    .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                    .order(SortOrder.Asc)
            ));
        }

        NativeQuery nativeQuery = queryBuilder.build();

        SearchHits<RestaurantDocument> hits = esOperations.search(nativeQuery, RestaurantDocument.class);
        log.info("Search '{}' returned {} results", query, hits.getTotalHits());
        return hits.getSearchHits();
    }

    /**
     * Indexation d'un restaurant dans Elasticsearch (création ou mise à jour).
     *
     * @param restaurantDocument restaurant indexé
     */
    public void indexRestaurant(RestaurantDocument restaurantDocument)
    {
        searchRepository.save(restaurantDocument);
        log.info("Indexed restaurant: {}", restaurantDocument.getName());
    }

    public void deleFromIndex(String restaurantId)
    {
        searchRepository.deleteById(restaurantId);
        log.info("Deleted restaurant from index: {}", restaurantId);
    }

    public void indexAll(List<RestaurantDocument> documents)
    {
        searchRepository.saveAll(documents);
        log.info("Bulk indexed {} restaurants", documents.size());
    }
}
