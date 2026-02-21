package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.dto.TaskRequest;
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.entity.Task;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.Role;
import com.nagar.parishad.backend.enums.TaskPriority;
import com.nagar.parishad.backend.enums.TaskStatus;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import com.nagar.parishad.backend.repository.TaskRepository;
import com.nagar.parishad.backend.repository.UserRepository;
import com.nagar.parishad.backend.repository.ComplaintRepository;
import com.nagar.parishad.backend.entity.Complaint;
import com.nagar.parishad.backend.enums.ComplaintStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    NotificationService notificationService;

    @Autowired
    com.nagar.parishad.backend.repository.TaskHistoryRepository taskHistoryRepository;

    @Autowired
    EmailService emailService;

    @Autowired
    EmailTemplateService emailTemplateService;

    @Autowired
    ComplaintRepository complaintRepository;

    @Autowired
    com.nagar.parishad.backend.repository.TaskAttachmentRepository taskAttachmentRepository;

    public Task createTask(TaskRequest request, User admin) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus());
        task.setType(request.getType());
        task.setDueDate(request.getDueDate());
        task.setAdmin(admin);

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            task.setDepartment(department);
        }

        if (request.getAssignedStaffId() != null) {
            User staff = userRepository.findById(request.getAssignedStaffId())
                    .orElseThrow(() -> new RuntimeException("Staff not found"));
            task.setAssignedStaff(staff);
        }

        Task savedTask = taskRepository.save(task);
        System.out.println("DEBUG: SAVED TASK DESC: '" + savedTask.getDescription() + "'");
        System.out.println("DEBUG: SAVED TASK ID: " + savedTask.getId());

        if (request.getComplaintId() != null) {
            Complaint complaint = complaintRepository.findById(request.getComplaintId()).orElse(null);
            complaint.setRelatedTask(savedTask);
            complaint.setStatus(ComplaintStatus.CONVERTED_TO_TASK);
            complaintRepository.save(complaint);

            // Auto-attach Complaint files to Task
            if (complaint.getAttachments() != null && !complaint.getAttachments().isEmpty()) {
                for (com.nagar.parishad.backend.entity.ComplaintAttachment ca : complaint.getAttachments()) {
                    com.nagar.parishad.backend.entity.TaskAttachment ta = new com.nagar.parishad.backend.entity.TaskAttachment();
                    ta.setTask(savedTask);
                    ta.setFileName(ca.getFileName());
                    ta.setFileType(ca.getFileType());
                    ta.setFilePath(ca.getFilePath());
                    ta.setFileSize(ca.getFileSize());
                    ta.setUploadedBy(ca.getUploadedBy()); // Keep original uploader or set to admin? Original is better
                                                          // for trace.

                    taskAttachmentRepository.save(ta);
                }
            }
        }

        return savedTask;
    }

    public void sendTaskCreatedNotification(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task != null) {
            notificationService.sendTaskAssignmentNotification(task);
        }
    }

    public Page<Task> getTasks(User user, TaskStatus status, TaskPriority priority, Long departmentId, Long staffId,
            String search, String type,
            Pageable pageable) {
        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Role based base filters
            if (user.getRole() == Role.ADMIN) {
                // Admin sees all tasks within their own scope (Tenant Isolation)
                predicates.add(cb.equal(root.get("admin").get("id"), user.getId()));
            } else if (user.getRole() == Role.DEPARTMENT_HEAD) {
                if (user.getDepartment() != null) {
                    predicates.add(cb.equal(root.get("department").get("id"), user.getDepartment().getId()));
                } else {
                    // Fallback: If Dept Head has no department, they see nothing (or only
                    // assigned?)
                    // For safety, force empty/false. But logic typically assumes they have one.
                    // Or check for assignedStaff == user OR department == null (impossible for
                    // valid logic)
                    // Let's stick to restricting to Department.
                    predicates.add(cb.disjunction()); // Always false
                }
            } else if (user.getRole() == Role.STAFF) {
                predicates.add(cb.equal(root.get("assignedStaff").get("id"), user.getId()));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (staffId != null) {
                predicates.add(cb.equal(root.get("assignedStaff").get("id"), staffId));
            }

            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate titlePredicate = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate descPredicate = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(titlePredicate, descPredicate));
            }

            if (type != null && !type.isEmpty()) {
                if (type.equals("Internal")) {
                    // "Internal" (Non-Complaint Based) means everything except "Complaint"
                    predicates.add(cb.notEqual(root.get("type"), "Complaint"));
                } else {
                    predicates.add(cb.equal(root.get("type"), type));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return taskRepository.findAll(spec, pageable);
    }

    public Task updateTask(Long taskId, TaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Capture Old State
        Task oldTask = new Task();
        oldTask.setTitle(task.getTitle());
        oldTask.setDescription(task.getDescription());
        oldTask.setPriority(task.getPriority());
        oldTask.setStatus(task.getStatus());
        oldTask.setDueDate(task.getDueDate());
        oldTask.setDepartment(task.getDepartment());
        oldTask.setAssignedStaff(task.getAssignedStaff());

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus());
        task.setDueDate(request.getDueDate());

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            task.setDepartment(department);
        }

        if (request.getAssignedStaffId() != null) {
            User staff = userRepository.findById(request.getAssignedStaffId())
                    .orElseThrow(() -> new RuntimeException("Staff not found"));
            task.setAssignedStaff(staff);
        }

        Task updatedTask = taskRepository.save(task);

        // Send Detailed Email Notification
        try {
            String subject = "Task Details Updated: " + updatedTask.getTitle();
            String body = emailTemplateService.getTaskUpdateEmail(oldTask, updatedTask);

            // Notify Assigned Staff
            if (updatedTask.getAssignedStaff() != null) {
                emailService.sendEmail(updatedTask.getAssignedStaff().getEmail(), subject, body);

                // Also create internal notification
                notificationService.createNotification(
                        updatedTask.getAssignedStaff(),
                        updatedTask.getAdmin(),
                        "Task updated: " + updatedTask.getTitle(),
                        com.nagar.parishad.backend.enums.NotificationType.STATUS_CHANGED,
                        updatedTask.getId());
            }

            // Send Copy to System Owner
            try {
                User owner = userRepository.findByRole(Role.OWNER).stream().findFirst().orElse(null);
                if (owner != null) {
                    if (updatedTask.getAssignedStaff() == null
                            || !owner.getId().equals(updatedTask.getAssignedStaff().getId())) {
                        emailService.sendEmail(owner.getEmail(), "COPY: " + subject, body);
                    }
                }

                // Send Copy to Admin (Chief Officer) if distinct from Owner
                User admin = userRepository.findByRole(Role.ADMIN).stream().findFirst().orElse(null);
                if (admin != null) {
                    boolean isOwner = owner != null && admin.getId().equals(owner.getId());
                    boolean isStaff = updatedTask.getAssignedStaff() != null
                            && admin.getId().equals(updatedTask.getAssignedStaff().getId());

                    if (!isOwner && !isStaff) {
                        emailService.sendEmail(admin.getEmail(), "COPY: " + subject, body);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Failed to send task update copy to owner/admin: " + ex.getMessage());
            }

            // Notify Admin if they didn't do it? (Assume context unclear, safe to notify
            // admin too if staff updated, but usually Auth checks role)
            // For now just Staff.
        } catch (Exception e) {
            System.err.println("Failed to send task update email: " + e.getMessage());
        }

        return updatedTask;
    }

    public Task updateTaskStatus(Long taskId, TaskStatus status, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Capture old status
        String oldStatus = task.getStatus().toString();

        // Update status
        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);

        // Record History
        com.nagar.parishad.backend.entity.TaskHistory history = new com.nagar.parishad.backend.entity.TaskHistory();
        history.setTask(updatedTask);
        history.setModifiedBy(user);
        history.setAction("STATUS_CHANGE");
        history.setOldValue(oldStatus);
        history.setNewValue(status.toString());
        taskHistoryRepository.save(history);

        // Notify All Stakeholders
        notificationService.sendStatusUpdateNotification(updatedTask, oldStatus, status.toString(), user);

        return updatedTask;
    }

    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task != null) {
            // Notify Assigned Staff before deletion (since ID might reference null after?
            // No, ID persists until transaction commit)
            // However, relatedTaskId in notification might be orphan if task deleted.
            // Usually we keep task or soft delete. If hard delete, email might fail to
            // fetch details.
            // We will try sending notification before delete.
            if (task.getAssignedStaff() != null) {
                notificationService.createNotification(
                        task.getAssignedStaff(),
                        task.getAdmin(),
                        "Task deleted: " + task.getTitle(),
                        com.nagar.parishad.backend.enums.NotificationType.STATUS_CHANGED, // Generic
                        null); // ID null because it will be gone
            }

            // Notify Admin/Chief Officer
            User admin = userRepository.findByRole(Role.ADMIN).stream().findFirst().orElse(null);
            if (admin != null
                    && (task.getAssignedStaff() == null || !admin.getId().equals(task.getAssignedStaff().getId()))) {
                notificationService.createNotification(
                        admin,
                        task.getAdmin(), // Sender (Task Creator or System)
                        "Task deleted: " + task.getTitle(),
                        com.nagar.parishad.backend.enums.NotificationType.STATUS_CHANGED,
                        null);
            }
        }

        if (!taskRepository.existsById(taskId)) {
            throw new RuntimeException("Task not found");
        }

        // Delete Task History manually (No cascade on entity)
        List<com.nagar.parishad.backend.entity.TaskHistory> historyList = taskHistoryRepository
                .findByTaskIdOrderByTimestampDesc(taskId);
        if (historyList != null && !historyList.isEmpty()) {
            taskHistoryRepository.deleteAll(historyList);
        }

        taskRepository.deleteById(taskId);
    }

    public Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }
}
