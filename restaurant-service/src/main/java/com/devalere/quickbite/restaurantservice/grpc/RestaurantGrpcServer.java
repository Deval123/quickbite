package com.devalere.quickbite.restaurantservice.grpc;

import java.util.List;
import java.util.UUID;

import com.devalere.quickbite.grpc.restaurant.CheckItemsRequest;
import com.devalere.quickbite.grpc.restaurant.CheckItemsResponse;
import com.devalere.quickbite.grpc.restaurant.GetMenuItemsRequest;
import com.devalere.quickbite.grpc.restaurant.GetMenuItemsResponse;
import com.devalere.quickbite.grpc.restaurant.ItemAvailability;
import com.devalere.quickbite.grpc.restaurant.MenuItemProto;
import com.devalere.quickbite.grpc.restaurant.RestaurantServiceGrpc;
import com.devalere.quickbite.restaurantservice.model.MenuItem;
import com.devalere.quickbite.restaurantservice.service.RestaurantService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class RestaurantGrpcServer extends RestaurantServiceGrpc.RestaurantServiceImplBase
{
    private final RestaurantService restaurantService;

    public RestaurantGrpcServer(RestaurantService restaurantService)
    {
        this.restaurantService = restaurantService;
    }

    @Override
    public void getMenuItems(GetMenuItemsRequest request,
            StreamObserver<GetMenuItemsResponse> responseObserver) {

        UUID restaurantId = UUID.fromString(request.getRestaurantId());
        List<MenuItem> items = restaurantService.getMenuItems(restaurantId);

        GetMenuItemsResponse.Builder responseBuilder = GetMenuItemsResponse.newBuilder()
                .setRestaurantId(request.getRestaurantId());

        for (MenuItem item : items) {
            responseBuilder.addItems(
                    MenuItemProto.newBuilder()
                            .setId(item.getId().toString())
                            .setName(item.getName())
                            .setPrice(item.getPrice().doubleValue())
                            .setAvailable(item.isAvailable())
                            .setCategory(item.getCategory())
                            .build()
            );
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void checkItemsAvailability(CheckItemsRequest request,
            StreamObserver<CheckItemsResponse> responseObserver) {

        UUID restaurantId = UUID.fromString(request.getRestaurantId());
        List<UUID> itemIds = request.getItemIdsList().stream()
                .map(UUID::fromString)
                .toList();

        List<MenuItem> items = restaurantService.getMenuItemsByIds(restaurantId, itemIds);

        CheckItemsResponse.Builder responseBuilder = CheckItemsResponse.newBuilder();
        boolean allAvailable = true;

        for (UUID requestedId : itemIds) {
            MenuItem found = items.stream()
                    .filter(i -> i.getId().equals(requestedId))
                    .findFirst()
                    .orElse(null);

            boolean available = found != null && found.isAvailable();
            double price = found != null ? found.getPrice().doubleValue() : 0.0;
            String name = found != null ? found.getName() : "";

            if (!available) {
                allAvailable = false;
            }

            responseBuilder.addItems(
                    ItemAvailability.newBuilder()
                            .setItemId(requestedId.toString())
                            .setAvailable(available)
                            .setPrice(price)
                            .setName(name)
                            .build()
            );
        }

        responseBuilder.setAllAvailable(allAvailable);
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();

    }
}
