package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.entity.Task;
import com.nagar.parishad.backend.entity.TaskAttachment;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.repository.TaskAttachmentRepository;
import com.nagar.parishad.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Autowired
    TaskAttachmentRepository attachmentRepository;

    @Autowired
    com.nagar.parishad.backend.repository.TaskCommentRepository commentRepository;

    @Autowired
    TaskRepository taskRepository;

    public TaskAttachment storeFile(Long taskId, MultipartFile file, User user) {
        return storeAttachment(taskId, null, file, user);
    }

    public TaskAttachment storeCommentAttachment(Long taskId, Long commentId, MultipartFile file, User user) {
        return storeAttachment(taskId, commentId, file, user);
    }

    public List<TaskAttachment> storeFiles(Long taskId, List<MultipartFile> files, User user) {
        return files.stream().map(file -> storeFile(taskId, file, user)).collect(java.util.stream.Collectors.toList());
    }

    public List<TaskAttachment> storeCommentFiles(Long taskId, Long commentId, List<MultipartFile> files, User user) {
        return files.stream().map(file -> storeCommentAttachment(taskId, commentId, file, user))
                .collect(java.util.stream.Collectors.toList());
    }

    private TaskAttachment storeAttachment(Long taskId, Long commentId, MultipartFile file, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        com.nagar.parishad.backend.entity.TaskComment comment = null;
        if (commentId != null) {
            comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found"));

            // Optional: Check if comment actually belongs to the task
            if (!comment.getTask().getId().equals(taskId)) {
                throw new RuntimeException("Comment does not belong to the specified task");
            }
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

        // Create tasks/{taskId} subdirectory logic
        Path uploadPath = Paths.get(uploadDir, "tasks", String.valueOf(taskId));

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            TaskAttachment attachment = new TaskAttachment();
            attachment.setTask(task);
            attachment.setFileName(originalFileName);
            attachment.setFileType(file.getContentType());
            attachment.setFilePath(targetLocation.toAbsolutePath().toString());
            attachment.setFileSize(file.getSize());
            attachment.setUploadedBy(user);

            if (comment != null) {
                attachment.setComment(comment);
            }

            return attachmentRepository.save(attachment);

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public List<TaskAttachment> getAttachments(Long taskId) {
        return attachmentRepository.findByTaskId(taskId);
    }

    public TaskAttachment getAttachment(Long id) {
        return attachmentRepository.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
    }

    public void deleteAttachment(Long id, User user) {
        TaskAttachment attachment = getAttachment(id);

        // Permission check: Allow if user is ADMIN, DEPT_HEAD, or the uploader
        com.nagar.parishad.backend.enums.Role role = user.getRole();
        boolean isAuthorized = role == com.nagar.parishad.backend.enums.Role.ADMIN ||
                role == com.nagar.parishad.backend.enums.Role.DEPARTMENT_HEAD ||
                (attachment.getUploadedBy() != null && attachment.getUploadedBy().getId().equals(user.getId()));

        if (!isAuthorized) {
            throw new RuntimeException("Unauthorized to delete this attachment");
        }

        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Files.deleteIfExists(filePath);
            attachmentRepository.delete(attachment);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file from storage", e);
        }
    }

    public String storeProfilePhoto(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileName = "profile_" + UUID.randomUUID().toString() + "_" + originalFileName;

        Path targetLocation = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(fileName);

        try {
            Files.createDirectories(targetLocation.getParent());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            // Return relative path or filename
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }
}
