package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.service.WhatsappService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/bot-sim")
public class PublicBotSimController {

    @Autowired
    private WhatsappService whatsappService;

    @PostMapping("/interact")
    public Map<String, String> interact(@RequestBody Map<String, String> payload) {
        try {
            // "mobile" is a simulator ID, e.g., "SIM_ADMIN" or provided
            String mobile = payload.getOrDefault("mobile", "SIM_ADMIN");
            String message = payload.get("message");

            // Require adminId in payload for public access
            Long adminId = 1L;
            if (payload.containsKey("adminId")) {
                try {
                    adminId = Long.parseLong(payload.get("adminId"));
                } catch (NumberFormatException e) {
                }
            } else {
                return Map.of("response", "Error: adminId is required for public simulation");
            }

            // Append adminId to ensure uniqueness per admin
            mobile = mobile + "_" + adminId;

            String mediaUrl = payload.get("mediaUrl");
            int numMedia = payload.containsKey("numMedia") ? Integer.parseInt(String.valueOf(payload.get("numMedia")))
                    : 0;
            if (mediaUrl != null && !mediaUrl.isEmpty() && numMedia == 0) {
                numMedia = 1;
            }

            String response = whatsappService.simulate(mobile, message, numMedia, mediaUrl, adminId);
            return Map.of("response", response != null ? response : "");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("response", "Error: " + e.getMessage());
        }
    }
}
