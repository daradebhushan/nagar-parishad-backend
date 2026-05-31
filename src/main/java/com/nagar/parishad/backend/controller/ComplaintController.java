package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ComplaintDTO;
import com.nagar.parishad.backend.dto.TaskRequest;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.ComplaintStatus;
import com.nagar.parishad.backend.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/complaints")

public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<List<ComplaintDTO>> getAllComplaints(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(complaintService.getComplaintsForUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintDTO> getComplaintById(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(complaintService.getComplaintByIdForUser(id, user));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ComplaintDTO> updateStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        complaintService.getComplaintEntityForUser(id, user);
        ComplaintStatus status = ComplaintStatus.valueOf(body.get("status"));
        String reason = body.get("reason");
        return ResponseEntity.ok(complaintService.updateStatus(id, status, reason));
    }

    @PostMapping("/{id}/create-task")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ComplaintDTO> createTaskFromComplaint(@PathVariable Long id,
            @RequestBody TaskRequest taskRequest,
            @AuthenticationPrincipal User admin) {
        complaintService.getComplaintEntityForUser(id, admin);
        return ResponseEntity.ok(complaintService.createTaskFromComplaint(id, taskRequest, admin));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ComplaintDTO> addComment(@PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        complaintService.getComplaintEntityForUser(id, user);
        return ResponseEntity.ok(complaintService.addComment(id, body.get("text"), user));
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ComplaintDTO> addAttachment(@PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal User user) {
        complaintService.getComplaintEntityForUser(id, user);
        return ResponseEntity.ok(complaintService.addAttachment(id, file, user));
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return complaintService.downloadAttachment(id, user);
    }
}
