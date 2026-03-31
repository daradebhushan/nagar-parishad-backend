package com.nagar.parishad.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nagar.parishad.backend.dto.ComplaintSubmissionDTO;
import java.util.List;
import java.util.ArrayList;
import com.nagar.parishad.backend.entity.ChatbotConfig;
import com.nagar.parishad.backend.entity.ChatbotSession;
import com.nagar.parishad.backend.entity.Complaint;
import com.nagar.parishad.backend.entity.ComplaintType;
import com.nagar.parishad.backend.entity.Task;
import com.nagar.parishad.backend.enums.ChatbotState;
import com.nagar.parishad.backend.enums.ComplaintStatus;
import com.nagar.parishad.backend.enums.TaskPriority;
import com.nagar.parishad.backend.enums.TaskStatus;
import com.nagar.parishad.backend.repository.ChatbotConfigRepository;
import com.nagar.parishad.backend.repository.ChatbotSessionRepository;
import com.nagar.parishad.backend.repository.ComplaintRepository;
import com.nagar.parishad.backend.repository.ComplaintTypeRepository;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import com.nagar.parishad.backend.repository.TaskRepository;
import com.nagar.parishad.backend.repository.UserRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Service
public class WhatsappService {

    @Autowired
    private ChatbotSessionRepository sessionRepository;

    @Autowired
    private ChatbotConfigRepository configRepository;

    @Autowired
    private ComplaintService complaintService;

