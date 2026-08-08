package com.devalere.quickbite.paymentservice.controller;

import com.devalere.quickbite.paymentservice.service.PaymentService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final PaymentService paymentService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public StripeWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        // 1. Verifier la signature HMAC (securite)
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.error("Signature webhook invalide: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid signature");
        }

        log.info("Webhook Stripe recu: type={}, id={}", event.getType(), event.getId());

        // 2. Traiter selon le type d'event
        try {
            switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = (PaymentIntent)
                        event.getDataObjectDeserializer()
                                .getObject().orElseThrow();
                paymentService.handlePaymentIntentSucceeded(
                        intent.getId(), event.getId());
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = (PaymentIntent)
                        event.getDataObjectDeserializer()
                                .getObject().orElseThrow();
                String failureMessage = intent.getLastPaymentError() != null
                        ? intent.getLastPaymentError().getMessage()
                        : "Unknown failure";
                paymentService.handlePaymentIntentFailed(
                        intent.getId(), event.getId(), failureMessage);
            }
            default -> log.info("Event Stripe ignore: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Erreur traitement webhook {}: {}",
                    event.getType(), e.getMessage());
            // On retourne 200 quand meme pour éviter les retries infinis
            // L'erreur sera traitée via les logs/alerting
        }

        // 3. Répondre 200 OK le plus vite possible
        return ResponseEntity.ok("OK");
    }
}