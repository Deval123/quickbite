package com.devalere.quickbite.paymentservice.repository;

import java.util.Optional;
import java.util.UUID;

import com.devalere.quickbite.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>
{
    /**
     * Chercher un paiement par orderId.
     * Utiliser pour lier un event kafka à un paiement existant.
     * @param id order id
     * @return payment object
     */
    Optional<Payment> findByOrderId(UUID id);

    /**
     * Chercher un paiement par idempotencyKey.
     * Empêche la création de doublons.
     * @param idempotencyKey idempotencyKey
     * @return payment
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Chercher par stripe Payment ID.
     * Utiliser quand stripe envoie un webhook.
     * @param stripeEventId stripe event id
     * @return payment
     */
    Optional<Payment> findByStripePaymentIntentId(String stripeEventId);

    /**
     * Vérifier si un event stripe a déjà été traité.
     * Déduplication des webhooks.
     * @param stripeEventId stripe event id
     * @return boolean
     */
    boolean existsByStripeEventId(String stripeEventId);

}
