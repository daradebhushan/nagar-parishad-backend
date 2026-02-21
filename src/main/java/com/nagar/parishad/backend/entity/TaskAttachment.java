package com.nagar.parishad.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "task_attachments")
public class TaskAttachment {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "task_id", nullable = false)
        @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "attachments", "comments", "hibernateLazyInitializer",
                        "handler" })
        private Task task;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "comment_id")
        @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "attachments", "task", "user",
                        "hibernateLazyInitializer",
                        "handler" })
        private TaskComment comment;

        @Column(name = "file_name", nullable = false)
        private String fileName;

        @Column(name = "file_type")
        private String fileType;

        @Column(name = "file_path", nullable = false)
        private String filePath;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "uploaded_by", nullable = false)
        @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "department",
                        "roles" })
        private User uploadedBy;

        @Column(name = "uploaded_on", updatable = false)
        private LocalDateTime uploadedOn;

        @Column(name = "file_size")
        private Long fileSize;

        @PrePersist
        protected void onCreate() {
                uploadedOn = LocalDateTime.now();
        }

        // Manual Getters and Setters
        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public String getFileName() {
                return fileName;
        }

        public void setFileName(String fileName) {
                this.fileName = fileName;
        }

        public String getFilePath() {
                return filePath;
        }

        public void setFilePath(String filePath) {
                this.filePath = filePath;
        }

        public String getFileType() {
                return fileType;
        }

        public void setFileType(String fileType) {
                this.fileType = fileType;
        }

        public Task getTask() {
                return task;
        }

        public void setTask(Task task) {
                this.task = task;
        }

        public TaskComment getComment() {
                return comment;
        }

        public void setComment(TaskComment comment) {
                this.comment = comment;
        }

        public User getUploadedBy() {
                return uploadedBy;
        }

        public void setUploadedBy(User uploadedBy) {
                this.uploadedBy = uploadedBy;
        }

        public LocalDateTime getUploadedOn() {
                return uploadedOn;
        }

        public void setUploadedOn(LocalDateTime uploadedOn) {
                this.uploadedOn = uploadedOn;
        }

        public Long getFileSize() {
                return fileSize;
        }

        public void setFileSize(Long fileSize) {
                this.fileSize = fileSize;
        }
}
