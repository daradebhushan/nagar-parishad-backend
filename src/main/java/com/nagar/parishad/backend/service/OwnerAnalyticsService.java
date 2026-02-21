package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.Role;
import com.nagar.parishad.backend.repository.TaskAttachmentRepository;
import com.nagar.parishad.backend.repository.TaskCommentRepository;
import com.nagar.parishad.backend.repository.TaskRepository;
import com.nagar.parishad.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OwnerAnalyticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskAttachmentRepository attachmentRepository;

    @Autowired
    private TaskCommentRepository commentRepository;

    /**
     * Get IDs of all users belonging to this Admin's tenant.
     * Hierarchy: Admin -> Dept Head -> Staff
     */
    public List<Long> getTenantUserIds(User admin) {
        Set<Long> userIds = new HashSet<>();
        userIds.add(admin.getId()); // Include Admin themselves

        // 1. Direct Reports (Dept Heads usually)
        List<User> directReports = findByAdmin(admin);
        for (User direct : directReports) {
            userIds.add(direct.getId());

            // 2. Indirect Reports (Staff reporting to Dept Head)
            List<User> indirectReports = findByAdmin(direct);
            indirectReports.forEach(u -> userIds.add(u.getId()));
        }

        return new ArrayList<>(userIds);
    }

    private List<User> findByAdmin(User admin) {
        // Since we don't have a simple list method in Repo for finding by logic,
        // we'll use findAll and filter. For optimization, repo method is better,
        // but User repo has countByAdmin, let's assume we can fetch too.
        // Wait, repo has `Page<User> findByAdminId`. We can use findAll with Stream
        // filter
        // to avoid pagination issues or complex JPA specs for now if dataset is small.
        // Actually, let's use userRepository.findByRole and check admin.

        // Better: Let's assume the repo has findByAdmin(User) or findByAdminId(Long)
        // returning List.
        // Checking repo content... it has `Page<User> findByAdminId`.
        // We'll use a hacky findAll() stream for absolute correctness without new repo
        // methods,
        // or just rely on `findByAdminId` with large page size.
        // Let's use `userRepository.findAll()` and filter in memory to be safe against
        // repo limits for now.
        // Note: In production this is bad, but for MVP it ensures logic works.

        return userRepository.findAll().stream()
                .filter(u -> u.getAdmin() != null && u.getAdmin().getId().equals(admin.getId()))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getTenantStats(User admin) {
        List<Long> userIds = getTenantUserIds(admin);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userIds.size());

        if (userIds.isEmpty()) {
            stats.put("totalTasks", 0);
            stats.put("totalComments", 0);
            stats.put("totalDataUsageGB", 0.0);
            return stats;
        }

        // Tasks
        long taskCount = taskRepository.countByAssignedStaffIdIn(userIds);
        stats.put("totalTasks", taskCount);

        // Comments
        long commentCount = commentRepository.countByUserIdIn(userIds);
        stats.put("totalComments", commentCount);

        // Data Usage
        Long totalBytes = attachmentRepository.sumFileSizeByUserIds(userIds);
        stats.put("totalDataUsageBytes", totalBytes == null ? 0 : totalBytes);

        return stats;
    }
}
