package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ChatbotConfigDTO;
import com.nagar.parishad.backend.service.ChatbotConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/chatbot-config")

public class ChatbotConfigController {

    @Autowired
    private ChatbotConfigService chatbotConfigService;

    @GetMapping
    public ResponseEntity<List<ChatbotConfigDTO>> getAllConfigs() {
        return ResponseEntity.ok(chatbotConfigService.getAllConfigs());
    }

    @PostMapping
    public ResponseEntity<ChatbotConfigDTO> createOrUpdateConfig(@RequestBody ChatbotConfigDTO dto) {
        return ResponseEntity.ok(chatbotConfigService.createOrUpdateConfig(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        chatbotConfigService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }
}
