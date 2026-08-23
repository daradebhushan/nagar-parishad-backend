package com.nagar.parishad.backend.dto;

import lombok.Data;

@Data
public class ComplaintSubmissionDTO {
    private String name;
    private String mobile;
    private String email;
    private Long departmentId;
    private Long complaintTypeId;
    private String description;
    private String photoUrl;
    private java.util.List<String> attachmentUrls;
    private String location;
}
