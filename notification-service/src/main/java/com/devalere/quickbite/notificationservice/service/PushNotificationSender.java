package com.devalere.quickbite.notificationservice.service;

import com.devalere.quickbite.notificationservice.model.Notification;
import com.devalere.quickbite.notificationservice.model.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Envoi Push via Firebase Cloud Messaging (FCM).
 * En prod : utiliser le SDK Firebase Admin.
 * Ici : simulation avec log.
 */
@Component
public class PushNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public boolean send(Notification notification, String renderedMessage,
                        String recipientToken) {
        if (recipientToken == null || recipientToken.isBlank()) {
            log.warn("[PUSH] Pas de token FCM pour userId={}", notification.userId());
            return false;
        }

        // ---- En production : Firebase Admin SDK ----
        // Message message = Message.builder()
        //     .setToken(recipientToken)
        //     .setNotification(Notification.builder()
        //         .setTitle("QuickBite")
        //         .setBody(renderedMessage)
        //         .build())
        //     .build();
        // FirebaseMessaging.getInstance().send(message);

        log.info("[PUSH] Envoye a token={} | message={}",
                recipientToken.substring(0, Math.min(8, recipientToken.length())) + "...",
                renderedMessage);
        return true;
    }
}
