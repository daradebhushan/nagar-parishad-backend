package com.nagar.parishad.backend.entity;

import com.nagar.parishad.backend.enums.ChatbotState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.nagar.parishad.backend.entity.User;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chatbot_sessions", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "mobile_number", "admin_id" })
})
public class ChatbotSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ChatbotState state;

    @Column(name = "language") // en, mr, hi
    private String language;

    @Column(name = "temp_data", columnDefinition = "TEXT")
    private String tempData; // JSON string to store draft complaint e.g. {"deptId":1, "typeId":2}

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
