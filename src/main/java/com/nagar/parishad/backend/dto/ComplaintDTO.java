package com.nagar.parishad.backend.dto;

import com.nagar.parishad.backend.enums.ComplaintStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ComplaintDTO {
    private Long id;
    private String complaintNo;
    private String citizenName;
    private String citizenMobile;
    private Long departmentId;
    private String departmentName;
    private Long complaintTypeId;
    private String complaintTypeName;
    private String subComplaintType;
    private String description;
    private String photoUrl;
    private String location;
    private ComplaintStatus status;
    private String rejectionReason;
    private Long relatedTaskId;
    private String relatedTaskTitle;
    private LocalDateTime createdAt;
    private java.util.List<ComplaintCommentDTO> comments;
    private java.util.List<ComplaintAttachmentDTO> attachments;

    @Data
    public static class ComplaintCommentDTO {
        private Long id;
        private String text;
        private String userName;
        private LocalDateTime timestamp;
    }

    @Data
    public static class ComplaintAttachmentDTO {
        private Long id;
        private String fileName;
        private String filePath;
        private String fileType;
        private String uploadedBy;
    }
}
