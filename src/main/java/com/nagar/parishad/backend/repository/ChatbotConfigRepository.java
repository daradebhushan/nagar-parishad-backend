package com.nagar.parishad.backend.repository;

import com.nagar.parishad.backend.entity.ChatbotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatbotConfigRepository extends JpaRepository<ChatbotConfig, Long> {
    Optional<ChatbotConfig> findByConfKey(String confKey);

    Optional<ChatbotConfig> findByAdminIdAndConfKey(Long adminId, String confKey);

    Optional<ChatbotConfig> findByAdminAndConfKey(com.nagar.parishad.backend.entity.User admin, String confKey);
}
