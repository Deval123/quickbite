package com.devalere.quickbite.notificationservice.service;

import com.devalere.quickbite.notificationservice.model.Notification;
import com.devalere.quickbite.notificationservice.model.NotificationChannel;

/**
 * Strategy Pattern : une implementation par canal de notification.
 */
public interface NotificationSender {

    /**
     * Le canal supporte par ce sender.
     */
    NotificationChannel channel();

    /**
     * Envoie la notification. Retourne true si succes, false si echec.
     */
    boolean send(Notification notification, String renderedMessage,
                 String recipientToken);
}
