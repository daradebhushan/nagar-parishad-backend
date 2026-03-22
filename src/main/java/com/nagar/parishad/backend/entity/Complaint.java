package com.nagar.parishad.backend.entity;

import com.nagar.parishad.backend.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_no", unique = true, nullable = false)
    private String complaintNo;

    @Column(name = "citizen_name")
    private String citizenName;

    @Column(name = "citizen_mobile")
    private String citizenMobile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_type_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private ComplaintType complaintType;

    @Column(name = "sub_complaint_type")
    private String subComplaintType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(columnDefinition = "TEXT")
    private String location; // Text description or standard GPS string

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToOne
    @JoinColumn(name = "related_task_id")
    private Task relatedTask;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("complaint")
    private java.util.List<ComplaintComment> comments = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("complaint")
    private java.util.List<ComplaintAttachment> attachments = new java.util.ArrayList<>();

    // Getters and Setters for lists
    public java.util.List<ComplaintComment> getComments() {
        return comments;
    }

    public void setComments(java.util.List<ComplaintComment> comments) {
        this.comments = comments;
    }

    public java.util.List<ComplaintAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(java.util.List<ComplaintAttachment> attachments) {
        this.attachments = attachments;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ComplaintStatus.PENDING;
        }
        if (complaintNo == null) {
            // CMP + Timestamp + Random 3 digits
            complaintNo = "CMP" + System.currentTimeMillis() + (int) (Math.random() * 1000);
        }
    }
}
