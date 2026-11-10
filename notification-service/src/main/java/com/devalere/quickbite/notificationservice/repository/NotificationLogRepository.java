package com.devalere.quickbite.notificationservice.repository;

import com.devalere.quickbite.notificationservice.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
}
