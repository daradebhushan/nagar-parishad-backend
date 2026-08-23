package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ChatbotConfigDTO;
import com.nagar.parishad.backend.dto.ComplaintDTO;
import com.nagar.parishad.backend.dto.ComplaintSubmissionDTO;
import com.nagar.parishad.backend.dto.ComplaintTypeDTO;
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import com.nagar.parishad.backend.service.ChatbotConfigService;
import com.nagar.parishad.backend.service.ComplaintService;
import com.nagar.parishad.backend.service.ComplaintTypeService;
import com.nagar.parishad.backend.repository.UserRepository;
import com.nagar.parishad.backend.repository.ComplaintRepository;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.entity.Complaint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

import java.util.List;

@RestController
@RequestMapping("/api/public")

public class PublicController {

    @Autowired
    private ChatbotConfigService chatbotConfigService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ComplaintTypeService complaintTypeService;

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @org.springframework.beans.factory.annotation.Value("${app.file.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadPublicFile(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "File is empty");
            return ResponseEntity.badRequest().body(err);
        }
        try {
            String originalFileName = org.springframework.util.StringUtils.cleanPath(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo.jpg");
            String fileName = "citizen_" + System.currentTimeMillis() + "_" 
                    + java.util.UUID.randomUUID().toString().substring(0, 8) + "_" + originalFileName;
            
            java.nio.file.Path targetLocation = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize().resolve(fileName);
            java.nio.file.Files.createDirectories(targetLocation.getParent());
            java.nio.file.Files.copy(file.getInputStream(), targetLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            String fileUrl = "/uploads/" + fileName;
            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            response.put("fileName", fileName);
            response.put("fileType", file.getContentType() != null ? file.getContentType() : "image/jpeg");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/config")
    public ResponseEntity<List<ChatbotConfigDTO>> getChatbotConfig() {
        return ResponseEntity.ok(chatbotConfigService.getAllConfigs());
    }

    @GetMapping("/tenant/{publicId}")
    public ResponseEntity<Map<String, Object>> getTenantInfo(@PathVariable String publicId) {
        User admin = userRepository.findByPublicId(publicId).orElse(null);
        Map<String, Object> response = new HashMap<>();
        if (admin != null) {
            response.put("tenantId", admin.getPublicId());
            response.put("organizationName", admin.getOrganizationName());
            response.put("logoUrl", admin.getOrganizationLogo()); // Optional
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/tenant/by-domain")
    public ResponseEntity<Map<String, Object>> getTenantInfoByDomain(@RequestParam String domain) {
        User admin = userRepository.findByDomain(domain).orElse(null);
        Map<String, Object> response = new HashMap<>();
        if (admin != null) {
            response.put("tenantId", admin.getPublicId());
            response.put("organizationName", admin.getOrganizationName());
            response.put("logoUrl", admin.getOrganizationLogo()); // Optional
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getActiveDepartments(@RequestParam(required = false) String tenantId) {
        List<Department> departments = new java.util.ArrayList<>();
        if (tenantId != null) {
            User admin = userRepository.findByPublicId(tenantId).orElse(null);
            if (admin != null) {
                departments = departmentRepository.findByAdminId(admin.getId());
            }
        } else {
            departments = departmentRepository.findAll();
        }

        return ResponseEntity.ok(departments.stream()
                .filter(Department::isActive)
                .collect(java.util.stream.Collectors.toList()));
    }

    @GetMapping("/complaint-types/{deptId}")
    public ResponseEntity<List<ComplaintTypeDTO>> getComplaintTypes(@PathVariable Long deptId) {
        // Retrieve active types only
        List<ComplaintTypeDTO> types = complaintTypeService.getComplaintTypesByDepartment(deptId);
        return ResponseEntity
                .ok(types.stream().filter(ComplaintTypeDTO::isActive).collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping("/complaints")
    public ResponseEntity<ComplaintDTO> submitComplaint(@RequestBody ComplaintSubmissionDTO dto) {
        return ResponseEntity.ok(complaintService.submitComplaint(dto));
    }

    @GetMapping("/complaints/track")
    public ResponseEntity<List<ComplaintDTO>> trackComplaint(
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String complaintNo,
            @RequestParam(required = false) String tenantId) {
        
        List<Complaint> complaints = new java.util.ArrayList<>();

        if (complaintNo != null && !complaintNo.trim().isEmpty()) {
            String cleanNo = complaintNo.trim().toUpperCase();
            if (mobile != null && !mobile.trim().isEmpty()) {
                complaintRepository.findByComplaintNoAndCitizenMobile(cleanNo, mobile.trim())
                        .ifPresent(complaints::add);
                if (complaints.isEmpty()) {
                    // Fallback to complaintNo directly
                    complaintRepository.findByComplaintNo(cleanNo).ifPresent(complaints::add);
                }
            } else {
                complaintRepository.findByComplaintNo(cleanNo).ifPresent(complaints::add);
            }
        } else if (mobile != null && !mobile.trim().isEmpty()) {
            String cleanMobile = mobile.trim();
            if (tenantId != null && !tenantId.trim().isEmpty()) {
                User admin = userRepository.findByPublicId(tenantId.trim()).orElse(null);
                if (admin != null) {
                    complaints = complaintRepository.findByCitizenMobileAndAdminIdOrderByCreatedAtDesc(cleanMobile, admin.getId());
                }
            }
            if (complaints.isEmpty()) {
                complaints = complaintRepository.findByCitizenMobile(cleanMobile);
            }
        } else {
            return ResponseEntity.badRequest().build();
        }

        if (complaints.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<ComplaintDTO> dtos = complaints.stream()
                .map(complaintService::convertToDTO)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}
