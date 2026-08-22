package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.SubscriptionRequest;
import com.nagar.parishad.backend.entity.Subscription;
import com.nagar.parishad.backend.entity.TaskAttachment;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.Role;
import com.nagar.parishad.backend.repository.SubscriptionRepository;
import com.nagar.parishad.backend.repository.UserRepository;
import com.nagar.parishad.backend.service.AuthService;
import com.nagar.parishad.backend.service.OwnerAnalyticsService;
import com.nagar.parishad.backend.repository.TaskAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
public class OwnerController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private OwnerAnalyticsService analyticsService;

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    @Autowired
    private com.nagar.parishad.backend.service.EmailService emailService;

    @Autowired
    private com.nagar.parishad.backend.service.DepartmentService departmentService;

    @Autowired
    private PasswordEncoder encoder;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalAdmins = userRepository.countByRole(Role.ADMIN);
        long totalUsers = userRepository.count();
        long activeSubscriptions = subscriptionRepository.findAll().stream().filter(Subscription::isActive).count();

        stats.put("totalAdmins", totalAdmins);
        stats.put("totalUsers", totalUsers);
        stats.put("activeSubscriptions", activeSubscriptions);

        // Calculate Data Usage (Global)
        List<TaskAttachment> attachments = taskAttachmentRepository.findAll();
        long totalBytes = attachments.stream()
                .mapToLong(att -> att.getFileSize() != null ? att.getFileSize() : 0)
                .sum();

        stats.put("totalDataUsageBytes", totalBytes);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/admins")
    public ResponseEntity<List<Map<String, Object>>> getAllAdmins() {
        List<User> admins = userRepository.findByRole(Role.ADMIN);

        List<Map<String, Object>> response = admins.stream().map(admin -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", admin.getId());
            data.put("name", admin.getName());
            data.put("email", admin.getEmail());
            data.put("mobile", admin.getMobile());
            data.put("active", admin.isActive());

            Subscription sub = subscriptionRepository.findByAdmin(admin).orElse(null);
            if (sub != null) {
                data.put("subscriptionActive", sub.isActive());
                data.put("planType", sub.getPlanType());
                data.put("endDate", sub.getEndDate());
            } else {
                data.put("subscriptionActive", false);
                data.put("planType", "NONE");
            }

            // Deep Analytics
            Map<String, Object> deepStats = analyticsService.getTenantStats(admin);
            data.putAll(deepStats);

            return data;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admins")
    public ResponseEntity<?> createAdminWithSubscription(
            @RequestBody com.nagar.parishad.backend.dto.SignupRequest request) {
        request.setRole(Role.ADMIN); // Force Admin Role
        // Create User
        // We need a special register method or reuse registerUser but passing Owner as
        // admin?
        // Admin doesn't have an 'admin' (supervisor) usually, or it's the Owner.
        // Let's set Owner as the 'admin' for hierarchy tracking if needed.

        // For simplicity, we create user manually or add a method in AuthService.
        // Let's use existing logic but we need to handle the subscription manually
        // after.

        // Actually best to create a custom method here or in Service.
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setRole(Role.ADMIN);
        user.setMobile(request.getMobile());
        user.setActive(true);
        User savedUser = userRepository.save(user);

        // Create Default Subscription
        Subscription sub = new Subscription();
        sub.setAdmin(savedUser);
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusYears(1)); // Default 1 year
        sub.setPlanType("ANNUAL");
        sub.setActive(true);
        subscriptionRepository.save(sub);

        // Seed Default Departments
        try {
            departmentService.createDefaultDepartments(savedUser);
        } catch (Exception e) {
            System.err
                    .println("Failed to seed default departments for " + savedUser.getEmail() + ": " + e.getMessage());
        }

        // Send Welcome Email
        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName(), request.getPassword());
        } catch (Exception e) {
            System.err.println("Failed to send welcome email to " + savedUser.getEmail() + ": " + e.getMessage());
            // We don't fail the request if email fails, but we log it
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Admin registered successfully with Annual subscription. Welcome email sent.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/admins/{id}/status")
    public ResponseEntity<?> updateAdminStatus(@PathVariable Long id, @RequestParam boolean active) {
        User admin = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Admin not found"));
        if (admin.getRole() != Role.ADMIN) {
            return ResponseEntity.badRequest().body("User is not an Admin");
        }

        admin.setActive(active);
        userRepository.save(admin);

        // Update subscription too?
        Subscription sub = subscriptionRepository.findByAdmin(admin).orElse(null);
        if (sub != null) {
            sub.setActive(active);
            subscriptionRepository.save(sub);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Admin status updated.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/reset-password")
    public ResponseEntity<?> resetUserPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String newPassword = payload.get("newPassword");

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset successfully for " + email);
        return ResponseEntity.ok(response);
    }

    @Autowired
    private com.nagar.parishad.backend.repository.TenantTwilioConfigRepository tenantTwilioConfigRepository;

    @GetMapping("/whatsapp/gateway")
    public ResponseEntity<Map<String, Object>> getWhatsappGatewayStatus(@RequestParam(required = false, defaultValue = "1") Long adminId) {
        Map<String, Object> result = new HashMap<>();
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:9092/api/status?adminId=" + adminId))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> gatewayResp = mapper.readValue(resp.body(), Map.class);
            result.putAll(gatewayResp);
        } catch (Exception e) {
            result.put("success", false);
            result.put("status", "GATEWAY_OFFLINE");
            result.put("error", e.getMessage());
        }
        result.put("adminId", adminId);
        result.put("qrUrl", "http://localhost:9092/qr?adminId=" + adminId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/whatsapp/gateway/unlink")
    public ResponseEntity<Map<String, Object>> unlinkWhatsappGateway(@RequestParam(required = false, defaultValue = "1") Long adminId) {
        Map<String, Object> result = new HashMap<>();
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:9092/api/unlink?adminId=" + adminId))
                    .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();
            client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            result.put("success", true);
            result.put("adminId", adminId);
            result.put("message", "WhatsApp session unlinked successfully for tenant " + adminId);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/admins/{id}/whatsapp")
    public ResponseEntity<Map<String, Object>> getAdminWhatsappConfig(@PathVariable Long id) {
        User admin = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Admin not found"));
        com.nagar.parishad.backend.entity.TenantTwilioConfig config = tenantTwilioConfigRepository.findByAdminId(admin.getId())
                .orElse(new com.nagar.parishad.backend.entity.TenantTwilioConfig());
        
        Map<String, Object> data = new HashMap<>();
        data.put("adminId", admin.getId());
        data.put("adminName", admin.getName());
        data.put("phoneNumber", config.getPhoneNumber());
        data.put("active", config.isActive());
        data.put("accountSid", config.getAccountSid());
        return ResponseEntity.ok(data);
    }

    @PutMapping("/admins/{id}/whatsapp")
    public ResponseEntity<Map<String, Object>> updateAdminWhatsappConfig(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        User admin = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Admin not found"));
        com.nagar.parishad.backend.entity.TenantTwilioConfig config = tenantTwilioConfigRepository.findByAdminId(admin.getId())
                .orElse(new com.nagar.parishad.backend.entity.TenantTwilioConfig());
        
        config.setAdmin(admin);
        if (payload.containsKey("phoneNumber")) {
            config.setPhoneNumber((String) payload.get("phoneNumber"));
        }
        if (payload.containsKey("active")) {
            config.setActive((Boolean) payload.get("active"));
        }
        tenantTwilioConfigRepository.save(config);

        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("message", "WhatsApp configuration updated for " + admin.getName());
        return ResponseEntity.ok(data);
    }
}
