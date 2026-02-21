package com.nagar.parishad.backend.repository;

import com.nagar.parishad.backend.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {
    List<TaskAttachment> findByTaskId(Long taskId);

    List<TaskAttachment> findByCommentId(Long commentId);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(a.fileSize) FROM TaskAttachment a WHERE a.uploadedBy.id IN :userIds")
    Long sumFileSizeByUserIds(@org.springframework.data.repository.query.Param("userIds") List<Long> userIds);
}
