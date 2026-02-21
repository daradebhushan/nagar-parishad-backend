package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.entity.TaskAttachment;
import com.nagar.parishad.backend.repository.TaskAttachmentRepository;
import com.nagar.parishad.backend.repository.TaskRepository;
import com.nagar.parishad.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/debug")
public class AttachmentDebugController {

    @Autowired
    TaskAttachmentRepository attachmentRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    FileStorageService fileStorageService;

    @GetMapping("/attachments/{taskId}")
    public List<TaskAttachment> debugGetAttachments(@PathVariable Long taskId) {
        System.out.println("DEBUG: Fetching attachments for task " + taskId);
        List<TaskAttachment> attachments = attachmentRepository.findByTaskId(taskId);
        System.out.println("DEBUG: Found " + attachments.size() + " attachments");
        return attachments;
    }
}
