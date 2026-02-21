package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ApiResponse;
import com.nagar.parishad.backend.dto.NotificationDto;
import com.nagar.parishad.backend.entity.Notification;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.service.NotificationService;
import com.nagar.parishad.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")

public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuthService authService; // Assuming you have a way to get current user

    @GetMapping
    public ResponseEntity<ApiResponse> getUserNotifications(@RequestHeader("Authorization") String token) {
        User user = authService.getUserFromToken(token); // Or however you get the user
        List<Notification> notifications = notificationService.getUserNotifications(user);

        List<NotificationDto> dtos = notifications.stream().map(n -> {
            NotificationDto dto = new NotificationDto();
            dto.setId(n.getId());
            dto.setMessage(n.getMessage());
            dto.setType(n.getType());
            dto.setRelatedTaskId(n.getRelatedTaskId());
            dto.setRead(n.isRead());
            dto.setCreatedAt(n.getCreatedAt());
            dto.setSenderName(n.getSender() != null ? n.getSender().getName() : "System");
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse(true, "Notifications fetched", dtos));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse> getUnreadCount(@RequestHeader("Authorization") String token) {
        User user = authService.getUserFromToken(token);
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(new ApiResponse(true, "Unread count fetched", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse> markAsRead(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        User user = authService.getUserFromToken(token);
        notificationService.markAsRead(id, user);
        return ResponseEntity.ok(new ApiResponse(true, "Marked as read", null));
    }

    @Autowired
    private com.nagar.parishad.backend.service.EmailService emailService;

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse> markAllAsRead(@RequestHeader("Authorization") String token) {
        User user = authService.getUserFromToken(token);
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(new ApiResponse(true, "All marked as read", null));
    }

    @PostMapping("/test-email")
    public ResponseEntity<ApiResponse> sendTestEmail(@RequestParam String to) {
        emailService.sendEmail(to, "Test Notification Email",
                "This is a test email from Nagar Parishad System to verify SMTP configuration.");
        return ResponseEntity.ok(new ApiResponse(true, "Test email sent to " + to, null));
    }
}
