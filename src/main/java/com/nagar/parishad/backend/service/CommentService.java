package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.entity.Task;
import com.nagar.parishad.backend.entity.TaskComment;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.repository.TaskCommentRepository;
import com.nagar.parishad.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    TaskCommentRepository commentRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    NotificationService notificationService;

    public TaskComment addComment(Long taskId, String text, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Basic authorization check: Ensure user is related to task ecosystem?
        // For simplicity assuming Controller layer handles main role checks,
        // but here we could verify if user belongs to same admin/department if strictly
        // needed.

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setUser(user);
        comment.setText(text);

        TaskComment savedComment = commentRepository.save(comment);

        // Notify All Stakeholders
        notificationService.sendTaskCommentNotification(savedComment);

        return savedComment;
    }

    public List<TaskComment> getComments(Long taskId) {
        return commentRepository.findByTaskIdOrderByTimestampAsc(taskId);
    }

    public TaskComment updateComment(Long commentId, String text, User user) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Authorization: Only owner or ADMIN can edit
        boolean isOwner = comment.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole().name().equals("ADMIN");

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

        // Authorization: Only owner or ADMIN can delete
        boolean isOwner = comment.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole().name().equals("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Unauthorized: You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }
}
