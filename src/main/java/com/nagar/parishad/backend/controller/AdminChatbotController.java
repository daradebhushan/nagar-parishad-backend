package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.service.WhatsappService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/bot-sim")
public class AdminChatbotController {

    @Autowired
    private WhatsappService whatsappService;

    @Autowired
    private com.nagar.parishad.backend.repository.UserRepository userRepository;

    @PostMapping("/interact")
    public Map<String, String> interact(@RequestBody Map<String, String> payload) {
        try {
            // "mobile" is a simulator ID, e.g., "SIM_ADMIN" or provided
            String mobile = payload.getOrDefault("mobile", "SIM_ADMIN");
            String message = payload.get("message");

            // Get Admin from Security Context (Auth Token)
            Long adminId = 1L; // Fallback
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String email = auth.getName();
                com.nagar.parishad.backend.entity.User admin = userRepository.findByEmail(email).orElse(null);
                if (admin != null) {
                    adminId = admin.getId();
                }
            } else {
                // Try payload if auth missing (dev mode?)
                if (payload.containsKey("adminId")) {
                    try {
                        adminId = Long.parseLong(payload.get("adminId"));
                    } catch (NumberFormatException e) {
                    }
                }
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

    @PostMapping("/upload")
    public Map<String, String> uploadMedia(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            java.io.File uploadDir = new java.io.File("uploads/temp/");
            if (!uploadDir.exists())
                uploadDir.mkdirs();

            java.io.File dest = new java.io.File(uploadDir, fileName);
            file.transferTo(dest.getAbsoluteFile());

            // Construct URL
            String fileUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/uploads/temp/")
                    .path(fileName)
                    .toUriString();

            return Map.of("url", fileUrl);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Upload failed", e);
        }
    }
}
