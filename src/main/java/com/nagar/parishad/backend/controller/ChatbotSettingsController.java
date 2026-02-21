package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ChatbotSettingsDTO;
import com.nagar.parishad.backend.entity.ChatbotConfig;
import com.nagar.parishad.backend.entity.TenantTwilioConfig;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.ChatbotConfigType;
import com.nagar.parishad.backend.repository.ChatbotConfigRepository;
import com.nagar.parishad.backend.repository.TenantTwilioConfigRepository;
import com.nagar.parishad.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin/chatbot/settings")

public class ChatbotSettingsController {

    @Autowired
    private TenantTwilioConfigRepository twilioConfigRepository;

    @Autowired
    private ChatbotConfigRepository chatbotConfigRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ChatbotSettingsDTO> getSettings() {
        User admin = getAuthenticatedAdmin();
        if (admin == null)
            return ResponseEntity.status(401).build();

        ChatbotSettingsDTO dto = new ChatbotSettingsDTO();

        // Load Twilio Config
        Optional<TenantTwilioConfig> twilioConfig = twilioConfigRepository.findByAdminId(admin.getId());
        if (twilioConfig.isPresent()) {
            dto.setAccountSid(twilioConfig.get().getAccountSid());
            dto.setAuthToken(twilioConfig.get().getAuthToken());
            dto.setPhoneNumber(twilioConfig.get().getPhoneNumber());
        }

        // Load Welcome Messages
        dto.setWelcomeMsgEn(getConfigValue(admin.getId(), "WELCOME_MSG_EN"));
        dto.setWelcomeMsgMr(getConfigValue(admin.getId(), "WELCOME_MSG_MR"));
        dto.setWelcomeMsgHi(getConfigValue(admin.getId(), "WELCOME_MSG_HI"));

        // Load Chatbot Flow
        dto.setChatbotFlow(getConfigValue(admin.getId(), "CHATBOT_FLOW"));

        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<String> saveSettings(@RequestBody ChatbotSettingsDTO dto) {
        User admin = getAuthenticatedAdmin();
        if (admin == null)
            return ResponseEntity.status(401).build();

        // Save Twilio Config
        TenantTwilioConfig twilioConfig = twilioConfigRepository.findByAdminId(admin.getId())
                .orElse(new TenantTwilioConfig());

        twilioConfig.setAdmin(admin);
        twilioConfig.setAccountSid(dto.getAccountSid());
        twilioConfig.setAuthToken(dto.getAuthToken());
        twilioConfig.setPhoneNumber(dto.getPhoneNumber());
        twilioConfig.setActive(true);
        twilioConfigRepository.save(twilioConfig);

        // Save Welcome Messages
        saveConfig(admin, "WELCOME_MSG_EN", dto.getWelcomeMsgEn());
        saveConfig(admin, "WELCOME_MSG_MR", dto.getWelcomeMsgMr());
        saveConfig(admin, "WELCOME_MSG_HI", dto.getWelcomeMsgHi());

        // Save Chatbot Flow
        saveConfig(admin, "CHATBOT_FLOW", dto.getChatbotFlow());

        return ResponseEntity.ok("Settings saved successfully.");
    }

    private String getConfigValue(Long adminId, String key) {
        return chatbotConfigRepository.findByAdminIdAndConfKey(adminId, key)
                .map(ChatbotConfig::getConfValue)
                .orElse("");
    }

    private void saveConfig(User admin, String key, String value) {
        if (value == null)
            return;

        ChatbotConfig config = chatbotConfigRepository.findByAdminAndConfKey(admin, key)
                .orElse(new ChatbotConfig());

        if (config.getId() == null) {
            config.setAdmin(admin);
            config.setConfKey(key);
            config.setType(ChatbotConfigType.TEXT);
        }
        config.setConfValue(value);
        chatbotConfigRepository.save(config);
    }

    private User getAuthenticatedAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            // In a real app, strict UserDetails casting.
            // Here simplified assuming email is principal or we look up by name
            String email = auth.getName();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }
}
