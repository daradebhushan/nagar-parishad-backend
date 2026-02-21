package com.nagar.parishad.backend.dto;

import com.nagar.parishad.backend.enums.ChatbotConfigType;
import lombok.Data;

@Data
public class ChatbotConfigDTO {
    private Long id;
    private String confKey;
    private String confValue;
    private ChatbotConfigType type;
}
