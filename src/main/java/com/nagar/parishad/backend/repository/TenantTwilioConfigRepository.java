package com.nagar.parishad.backend.repository;

import com.nagar.parishad.backend.entity.TenantTwilioConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantTwilioConfigRepository extends JpaRepository<TenantTwilioConfig, Long> {
    Optional<TenantTwilioConfig> findByPhoneNumber(String phoneNumber);

    Optional<TenantTwilioConfig> findByAdminId(Long adminId);
}
