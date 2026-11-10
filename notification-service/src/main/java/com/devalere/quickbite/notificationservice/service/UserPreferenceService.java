package com.devalere.quickbite.notificationservice.service;

import com.devalere.quickbite.notificationservice.model.NotificationChannel;
import com.devalere.quickbite.notificationservice.model.UserNotificationPreference;
import com.devalere.quickbite.notificationservice.repository.UserNotificationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Optional;

@Service
public class UserPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(UserPreferenceService.class);

    private final UserNotificationPreferenceRepository preferenceRepository;

    public UserPreferenceService(
            UserNotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    @Cacheable(value = "user-preferences", key = "#userId")
    public Optional<UserNotificationPreference> getPreference(String userId) {
        return preferenceRepository.findByUserId(userId);
    }

    /**
     * Verifie si le canal est autorise pour cet utilisateur.
     */
    public boolean isChannelAllowed(String userId, NotificationChannel channel) {
        return getPreference(userId)
                .map(pref -> pref.isChannelEnabled(channel))
                .orElse(true); // Si pas de preference, tout est actif par defaut
    }

    /**
     * Verifie si on est dans les quiet hours du user.
     */
    public boolean isInQuietHours(String userId) {
        return getPreference(userId)
                .map(pref -> {
                    LocalTime now = LocalTime.now();
                    LocalTime start = pref.getQuietStart();
                    LocalTime end = pref.getQuietEnd();

                    // Gere le cas ou quiet hours traversent minuit (22h -> 8h)
                    if (start.isAfter(end)) {
                        return now.isAfter(start) || now.isBefore(end);
                    }
                    return now.isAfter(start) && now.isBefore(end);
                })
                .orElse(false);
    }

    /**
     * Recupere le token/destinataire pour un canal donne.
     */
    public String getRecipientToken(String userId, NotificationChannel channel) {
        return getPreference(userId)
                .map(pref -> switch (channel) {
                    case PUSH -> pref.getFcmToken();
                    case SMS -> pref.getPhoneNumber();
                    case EMAIL -> pref.getEmail();
                })
                .orElse(null);
    }
}
