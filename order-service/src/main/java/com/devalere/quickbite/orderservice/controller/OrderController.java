package com.devalere.quickbite.orderservice.controller;

import com.devalere.quickbite.dto.CreateOrderRequest;
import com.devalere.quickbite.dto.OrderResponse;
import com.devalere.quickbite.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        OrderResponse order = orderService.createOrder(request, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    // Le propriétaire de la commande (client) ou un admin peuvent la consulter.
    @GetMapping("/{id}")
    @PostAuthorize("returnObject.body.userId() == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        OrderResponse order = orderService.findById(id);
        return ResponseEntity.ok(order);
    }

    // seuls les acteurs opérationnels changent le statut : jamais le client lui-meme.
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT', 'DRIVER', 'ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        OrderResponse order = orderService.updateStatus(id, status);
        return ResponseEntity.ok(order);
    }
}
