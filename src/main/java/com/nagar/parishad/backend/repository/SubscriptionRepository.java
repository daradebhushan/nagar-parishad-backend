package com.nagar.parishad.backend.repository;

import com.nagar.parishad.backend.entity.Subscription;
import com.nagar.parishad.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByAdmin(User admin);

    Optional<Subscription> findByAdminId(Long adminId);
}
