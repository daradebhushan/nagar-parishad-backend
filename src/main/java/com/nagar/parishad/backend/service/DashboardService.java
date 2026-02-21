package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.dto.StatsDto;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.Role;
import com.nagar.parishad.backend.enums.TaskPriority;
import com.nagar.parishad.backend.enums.TaskStatus;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import com.nagar.parishad.backend.repository.TaskRepository;
import com.nagar.parishad.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

        @Autowired
        private TaskRepository taskRepository;

        @Autowired
        private DepartmentRepository departmentRepository;

        @Autowired
        private UserRepository userRepository;

        public StatsDto getStats(User user) {
                long totalDepartments = 0;
                long totalUsers = 0;
                long totalTasks = 0;
                long toDoTasks = 0;
                long inProgressTasks = 0;
                long onHoldTasks = 0;
                long completedTasks = 0;
                long criticalTasks = 0;
                long directToDoTasks = 0;
                long myAssignedTasks = 0;

                if (user.getRole() == Role.OWNER) {
                        // OWNER (Global) Logic
                        totalDepartments = departmentRepository.count();
                        totalUsers = userRepository.count();
                        totalTasks = taskRepository.count();
                        toDoTasks = taskRepository.countByStatus(TaskStatus.TO_DO);
                        inProgressTasks = taskRepository.countByStatus(TaskStatus.IN_PROGRESS);
                        onHoldTasks = taskRepository.countByStatus(TaskStatus.ON_HOLD);
                        completedTasks = taskRepository.countByStatus(TaskStatus.COMPLETED);
                        criticalTasks = taskRepository.countByPriority(TaskPriority.CRITICAL);
                        directToDoTasks = taskRepository.countByStatusAndType(TaskStatus.TO_DO, "Internal");

                        // Populate Department Stats (Global)
                        java.util.List<com.nagar.parishad.backend.dto.DepartmentStatDto> departmentStats = new java.util.ArrayList<>();
                        java.util.List<com.nagar.parishad.backend.entity.Department> departments = departmentRepository
                                        .findAll();
                        for (com.nagar.parishad.backend.entity.Department dept : departments) {
                                long count = userRepository.countByDepartment_Id(dept.getId());
                                departmentStats.add(new com.nagar.parishad.backend.dto.DepartmentStatDto(dept.getId(),
                                                dept.getName(), count));
                        }

                        StatsDto stats = new StatsDto();
                        stats.setDepartmentStats(departmentStats); // Set the dynamic list
                        stats.setTotalDepartments(totalDepartments);
                        stats.setTotalUsers(totalUsers);
                        stats.setTotalTasks(totalTasks);
                        stats.setToDoTasks(toDoTasks);
                        stats.setInProgressTasks(inProgressTasks);
                        stats.setOnHoldTasks(onHoldTasks);
                        stats.setCompletedTasks(completedTasks);
                        stats.setCriticalTasks(criticalTasks);
                        stats.setDirectToDoTasks(directToDoTasks);
                        stats.setMyAssignedTasks(myAssignedTasks);
                        return stats;

                } else if (user.getRole() == Role.ADMIN) {
                        // ADMIN (Tenant) Logic
                        Long adminId = user.getId();
                        totalDepartments = departmentRepository.countByAdminId(adminId);
                        totalUsers = userRepository.countByAdminId(adminId);
                        totalTasks = taskRepository.countByAdminId(adminId);
                        toDoTasks = taskRepository.countByAdminIdAndStatus(adminId, TaskStatus.TO_DO);
                        inProgressTasks = taskRepository.countByAdminIdAndStatus(adminId, TaskStatus.IN_PROGRESS);
                        onHoldTasks = taskRepository.countByAdminIdAndStatus(adminId, TaskStatus.ON_HOLD);
                        completedTasks = taskRepository.countByAdminIdAndStatus(adminId, TaskStatus.COMPLETED);
                        criticalTasks = taskRepository.countByAdminIdAndPriority(adminId, TaskPriority.CRITICAL);
                        directToDoTasks = taskRepository.countByAdminIdAndStatusAndType(adminId, TaskStatus.TO_DO,
                                        "Internal");

                        // Populate Department Stats (Scoped to Admin)
                        java.util.List<com.nagar.parishad.backend.dto.DepartmentStatDto> departmentStats = new java.util.ArrayList<>();
                        java.util.List<com.nagar.parishad.backend.entity.Department> departments = departmentRepository
                                        .findByAdminId(adminId);
                        for (com.nagar.parishad.backend.entity.Department dept : departments) {
                                long count = userRepository.countByAdminIdAndDepartment_Id(adminId, dept.getId());
                                departmentStats.add(new com.nagar.parishad.backend.dto.DepartmentStatDto(dept.getId(),
                                                dept.getName(), count));
                        }

                        StatsDto stats = new StatsDto();
                        stats.setDepartmentStats(departmentStats); // Set the dynamic list
                        stats.setTotalDepartments(totalDepartments);
                        stats.setTotalUsers(totalUsers);
                        stats.setTotalTasks(totalTasks);
                        stats.setToDoTasks(toDoTasks);
                        stats.setInProgressTasks(inProgressTasks);
                        stats.setOnHoldTasks(onHoldTasks);
                        stats.setCompletedTasks(completedTasks);
                        stats.setCriticalTasks(criticalTasks);
                        stats.setDirectToDoTasks(directToDoTasks);
                        stats.setMyAssignedTasks(myAssignedTasks);
                        return stats;

                } else if (user.getRole() == Role.DEPARTMENT_HEAD) {
                        if (user.getDepartment() != null) {
                                Long deptId = user.getDepartment().getId();
                                totalTasks = taskRepository.countByDepartmentId(deptId);
                                toDoTasks = taskRepository.countByDepartmentIdAndStatus(deptId, TaskStatus.TO_DO);
                                inProgressTasks = taskRepository.countByDepartmentIdAndStatus(deptId,
                                                TaskStatus.IN_PROGRESS);
                                onHoldTasks = taskRepository.countByDepartmentIdAndStatus(deptId, TaskStatus.ON_HOLD);
                                completedTasks = taskRepository.countByDepartmentIdAndStatus(deptId,
                                                TaskStatus.COMPLETED);
                                directToDoTasks = taskRepository.countByDepartmentIdAndStatusAndType(deptId,
                                                TaskStatus.TO_DO, "Internal");
                                // Critical in dept - reusing spec or adding method. Spec for now as I didn't
                                // add countByDeptAndPriority
                                criticalTasks = taskRepository.count((root, query, cb) -> cb.and(
                                                cb.equal(root.get("department").get("id"), deptId),
                                                cb.equal(root.get("priority"), TaskPriority.CRITICAL)));

                                totalUsers = userRepository
                                                .count((root, query, cb) -> cb.equal(root.get("department").get("id"),
                                                                deptId));

                                // Tasks from CO (Admin/Owner)
                                java.util.List<Role> adminRoles = java.util.Arrays.asList(Role.ADMIN, Role.OWNER);
                                long tasksFromCo = taskRepository.countByDepartmentIdAndAdminRoleIn(deptId, adminRoles);

                                // Employee Stats
                                java.util.List<com.nagar.parishad.backend.dto.EmployeeTaskStatDto> employeeStats = new java.util.ArrayList<>();
                                java.util.List<User> deptUsers = userRepository.findAll((root, query, cb) -> cb
                                                .equal(root.get("department").get("id"), deptId));

                                for (User emp : deptUsers) {
                                        long empTotal = taskRepository.countByAssignedStaffId(emp.getId());
                                        long empCompleted = taskRepository.countByAssignedStaffIdAndStatus(emp.getId(),
                                                        TaskStatus.COMPLETED);
                                        employeeStats.add(new com.nagar.parishad.backend.dto.EmployeeTaskStatDto(
                                                        emp.getId(),
                                                        emp.getName(),
                                                        emp.getDesignation() != null ? emp.getDesignation().getName()
                                                                        : "",
                                                        empTotal,
                                                        empCompleted));
                                }

                                StatsDto stats = new StatsDto();
                                stats.setTotalDepartments(1); // Dept Head sees their own dept
                                stats.setTotalUsers(totalUsers);
                                stats.setTotalTasks(totalTasks);
                                stats.setToDoTasks(toDoTasks);
                                stats.setInProgressTasks(inProgressTasks);
                                stats.setOnHoldTasks(onHoldTasks);
                                stats.setCompletedTasks(completedTasks);
                                stats.setCriticalTasks(criticalTasks);
                                stats.setDirectToDoTasks(directToDoTasks);
                                stats.setTasksFromCo(tasksFromCo);
                                stats.setEmployeeStats(employeeStats);
                                return stats;
                        }
                } else if (user.getRole() == Role.STAFF) {
                        myAssignedTasks = taskRepository.countByAssignedStaffId(user.getId());
                        totalTasks = myAssignedTasks; // Set totalTasks to match myAssignedTasks
                        toDoTasks = taskRepository.countByAssignedStaffIdAndStatus(user.getId(), TaskStatus.TO_DO);
                        inProgressTasks = taskRepository.countByAssignedStaffIdAndStatus(user.getId(),
                                        TaskStatus.IN_PROGRESS);
                        onHoldTasks = taskRepository.countByAssignedStaffIdAndStatus(user.getId(), TaskStatus.ON_HOLD);
                        completedTasks = taskRepository.countByAssignedStaffIdAndStatus(user.getId(),
                                        TaskStatus.COMPLETED);
                        directToDoTasks = taskRepository.countByAssignedStaffIdAndStatusAndType(user.getId(),
                                        TaskStatus.TO_DO, "Internal");
                }

                StatsDto stats = new StatsDto();
                stats.setTotalDepartments(totalDepartments);
                stats.setTotalUsers(totalUsers);
                stats.setTotalTasks(totalTasks);
                stats.setToDoTasks(toDoTasks);
                stats.setInProgressTasks(inProgressTasks);
                stats.setOnHoldTasks(onHoldTasks);
                stats.setCompletedTasks(completedTasks);
                stats.setCriticalTasks(criticalTasks);
                stats.setDirectToDoTasks(directToDoTasks);
                stats.setMyAssignedTasks(myAssignedTasks);
                return stats;
        }
}
