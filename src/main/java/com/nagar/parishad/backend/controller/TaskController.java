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
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.repository.UserRepository;
import com.nagar.parishad.backend.repository.DepartmentRepository;
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
    // Only chief-officer/admin scope can create and assign tasks.
    @PostMapping({"/tasks/create", "/tasks"})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'NAGARADHYAKSHA')")
    public ResponseEntity<ApiResponse<TaskDTO>> createTask(@Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User user) {
        if (!taskService.isTaskManager(user)) {
            throw new org.springframework.security.access.AccessDeniedException("Only chief officer or authorized role can create tasks");
        }
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
    public ResponseEntity<ApiResponse<TaskDTO>> getTask(@PathVariable Long taskId,
            @AuthenticationPrincipal User user) {
        Task task = taskService.getTaskById(taskId, user);
        return ResponseEntity.ok(ApiResponse.success("Task fetched successfully", convertToDTO(task)));
    }

    @PutMapping("/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'NAGARADHYAKSHA')")
    public ResponseEntity<ApiResponse<TaskDTO>> updateTask(@PathVariable Long taskId,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User user) {
        Task task = taskService.updateTask(taskId, request, user);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", convertToDTO(task)));
    }

    @RequestMapping(value = "/tasks/{taskId}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'NAGARADHYAKSHA', 'DEPARTMENT_HEAD', 'STAFF')")
    public ResponseEntity<ApiResponse<TaskDTO>> updateTaskStatus(@PathVariable Long taskId,
            @Valid @RequestBody com.nagar.parishad.backend.dto.TaskStatusUpdateRequest request,
            @AuthenticationPrincipal User user) {
        TaskStatus status = TaskStatus.valueOf(request.getStatus());
        System.out.println("DEBUG: Controller - Received Status Update Request for Task " + taskId + " to " + status);
        Task task = taskService.updateTaskStatus(taskId, status, user);
        return ResponseEntity.ok(ApiResponse.success("Task status updated successfully", convertToDTO(task)));
    }

    @DeleteMapping("/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long taskId, @AuthenticationPrincipal User user) {
        taskService.deleteTask(taskId, user);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }

    // Comments
    @PostMapping("/tasks/{taskId}/comments")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'NAGARADHYAKSHA', 'DEPARTMENT_HEAD', 'STAFF')")
    public ResponseEntity<ApiResponse<TaskComment>> addComment(@PathVariable Long taskId,
            @Valid @RequestBody CommentRequest request, @AuthenticationPrincipal User user) {
        TaskComment comment = commentService.addComment(taskId, request.getText(), user);
        return ResponseEntity.ok(ApiResponse.success("Comment added", comment));
    }

    @GetMapping("/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<List<TaskComment>>> getComments(@PathVariable Long taskId,
            @AuthenticationPrincipal User user) {
        List<TaskComment> comments = commentService.getComments(taskId, user);
        return ResponseEntity.ok(ApiResponse.success("Comments fetched", comments));
    }

    @PutMapping("/tasks/{taskId}/comments/{commentId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<TaskComment>> updateComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User user) {
        TaskComment comment = commentService.updateComment(commentId, request.getText(), user);
        return ResponseEntity.ok(ApiResponse.success("Comment updated", comment));
    }

    @DeleteMapping("/tasks/{taskId}/comments/{commentId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal User user) {
        commentService.deleteComment(commentId, user);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted", null));
    }

    // Attachments
    @PostMapping("/tasks/{taskId}/attachments")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<TaskAttachment>> uploadAttachment(@PathVariable Long taskId,
            @RequestParam("file") MultipartFile file, @AuthenticationPrincipal User user) {
        TaskAttachment attachment = fileStorageService.storeFile(taskId, file, user);
        return ResponseEntity.ok(ApiResponse.success("File uploaded", attachment));
    }

    @PostMapping("/tasks/{taskId}/attachments/batch")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TaskAttachment>>> uploadAttachments(@PathVariable Long taskId,
            @RequestParam("files") List<MultipartFile> files, @AuthenticationPrincipal User user) {
        List<TaskAttachment> attachments = fileStorageService.storeFiles(taskId, files, user);
        return ResponseEntity.ok(ApiResponse.success("Files uploaded", attachments));
    }

    @PostMapping("/tasks/{taskId}/comments/{commentId}/attachments")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<TaskAttachment>> uploadCommentAttachment(@PathVariable Long taskId,
            @PathVariable Long commentId,
            @RequestParam("file") MultipartFile file, @AuthenticationPrincipal User user) {
        TaskAttachment attachment = fileStorageService.storeCommentAttachment(taskId, commentId, file, user);
        return ResponseEntity.ok(ApiResponse.success("Comment file uploaded", attachment));
    }

    @PostMapping("/tasks/{taskId}/comments/{commentId}/attachments/batch")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TaskAttachment>>> uploadCommentAttachments(@PathVariable Long taskId,
            @PathVariable Long commentId,
            @RequestParam("files") List<MultipartFile> files, @AuthenticationPrincipal User user) {
        List<TaskAttachment> attachments = fileStorageService.storeCommentFiles(taskId, commentId, files, user);
        return ResponseEntity.ok(ApiResponse.success("Comment files uploaded", attachments));
    }

    @GetMapping("/tasks/{taskId}/attachments")
    public ResponseEntity<ApiResponse<List<TaskAttachment>>> getAttachments(@PathVariable Long taskId,
            @AuthenticationPrincipal User user) {
        List<TaskAttachment> attachments = fileStorageService.getAttachments(taskId, user);
        return ResponseEntity.ok(ApiResponse.success("Attachments fetched", attachments));
    }

    @GetMapping("/tasks/attachments/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        TaskAttachment attachment = fileStorageService.getAttachment(id, user);
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
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        fileStorageService.deleteAttachment(id, user);
        return ResponseEntity.ok(ApiResponse.success("Attachment deleted successfully", null));
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

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
            String deptName = "";
            try {
                deptName = entity.getDepartment().getName();
            } catch (Exception e) {
                deptName = departmentRepository.findById(entity.getDepartment().getId()).map(Department::getName).orElse("");
            }
            dto.setDepartment(new TaskDTO.IdNameDTO(entity.getDepartment().getId(), deptName));
        }

        if (entity.getAssignedStaff() != null) {
            String staffName = "";
            try {
                staffName = entity.getAssignedStaff().getName();
            } catch (Exception e) {
                staffName = userRepository.findById(entity.getAssignedStaff().getId()).map(User::getName).orElse("");
            }
            dto.setAssignedStaff(new TaskDTO.IdNameDTO(entity.getAssignedStaff().getId(), staffName));
        }

        if (entity.getRelatedComplaint() != null) {
            dto.setRelatedComplaintId(entity.getRelatedComplaint().getId());
            try {
                dto.setRelatedComplaintNo(entity.getRelatedComplaint().getComplaintNo());
            } catch (Exception e) {}
        }

        return dto;
    }
}
