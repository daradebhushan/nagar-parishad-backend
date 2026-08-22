package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.entity.Notification;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.NotificationType;
import com.nagar.parishad.backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private com.nagar.parishad.backend.util.JwtUtils jwtUtils;

    @Autowired
    private com.nagar.parishad.backend.repository.TaskRepository taskRepository;

    @Autowired
    private com.nagar.parishad.backend.repository.TaskAttachmentRepository taskAttachmentRepository;

    @Autowired
    private com.nagar.parishad.backend.repository.TaskCommentRepository taskCommentRepository;

    @Autowired
    private com.nagar.parishad.backend.repository.UserRepository userRepository;

    @Autowired
    private com.nagar.parishad.backend.repository.DepartmentRepository departmentRepository;

    @Autowired
    private com.nagar.parishad.backend.repository.ComplaintRepository complaintRepository;

    @org.springframework.beans.factory.annotation.Value("${app.file.upload-dir:uploads}")
    private String uploadDirConfig;

    @org.springframework.transaction.annotation.Transactional
    public void createNotification(User recipient, User sender, String message, NotificationType type,
            Long relatedTaskId) {
        createNotification(recipient, sender, message, type, relatedTaskId, null);
    }

    @org.springframework.transaction.annotation.Transactional
    public void createNotification(User recipient, User sender, String message, NotificationType type,
            Long relatedTaskId, Long commentId) {
        createNotification(recipient, sender, message, type, relatedTaskId, commentId, true);
    }

    @org.springframework.transaction.annotation.Transactional
    public void createNotification(User recipient, User sender, String message, NotificationType type,
            Long relatedTaskId, Long commentId, boolean sendEmail) {
        if (recipient.getId().equals(sender.getId())) {
            return; // Don't notify self
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setSender(sender);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRelatedTaskId(relatedTaskId);

        notificationRepository.save(notification);

        // Send Email Notification only if requested and allowed by user preference
        if (sendEmail && recipient.isEmailNotifications()) {
            try {
                String emailSubject = "New Notification: " + type;
                String emailBody;
                java.util.List<java.io.File> filesToAttach = new java.util.ArrayList<>();

                String magicToken = jwtUtils.generateMagicLoginToken(recipient);

                if (type == NotificationType.TASK_ASSIGNED && relatedTaskId != null) {
                    com.nagar.parishad.backend.entity.Task task = taskRepository.findById(relatedTaskId).orElse(null);
                    if (task != null) {
                        emailSubject = "Task Assignment: " + task.getTitle();

                        // Distinguish between Assigned Staff and Others (Dept Head/Admin)
                        if (task.getAssignedStaff() != null
                                && task.getAssignedStaff().getId().equals(recipient.getId())) {
                            emailBody = emailTemplateService.getTaskAssignmentEmail(task, magicToken);
                        } else {
                            emailBody = emailTemplateService.getTaskNotificationForSupervisorEmail(task, recipient,
                                    magicToken);
                        }

                        if (task.getAttachments() != null) {
                            for (com.nagar.parishad.backend.entity.TaskAttachment ta : task.getAttachments()) {
                                addFileToAttachments(ta, filesToAttach);
                            }
                        }

                        // Also attach related Complaint Photo & Attachments if converted from a Complaint
                        com.nagar.parishad.backend.entity.Complaint relatedComplaint = complaintRepository
                                .findByRelatedTaskId(task.getId()).orElse(null);
                        if (relatedComplaint != null) {
                            if (relatedComplaint.getPhotoUrl() != null) {
                                resolveAndAddFile(relatedComplaint.getPhotoUrl(), filesToAttach);
                            }
                            if (relatedComplaint.getAttachments() != null) {
                                for (com.nagar.parishad.backend.entity.ComplaintAttachment ca : relatedComplaint.getAttachments()) {
                                    resolveAndAddFile(ca.getFilePath(), filesToAttach);
                                }
                            }
                        }
                    } else {
                        emailBody = emailTemplateService.getNotificationEmail(recipient.getName(), message,
                                type.toString(), magicToken, "/tasks");
                    }
                } else if (type == NotificationType.COMMENT_ADDED && commentId != null) {
                    com.nagar.parishad.backend.entity.TaskComment comment = taskCommentRepository.findById(commentId)
                            .orElse(null);

                    if (comment != null) {
                        emailSubject = "New Comment on Task: " + comment.getTask().getTitle();
                        emailBody = emailTemplateService.getCommentNotificationEmail(comment.getTask(), comment);

                        java.util.List<com.nagar.parishad.backend.entity.TaskAttachment> attachments = taskAttachmentRepository
                                .findByCommentId(commentId);
                        if (attachments != null) {
                            for (com.nagar.parishad.backend.entity.TaskAttachment ta : attachments) {
                                addFileToAttachments(ta, filesToAttach);
                            }
                        }
                    } else {
                        emailBody = emailTemplateService.getNotificationEmail(recipient.getName(), message,
                                type.toString(), magicToken, "/tasks");
                    }
                } else {
                    emailBody = emailTemplateService.getNotificationEmail(recipient.getName(), message,
                            type.toString(), magicToken, "/tasks");
                }

                emailService.sendEmail(recipient.getEmail(), emailSubject, emailBody, filesToAttach);
            } catch (Exception e) {
                System.err.println("Failed to send notification email: " + e.getMessage());
            }
        }
    }

    private void addFileToAttachments(com.nagar.parishad.backend.entity.TaskAttachment ta,
            java.util.List<java.io.File> filesToAttach) {
        if (ta != null && ta.getFilePath() != null) {
            resolveAndAddFile(ta.getFilePath(), filesToAttach);
        }
    }

    private void resolveAndAddFile(String pathStr, java.util.List<java.io.File> filesToAttach) {
        try {
            if (pathStr == null || pathStr.trim().isEmpty()) return;
            java.io.File file = new java.io.File(pathStr);
            if (file.exists() && file.isFile()) {
                if (!filesToAttach.contains(file)) filesToAttach.add(file);
                return;
            }

            String cleanPath = pathStr.startsWith("/") ? pathStr.substring(1) : pathStr;
            if (cleanPath.startsWith("uploads/")) {
                cleanPath = cleanPath.substring("uploads/".length());
            }

            java.io.File resolvedFile = new java.io.File(uploadDirConfig, cleanPath);
            if (resolvedFile.exists() && resolvedFile.isFile()) {
                if (!filesToAttach.contains(resolvedFile)) filesToAttach.add(resolvedFile);
                return;
            }

            java.io.File cwdFile = new java.io.File("uploads", cleanPath);
            if (cwdFile.exists() && cwdFile.isFile()) {
                if (!filesToAttach.contains(cwdFile)) filesToAttach.add(cwdFile);
            }
        } catch (Exception e) {
            System.err.println("Error resolving file path: " + e.getMessage());
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }

    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @org.springframework.transaction.annotation.Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public void createStatusChangeNotification(User recipient, User sender, com.nagar.parishad.backend.entity.Task task,
            String oldStatus, String newStatus) {
        // Create DB notification BUT SKIP GENERIC EMAIL (sendEmail = false)
        createNotification(recipient, sender, "Task status updated from " + oldStatus + " to " + newStatus,
                NotificationType.STATUS_CHANGED, task.getId(), null, false);

        // Send specific email
        try {
            System.out.println("DEBUG: Preparing Status Update Email for: " + recipient.getEmail());
            String emailSubject = "Task Status Update: " + task.getTitle();
            String emailBody = emailTemplateService.getStatusUpdateEmail(task, sender, oldStatus, newStatus);
            emailService.sendEmail(recipient.getEmail(), emailSubject, emailBody, new java.util.ArrayList<>());
            System.out.println("DEBUG: Status Update Email SENT to: " + recipient.getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send status notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendTaskAssignmentNotification(com.nagar.parishad.backend.entity.Task task, User actor) {
        // 1. Notify Assigned Staff (if not actor)
        if (task.getAssignedStaff() != null && !isSameUser(task.getAssignedStaff(), actor)) {
            createNotification(task.getAssignedStaff(), actor,
                    "New task assigned: " + task.getTitle(),
                    NotificationType.TASK_ASSIGNED, task.getId());
        }

        // 2. Notify ALL Department Heads (if not actor)
        if (task.getDepartment() != null) {
            List<User> deptHeads = getDepartmentHeads(task.getDepartment().getId());
            for (User deptHead : deptHeads) {
                if (deptHead != null && !isSameUser(deptHead, actor)) {
                    String assigneeName = (task.getAssignedStaff() != null) ? task.getAssignedStaff().getName()
                            : "Unassigned";
                    createNotification(deptHead, actor,
                            "New task assigned to " + assigneeName + " in your department: " + task.getTitle(),
                            NotificationType.TASK_ASSIGNED, task.getId());
                }
            }
        }

        // 3. Notify The Admin/Chief Officer (if not actor)
        if (task.getAdmin() != null && !isSameUser(task.getAdmin(), actor)) {
            createNotification(task.getAdmin(), actor,
                    "New task created: " + task.getTitle(),
                    NotificationType.TASK_ASSIGNED, task.getId());
        }
    }

    public void sendTaskCommentNotification(com.nagar.parishad.backend.entity.TaskComment comment) {
        com.nagar.parishad.backend.entity.Task task = comment.getTask();
        User commenter = comment.getUser();

        // 1. Notify Assigned Staff (if not commenter)
        if (task.getAssignedStaff() != null && !isSameUser(task.getAssignedStaff(), commenter)) {
            createNotification(task.getAssignedStaff(), commenter,
                    "New comment on task: " + task.getTitle(),
                    NotificationType.COMMENT_ADDED, task.getId(), comment.getId());
        }

        // 2. Notify ALL Department Heads (if not commenter)
        if (task.getDepartment() != null) {
            List<User> deptHeads = getDepartmentHeads(task.getDepartment().getId());
            for (User deptHead : deptHeads) {
                if (deptHead != null && !isSameUser(deptHead, commenter)
                        && !isSameUser(deptHead, task.getAssignedStaff())) {
                    createNotification(deptHead, commenter,
                            "New comment on task in your department: " + task.getTitle(),
                            NotificationType.COMMENT_ADDED, task.getId(), comment.getId());
                }
            }
        }

        // 3. Notify The Admin (Tenant Isolation)
        if (task.getAdmin() != null && !isSameUser(task.getAdmin(), commenter)) {
            createNotification(task.getAdmin(), commenter,
                    "New comment on task: " + task.getTitle(),
                    NotificationType.COMMENT_ADDED, task.getId(), comment.getId());
        }
    }

    public void sendTaskUpdatedNotification(com.nagar.parishad.backend.entity.Task task, User actor) {
        if (task.getAssignedStaff() != null && !isSameUser(task.getAssignedStaff(), actor)) {
            createNotification(task.getAssignedStaff(), actor, "Task updated: " + task.getTitle(), NotificationType.STATUS_CHANGED, task.getId());
        }
        if (task.getDepartment() != null) {
            for (User deptHead : getDepartmentHeads(task.getDepartment().getId())) {
                if (deptHead != null && !isSameUser(deptHead, actor)) {
                    createNotification(deptHead, actor, "Task updated in your department: " + task.getTitle(), NotificationType.STATUS_CHANGED, task.getId());
                }
            }
        }
        if (task.getAdmin() != null && !isSameUser(task.getAdmin(), actor)) {
            createNotification(task.getAdmin(), actor, "Task updated: " + task.getTitle(), NotificationType.STATUS_CHANGED, task.getId());
        }
    }

    public void sendTaskDeletedNotification(com.nagar.parishad.backend.entity.Task task, User actor) {
        if (task.getAssignedStaff() != null && !isSameUser(task.getAssignedStaff(), actor)) {
            createNotification(task.getAssignedStaff(), actor, "Task deleted: " + task.getTitle(), NotificationType.STATUS_CHANGED, null);
        }
        if (task.getDepartment() != null) {
            for (User deptHead : getDepartmentHeads(task.getDepartment().getId())) {
                if (deptHead != null && !isSameUser(deptHead, actor)) {
                    createNotification(deptHead, actor, "Task deleted from your department: " + task.getTitle(), NotificationType.STATUS_CHANGED, null);
                }
            }
        }
        if (task.getAdmin() != null && !isSameUser(task.getAdmin(), actor)) {
            createNotification(task.getAdmin(), actor, "Task deleted: " + task.getTitle(), NotificationType.STATUS_CHANGED, null);
        }
    }

    public void sendUserManagementNotification(User targetUser, String action, User actor) {
        String message = "User " + targetUser.getName() + " (" + targetUser.getRole() + ") was " + action;
        if (targetUser.getDepartment() != null && targetUser.getDepartment().getName() != null) {
            message += " in Department: " + targetUser.getDepartment().getName();
        }
        String subject = "User Management: " + action;
        String body = "<p>" + message + "</p>";

        // 1. Notify ALL Chief Officers (if not actor)
        // 1. Notify The Admin (Tenant Isolation) - Use targetUser's admin or actor's
        // admin
        User tenantAdmin = targetUser.getAdmin();
        if (tenantAdmin == null && targetUser.getRole() == com.nagar.parishad.backend.enums.Role.ADMIN) {
            // If target is Admin, arguably notify OWNER? Or no-one?
            // Assuming System Owner exists
        } else if (tenantAdmin != null && !isSameUser(tenantAdmin, actor)) {
            sendGenericEmailAndNotification(tenantAdmin, subject, body, message);
        }

        // 2. Notify ALL Department Heads
        if (targetUser.getDepartment() != null) {
            List<User> deptHeads = getDepartmentHeads(targetUser.getDepartment().getId());
            for (User deptHead : deptHeads) {
                if (deptHead != null && !isSameUser(deptHead, actor) && !isSameUser(deptHead, targetUser)) {
                    // Check if already notified as Chief Officer
                    // Simple check: if Role is ADMIN, they got it. If they are Dept Head AND Admin,
                    // they might get twice.
                    // We can accept that or verify role.
                    if (deptHead.getRole() != com.nagar.parishad.backend.enums.Role.ADMIN) {
                        sendGenericEmailAndNotification(deptHead, subject, body, message);
                    }
                }
            }
        }

        // 3. Notify Target User (if ADDED, EDITED, or DELETED)
        if ("ADDED".equals(action) || "EDITED".equals(action) || "DELETED".equals(action)) {
            try {
                // For ADDED/EDITED, also create in-app notification for the user
                if (!"DELETED".equals(action)) {
                    createNotification(targetUser, actor, message, NotificationType.STATUS_CHANGED, null, null, false);
                }
                emailService.sendEmail(targetUser.getEmail(), subject, body);
            } catch (Exception e) {
                System.err.println("Failed to send user management email to target: " + e.getMessage());
            }
        }
    }

    private void sendGenericEmailAndNotification(User recipient, String subject, String body, String notificationMsg) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setSender(null);
        notification.setMessage(notificationMsg);
        notification.setType(NotificationType.STATUS_CHANGED);
        notificationRepository.save(notification);

        try {
            emailService.sendEmail(recipient.getEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send generic email: " + e.getMessage());
        }
    }

    public void sendStatusUpdateNotification(com.nagar.parishad.backend.entity.Task task, String oldStatus,
            String newStatus, User actor) {
        if (actor == null || task == null) return;
        User freshActor = userRepository.findById(actor.getId()).orElse(actor);
        System.out.println("DEBUG: NotificationService - Preparing notifications for Task " + task.getId()
                + " (Status: " + oldStatus + " -> " + newStatus + ") by " + freshActor.getName());

        // 1. Notify Assigned Staff (if not actor)
        if (task.getAssignedStaff() != null && !isSameUser(task.getAssignedStaff(), freshActor)) {
            User staff = userRepository.findById(task.getAssignedStaff().getId()).orElse(task.getAssignedStaff());
            createStatusChangeNotification(staff, freshActor, task, oldStatus, newStatus);
        }

        // 2. Notify ALL Department Heads (if not actor)
        if (task.getDepartment() != null) {
            List<User> deptHeads = getDepartmentHeads(task.getDepartment().getId());
            for (User deptHead : deptHeads) {
                if (deptHead != null && !isSameUser(deptHead, freshActor)
                        && !isSameUser(deptHead, task.getAssignedStaff())) {
                    createStatusChangeNotification(deptHead, freshActor, task, oldStatus, newStatus);
                }
            }
        }

        // 3. Notify The Admin (Chief Officer)
        if (task.getAdmin() != null && !isSameUser(task.getAdmin(), freshActor)) {
            User admin = userRepository.findById(task.getAdmin().getId()).orElse(task.getAdmin());
            createStatusChangeNotification(admin, freshActor, task, oldStatus, newStatus);
        }
    }

    private List<User> getDepartmentHeads(Long departmentId) {
        return userRepository.findByDepartmentIdAndRole(departmentId,
                com.nagar.parishad.backend.enums.Role.DEPARTMENT_HEAD);
    }

    // Removed getChiefOfficers() as it violates Multi-Tenancy by exposing all
    // global admins
    // private List<User> getChiefOfficers() {
    // return
    // userRepository.findByRole(com.nagar.parishad.backend.enums.Role.ADMIN);
    // }

    private boolean isSameUser(User u1, User u2) {
        if (u1 == null || u2 == null)
            return false;
        return u1.getId().equals(u2.getId());
    }
}
