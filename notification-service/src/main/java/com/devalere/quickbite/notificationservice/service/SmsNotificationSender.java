package com.devalere.quickbite.notificationservice.service;

import com.devalere.quickbite.notificationservice.model.Notification;
import com.devalere.quickbite.notificationservice.model.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Envoi SMS via Twilio.
 * En prod : utiliser le SDK Twilio Java.
 * Ici : simulation avec log.
 */
@Component
public class SmsNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean send(Notification notification, String renderedMessage,
                        String recipientToken) {
        if (recipientToken == null || recipientToken.isBlank()) {
            log.warn("[SMS] Pas de numero de telephone pour userId={}",
                    notification.userId());
            return false;
        }

        // ---- En production : Twilio SDK ----
        // Message.creator(
        //     new PhoneNumber(recipientToken),
        //     new PhoneNumber(twilioFromNumber),
        //     renderedMessage
        // ).create();

        log.info("[SMS] Envoye a {} | message={}", recipientToken, renderedMessage);
        return true;
    }
}
