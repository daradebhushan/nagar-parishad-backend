package com.nagar.parishad.backend.repository;

import com.nagar.parishad.backend.entity.Task;
import com.nagar.parishad.backend.enums.TaskPriority;
import com.nagar.parishad.backend.enums.TaskStatus;
import com.nagar.parishad.backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    Page<Task> findByAdminId(Long adminId, Pageable pageable);

    List<Task> findByAdminIdOrderByCreatedDateDesc(Long adminId);

    Page<Task> findByDepartmentId(Long departmentId, Pageable pageable);

    List<Task> findByDepartmentId(Long departmentId);

    Page<Task> findByAssignedStaffId(Long staffId, Pageable pageable);

    long countByStatus(TaskStatus status);

    long countByPriority(TaskPriority priority);

    long countByDepartmentIdAndStatus(Long departmentId, TaskStatus status);

    long countByDepartmentId(Long departmentId);

    long countByAssignedStaffIdAndStatus(Long staffId, TaskStatus status);

    long countByAssignedStaffId(Long staffId);

    long countByDepartmentIdAndAdminRoleIn(Long departmentId, java.util.Collection<Role> roles);

    long countByStatusAndType(TaskStatus status, String type);

    long countByDepartmentIdAndStatusAndType(Long departmentId, TaskStatus status, String type);

    long countByAssignedStaffIdAndStatusAndType(Long staffId, TaskStatus status, String type);

    long countByAssignedStaffIdIn(java.util.Collection<Long> staffIds);

    long countByAdminId(Long adminId);

    long countByAdminIdAndStatus(Long adminId, TaskStatus status);

    long countByAdminIdAndPriority(Long adminId, TaskPriority priority);

    long countByAdminIdAndStatusAndType(Long adminId, TaskStatus status, String type);

    long countByRelatedComplaintIsNotNull();

    long countByAdminIdAndRelatedComplaintIsNotNull(Long adminId);

    long countByDepartmentIdAndRelatedComplaintIsNotNull(Long departmentId);

    long countByAssignedStaffIdAndRelatedComplaintIsNotNull(Long staffId);
}
