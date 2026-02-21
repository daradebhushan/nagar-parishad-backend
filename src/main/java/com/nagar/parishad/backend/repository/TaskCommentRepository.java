package com.nagar.parishad.backend.repository;

import com.nagar.parishad.backend.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "attachments")
    List<TaskComment> findByTaskIdOrderByTimestampAsc(Long taskId);

    long countByUserIdIn(java.util.Collection<Long> userIds);
}
