package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.dto.ChatbotConfigDTO;
import com.nagar.parishad.backend.entity.ChatbotConfig;
import com.nagar.parishad.backend.entity.ChatbotConfig;
import com.nagar.parishad.backend.repository.ChatbotConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatbotConfigService {

    @Autowired
    private ChatbotConfigRepository chatbotConfigRepository;

    public List<ChatbotConfigDTO> getAllConfigs() {
        return chatbotConfigRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public ChatbotConfigDTO createOrUpdateConfig(ChatbotConfigDTO dto) {
        ChatbotConfig config = chatbotConfigRepository.findByConfKey(dto.getConfKey())
                .orElse(new ChatbotConfig());

        config.setConfKey(dto.getConfKey());
        config.setConfValue(dto.getConfValue());
        config.setType(dto.getType());

        return convertToDTO(chatbotConfigRepository.save(config));
    }

    public void deleteConfig(Long id) {
        chatbotConfigRepository.deleteById(id);
    }

    private ChatbotConfigDTO convertToDTO(ChatbotConfig entity) {
        ChatbotConfigDTO dto = new ChatbotConfigDTO();
        dto.setId(entity.getId());
        dto.setConfKey(entity.getConfKey());
        dto.setConfValue(entity.getConfValue());
        dto.setType(entity.getType());
        return dto;
    }
}
