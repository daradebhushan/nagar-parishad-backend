package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.service.WhatsappService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/whatsapp")

public class WhatsappController {

    public WhatsappController() {
        System.err.println("### WHATSAPP CONTROLLER INSTANTIATED ###");
    }

    @GetMapping("/ping")
    public String ping() {
        System.err.println("WhatsappController: PING HIT");
        return "PONG";
    }

    @Autowired
    private WhatsappService whatsappService;

    @Autowired
    private com.nagar.parishad.backend.repository.TenantTwilioConfigRepository tenantTwilioConfigRepository;

    @Autowired
    private com.nagar.parishad.backend.repository.UserRepository userRepository;

    @GetMapping("/check-config")
    public String checkConfig() {
        // Dynamic check or fallback to Admin 1 (Owner)
        return tenantTwilioConfigRepository.findAll().stream()
                .findFirst()
                .map(c -> "Current Config (ID:" + c.getId() + ", Admin:" + c.getAdmin().getId() + "): "
                        + c.getPhoneNumber())
                .orElse("No Config Found");
    }

    @GetMapping("/setup-config")
    public String setupConfig() {
        try {
            Long adminId = 2L;
            com.nagar.parishad.backend.entity.User admin = userRepository.findById(adminId).orElse(null);
            if (admin == null)
                return "Admin not found";

            com.nagar.parishad.backend.entity.TenantTwilioConfig config = tenantTwilioConfigRepository
                    .findByAdminId(adminId)
                    .orElse(new com.nagar.parishad.backend.entity.TenantTwilioConfig());

            config.setAdmin(admin);
            config.setAccountSid("AC349613979568d34da4845c4a4d28f13d");
            config.setAuthToken("ea718eeea53c3c746007655e9f22d3f0");
            config.setPhoneNumber("whatsapp:+14155238886"); // SANDBOX
            config.setActive(true);

            tenantTwilioConfigRepository.save(config);
            return "Config Updated to Sandbox: " + config.getPhoneNumber();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping
    public String receiveMessage(jakarta.servlet.http.HttpServletRequest request) {
        System.err.println("CONTROLLER: HIT! URI: " + request.getRequestURI());
        String rawBody = "";
        try {
            rawBody = new String(request.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            System.err.println("CONTROLLER: RAW BODY LENGTH: " + rawBody.length());
            System.err.println("CONTROLLER: RAW BODY: " + rawBody);
        } catch (Exception e) {
            System.err.println("CONTROLLER: Error reading body: " + e.getMessage());
            e.printStackTrace();
        }

        // Manual Parsing for Twilio Form Data
        java.util.Map<String, String> params = new java.util.HashMap<>();
        if (!rawBody.isEmpty()) {
            String[] pairs = rawBody.split("&");
            for (String pair : pairs) {
                String[] idx = pair.split("=", 2);
                try {
                    String key = java.net.URLDecoder.decode(idx[0], "UTF-8");
                    String value = idx.length > 1 ? java.net.URLDecoder.decode(idx[1], "UTF-8") : "";
                    params.put(key, value);
                } catch (java.io.UnsupportedEncodingException e) {
                }
            }
        }

        System.err.println("CONTROLLER: Manually Parsed Params: " + params);

        String from = params.get("From");
        String to = params.get("To");
        String body = params.getOrDefault("Body", "");
        String numMediaStr = params.getOrDefault("NumMedia", "0");

        int numMedia = 0;
        try {
            numMedia = Integer.parseInt(numMediaStr);
        } catch (NumberFormatException e) {
        }

        java.util.List<String> mediaUrls = new java.util.ArrayList<>();
        if (numMedia > 0) {
            for (int i = 0; i < numMedia; i++) {
                String key = "MediaUrl" + i;
                String url = params.get(key);
                if (url != null && !url.isEmpty()) {
                    mediaUrls.add(url);
                }
            }
        }

        if (from == null || to == null) {
            System.err.println("CONTROLLER: Missing From/To. Params: " + params);
            return "";
        }

        String mobile = from.replace("whatsapp:", "");
        try {
            System.err.println("CONTROLLER: Calling service with mobile=" + mobile + ", body=" + body + ", mediaCount="
                    + mediaUrls.size());
            whatsappService.processMessage(mobile, to, body, numMedia, mediaUrls);
            System.err.println("CONTROLLER: Service call returned success.");
        } catch (Exception e) {
            System.err.println("CONTROLLER: CRITICAL SERVICE ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return "";
    }
}
