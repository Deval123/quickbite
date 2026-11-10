package com.devalere.quickbite.notificationservice.repository;

import com.devalere.quickbite.notificationservice.model.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNotificationPreferenceRepository
        extends JpaRepository<UserNotificationPreference, Long> {

    Optional<UserNotificationPreference> findByUserId(String userId);
}
