package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ApiResponse;
import com.nagar.parishad.backend.dto.CommentRequest;
import com.nagar.parishad.backend.dto.TaskRequest;
import com.nagar.parishad.backend.dto.TaskDTO;
import com.nagar.parishad.backend.entity.Task;
import com.nagar.parishad.backend.entity.TaskAttachment;
import com.nagar.parishad.backend.entity.TaskComment;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.TaskPriority;
import com.nagar.parishad.backend.enums.TaskStatus;
import com.nagar.parishad.backend.service.CommentService;
import com.nagar.parishad.backend.service.FileStorageService;
import com.nagar.parishad.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {

    @Autowired
    TaskService taskService;

    @Autowired
    CommentService commentService;

    @Autowired
    FileStorageService fileStorageService;

    // Create Task (ADMIN, DEPT_HEAD?) - Request says "AUTH /admin/login", implies
    // Admin context mainly.
    // "API Requirements: Create Department... Task Model (Like Jira)..."
    // Usually Admin or Dept Head creates tasks?
    // Let's assumie ADMIN and DEPT_HEAD can create tasks.
    @PostMapping("/tasks/create")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'DEPARTMENT_HEAD')")
    public ResponseEntity<ApiResponse<TaskDTO>> createTask(@Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User user) {
        Task task = taskService.createTask(request, user);
        return ResponseEntity.ok(ApiResponse.success("Task created successfully", convertToDTO(task)));
    }

    @PostMapping("/tasks/{taskId}/notify")
    public ResponseEntity<ApiResponse<Void>> notifyTaskCreated(@PathVariable Long taskId, @AuthenticationPrincipal User user) {
        taskService.sendTaskCreatedNotification(taskId, user);
        return ResponseEntity.ok(ApiResponse.success("Notification sent successfully", null));
    }

    // Filter Tasks (Use query params for filtering)
    // ADMIN: /admin/tasks?status=...
    // STAFF: /staff/tasks
    // This endpoint `/api/tasks` can be smart.
    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<Page<TaskDTO>>> filterTasks(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String assignedByRole,
            Pageable pageable) {

        System.out.println(
                "FilterTasks: Status=" + status + ", Priority=" + priority + ", Search=" + search + ", Type=" + type + ", AssignedByRole=" + assignedByRole);
        Page<Task> tasks = taskService.getTasks(user, status, priority, departmentId, staffId, search, type, assignedByRole, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tasks fetched successfully", tasks.map(this::convertToDTO)));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskDTO>> getTask(@PathVariable Long taskId) {
        Task task = taskService.getTaskById(taskId);
        return ResponseEntity.ok(ApiResponse.success("Task fetched successfully", convertToDTO(task)));
    }

    @PutMapping("/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'DEPARTMENT_HEAD')") // Restricted to Admin/Dept Head
    public ResponseEntity<ApiResponse<TaskDTO>> updateTask(@PathVariable Long taskId,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User user) {
        Task task = taskService.updateTask(taskId, request, user);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", convertToDTO(task)));
    }

    @PatchMapping("/tasks/{taskId}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'DEPARTMENT_HEAD', 'STAFF')")
    public ResponseEntity<ApiResponse<TaskDTO>> updateTaskStatus(@PathVariable Long taskId,
            @Valid @RequestBody com.nagar.parishad.backend.dto.TaskStatusUpdateRequest request,
            @AuthenticationPrincipal User user) {
        TaskStatus status = TaskStatus.valueOf(request.getStatus());
        System.out.println("DEBUG: Controller - Received Status Update Request for Task " + taskId + " to " + status);
        Task task = taskService.updateTaskStatus(taskId, status, user);
        return ResponseEntity.ok(ApiResponse.success("Task status updated successfully", convertToDTO(task)));
    }

    @DeleteMapping("/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'DEPARTMENT_HEAD')")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long taskId, @AuthenticationPrincipal User user) {
        taskService.deleteTask(taskId, user);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }

    // Comments
    @PostMapping("/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<TaskComment>> addComment(@PathVariable Long taskId,
            @Valid @RequestBody CommentRequest request, @AuthenticationPrincipal User user) {
        TaskComment comment = commentService.addComment(taskId, request.getText(), user);
        return ResponseEntity.ok(ApiResponse.success("Comment added", comment));
    }

    @GetMapping("/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<List<TaskComment>>> getComments(@PathVariable Long taskId) {
        List<TaskComment> comments = commentService.getComments(taskId);
        return ResponseEntity.ok(ApiResponse.success("Comments fetched", comments));
    }

    @PutMapping("/tasks/{taskId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<TaskComment>> updateComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User user) {
        TaskComment comment = commentService.updateComment(commentId, request.getText(), user);
        return ResponseEntity.ok(ApiResponse.success("Comment updated", comment));
    }

    @DeleteMapping("/tasks/{taskId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal User user) {
        commentService.deleteComment(commentId, user);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted", null));
    }

    // Attachments
    @PostMapping("/tasks/{taskId}/attachments")
    public ResponseEntity<ApiResponse<TaskAttachment>> uploadAttachment(@PathVariable Long taskId,
            @RequestParam("file") MultipartFile file, @AuthenticationPrincipal User user) {
        TaskAttachment attachment = fileStorageService.storeFile(taskId, file, user);
        return ResponseEntity.ok(ApiResponse.success("File uploaded", attachment));
    }

    @PostMapping("/tasks/{taskId}/attachments/batch")
    public ResponseEntity<ApiResponse<List<TaskAttachment>>> uploadAttachments(@PathVariable Long taskId,
            @RequestParam("files") List<MultipartFile> files, @AuthenticationPrincipal User user) {
        List<TaskAttachment> attachments = fileStorageService.storeFiles(taskId, files, user);
        return ResponseEntity.ok(ApiResponse.success("Files uploaded", attachments));
    }

    @PostMapping("/tasks/{taskId}/comments/{commentId}/attachments")
    public ResponseEntity<ApiResponse<TaskAttachment>> uploadCommentAttachment(@PathVariable Long taskId,
            @PathVariable Long commentId,
            @RequestParam("file") MultipartFile file, @AuthenticationPrincipal User user) {
        TaskAttachment attachment = fileStorageService.storeCommentAttachment(taskId, commentId, file, user);
        return ResponseEntity.ok(ApiResponse.success("Comment file uploaded", attachment));
    }

    @PostMapping("/tasks/{taskId}/comments/{commentId}/attachments/batch")
    public ResponseEntity<ApiResponse<List<TaskAttachment>>> uploadCommentAttachments(@PathVariable Long taskId,
            @PathVariable Long commentId,
            @RequestParam("files") List<MultipartFile> files, @AuthenticationPrincipal User user) {
        List<TaskAttachment> attachments = fileStorageService.storeCommentFiles(taskId, commentId, files, user);
        return ResponseEntity.ok(ApiResponse.success("Comment files uploaded", attachments));
    }

    @GetMapping("/tasks/{taskId}/attachments")
    public ResponseEntity<ApiResponse<List<TaskAttachment>>> getAttachments(@PathVariable Long taskId) {
        List<TaskAttachment> attachments = fileStorageService.getAttachments(taskId);
        return ResponseEntity.ok(ApiResponse.success("Attachments fetched", attachments));
    }

    @GetMapping("/tasks/attachments/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        TaskAttachment attachment = fileStorageService.getAttachment(id);
        Path path = Paths.get(attachment.getFilePath());
        Resource resource;
        try {
            resource = new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("File not found", e);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/tasks/attachments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        fileStorageService.deleteAttachment(id, user);
        return ResponseEntity.ok(ApiResponse.success("Attachment deleted successfully", null));
    }

    private TaskDTO convertToDTO(Task entity) {
        TaskDTO dto = new TaskDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setPriority(entity.getPriority());
        dto.setStatus(entity.getStatus());
        dto.setType(entity.getType());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setDueDate(entity.getDueDate());

        if (entity.getDepartment() != null) {
            dto.setDepartment(new TaskDTO.IdNameDTO(entity.getDepartment().getId(), entity.getDepartment().getName()));
        }

        if (entity.getAssignedStaff() != null) {
            dto.setAssignedStaff(new TaskDTO.IdNameDTO(entity.getAssignedStaff().getId(), entity.getAssignedStaff().getName()));
        }

        if (entity.getRelatedComplaint() != null) {
            dto.setRelatedComplaintId(entity.getRelatedComplaint().getId());
            dto.setRelatedComplaintNo(entity.getRelatedComplaint().getComplaintNo());
        }

        return dto;
    }
}
