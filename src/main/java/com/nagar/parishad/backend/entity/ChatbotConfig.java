package com.nagar.parishad.backend.entity;

import com.nagar.parishad.backend.enums.ChatbotConfigType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.nagar.parishad.backend.entity.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chatbot_config", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "admin_id", "confKey" })
})
public class ChatbotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id") // Nullable for system defaults if needed, but best to enforce
    private User admin;

    @Column(nullable = false)
    private String confKey;

    @Column(columnDefinition = "TEXT")
    private String confValue;

    @Enumerated(EnumType.STRING)
    private ChatbotConfigType type;
}
