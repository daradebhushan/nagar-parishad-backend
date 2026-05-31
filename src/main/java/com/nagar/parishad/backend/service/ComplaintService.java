package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.dto.ComplaintDTO;
import com.nagar.parishad.backend.dto.ComplaintSubmissionDTO;
import com.nagar.parishad.backend.dto.TaskRequest;
import com.nagar.parishad.backend.entity.*;
import com.nagar.parishad.backend.enums.ComplaintStatus;
import com.nagar.parishad.backend.enums.Role;
import com.nagar.parishad.backend.repository.ComplaintRepository;
import com.nagar.parishad.backend.repository.ComplaintTypeRepository;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import com.nagar.parishad.backend.repository.ComplaintTypeRepository;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import com.nagar.parishad.backend.repository.TaskRepository;
import com.nagar.parishad.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.MalformedURLException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    @Autowired
    private com.nagar.parishad.backend.repository.ComplaintAttachmentRepository complaintAttachmentRepository;

    @org.springframework.beans.factory.annotation.Value("${app.file.upload-dir}")
    private String rootUploadDir;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintTypeRepository complaintTypeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private WhatsappService whatsappService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository; // Needed to update Task entity with back-ref if needed, though OneToOne
                                           // mappedBy usually handles owner.
                                           // But Task is the owner? No, Complaint has `relatedTask` JoinColumn.
                                           // Let's check Schema: Complaint has @OneToOne @JoinColumn(name =
                                           // "related_task_id")
                                           // So Complaint owns the relationship.

    @Transactional
    public ComplaintDTO submitComplaint(ComplaintSubmissionDTO dto) {
        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        ComplaintType type = complaintTypeRepository.findById(dto.getComplaintTypeId())
                .orElseThrow(() -> new RuntimeException("Complaint Type not found"));

        Complaint complaint = new Complaint();
        complaint.setCitizenName(dto.getName());
        complaint.setCitizenMobile(dto.getMobile());
        complaint.setDepartment(dept);
        complaint.setComplaintType(type);
        complaint.setDescription(dto.getDescription());
        complaint.setPhotoUrl(dto.getPhotoUrl());
        complaint.setLocation(dto.getLocation());
        complaint.setStatus(ComplaintStatus.PENDING);

        // Generate Complaint No: "CMP-{DeptID}-{Random4}"
        String complaintNo = "CMP-" + dept.getId() + "-" + (1000 + new Random().nextInt(9000));
        complaint.setComplaintNo(complaintNo);

        Complaint savedComplaint = complaintRepository.save(complaint);

        // Process Multiple Attachments
        if (dto.getAttachmentUrls() != null && !dto.getAttachmentUrls().isEmpty()) {
            for (String url : dto.getAttachmentUrls()) {
                addAttachmentFromUrl(savedComplaint, url);
            }
        } else if (dto.getPhotoUrl() != null && !dto.getPhotoUrl().isEmpty()) {
            // Fallback for singular photoUrl field
            addAttachmentFromUrl(savedComplaint, dto.getPhotoUrl());
        }

        return convertToDTO(savedComplaint);
    }

    public List<ComplaintDTO> getAllComplaints() {
        return complaintRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<ComplaintDTO> getComplaintsByAdmin(Long adminId) {
        return complaintRepository.findByAdminId(adminId).stream().map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ComplaintDTO> getComplaintsByDepartment(Long departmentId) {
        return complaintRepository.findByDepartmentId(departmentId).stream().map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ComplaintDTO> getComplaintsForUser(User user) {
        Role role = user.getRole();
        if (role == Role.OWNER) {
            return getAllComplaints();
        }
        if (role == Role.ADMIN) {
            return getComplaintsByAdmin(user.getId());
        }
        if (role == Role.DEPARTMENT_HEAD) {
            if (user.getDepartment() == null) {
                return List.of();
            }
            return getComplaintsByDepartment(user.getDepartment().getId());
        }
        if (role == Role.STAFF) {
            return complaintRepository.findByRelatedTask_AssignedStaff_Id(user.getId()).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public ComplaintDTO getComplaintById(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
        return convertToDTO(complaint);
    }

    public ComplaintDTO getComplaintByIdForUser(Long id, User user) {
        return convertToDTO(getComplaintEntityForUser(id, user));
    }

    public Complaint getComplaintEntityForUser(Long id, User user) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
        assertComplaintAccess(complaint, user);
        return complaint;
    }

    public void assertComplaintAccess(Complaint complaint, User user) {
        Role role = user.getRole();
        if (role == Role.OWNER) {
            return;
        }
        if (role == Role.ADMIN) {
            boolean inAdminScope = (complaint.getAdmin() != null && complaint.getAdmin().getId().equals(user.getId()))
                    || (complaint.getDepartment() != null
                            && complaint.getDepartment().getAdmin() != null
                            && complaint.getDepartment().getAdmin().getId().equals(user.getId()))
                    || (complaint.getRelatedTask() != null
                            && complaint.getRelatedTask().getAdmin() != null
                            && complaint.getRelatedTask().getAdmin().getId().equals(user.getId()));
            if (inAdminScope) {
                return;
            }
        } else if (role == Role.DEPARTMENT_HEAD) {
            if (user.getDepartment() != null && complaint.getDepartment() != null
                    && user.getDepartment().getId().equals(complaint.getDepartment().getId())) {
                return;
            }
        } else if (role == Role.STAFF) {
            if (complaint.getRelatedTask() != null
                    && complaint.getRelatedTask().getAssignedStaff() != null
                    && complaint.getRelatedTask().getAssignedStaff().getId().equals(user.getId())) {
                return;
            }
        }

        throw new AccessDeniedException("Access denied to this complaint");
    }

    @Transactional
    public ComplaintDTO updateStatus(Long id, ComplaintStatus status, String reason) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        complaint.setStatus(status);
        if (status == ComplaintStatus.REJECTED && reason != null) {
            complaint.setRejectionReason(reason);
        }

        Complaint saved = complaintRepository.save(complaint);

        // Send Notification if Rejected
        if (status == ComplaintStatus.REJECTED && reason != null && complaint.getDepartment() != null) {
            // Find Admin for the department (Owner of the tenant)
            // Department -> Admin mapping should exist.
            // Assumption: Department has an Admin field or we use the Admin from the
            // Context?
            // "Admin" here refers to the Tenant Admin.
            // Department entity has admin_id?
            // Let's check Department entity or use the one from context if available,
            // but updateStatus context might be web admin.
            // Safe bet: The Department belongs to an Admin.
            User admin = saved.getDepartment().getAdmin();
            if (admin != null) {
                String msg = "Your complaint (" + saved.getComplaintNo() + ") has been REJECTED.\nReason: " + reason;
                whatsappService.sendNotification(saved.getCitizenMobile(), msg, admin.getId());
            }
        }

        return convertToDTO(saved);
    }

    @Transactional
    public ComplaintDTO createTaskFromComplaint(Long complaintId, TaskRequest taskRequest, User admin) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        // Auto-fill description if empty
        if (taskRequest.getDescription() == null || taskRequest.getDescription().trim().isEmpty()) {
            String originalDesc = complaint.getDescription();
            if (originalDesc == null)
                originalDesc = "No description provided.";

            String newDesc = originalDesc + "\n\n(Converted from Complaint #" + complaint.getComplaintNo() + ")";
            taskRequest.setDescription(newDesc);
        }

        // Create Task
        Task task = taskService.createTask(taskRequest, admin);

        // Link Task to Complaint
        complaint.setRelatedTask(task);
        complaint.setStatus(ComplaintStatus.CONVERTED_TO_TASK);

        Complaint saved = complaintRepository.save(complaint);
        return convertToDTO(saved);
    }

    @Transactional
    public ComplaintDTO addComment(Long complaintId, String text, User user) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        ComplaintComment comment = new ComplaintComment();
        comment.setComplaint(complaint);
        comment.setUser(user);
        comment.setText(text);

        complaint.getComments().add(comment);
        complaintRepository.save(complaint);

        return convertToDTO(complaint);
    }

    @Transactional
    public ComplaintDTO addAttachment(Long complaintId, org.springframework.web.multipart.MultipartFile file,
            User user) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        try {
            // Basic file saving logic - in real app, use FileStorageService and return path
            String fileName = file.getOriginalFilename();
            // Use configured root + complaints/{id}
            Path uploadPath = Paths.get(rootUploadDir, "complaints", String.valueOf(complaintId));
            java.io.File dir = uploadPath.toFile();
            if (!dir.exists())
                dir.mkdirs();

            String filePath = uploadPath.resolve(System.currentTimeMillis() + "_" + fileName).toString();
            file.transferTo(new java.io.File(filePath));

            ComplaintAttachment attachment = new ComplaintAttachment();
            attachment.setComplaint(complaint);
            attachment.setFileName(fileName);
            attachment.setFileType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setFilePath(filePath);
            attachment.setUploadedBy(user);

            complaint.getAttachments().add(attachment);
            complaintRepository.save(complaint);

            return convertToDTO(complaint);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    private ComplaintDTO convertToDTO(Complaint entity) {
        ComplaintDTO dto = new ComplaintDTO();
        dto.setId(entity.getId());
        dto.setComplaintNo(entity.getComplaintNo());
        dto.setCitizenName(entity.getCitizenName());
        dto.setCitizenMobile(entity.getCitizenMobile());

        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
            dto.setDepartmentName(entity.getDepartment().getName());
        }

        if (entity.getComplaintType() != null) {
            dto.setComplaintTypeId(entity.getComplaintType().getId());
            dto.setComplaintTypeName(entity.getComplaintType().getNameEn()); // Default EN
        }

        dto.setSubComplaintType(entity.getSubComplaintType());

        dto.setDescription(entity.getDescription());
        dto.setPhotoUrl(entity.getPhotoUrl());
        dto.setLocation(entity.getLocation());
        dto.setStatus(entity.getStatus());
        dto.setRejectionReason(entity.getRejectionReason());
        dto.setCreatedAt(entity.getCreatedAt());

        if (entity.getRelatedTask() != null) {
            dto.setRelatedTaskId(entity.getRelatedTask().getId());
            dto.setRelatedTaskTitle(entity.getRelatedTask().getTitle());
        }

        if (entity.getComments() != null) {
            dto.setComments(entity.getComments().stream().map(c -> {
                ComplaintDTO.ComplaintCommentDTO cDto = new ComplaintDTO.ComplaintCommentDTO();
                cDto.setId(c.getId());
                cDto.setText(c.getText());
                cDto.setUserName(c.getUser() != null ? c.getUser().getName() : "System");
                cDto.setTimestamp(c.getTimestamp());
                return cDto;
            }).collect(Collectors.toList()));
        }

        if (entity.getAttachments() != null) {
            dto.setAttachments(entity.getAttachments().stream().map(a -> {
                ComplaintDTO.ComplaintAttachmentDTO aDto = new ComplaintDTO.ComplaintAttachmentDTO();
                aDto.setId(a.getId());
                aDto.setFileName(a.getFileName());
                aDto.setFilePath(a.getFilePath()); // Or a download URL
                aDto.setFileType(a.getFileType());
                aDto.setUploadedBy(a.getUploadedBy() != null ? a.getUploadedBy().getName() : "System");
                return aDto;
            }).collect(Collectors.toList()));
        }

        return dto;
    }

    public ResponseEntity<Resource> downloadAttachment(Long attachmentId, User user) {
        ComplaintAttachment attachment = complaintAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
        assertComplaintAccess(attachment.getComplaint(), user);

        try {
            Path path = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(attachment.getFileType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + attachment.getFileName() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Transactional
    public void addAttachmentFromUrl(Complaint complaint, String urlString) {
        try {
            // Generate filename with randomness to avoid overwrites in loops
            String fileName = "whatsapp_media_" + System.currentTimeMillis() + "_"
                    + (new java.util.Random().nextInt(10000)) + ".jpg";

            // Download or Copy Local
            String filePath;
            Path complaintDir = Paths.get(rootUploadDir, "complaints", String.valueOf(complaint.getId()));
            if (!java.nio.file.Files.exists(complaintDir)) {
                java.nio.file.Files.createDirectories(complaintDir);
            }

            // Check if it is a local path (starts with /uploads/)
            if (urlString.startsWith("/uploads/")) {
                // ... (handling logic is simplified as we just copy to new location)
                String sourcePathStr = urlString.startsWith("/") ? urlString.substring(1) : urlString;
                // Caution: sourcePath might be relative to old 'uploads'. PROD won't have this
                // issue usually.
                // We just want to copy valid source to new dest.
                java.nio.file.Path sourcePath = java.nio.file.Paths.get(sourcePathStr);

                filePath = complaintDir.resolve(fileName).toString();
                // Copy if source exists
                if (java.nio.file.Files.exists(sourcePath)) {
                    java.nio.file.Files.copy(sourcePath, java.nio.file.Paths.get(filePath),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                // Remote URL
                java.net.URL url = new java.net.URL(urlString);
                try (java.io.InputStream in = url.openStream()) {
                    filePath = complaintDir.resolve(fileName).toString();
                    java.nio.file.Files.copy(in, java.nio.file.Paths.get(filePath),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // Create Entity
            ComplaintAttachment attachment = new ComplaintAttachment();
            attachment.setComplaint(complaint);
            attachment.setFileName(fileName);
            attachment.setFilePath(filePath);
            attachment.setFileType("image/jpeg"); // Assumption for now
            attachment.setFileType("image/jpeg"); // Assumption for now

            // Fix: uploadedBy cannot be null as per Entity constraint
            // Fix: uploadedBy cannot be null. Try ADMIN, then OWNER, then ANY.
            User systemUser = userRepository.findByRole(com.nagar.parishad.backend.enums.Role.ADMIN)
                    .stream().findFirst().orElse(null);

            if (systemUser == null) {
                systemUser = userRepository.findByRole(com.nagar.parishad.backend.enums.Role.OWNER)
                        .stream().findFirst().orElse(null);
            }

            if (systemUser == null) {
                systemUser = userRepository.findAll().stream().findFirst().orElse(null);
            }

            if (systemUser == null) {
                System.err.println(
                        "WARNING: No user found to assign as uploadedBy for attachment. Attachment save will fail.");
                // We cannot precede without user.
                throw new RuntimeException("No user found for attachment attribution");
            }
            attachment.setUploadedBy(systemUser);

            complaint.getAttachments().add(attachment);

            complaintAttachmentRepository.saveAndFlush(attachment);

        } catch (Exception e) {
            System.err.println("Failed to download media from URL: " + urlString + " Error: " + e.getMessage());
            // Do not rethrow, just log and fail gracefully so the Complaint is preserved.
        }
    }
}
