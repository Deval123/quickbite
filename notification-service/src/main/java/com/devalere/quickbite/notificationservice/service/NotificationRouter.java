package com.devalere.quickbite.notificationservice.service;

import com.devalere.quickbite.notificationservice.model.*;
import com.devalere.quickbite.notificationservice.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Le cerveau du systeme de notification.
 * Recoit un event, determine le(s) canal(aux), deduplique, personalise, envoie.
 */
@Service
public class NotificationRouter {

    private static final Logger log = LoggerFactory.getLogger(NotificationRouter.class);

    private final Map<NotificationChannel, NotificationSender> senders;
    private final DeduplicationService deduplicationService;
    private final NotificationTemplateService templateService;
    private final UserPreferenceService preferenceService;
    private final NotificationLogRepository logRepository;

    public NotificationRouter(
            List<NotificationSender> senderList,
            DeduplicationService deduplicationService,
            NotificationTemplateService templateService,
            UserPreferenceService preferenceService,
            NotificationLogRepository logRepository) {
        // Indexe les senders par canal (Strategy Pattern)
        this.senders = new java.util.EnumMap<>(NotificationChannel.class);
        for (NotificationSender sender : senderList) {
            this.senders.put(sender.channel(), sender);
        }
        this.deduplicationService = deduplicationService;
        this.templateService = templateService;
        this.preferenceService = preferenceService;
        this.logRepository = logRepository;

        log.info("[ROUTER] {} canaux enregistres : {}",
                senders.size(), senders.keySet());
    }

    /**
     * Route un event vers le(s) bon(s) canal(aux).
     */
    public void route(String eventType, String orderId, String userId,
                      Map<String, Object> eventData) {

        // 1. Determiner les canaux et la priorite
        var routing = resolveRouting(eventType);

        for (var entry : routing) {
            NotificationChannel channel = entry.channel();
            NotificationPriority priority = entry.priority();
            boolean overrideQuiet = entry.overrideQuietHours();

            // 2. Deduplication
            if (!deduplicationService.tryAcquire(userId, channel.name(), eventType, orderId)) {
                saveLog(orderId, userId, channel, eventType, "SKIPPED_DEDUP", null);
                continue;
            }

            // 3. Verifier preferences utilisateur
            if (!preferenceService.isChannelAllowed(userId, channel)) {
                log.info("[ROUTER] Canal {} desactive pour userId={}", channel, userId);
                saveLog(orderId, userId, channel, eventType, "SKIPPED_PREFERENCE", null);
                continue;
            }

            // 4. Quiet hours
            if (!overrideQuiet && preferenceService.isInQuietHours(userId)) {
                log.info("[ROUTER] Quiet hours actives pour userId={}, skip {}",
                        userId, channel);
                saveLog(orderId, userId, channel, eventType, "SKIPPED_PREFERENCE",
                        "quiet hours");
                continue;
            }

            // 5. Render le template
            String message = templateService.render(channel, eventType, eventData);

            // 6. Recuperer le token/destinataire
            String recipientToken = preferenceService.getRecipientToken(userId, channel);

            // 7. Envoyer
            Notification notification = new Notification(
                    orderId, userId, eventType, channel, priority,
                    eventData, overrideQuiet);

            NotificationSender sender = senders.get(channel);
            if (sender == null) {
                log.error("[ROUTER] Aucun sender pour canal {}", channel);
                saveLog(orderId, userId, channel, eventType, "FAILED",
                        "No sender for channel");
                continue;
            }

            boolean success = sender.send(notification, message, recipientToken);

            if (success) {
                saveLog(orderId, userId, channel, eventType, "SENT", null);
            } else {
                saveLog(orderId, userId, channel, eventType, "FAILED", "Send failed");

                // 8. Fallback : si push echoue et priorite haute -> SMS
                if (channel == NotificationChannel.PUSH
                        && priority == NotificationPriority.HIGH) {
                    log.info("[ROUTER] Fallback PUSH -> SMS pour orderId={}", orderId);
                    fallbackToSms(notification, message, userId, orderId, eventType);
                }
            }
        }
    }

    private void fallbackToSms(Notification original, String message,
                                String userId, String orderId, String eventType) {
        String phone = preferenceService.getRecipientToken(userId, NotificationChannel.SMS);
        NotificationSender smsSender = senders.get(NotificationChannel.SMS);

        if (smsSender != null && phone != null) {
            String smsMessage = templateService.render(
                    NotificationChannel.SMS, eventType, original.templateData());
            boolean success = smsSender.send(original, smsMessage, phone);
            saveLog(orderId, userId, NotificationChannel.SMS, eventType,
                    success ? "SENT" : "FAILED",
                    success ? "fallback from PUSH" : "SMS fallback failed");
        }
    }

    /**
     * Mapping event -> canal(aux) + priorite.
     * C'est ici qu'on decide QUI recoit QUOI sur QUEL canal.
     */
    private List<ChannelRouting> resolveRouting(String eventType) {
        return switch (eventType) {
            // Email : confirmation + recapitulatif complet
            case "OrderCreatedEvent" -> List.of(
                    new ChannelRouting(NotificationChannel.EMAIL,
                            NotificationPriority.NORMAL, false));

            // Push : le restaurant prepare
            case "OrderConfirmedEvent" -> List.of(
                    new ChannelRouting(NotificationChannel.PUSH,
                            NotificationPriority.NORMAL, false));

            // Push : un livreur est assigne
            case "DeliveryAssignedEvent" -> List.of(
                    new ChannelRouting(NotificationChannel.PUSH,
                            NotificationPriority.NORMAL, false));

            // Push : commande prete
            case "OrderReadyEvent" -> List.of(
                    new ChannelRouting(NotificationChannel.PUSH,
                            NotificationPriority.NORMAL, false));

            // Push + Email (facture) : livraison terminee
            case "DeliveryCompletedEvent" -> List.of(
                    new ChannelRouting(NotificationChannel.PUSH,
                            NotificationPriority.NORMAL, false),
                    new ChannelRouting(NotificationChannel.EMAIL,
                            NotificationPriority.LOW, false));

            // Push (HIGH) + SMS fallback : paiement echoue = critique
            case "PaymentFailedEvent" -> List.of(
                    new ChannelRouting(NotificationChannel.PUSH,
                            NotificationPriority.HIGH, true),
                    new ChannelRouting(NotificationChannel.SMS,
                            NotificationPriority.HIGH, true));

            // Push : commande annulee
            case "OrderCancelledEvent" -> List.of(
                    new ChannelRouting(NotificationChannel.PUSH,
                            NotificationPriority.HIGH, true));

            // Par defaut : push normal
            default -> List.of(
                    new ChannelRouting(NotificationChannel.PUSH,
                            NotificationPriority.LOW, false));
        };
    }

    private void saveLog(String orderId, String userId,
                         NotificationChannel channel, String eventType,
                         String status, String errorMessage) {
        try {
            logRepository.save(new NotificationLog(
                    orderId, userId, channel, eventType, status, errorMessage));
        } catch (Exception e) {
            log.error("[LOG] Erreur save notification_log : {}", e.getMessage());
        }
    }

    private record ChannelRouting(
            NotificationChannel channel,
            NotificationPriority priority,
            boolean overrideQuietHours) {}
}
