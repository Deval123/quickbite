package com.devalere.quickbite.notificationservice.service;

import com.devalere.quickbite.notificationservice.model.Notification;
import com.devalere.quickbite.notificationservice.model.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Envoi Email via SendGrid.
 * En prod : utiliser le SDK SendGrid Java.
 * Ici : simulation avec log.
 */
@Component
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean send(Notification notification, String renderedMessage,
                        String recipientToken) {
        if (recipientToken == null || recipientToken.isBlank()) {
            log.warn("[EMAIL] Pas d'adresse email pour userId={}",
                    notification.userId());
            return false;
        }

        // ---- En production : SendGrid SDK ----
        // Email from = new Email("noreply@quickbite.com");
        // Email to = new Email(recipientToken);
        // Content content = new Content("text/html", renderedMessage);
        // Mail mail = new Mail(from, "QuickBite", to, content);
        // SendGrid sg = new SendGrid(apiKey);
        // sg.api(new Request(...));

        log.info("[EMAIL] Envoye a {} | taille={} chars",
                recipientToken, renderedMessage.length());
        return true;
    }
}
