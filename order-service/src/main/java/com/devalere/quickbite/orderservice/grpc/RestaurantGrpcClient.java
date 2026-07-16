package com.devalere.quickbite.orderservice.grpc;

import com.devalere.quickbite.grpc.restaurant.*;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RestaurantGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(RestaurantGrpcClient.class);

    @GrpcClient("restaurant-service")
    private RestaurantServiceGrpc.RestaurantServiceBlockingStub restaurantStub;


    /**
     * Vérifie la disponibilité des items avant de créer une commande. Appel gRPC synchrone vers restaurant-service.
     *
     * @param restaurantId l'id du restaurant.
     * @param itemIds les ids des items.
     * @return la réponse gRPC contenant la disponibilité et les prix des items.
     */
    public CheckItemsResponse checkItemsAvailability(String restaurantId, List<String> itemIds)
    {
        try
        {
            CheckItemsRequest request = CheckItemsRequest.newBuilder()
                    .setRestaurantId(restaurantId)
                    .addAllItemIds(itemIds)
                    .build();

            log.info("gRPC call: checkItemsAvailability for restaurant {} with {} items",
                    restaurantId, itemIds.size());

            CheckItemsResponse response = restaurantStub.checkItemsAvailability(request);

            log.info("gRPC response: allAvailable={}", response.getAllAvailable());
            return response;

        }
        catch (StatusRuntimeException e)
        {
            log.error("gRPC error calling restaurant-service: {}", e.getStatus(), e);
            throw new RuntimeException("Failed to verify menu items: " + e.getStatus(), e);
        }
    }

    /**
     * Récupère tous les items du menu d'un restaurant.
     *
     * @param restaurantId l'id du restaurant.
     * @return la liste des items.
     */
    public GetMenuItemsResponse getMenuItems(String restaurantId)
    {
        try
        {
            GetMenuItemsRequest request = GetMenuItemsRequest.newBuilder()
                    .setRestaurantId(restaurantId)
                    .build();

            return restaurantStub.getMenuItems(request);

        }
        catch (StatusRuntimeException e)
        {
            log.error("gRPC error fetching menu items: {}", e.getStatus(), e);
            throw new RuntimeException("Failed to fetch menu items: " + e.getStatus(), e);
        }
    }
}