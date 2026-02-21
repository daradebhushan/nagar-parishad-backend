package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ComplaintDTO;
import com.nagar.parishad.backend.entity.Complaint;
import com.nagar.parishad.backend.repository.ComplaintRepository;
import com.nagar.parishad.backend.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/diag")
public class DiagnosticController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private com.nagar.parishad.backend.repository.ComplaintAttachmentRepository attachmentRepo;

    @GetMapping("/complaint/{complaintNo}")
    public ComplaintDTO getComplaint(@PathVariable String complaintNo) {
        Complaint complaint = complaintRepository.findAll().stream()
                .filter(c -> c.getComplaintNo().equalsIgnoreCase(complaintNo))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Not found"));

        return complaintService.getComplaintById(complaint.getId());
    }

    @GetMapping("/attachments/all")
    public List<String> getAll() {
        return attachmentRepo.findAll().stream()
                .map(a -> "ID:" + a.getId() + " | C_ID:"
                        + (a.getComplaint() != null ? a.getComplaint().getId() : "NULL") + " | File:" + a.getFileName())
                .collect(Collectors.toList());
    }

    @Autowired
    private com.nagar.parishad.backend.service.WhatsappService whatsappService;

    @GetMapping("/simulate")
    public String simulate(@org.springframework.web.bind.annotation.RequestParam String mobile,
            @org.springframework.web.bind.annotation.RequestParam String text,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String mediaUrl,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") Long adminId) {
        int numMedia = (mediaUrl != null && !mediaUrl.isEmpty()) ? 1 : 0;
        return whatsappService.simulate(mobile, text, numMedia, mediaUrl, adminId);
    }

    @GetMapping("/list")
    public List<ComplaintDTO> listLast5() {
        return complaintService.getAllComplaints().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(5)
                .collect(Collectors.toList());
    }

    @Autowired
    private com.nagar.parishad.backend.repository.TenantTwilioConfigRepository configRepo;

    @GetMapping("/configs")
    public List<String> getAllConfigs() {
        return configRepo.findAll().stream()
                .map(c -> "ID:" + c.getId() + " | AdminID:" + c.getAdmin().getId() + " | Phone:" + c.getPhoneNumber())
                .collect(Collectors.toList());
    }
}
