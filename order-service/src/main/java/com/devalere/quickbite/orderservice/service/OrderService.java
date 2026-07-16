package com.devalere.quickbite.orderservice.service;

import com.devalere.quickbite.orderservice.grpc.RestaurantGrpcClient;
import com.devalere.quickbite.orderservice.model.Order;
import com.devalere.quickbite.orderservice.model.OrderItem;
import com.devalere.quickbite.orderservice.model.OrderStatus;
import com.devalere.quickbite.orderservice.repository.OrderRepository;
import com.devalere.quickbite.dto.CreateOrderRequest;
import com.devalere.quickbite.dto.OrderItemRequest;
import com.devalere.quickbite.dto.OrderResponse;
import com.devalere.quickbite.grpc.restaurant.CheckItemsResponse;
import com.devalere.quickbite.grpc.restaurant.ItemAvailability;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantGrpcClient restaurantGrpcClient;

    public OrderService(OrderRepository orderRepository,
            RestaurantGrpcClient restaurantGrpcClient) {
        this.orderRepository = orderRepository;
        this.restaurantGrpcClient = restaurantGrpcClient;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String userId) {

        // 1. Verifier les items via gRPC (appel interne vers restaurant-service)
        List<String> itemIds = request.items().stream()
                .map(OrderItemRequest::menuItemId)
                .toList();

        CheckItemsResponse grpcResponse = restaurantGrpcClient
                .checkItemsAvailability(request.restaurantId(), itemIds);

        if (!grpcResponse.getAllAvailable()) {
            List<String> unavailable = grpcResponse.getItemsList().stream()
                    .filter(item -> !item.getAvailable())
                    .map(ItemAvailability::getItemId)
                    .toList();
            throw new RuntimeException("Items unavailable: " + unavailable);
        }

        // 2. Construire les mappings itemId -> price / name depuis la reponse gRPC
        Map<String, Double> priceMap = grpcResponse.getItemsList().stream()
                .collect(Collectors.toMap(
                        ItemAvailability::getItemId,
                        ItemAvailability::getPrice
                ));
        Map<String, String> nameMap = grpcResponse.getItemsList().stream()
                .collect(Collectors.toMap(
                        ItemAvailability::getItemId,
                        ItemAvailability::getName
                ));

        // 3. Créer la commande
        Order order = new Order();
        order.setUserId(userId);
        order.setRestaurantId(UUID.fromString(request.restaurantId()));
        order.setStatus(OrderStatus.CREATED);
        order.setDeliveryAddress(request.deliveryAddress());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.items()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItemId(UUID.fromString(itemReq.menuItemId()));
            orderItem.setMenuItemName(nameMap.get(itemReq.menuItemId()));
            orderItem.setQuantity(itemReq.quantity());

            BigDecimal unitPrice = BigDecimal.valueOf(priceMap.get(itemReq.menuItemId()));
            orderItem.setUnitPrice(unitPrice);

            total = total.add(unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity())));
            orderItems.add(orderItem);
        }

        order.setItems(orderItems);
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        return toResponse(saved);
    }

    public OrderResponse findById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderResponse.OrderItemResponse(
                        item.getMenuItemId().toString(),
                        item.getMenuItemName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId().toString(),
                order.getRestaurantId().toString(),
                order.getUserId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                items,
                order.getDeliveryAddress(),
                order.getCreatedAt()
        );
    }
}
