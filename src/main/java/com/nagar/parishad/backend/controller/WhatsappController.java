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

    @org.springframework.beans.factory.annotation.Value("${twilio.account_sid:}")
    private String defaultTwilioSid;

    @org.springframework.beans.factory.annotation.Value("${twilio.auth_token:}")
    private String defaultTwilioToken;

    @org.springframework.beans.factory.annotation.Value("${twilio.phone_number:}")
    private String defaultTwilioPhone;

    @org.springframework.beans.factory.annotation.Value("${whatsapp.wabridge.verify_token:loknagar_wa_secret_2026}")
    private String wabridgeVerifyToken;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        System.err.println("CONTROLLER: Webhook Verification Requested: mode=" + mode + ", token=" + token);
        if (token != null && token.equals(wabridgeVerifyToken)) {
            System.err.println("CONTROLLER: Webhook Verification SUCCESSFUL!");
            return ResponseEntity.ok(challenge != null ? challenge : "VERIFIED");
        }
        return ResponseEntity.status(403).body("Forbidden: Invalid verify token");
    }

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
            // Find any valid admin/owner to bind the sandbox to. Fallback securely.
            com.nagar.parishad.backend.entity.User admin = userRepository.findAll().stream()
                    .findFirst()
                    .orElse(null);

            if (admin == null)
                return "Admin not found";

            com.nagar.parishad.backend.entity.TenantTwilioConfig config = tenantTwilioConfigRepository.findAll()
                    .stream()
                    .filter(c -> c.getAdmin().getId().equals(admin.getId()))
                    .findFirst()
                    .orElse(new com.nagar.parishad.backend.entity.TenantTwilioConfig());

            config.setAdmin(admin);
            config.setAccountSid(defaultTwilioSid);
            config.setAuthToken(defaultTwilioToken);
            config.setPhoneNumber(defaultTwilioPhone);
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

        String from = null;
        String to = null;
        String body = "";
        int numMedia = 0;
        java.util.List<String> mediaUrls = new java.util.ArrayList<>();

        // Check if rawBody is JSON (Meta Cloud API / WABridge gateway format)
        String trimmedBody = rawBody.trim();
        if (trimmedBody.startsWith("{") || trimmedBody.startsWith("[")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(rawBody);

                // Meta Cloud API / WABridge nested structure check:
                // Case 1: entry[0].changes[0].value
                // Case 2: changes[0].value (Direct WA Bridge payload)
                com.fasterxml.jackson.databind.JsonNode changes = null;
                com.fasterxml.jackson.databind.JsonNode entryNode = rootNode.path("entry");
                if (entryNode.isArray() && entryNode.size() > 0) {
                    changes = entryNode.get(0).path("changes");
                } else if (rootNode.path("changes").isArray() && rootNode.path("changes").size() > 0) {
                    changes = rootNode.path("changes");
                }

                if (changes != null && changes.isArray() && changes.size() > 0) {
                    com.fasterxml.jackson.databind.JsonNode valueNode = changes.get(0).path("value");

                    // Receiver Phone / Metadata
                    to = valueNode.path("metadata").path("display_phone_number").asText(null);

                    com.fasterxml.jackson.databind.JsonNode messagesNode = valueNode.path("messages");
                    if (messagesNode.isArray() && messagesNode.size() > 0) {
                        com.fasterxml.jackson.databind.JsonNode msg = messagesNode.get(0);
                        from = msg.path("from").asText(null);
                        String msgType = msg.path("type").asText("text");

                        if ("text".equals(msgType)) {
                            body = msg.path("text").path("body").asText("");
                        } else if ("button".equals(msgType)) {
                            body = msg.path("button").path("payload").asText("");
                        } else if ("interactive".equals(msgType)) {
                            com.fasterxml.jackson.databind.JsonNode interactive = msg.path("interactive");
                            if (interactive.has("button_reply")) {
                                body = interactive.path("button_reply").path("id").asText("");
                            } else if (interactive.has("list_reply")) {
                                body = interactive.path("list_reply").path("id").asText("");
                            }
                        } else if ("location".equals(msgType)) {
                            com.fasterxml.jackson.databind.JsonNode loc = msg.path("location");
                            String lat = loc.path("latitude").asText("");
                            String lon = loc.path("longitude").asText("");
                            if (!lat.isEmpty() && !lon.isEmpty()) {
                                body = lat + "," + lon;
                            }
                        } else if ("image".equals(msgType)) {
                            com.fasterxml.jackson.databind.JsonNode img = msg.path("image");
                            String mediaUrl = img.path("url").asText(""); // Twilio
                            String mediaId = img.path("id").asText(""); // Meta
                            if (!mediaUrl.isEmpty()) {
                                mediaUrls.add(mediaUrl);
                                numMedia = 1;
                            } else if (!mediaId.isEmpty()) {
                                mediaUrls.add("meta_media_id:" + mediaId);
                                numMedia = 1;
                            }
                            body = img.path("caption").asText("");
                        }
                    }
                }

                // Fallback for flat JSON structure: {"from":"...", "to":"...", "body":"...", "mediaUrl":"..."}
                if (from == null) {
                    from = rootNode.path("from").asText(rootNode.path("mobile").asText(null));
                    to = rootNode.path("to").asText(null);
                    if ((to == null || to.isEmpty() || "null".equalsIgnoreCase(to)) && rootNode.has("botPhone")) {
                        to = rootNode.path("botPhone").asText(null);
                    }
                    body = rootNode.path("body").asText(rootNode.path("text").asText(""));
                    if (rootNode.has("mediaUrl") && !rootNode.path("mediaUrl").isNull()) {
                        String mUrl = rootNode.path("mediaUrl").asText("").trim();
                        if (!mUrl.isEmpty() && !"null".equalsIgnoreCase(mUrl)) {
                            mediaUrls.add(mUrl);
                            numMedia = 1;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("CONTROLLER: JSON parsing failed, falling back to form parsing: " + e.getMessage());
            }
        }

        // Manual Parsing for Twilio Form Data (Fallback)
        if (from == null) {
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

            System.err.println("CONTROLLER: Manually Parsed Form Params: " + params);

            from = params.get("From");
            to = params.get("To");
            body = params.getOrDefault("Body", "");

            if (params.containsKey("ButtonPayload")) {
                body = params.get("ButtonPayload");
            } else if (params.containsKey("ListId")) {
                body = params.get("ListId");
            }

            String latitude = params.get("Latitude");
            String longitude = params.get("Longitude");
            if ((body == null || body.trim().isEmpty()) && latitude != null && longitude != null && !latitude.isEmpty() && !longitude.isEmpty()) {
                body = latitude + "," + longitude;
                System.err.println("CONTROLLER: Extracted GPS Location pin: " + body);
            }

            String numMediaStr = params.getOrDefault("NumMedia", "0");
            try {
                numMedia = Integer.parseInt(numMediaStr);
            } catch (NumberFormatException e) {
            }

            if (numMedia > 0) {
                for (int i = 0; i < numMedia; i++) {
                    String key = "MediaUrl" + i;
                    String url = params.get(key);
                    if (url != null && !url.isEmpty()) {
                        mediaUrls.add(url);
                    }
                }
            }
        }

        if (from == null) {
            System.err.println("CONTROLLER: Missing Sender ('from'). Payload ignored.");
            return "";
        }

        String mobile = from.replace("whatsapp:", "");
        final String finalTo = to;
        final String finalBody = body;
        final java.util.List<String> finalMediaUrls = mediaUrls;
        final int finalNumMedia = numMedia;

        // Process message asynchronously in dedicated High-Throughput Thread Pool
        CHATBOT_EXECUTOR.submit(() -> {
            try {
                System.err.println("CONTROLLER: Processing message asynchronously for mobile=" + mobile + ", to=" + finalTo + ", body=" + finalBody + ", mediaCount=" + finalMediaUrls.size());
                whatsappService.processMessage(mobile, finalTo, finalBody, finalNumMedia, finalMediaUrls);
                System.err.println("CONTROLLER: Async processing completed for " + mobile);
            } catch (Exception e) {
                System.err.println("CONTROLLER: CRITICAL SERVICE ERROR for " + mobile + ": " + e.getMessage());
                e.printStackTrace();
            }
        });

        return "";
    }

    private static final java.util.concurrent.ExecutorService CHATBOT_EXECUTOR = new java.util.concurrent.ThreadPoolExecutor(
            50,
            250,
            60L, java.util.concurrent.TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(10000),
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
    );
}

