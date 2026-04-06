package com.nagar.parishad.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.nagar.parishad.backend.entity.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenant_twilio_config")
public class TenantTwilioConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false, unique = true)
    private User admin;

    private String accountSid;

    private String authToken;

    private String phoneNumber; // The "From" number (e.g., whatsapp:+91...)

    private boolean active = true;
}