    // Helper to download media
    // Helper to download media with Auth
    private String downloadMedia(String mediaUrl, com.nagar.parishad.backend.entity.TenantTwilioConfig config) {
        try {
            if (mediaUrl == null || mediaUrl.isEmpty())
                return null;

            // Generate unique filename with UUID to prevent collision during batch
            // processing
            String fileName = "WA_" + System.currentTimeMillis() + "_"
                    + java.util.UUID.randomUUID().toString().substring(0, 8) + ".jpg";
            java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads");
            if (!java.nio.file.Files.exists(uploadDir)) {
                java.nio.file.Files.createDirectories(uploadDir);
            }

            java.nio.file.Path filePath = uploadDir.resolve(fileName);

            // Download with Basic Auth
            java.net.URL url = new java.net.URL(mediaUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

            String accountSid = (config != null && config.getAccountSid() != null) ? config.getAccountSid() : defaultTwilioSid;
            String authToken = (config != null && config.getAuthToken() != null) ? config.getAuthToken() : defaultTwilioToken;

            String auth = accountSid + ":" + authToken;
            String encodedAuth = java.util.Base64.getEncoder()
                    .encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            // Handle redirects just in case (HttpURLConnection does it automatically
            // usually, but Auth might drop)
            // For now standard connection.

            try (java.io.InputStream in = conn.getInputStream()) {
                java.nio.file.Files.copy(in, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            String localUrl = "/uploads/" + fileName;
            System.err.println("SERVICE: Downloaded Whatsapp Media to: " + localUrl);
            return localUrl;
        } catch (Exception e) {
            System.err.println("SERVICE: Failed to download media: " + e.getMessage());
            e.printStackTrace();
            return mediaUrl; // Fallback to original
        }
    }

    @Autowired
    private com.nagar.parishad.backend.repository.TenantTwilioConfigRepository tenantTwilioConfigRepository;

    @Autowired
    private com.nagar.parishad.backend.repository.UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DepartmentService departmentService;

    @Value("${whatsapp.api.url}")
    private String whatsappApiUrl;

    @Value("${twilio.account_sid:}")
    private String defaultTwilioSid;

    @Value("${twilio.auth_token:}")
    private String defaultTwilioToken;

    @Value("${twilio.phone_number:}")
    private String defaultTwilioPhone;

    @Autowired
    private ComplaintTypeRepository complaintTypeRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ThreadLocal to capture simulation output
    private static final ThreadLocal<StringBuilder> simulationOutput = new ThreadLocal<>();

    @Transactional
    public void processMessage(String mobile, String toNumber, String message, int numMedia,
            java.util.List<String> mediaUrls) {
        System.err.println("SERVICE: DEBUG: Incoming Message from " + mobile + " to " + toNumber + ": " + message);

        // 1. Identify Tenant (Admin) by 'To' number
        com.nagar.parishad.backend.entity.TenantTwilioConfig tenantConfig = tenantTwilioConfigRepository
                .findByPhoneNumber(toNumber)
                .orElse(null);

        if (tenantConfig == null) {
            System.err.println("SERVICE: ERROR: No tenant config found for incoming number: " + toNumber);
            return;
        } else {
            System.err.println("SERVICE: Tenant Found. Admin: " + tenantConfig.getAdmin().getEmail());
        }

        com.nagar.parishad.backend.entity.User admin = tenantConfig.getAdmin();

        // 2. Load/Create Session for this Citizen + Admin Combo
        ChatbotSession session = sessionRepository.findByMobileNumberAndAdminId(mobile, admin.getId())
                .orElseGet(() -> {
                    ChatbotSession newSession = new ChatbotSession();
                    newSession.setMobileNumber(mobile);
                    newSession.setAdmin(admin);
                    newSession.setState(ChatbotState.LANGUAGE_SELECTION);
                    return sessionRepository.save(newSession);
                });

        // 3. Expiry Check (1 hour)
        if (session.getLastUpdated().isBefore(LocalDateTime.now().minusHours(1))) {
            System.out.println("DEBUG: Session expired for " + mobile + ". Resetting.");
            session.setState(ChatbotState.LANGUAGE_SELECTION);
            session.setTempData(null);
            session.setLanguage(null);
        }

        handleState(session, message, numMedia, mediaUrls, tenantConfig);
        sessionRepository.save(session);
    }

    private void handleState(ChatbotSession session, String input, int numMedia, java.util.List<String> mediaUrls,
            com.nagar.parishad.backend.entity.TenantTwilioConfig tenantConfig) {
        Long adminId = session.getAdmin().getId();
        System.out.println("DEBUG: handleState input=" + input + ", State=" + session.getState());


        // Global Reset Command
        if (input != null && (input.equalsIgnoreCase("RESET") || input.equalsIgnoreCase("Hi"))) {
            // DIRECT JUMP TO MARATHI FLOW AS PER HARDCODED REQUIREMENT
            session.setState(ChatbotState.DYNAMIC_FLOW);
            session.setTempData(null);
            session.setLanguage("mr"); // Force Marathi context

            try {
                updateTempData(session, "currentNodeId", "start");
                handleDynamicFlow(session, "", 0, null, tenantConfig);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            if (session.getState() == ChatbotState.LANGUAGE_SELECTION) {
                handleLanguageSelection(session, input, tenantConfig);
            } else if (session.getState() == ChatbotState.DYNAMIC_FLOW) {
                handleDynamicFlow(session, input, numMedia, mediaUrls, tenantConfig);
            } else {
                // Fallback for legacy states or unexpected states -> Reset
                session.setState(ChatbotState.LANGUAGE_SELECTION);
                handleLanguageSelection(session, input, tenantConfig);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(session.getMobileNumber(),
                    getConfig("ERROR_MSG", adminId, "Error occurred. Send 'Hi' to reset."),
                    tenantConfig);
            session.setState(ChatbotState.LANGUAGE_SELECTION);
        }
    }

    private void handleLanguageSelection(ChatbotSession session, String input,
            com.nagar.parishad.backend.entity.TenantTwilioConfig tenantConfig) {
        if (isValidLanguageChoice(input)) {
            setLanguage(session, input);
            session.setState(ChatbotState.DYNAMIC_FLOW);

            // Initialize Dynamic Flow - Start at 'start' node
            try {
                updateTempData(session, "currentNodeId", "start");
                // Auto-trigger the first node display
                handleDynamicFlow(session, "", 0, null, tenantConfig);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        } else {
            String langMsg = getConfig("LANG_SELECT_MSG", session.getAdmin().getId(),
                    "Select your preferred language / आपली भाषा निवडा:\n1. English\n2. Marathi (मराठी)\n3. Hindi (हिंदी)");
            sendMessage(session.getMobileNumber(), langMsg, tenantConfig);
        }
    }

    private void handleDynamicFlow(ChatbotSession session, String input, int numMedia, java.util.List<String> mediaUrls,
            com.nagar.parishad.backend.entity.TenantTwilioConfig tenantConfig) throws JsonProcessingException {
        // ... (Flow Logic Start)
        Long adminId = session.getAdmin().getId();
        String flowJson = getHardCodedFlow(adminId);
        JsonNode flowArray = objectMapper.readTree(flowJson);

        String currentNodeId = getTempData(session, "currentNodeId", String.class);
        System.out.println("DEBUG: handleDynamicFlow currentNodeId: " + currentNodeId);
        if (currentNodeId == null || currentNodeId.isEmpty())
            currentNodeId = "start";

        JsonNode currentNode = findNodeById(flowArray, currentNodeId);

        if (currentNode == null) {
            sendMessage(session.getMobileNumber(), "Configuration Error: Node '" + currentNodeId + "' not found.",
                    tenantConfig);
            return;
        }

        String nodeType = currentNode.path("type").asText("MENU");

        Boolean waitingForInput = getTempData(session, "waitingForInput", Boolean.class);
        if (waitingForInput == null)
            waitingForInput = false;

        if (!waitingForInput) {
            // ... (Display Logic)
            if ("SUBMIT_COMPLAINT".equals(nodeType)) {
                Map<String, Object> tempData = objectMapper.readValue(session.getTempData(), HashMap.class);
                // ... (Extract fields)
                String deptIdStr = (String) tempData.get("departmentId");
                String subIssue = (String) tempData.get("description");
                String citizenName = (String) tempData.get("citizenName");
                String descDetail = (String) tempData.get("description_detail");
                String photo = (String) tempData.get("photo");
                String location = (String) tempData.get("location");
                String mobile = session.getMobileNumber();

                // Validate Dept
                Long deptId = null;
                if (deptIdStr != null) {
                    try {
                        deptId = Long.parseLong(deptIdStr);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }

                com.nagar.parishad.backend.entity.Department dept = null;
                if (deptId != null) {
                    dept = departmentRepository.findById(deptId).orElse(null);
                }

                Complaint complaint = new Complaint();
                complaint.setCitizenName(citizenName);
                complaint.setCitizenMobile(mobile);

                StringBuilder extraInfo = new StringBuilder();
                for (Map.Entry<String, Object> entry : tempData.entrySet()) {
                    String key = entry.getKey();
                    if (!key.equals("name") && !key.equals("mobile") && !key.equals("description")
                            && !key.equals("description_detail")
                            && !key.equals("photo") && !key.equals("location") && !key.equals("photoList")
                            && !key.equals("departmentId") && !key.equals("complaintTypeId")
                            && !key.equals("currentNodeId") && !key.equals("waitingForInput")) {
                        extraInfo.append("\n").append(key).append(": ").append(entry.getValue());
                    }
                }

                // FIX: Separate Sub-Issue and Description
                if (subIssue != null && !subIssue.isEmpty()) {
                    System.err.println("DEBUG: SETTING SUB COMPLAINT TYPE: " + subIssue);
                    complaint.setSubComplaintType(subIssue);
                } else {
                    System.err.println("DEBUG: SUB COMPLAINT TYPE IS NULL or EMPTY. TempData description: "
                            + tempData.get("description"));
                }

                String cleanDescription = descDetail;
                if (cleanDescription == null)
                    cleanDescription = "";

                if (extraInfo.length() > 0) {
                    cleanDescription += "\n\n--- Additional Details ---" + extraInfo.toString();
                }

                complaint.setDescription(cleanDescription);
                complaint.setLocation(location);
                complaint.setPhotoUrl(photo);
                complaint.setDepartment(dept);
                complaint.setAdmin(session.getAdmin());
                complaint.setStatus(ComplaintStatus.PENDING);
                complaint.setRelatedTask(null);

                String complaintNoStr = "CMP-" + (dept != null ? dept.getId() : "0") + "-"
                        + (1000 + new Random().nextInt(9000));
                complaint.setComplaintNo(complaintNoStr);

                Complaint savedComplaint = complaintRepository.save(complaint);

                // DYNAMIC TYPE ASSIGNMENT (Fix for N/A issue)
                // 1. Try from input
                Long typeId = null;
                String typeIdStr = (String) tempData.get("complaintTypeId");
                if (typeIdStr != null) {
                    try {
                        typeId = Long.parseLong(typeIdStr);
                    } catch (Exception e) {
                    }
                }

                // 2. If valid, set it on DTO logic (Wait, here we are saving Entity directly
                // via Repo below?? No, we call submitComplaint above? No, I rewrote it to save
                // here)
                // Ah, I see I am calling `complaintRepository.save(complaint)` above.
                // So I need to set the ComplaintType on the Entity BEFORE saving.

                com.nagar.parishad.backend.entity.ComplaintType finalType = null;

                if (typeId != null) {
                    finalType = complaintTypeRepository.findById(typeId).orElse(null);
                }
                if (finalType == null && dept != null) {
                    // Try to match SubIssue to a ComplaintType
                    if (subIssue != null && !subIssue.isEmpty()) {
                        java.util.List<com.nagar.parishad.backend.entity.ComplaintType> types = complaintTypeRepository
                                .findByDepartmentId(dept.getId());
                        for (com.nagar.parishad.backend.entity.ComplaintType t : types) {
                            // Check English or Marathi name
                            if (t.getNameEn().equalsIgnoreCase(subIssue) ||
                                    (t.getNameMr() != null && t.getNameMr().equalsIgnoreCase(subIssue))) {
                                finalType = t;
                                break;
                            }
                        }
                    }
                }

                // Fallback (General) if still null
                if (finalType == null && dept != null) {
                    // Fallback to "General" or similar if exists, but DO NOT arbitrarily pick the
                    // first one (e.g. Garbage).
                    // Logic removed to prevent "Garbage Issue" showing up for unrelated complaints.
                    java.util.List<com.nagar.parishad.backend.entity.ComplaintType> types = complaintTypeRepository
                            .findByDepartmentId(dept.getId());
                    for (com.nagar.parishad.backend.entity.ComplaintType t : types) {
                        if (t.getNameEn().equalsIgnoreCase("General Issue") || t.getNameEn().equalsIgnoreCase("General")
                                || t.getNameEn().contains("Other")) {
                            finalType = t;
                            break;
                        }
                    }
                }

                // 4. Default Fallback (Global) - REMOVED to avoid incorrect tagging
                // if (finalType == null) { ... }

                // ENSURE SUB-ISSUE IS IN DESCRIPTION AS REQUESTED ("SAVE AS IT IS")
                if (subIssue != null && !subIssue.isEmpty()) {
                    if (cleanDescription.isEmpty()) {
                        cleanDescription = subIssue;
                    } else {
                        cleanDescription = subIssue + "\n\n" + cleanDescription;
                    }
                }

                if (finalType != null) {
                    complaint.setComplaintType(finalType);
                    // Re-save with type
                    complaintRepository.save(complaint);
                }

                // Attachments
                String photoListJson = (String) tempData.get("photoList");

                System.err.println("DEBUG_SUBMIT: tempData length: "
                        + (session.getTempData() != null ? session.getTempData().length() : "null"));
                System.err.println("DEBUG_SUBMIT: photoListJson: " + photoListJson);

                if (photoListJson != null) {
                    try {
                        java.util.List<String> photoList = objectMapper.readValue(photoListJson,
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {
                                });
                        System.err.println("DEBUG_SUBMIT: photoList parsed size: " + photoList.size());
                        for (String url : photoList) {
                            try {
                                System.err.println("DEBUG_SUBMIT: Adding attachment: " + url);
                                complaintService.addAttachmentFromUrl(savedComplaint, url);
                            } catch (Exception e) {
                                System.err.println("DEBUG_SUBMIT: Failed to add attachment: " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (photo != null && !photo.isEmpty() && !photo.equals("Skip")) {
                    try {
                        complaintService.addAttachmentFromUrl(savedComplaint, photo);
                    } catch (Exception e) {
                    }
                }

                String successMsg = "तक्रार यशस्वीरीत्या नोंदवण्यात आली आहे\n" +
                        "तक्रार क्रमांक: " + savedComplaint.getComplaintNo() + "\n" +
                        "लवकरच योग्य विभागाकडे पाठवली जाईल";

                // INJECT METADATA FOR SIMULATOR
                if (simulationOutput.get() != null) {
                    try {
                        String cleanDesc = cleanDescription != null
                                ? cleanDescription.replace("\"", "'").replace("\n", " ")
                                : "";
                        String meta = " [META] {\"complaintId\": " + savedComplaint.getId() +
                                ", \"title\": \"" + (subIssue != null ? subIssue : "Complaint") + "\"" +
                                ", \"description\": \""
                                + (cleanDesc.length() > 50 ? cleanDesc.substring(0, 50) + "..." : cleanDesc) + "\"}";
                        successMsg += meta;
                    } catch (Exception e) {
                    }
                }

                sendMessage(session.getMobileNumber(), successMsg, tenantConfig);

                session.setState(ChatbotState.COMPLETED);
                session.setTempData(null);
                sessionRepository.save(session);
                return;
            }

            String prompt = getLocalizedText(currentNode, session.getLanguage());
            if ("MENU".equals(nodeType)) {
                prompt += "\n" + formatMenuOptions(currentNode, session.getLanguage());
            }

            sendMessage(session.getMobileNumber(), prompt, tenantConfig);
            updateTempData(session, "waitingForInput", true);
        } else

        {
            // Process Input
            String nextNodeId = null;

            if ("MENU".equals(nodeType)) {
                JsonNode selectedOption = findSelectedOption(currentNode, input);
                if (selectedOption != null) {
                    nextNodeId = selectedOption.path("nextId").asText();
                    String storageKey = currentNode.path("storageKey").asText(null);
                    String valueToStore = selectedOption.path("value").asText(null);
                    if (storageKey != null && !storageKey.isEmpty() && valueToStore != null) {
                        updateTempData(session, storageKey, valueToStore);
                    }
                } else {
                    sendMessage(session.getMobileNumber(), getLocalizedInvalidMsg(session.getLanguage()), tenantConfig);
                    return;
                }
            } else {
                // TEXT, PHOTO, LOCATION
                String storageKey = currentNode.path("storageKey").asText();
                String valueToStore = input;

                if ("ask_photo".equals(currentNodeId)) {
                    java.util.List<String> photoList = new java.util.ArrayList<>();
                    String existing = getTempData(session, "photoList", String.class);
                    if (existing != null) {
                        try {
                            photoList = objectMapper.readValue(existing,
                                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {
                                    });
                        } catch (Exception e) {
                        }
                    }

                    if (numMedia > 0 && mediaUrls != null && !mediaUrls.isEmpty()) {
                        // DOWNLOAD ALL MEDIA
                        for (String mediaUrl : mediaUrls) {
                            String localUrl = downloadMedia(mediaUrl, tenantConfig);
                            if (localUrl != null) {
                                photoList.add(localUrl);
                            }
                        }

                        updateTempData(session, "photoList", objectMapper.writeValueAsString(photoList));

                        // Update "photo" with the LAST one for backward compatibility
                        if (!photoList.isEmpty()) {
                            updateTempData(session, "photo", photoList.get(photoList.size() - 1));
                        }

                        String reply = "mr".equals(session.getLanguage())
                                ? "फोटो मिळाले (" + numMedia
                                        + "). अधिक फोटो पाठवा किंवा पुढे जाण्यासाठी 'Next' टाईप करा."
                                : "Photos received (" + numMedia + "). Send another or type 'Next' to proceed.";
                        sendMessage(session.getMobileNumber(), reply, tenantConfig);
                        return; // STAY ON SAME NODE
                    } else if (input != null
                            && (input.trim().equalsIgnoreCase("Skip") || input.trim().equalsIgnoreCase("Next"))) {
                        if (photoList.isEmpty() && input.trim().equalsIgnoreCase("Skip")) {
                            valueToStore = "Skip";
                        } else {
                            valueToStore = "Next";
                        }
                    } else {
                        String reply = "mr".equals(session.getLanguage()) ? "कृपया फोटो पाठवा किंवा 'Next' टाईप करा."
                                : "Please send photo or type 'Next'.";
                        sendMessage(session.getMobileNumber(), reply, tenantConfig);
                        return;
                    }
                }

                if (storageKey != null && !storageKey.isEmpty()) {
                    if (!"photo".equals(storageKey) || !"Next".equals(valueToStore)) {
                        updateTempData(session, storageKey, valueToStore);
                    }
                }
                nextNodeId = currentNode.path("nextId").asText(null);
            }

            if (nextNodeId != null) {
                updateTempData(session, "currentNodeId", nextNodeId);
                updateTempData(session, "waitingForInput", false);
                handleDynamicFlow(session, "", 0, null, tenantConfig);
            } else {
                sendMessage(session.getMobileNumber(), "End of flow.", tenantConfig);
                session.setState(ChatbotState.LANGUAGE_SELECTION);
            }
        }

    }

    private JsonNode findNodeById(JsonNode array, String id) {
        if (array.isArray()) {
            for (JsonNode node : array) {
                if (node.path("id").asText().equals(id)) {
                    return node;
                }
            }
        }
        return null;
    }

    private String getLocalizedText(JsonNode node, String lang) {
        if ("mr".equals(lang) && node.has("textMr"))
            return node.get("textMr").asText();
        if ("hi".equals(lang) && node.has("textHi"))
            return node.get("textHi").asText();
        return node.path("textEn").asText("");
    }

    private String getLocalizedInvalidMsg(String lang) {
        if ("mr".equals(lang))
            return "अवैध निवड. कृपया पुन्हा प्रयत्न करा.";
        if ("hi".equals(lang))
            return "अमान्य चयन. कृपया पुन: प्रयास करें.";
        return "Invalid selection. Please try again.";
    }

    private String formatMenuOptions(JsonNode node, String lang) {
        StringBuilder sb = new StringBuilder();
        JsonNode options = node.path("options");
        if (options.isArray()) {
            int idx = 1;
            for (JsonNode opt : options) {
                String label = "mr".equals(lang) && opt.has("labelMr") ? opt.get("labelMr").asText()
                        : "hi".equals(lang) && opt.has("labelHi") ? opt.get("labelHi").asText()
                                : opt.path("labelEn").asText();
                sb.append(idx++).append(". ").append(label).append("\n");
            }
            sb.append("\n_Note: You can also select an option using the menu button below._");
        }
        return sb.toString();
    }

    private JsonNode findSelectedOption(JsonNode node, String input) {
        try {
            System.out.println("DEBUG: findSelectedOption input: " + input);

            JsonNode options = node.path("options");
            System.out.println("DEBUG: Options Array Size: " + (options.isArray() ? options.size() : "Not Array"));

            if (options.isArray()) {
                // First try numerical index
                try {
                    int idx = Integer.parseInt(input.trim());
                    if (idx > 0 && idx <= options.size()) {
                        return options.get(idx - 1);
                    }
                } catch (NumberFormatException e) {
                    // Try to match interactive payload (id or exact text)
                    for (JsonNode opt : options) {
                        String id = opt.path("id").asText();
                        String lblEn = opt.path("labelEn").asText();
                        String lblMr = opt.path("labelMr").asText();
                        String lblHi = opt.path("labelHi").asText();

                        if (input.equals(id) || input.equalsIgnoreCase(lblEn)
                                || input.equalsIgnoreCase(lblMr) || input.equalsIgnoreCase(lblHi)) {
                            return opt;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    // Updated findNextNodeForMenu to support storage.
    // Wait, I cannot change signature easily inside the method above without
    // passing session.
    // Let's copy-paste and fix logic inside handleDynamicFlow

    // ... (Helper methods for Config and Twilio)

    private String getConfig(String key, Long adminId, String fallback) {
        return configRepository.findByAdminIdAndConfKey(adminId, key)
                .map(ChatbotConfig::getConfValue)
                .orElse(fallback);
    }

    public void sendNotification(String toMobile, String message, Long adminId) {
        com.nagar.parishad.backend.entity.TenantTwilioConfig config = tenantTwilioConfigRepository
                .findByAdminId(adminId)
                .orElse(null);

        if (config != null && config.isActive()) {
            if (!toMobile.startsWith("whatsapp:")) {
                toMobile = "whatsapp:" + toMobile;
            }
            toMobile = toMobile.replace("whatsapp:", "");

            sendMessage(toMobile, message, null, null, config);
        } else {
            System.err.println("Notification skipped: No active Twilio config for Admin " + adminId);
        }
    }

    private void sendMessage(String to, String text,
            com.nagar.parishad.backend.entity.TenantTwilioConfig tenantConfig) {
        sendMessage(to, text, null, null, tenantConfig);
    }

    private void sendMessage(String to, String text, JsonNode menuNode, String lang,
            com.nagar.parishad.backend.entity.TenantTwilioConfig tenantConfig) {
        try {
            System.err.println("SERVICE: Sending Message to " + to + ": " + text);
            if (simulationOutput.get() != null) {
                simulationOutput.get().append("Bot: ").append(text).append("\n");
                return;
            }

            String accountSid = (tenantConfig != null && tenantConfig.getAccountSid() != null) ? tenantConfig.getAccountSid() : defaultTwilioSid;
            String authToken = (tenantConfig != null && tenantConfig.getAuthToken() != null) ? tenantConfig.getAuthToken() : defaultTwilioToken;
            String fromNumber = (tenantConfig != null && tenantConfig.getPhoneNumber() != null) ? tenantConfig.getPhoneNumber() : defaultTwilioPhone;
            if (fromNumber != null && !fromNumber.startsWith("whatsapp:")) {
                fromNumber = "whatsapp:" + fromNumber;
            } else if (fromNumber == null) {
                fromNumber = "whatsapp:+14155238886"; // Ultimate fallback if properties are missing
            }

            Twilio.init(accountSid, authToken);

            com.twilio.rest.api.v2010.account.MessageCreator creator = Message.creator(
                    new com.twilio.type.PhoneNumber("whatsapp:" + to),
                    new com.twilio.type.PhoneNumber("whatsapp:" + fromNumber),
                    text);

            if (menuNode != null && menuNode.path("options").isArray()) {
                JsonNode options = menuNode.path("options");
                System.out.println("DEBUG: Generating WhatsApp Interactive Menu. Count=" + options.size());

                String interactiveJson = buildInteractivePayload(options, lang);
                if (interactiveJson != null) {
                    List<String> actions = new ArrayList<>();
                    actions.add(interactiveJson);
                    creator.setPersistentAction(actions);
                }
            }

            creator.create();
            System.err.println("SERVICE: Message Sent Successfully.");
        } catch (Exception e) {
            System.err.println("SERVICE: TWILIO SEND ERROR: " + e.getMessage());
        }
    }

    private String buildInteractivePayload(JsonNode options, String lang) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            if (options.size() > 0 && options.size() <= 3) {
                // Return Reply Buttons
                ObjectNode payload = mapper.createObjectNode();
                payload.put("type", "button");

                ObjectNode body = mapper.createObjectNode();
                body.put("text", "Please select an option:");
                payload.set("body", body);

                ObjectNode action = mapper.createObjectNode();
                ArrayNode buttons = mapper.createArrayNode();

                for (JsonNode opt : options) {
                    String label = "mr".equals(lang) && opt.has("labelMr") ? opt.get("labelMr").asText()
                            : "hi".equals(lang) && opt.has("labelHi") ? opt.get("labelHi").asText()
                                    : opt.path("labelEn").asText();

                    ObjectNode btn = mapper.createObjectNode();
                    btn.put("type", "reply");

                    ObjectNode reply = mapper.createObjectNode();
                    reply.put("id", opt.path("id").asText());
                    // WhatsApp allows 20 chars max for button label
                    reply.put("title", label.length() > 20 ? label.substring(0, 19) : label);

                    btn.set("reply", reply);
                    buttons.add(btn);
                }
                action.set("buttons", buttons);
                payload.set("action", action);

                return payload.toString();

            } else if (options.size() > 3) {
                // Return List Picker
                ObjectNode payload = mapper.createObjectNode();
                payload.put("type", "list");

                ObjectNode body = mapper.createObjectNode();
                body.put("text", "Please select from the options below:");
                payload.set("body", body);

                ObjectNode action = mapper.createObjectNode();
                action.put("button", "View Options");

                ArrayNode sections = mapper.createArrayNode();
                ObjectNode section = mapper.createObjectNode();
                section.put("title", "Select One");

                ArrayNode rows = mapper.createArrayNode();
                for (JsonNode opt : options) {
                    String label = "mr".equals(lang) && opt.has("labelMr") ? opt.get("labelMr").asText()
                            : "hi".equals(lang) && opt.has("labelHi") ? opt.get("labelHi").asText()
                                    : opt.path("labelEn").asText();

                    ObjectNode row = mapper.createObjectNode();
                    row.put("id", opt.path("id").asText());
                    row.put("title", label.length() > 24 ? label.substring(0, 23) : label);
                    rows.add(row);
                }

                section.set("rows", rows);
                sections.add(section);
                action.set("sections", sections);
                payload.set("action", action);

                return payload.toString();
            }
        } catch (Exception e) {
            System.err.println("SERVICE: ERROR Building Interactive Menu Payload: " + e.getMessage());
        }
        return null;
    }

    // @Transactional // Removed to prevent Session poisoning on duplicate entry
    // error
    public String simulate(String mobile, String message, int numMedia, String mediaUrl, Long adminId) {
        System.out.println(
                "DEBUG: Simulator.simulate called. Mobile=" + mobile + ", Msg=" + message + ", Media=" + mediaUrl
                        + ", AdminId=" + adminId);
        com.nagar.parishad.backend.entity.User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Sim Admin not found"));

        // Mock a Tenant Config
        com.nagar.parishad.backend.entity.TenantTwilioConfig mockConfig = new com.nagar.parishad.backend.entity.TenantTwilioConfig();
        mockConfig.setAdmin(admin);
        mockConfig.setPhoneNumber("whatsapp:+91SIMULATOR");
        mockConfig.setAccountSid("SIM_SID");
        mockConfig.setAuthToken("SIM_TOKEN");

        // Use Entity-based finder for robustness -> Switch to ID based for better
        // compatibility
        String cleanMobile = mobile.trim();
        ChatbotSession session = sessionRepository.findByMobileNumberAndAdminId(cleanMobile, admin.getId())
                .orElse(null);

        if (session == null) {
            try {
                System.out.println("DEBUG: Creating new session for simulation.");
                ChatbotSession s = new ChatbotSession();
                s.setMobileNumber(cleanMobile);
                s.setAdmin(admin);
                s.setState(ChatbotState.LANGUAGE_SELECTION);
                session = sessionRepository.save(s);
            } catch (org.springframework.dao.DataIntegrityViolationException
                    | org.hibernate.exception.ConstraintViolationException e) {
                System.out.println("DEBUG: Save failed. Exception: " + e.getMessage());
                e.printStackTrace();

                System.out.println("DEBUG: Checking if it was a duplicate session. Fetching existing via List Scan.");
                java.util.List<ChatbotSession> allSessions = sessionRepository.findByMobileNumber(cleanMobile);

                System.out.println("DEBUG: Found " + allSessions.size() + " sessions for mobile " + cleanMobile);
                for (ChatbotSession cs : allSessions) {
                    System.out.println(
                            "DEBUG: Candidate Session ID=" + cs.getId() + ", AdminID=" + cs.getAdmin().getId());
                }

                session = allSessions.stream()
                        .filter(s -> s.getAdmin().getId().equals(admin.getId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(
                                "Session retrieval failed even after List Scan (CleanMobile='" + cleanMobile
                                        + "'). AdminID=" + admin.getId()));
            }
        }

        try {
            simulationOutput.set(new StringBuilder());
            java.util.List<String> simMediaUrls = new java.util.ArrayList<>();
            if (mediaUrl != null && !mediaUrl.isEmpty()) {
                simMediaUrls.add(mediaUrl);
            }
            handleState(session, message, numMedia, simMediaUrls, mockConfig);
            sessionRepository.save(session);
            String result = simulationOutput.get().toString();
            System.out.println("DEBUG: Simulation Result for '" + message + "' is: [" + result + "]");
            return result;
        } catch (Exception e) {
            System.err.println("DEBUG: Simulation Exception: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        } finally {
            simulationOutput.remove();
        }
    }

    private boolean isValidLanguageChoice(String input) {
        String t = input.trim().toLowerCase();
        return t.equals("1") || t.equals("2") || t.equals("3") ||
                t.contains("english") || t.contains("marathi") || t.contains("hindi");
    }

    private void setLanguage(ChatbotSession session, String input) {
        String t = input.trim().toLowerCase();
        if (t.equals("1") || t.contains("english")) {
            session.setLanguage("en");
        } else if (t.equals("2") || t.contains("marathi") || t.contains("मराठी")) {
            session.setLanguage("mr");
        } else if (t.equals("3") || t.contains("hindi")) {
            session.setLanguage("hi");
        }
    }

    @SuppressWarnings("unchecked")
    private void updateTempData(ChatbotSession session, String key, Object value) throws JsonProcessingException {
        Map<String, Object> data = new HashMap<>();
        if (session.getTempData() != null) {
            try {
                data = objectMapper.readValue(session.getTempData(), HashMap.class);
            } catch (Exception e) {
                System.out.println("DEBUG: Error parsing tempData in update: " + e.getMessage());
            }
        }
        data.put(key, value);
        String json = objectMapper.writeValueAsString(data);
        System.out.println("DEBUG: Updating TempData [" + key + " = " + value + "]. New JSON: " + json);
        session.setTempData(json);
    }

    private <T> T getTempData(ChatbotSession session, String key, Class<T> clazz) throws JsonProcessingException {
        if (session.getTempData() == null)
            return null;
        Map<String, Object> data = objectMapper.readValue(session.getTempData(), HashMap.class);
        Object val = data.get(key);
        if (val == null)
            return null;

        // Handle JSON numeric types (Integer -> Long)
        if (clazz == Long.class && val instanceof Number) {
            return clazz.cast(((Number) val).longValue());
        }
        if (clazz == String.class && !(val instanceof String)) {
            return clazz.cast(String.valueOf(val));
        }

        try {
            return clazz.cast(val);
        } catch (ClassCastException e) {
            // Fallback: try to convert via string if possible or return null to avoid crash
            System.err.println("Warning: casting " + val.getClass().getName() + " to " + clazz.getName()
                    + " failed for key: " + key);
            return null;
        }
    }

    private String getHardCodedFlow(Long adminId) {
        return generateDynamicFlow(adminId);
    }

    private String generateDynamicFlow(Long adminId) {
        try {
            java.util.List<java.util.Map<String, Object>> flowNodes = new java.util.ArrayList<>();

            // 1. START NODE
            java.util.Map<String, Object> startNode = new java.util.LinkedHashMap<>();
            startNode.put("id", "start");
            startNode.put("type", "MENU");
            
            com.nagar.parishad.backend.entity.User tenantAdmin = userRepository.findById(adminId).orElse(null);
            String orgName = (tenantAdmin != null && tenantAdmin.getOrganizationName() != null) 
                             ? tenantAdmin.getOrganizationName() 
                             : "Nagar Panchayat";

            String welcomeEn = getConfig("WELCOME_MSG_EN", adminId, "Welcome to " + orgName + "\nPlease select the department for your complaint:");
            String welcomeMr = getConfig("WELCOME_MSG_MR", adminId, "आपले स्वागत आहे " + orgName + " मध्ये\nकृपया आपल्या तक्रारीसाठी योग्य विभाग निवडा:");
            
            startNode.put("textEn", welcomeEn);
            startNode.put("textMr", welcomeMr);
            startNode.put("storageKey", "departmentId");

            java.util.List<java.util.Map<String, String>> startOptions = new java.util.ArrayList<>();
            java.util.List<com.nagar.parishad.backend.entity.Department> depts = departmentRepository
                    .findByAdminId(adminId);

            if (depts == null || depts.isEmpty()) {
                if (tenantAdmin != null) {
                    try {
                        departmentService.createDefaultDepartments(tenantAdmin);
                        depts = departmentRepository.findByAdminId(adminId);
                        System.out.println("DEBUG: Auto-seeded default departments for Admin ID: " + adminId);
                    } catch (Exception e) {
                        System.err.println("Chatbot auto-seed failed: " + e.getMessage());
                    }
                }
            }

            // Filter active depts first to avoid trailing comma issues logic
            java.util.List<com.nagar.parishad.backend.entity.Department> activeDepts = new java.util.ArrayList<>();
            for (com.nagar.parishad.backend.entity.Department d : depts) {
                if (d.isActive() && d.isChatbotEnabled()) {
                    activeDepts.add(d);
                }
            }

            for (com.nagar.parishad.backend.entity.Department d : activeDepts) {
                java.util.Map<String, String> opt = new java.util.LinkedHashMap<>();
                opt.put("labelEn", d.getName());
                opt.put("labelMr", d.getNameMr() != null ? d.getNameMr() : d.getName());
                opt.put("nextId", "dept_" + d.getId());
                opt.put("value", String.valueOf(d.getId()));
                startOptions.add(opt);
            }
            startNode.put("options", startOptions);
            System.out.println("DEBUG: Generated Start Options: " + startOptions.size());
            flowNodes.add(startNode);

            // 2. DEPARTMENT SUB-NODES
            for (com.nagar.parishad.backend.entity.Department d : activeDepts) {
                java.util.Map<String, Object> deptNode = new java.util.LinkedHashMap<>();
                deptNode.put("id", "dept_" + d.getId());
                deptNode.put("type", "MENU");
                deptNode.put("textEn", "Select Sub-Issue:");
                deptNode.put("textMr", "कृपया उप-समस्या निवडा:");
                deptNode.put("storageKey", "description");

                java.util.List<java.util.Map<String, String>> deptOptions = new java.util.ArrayList<>();
                String subQ = d.getSubQuestions();
                boolean hasSubQ = false;

                if (subQ != null && !subQ.isEmpty()) {
                    try {
                        java.util.List<String> questions = objectMapper.readValue(subQ,
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {
                                });
                        if (!questions.isEmpty()) {
                            hasSubQ = true;
                            for (String q : questions) {
                                java.util.Map<String, String> subOpt = new java.util.LinkedHashMap<>();
                                subOpt.put("labelEn", q);
                                subOpt.put("labelMr", q);
                                subOpt.put("nextId", "ask_name");
                                subOpt.put("value", q);
                                deptOptions.add(subOpt);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing subQuestions for dept " + d.getId() + ": " + e.getMessage());
                    }
                }

                if (!hasSubQ) {
                    java.util.Map<String, String> defOpt = new java.util.LinkedHashMap<>();
                    defOpt.put("labelEn", "General Issue");
                    defOpt.put("labelMr", "इतर तक्रार");
                    defOpt.put("nextId", "ask_name");
                    defOpt.put("value", "General");
                    deptOptions.add(defOpt);
                }

                deptNode.put("options", deptOptions);
                flowNodes.add(deptNode);
            }

            // 3. COMMON NODES
            // ASK NAME
            java.util.Map<String, Object> askName = new java.util.LinkedHashMap<>();
            askName.put("id", "ask_name");
            askName.put("type", "TEXT");
            askName.put("textEn", "Please send your full name:");
            askName.put("textMr", "कृपया आपले पूर्ण नाव पाठवा:");
            askName.put("storageKey", "citizenName");
            askName.put("nextId", "ask_desc_detail");
            flowNodes.add(askName);

            // ASK DETAIL
            java.util.Map<String, Object> askDetail = new java.util.LinkedHashMap<>();
            askDetail.put("id", "ask_desc_detail");
            askDetail.put("type", "TEXT");
            askDetail.put("textEn", "Please provide brief details:");
            askDetail.put("textMr", "कृपया समस्येबाबत थोडक्यात माहिती द्या:");
            askDetail.put("storageKey", "description_detail");
            askDetail.put("nextId", "ask_photo");
            flowNodes.add(askDetail);

            // ASK PHOTO
            java.util.Map<String, Object> askPhoto = new java.util.LinkedHashMap<>();
            askPhoto.put("id", "ask_photo");
            askPhoto.put("type", "TEXT");
            askPhoto.put("textEn", "Please send a photo (or type 'Skip'):");
            askPhoto.put("textMr", "कृपया तक्रारीचा फोटो पाठवा (किंवा 'Skip' टाइप करा):");
            askPhoto.put("storageKey", "photo");
            askPhoto.put("nextId", "ask_location");
            flowNodes.add(askPhoto);

            // ASK LOCATION
            java.util.Map<String, Object> askLocation = new java.util.LinkedHashMap<>();
            askLocation.put("id", "ask_location");
            askLocation.put("type", "LOCATION");
            askLocation.put("textEn", "Please share location:");
            askLocation.put("textMr", "कृपया तक्रारीच्या ठिकाणाची लोकेशन शेअर करा / पत्ता सांगा:");
            askLocation.put("storageKey", "location");
            askLocation.put("nextId", "submit_node");
            flowNodes.add(askLocation);

            // SUBMIT
            java.util.Map<String, Object> submitNode = new java.util.LinkedHashMap<>();
            submitNode.put("id", "submit_node");
            submitNode.put("type", "SUBMIT_COMPLAINT");
            submitNode.put("textEn", "Submitting...");
            submitNode.put("textMr", "आपली तक्रार नोंदवली जात आहे...");
            flowNodes.add(submitNode);

            return objectMapper.writeValueAsString(flowNodes);
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }
}
