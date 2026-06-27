package com.devalere.quickbite.orderservice.controller;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    /**
     * Seuls les clients peuvent creer une commande.
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public String createOrder(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("sub");
        String email = jwt.getClaimAsString("email");
        return "Commande créée par " + email + " (id: " + userId + ")";
    }

    /**
     * Un client ne peut voir que SES commandes.
     * Un admin peut voir toutes les commandes.
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public String getOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("sub");
        // TODO: verifier que la commande appartient a cet userId
        return "Order " + orderId + " pour user " + userId;
    }
}