package com.nagar.parishad.backend.repository;

import com.nagar.parishad.backend.entity.ChatbotSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatbotSessionRepository extends JpaRepository<ChatbotSession, Long> {
    java.util.List<ChatbotSession> findByMobileNumber(String mobileNumber);

    Optional<ChatbotSession> findByMobileNumberAndAdminId(String mobileNumber, Long adminId);

    Optional<ChatbotSession> findByMobileNumberAndAdmin(String mobileNumber,
            com.nagar.parishad.backend.entity.User admin);
}
