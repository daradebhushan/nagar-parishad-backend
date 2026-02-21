package com.nagar.parishad.backend.dto;

import lombok.Data;
import com.nagar.parishad.backend.enums.ChatbotConfigType;

@Data
public class ChatbotSettingsDTO {
    // Twilio Config
    private String accountSid;
    private String authToken;
    private String phoneNumber;
    private String confValue;
    private ChatbotConfigType type;
    private String chatbotFlow; // JSON string for dynamic flow

    // Welcome Messages
    private String welcomeMsgEn;
    private String welcomeMsgMr;
    private String welcomeMsgHi;
}
