package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.entity.Task;
import com.nagar.parishad.backend.entity.TaskComment;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.repository.TaskCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    TaskCommentRepository commentRepository;

    @Autowired
    NotificationService notificationService;

    @Autowired
    TaskService taskService;

    public TaskComment addComment(Long taskId, String text, User user, boolean hasAttachments) {
        Task task = taskService.getTaskById(taskId, user);
        taskService.assertTaskCollaborationAccess(task, user);

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setUser(user);
        comment.setText(text);

        TaskComment savedComment = commentRepository.save(comment);

        // Notify stakeholders only if no attachments are pending
        if (!hasAttachments) {
            notificationService.sendTaskCommentNotification(savedComment);
        }

        return savedComment;
    }

    public void sendCommentNotification(Long commentId) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        notificationService.sendTaskCommentNotification(comment);
    }

    public List<TaskComment> getComments(Long taskId, User user) {
        taskService.getTaskById(taskId, user);
        return commentRepository.findByTaskIdOrderByTimestampAsc(taskId);
    }

    public TaskComment updateComment(Long commentId, String text, User user) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        taskService.assertTaskCollaborationAccess(comment.getTask(), user);

        boolean isOwner = comment.getUser().getId().equals(user.getId());
        boolean isAdmin = taskService.isTaskManager(user);

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Unauthorized: You can only edit your own comments");
        }

        comment.setText(text);
        // comment.setTimestamp(new Date()); // Optional: Update timestamp or add
        // updatedAt
        return commentRepository.save(comment);
    }

    public void deleteComment(Long commentId, User user) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        taskService.assertTaskCollaborationAccess(comment.getTask(), user);

        boolean isOwner = comment.getUser().getId().equals(user.getId());
        boolean isAdmin = taskService.isTaskManager(user);

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Unauthorized: You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }
}
