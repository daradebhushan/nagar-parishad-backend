package com.nagar.parishad.backend.repository;

import com.nagar.parishad.backend.entity.Notification;
import com.nagar.parishad.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    long countByRecipientAndIsReadFalse(User recipient);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient = :recipient")
    void markAllAsRead(@org.springframework.data.repository.query.Param("recipient") User recipient);

    void deleteByRecipient(User recipient);
}
