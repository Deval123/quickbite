package com.devalere.quickbite.paymentservice.service;

import java.util.UUID;

import com.devalere.quickbite.events.OrderCreatedEvent;
import com.devalere.quickbite.paymentservice.kafka.PaymentEventProducer;
import com.devalere.quickbite.paymentservice.model.Payment;
import com.devalere.quickbite.paymentservice.model.PaymentStatus;
import com.devalere.quickbite.paymentservice.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService
{
    private static  final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventProducer paymentEventProducer)
    {
        this.paymentRepository = paymentRepository;
        this.paymentEventProducer = paymentEventProducer;
    }

    /**
     * Créer un PaymentIntent Stripe (pré-authorisation) pour un paiement.
     * Appelé quand on reçoit un OrderCreatedEvent via kafka.
     * @param event un objet OrderCreatedEvent contenant les détails de la commande.
     */
    @Transactional
    public void createPaymentIntent(OrderCreatedEvent event){
        String idempotencyKey = "order_pay_" + event.orderId();

        if(paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent()){
            log.info("Paiement deja crée pour orderId={}, skip", event.orderId());
            return;
        }

        // Créer l'entrée en BD d'abord (avant l'appel de Stripe)
        Payment payment = new Payment();
        payment.setOrderId(UUID.fromString( event.orderId()));
        payment.setUserId(event.userId());
        payment.setAmount(event.totalAmount());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        try{
            //Appel Stripe avec idempotency-key
            Stripe.apiKey = stripeSecretKey;

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(event.totalAmount()
                            .multiply(new java.math.BigDecimal(100)).longValue()) // en centimes
                    .setCurrency("eur")
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL) // pre-autorisation
                    .setPaymentMethod("pm_card_visa")  // carte de test Stripe (dev only)
                    .setConfirm(true)                   // confirmer immediatement -> bloque les fonds
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .putMetadata("order_id", event.orderId())
                    .putMetadata("user_id", event.userId())
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params, options);

            // Mettre à jour avec l'ID Stripe.
            payment.setStripePaymentIntentId(intent.getId());
            payment.setStatus(PaymentStatus.REQUIRES_CAPTURE);
            paymentRepository.save(payment);

            log.info("PaymentIntent cree: {} pour orderId={}, amount={} EUR",
                    intent.getId(), event.orderId(), event.totalAmount());

        } catch (StripeException e){
            log.error("Erreur Stripe pour orderId={}: {}", event.orderId(), e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);

            // Publier un event d'échec pour que la Saga compense.
            paymentEventProducer.publishPaymentFailed(event.orderId(), "Stripe error : " + e.getMessage());
        }

    }

    /**
     * Traiter un webhook Stripe (payment_intent.succeeded ou .payment_failed).
     * Appelé par le StripeWebhookController.
     * @param paymentIntentId paymentIntentId
     * @param stripeEventId stripeEventId
     */
    @Transactional
    public void handlePaymentIntentSucceeded(String paymentIntentId, String stripeEventId){
        // Déduplication : si cet event a déjà été traité, on skip.
        if(paymentRepository.existsByStripeEventId(stripeEventId)){
            log.info("Webhook déjà traité: eventId={}, skip", stripeEventId);
            return;
        }

        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for PaymentIntent: " + paymentIntentId));

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setStripeEventId(stripeEventId);
        paymentRepository.save(payment);

        // Publier PaymentCompletedEvent sur kafka.
        paymentEventProducer.publishPaymentCompleted(
                payment.getOrderId().toString(), payment.getAmount());


        log.info("Paiement réussi: orderId={}, amount={} EUR",
                payment.getOrderId(), payment.getAmount());
    }

    /**
     * Traiter un webhook Stripe payment_intent.payment_failed.
     * @param paymentIntentId paymentIntentId
     * @param stripeEventId stripeEventId
     * @param failureMessage failureMessage
     */
    @Transactional
    public void handlePaymentIntentFailed(String paymentIntentId, String stripeEventId, String failureMessage){
        if(paymentRepository.existsByStripeEventId(stripeEventId)){
            log.info("Webhook deja traite: eventId={}, skip", stripeEventId);
            return;
        }

        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for PaymentIntent: " + paymentIntentId));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setStripeEventId(stripeEventId);
        payment.setFailureReason(failureMessage);
        paymentRepository.save(payment);

        // Publier PaymentCompletedEvent sur kafka.

        paymentEventProducer.publishPaymentFailed(
                payment.getOrderId().toString(), failureMessage);

        log.info("Paiement échoue: orderId={}, reason={}",
                payment.getOrderId(), failureMessage);
    }

    /**
     * Annuler un PaymentIntent (compensation Saga).
     * Appelé quand le restaurant refuse la commande.
     */
    @Transactional
    public void cancelPayment(String orderId) {
        Payment payment = paymentRepository.findByOrderId(UUID.fromString(orderId))
                .orElseThrow(() -> new RuntimeException(
                        "Payment not found for orderId: " + orderId));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.warn("Impossible d'annuler un paiement deja capture: orderId={}", orderId);
            // Ici il faudrait faire un refund, pas un cancel
            return;
        }

        try {
            Stripe.apiKey = stripeSecretKey;
            PaymentIntent intent = PaymentIntent.retrieve(payment.getStripePaymentIntentId());
            intent.cancel(PaymentIntentCancelParams.builder().build());

            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);

            log.info("PaymentIntent annule pour orderId={}", orderId);
        } catch (StripeException e) {
            log.error("Erreur annulation Stripe pour orderId={}: {}",
                    orderId, e.getMessage());
        }
    }
}
