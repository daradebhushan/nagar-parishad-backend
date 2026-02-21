package com.nagar.parishad.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nagar.parishad.backend.dto.TaskRequest;
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.entity.Task;
import com.nagar.parishad.backend.entity.TaskHistory;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.Role;
import com.nagar.parishad.backend.enums.TaskPriority;
import com.nagar.parishad.backend.enums.TaskStatus;
import com.nagar.parishad.backend.repository.ComplaintRepository;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import com.nagar.parishad.backend.repository.TaskHistoryRepository;
import com.nagar.parishad.backend.repository.TaskRepository;
import com.nagar.parishad.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private TaskService taskService;

    private User admin;
    private User staff;
    private Department department;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);

        staff = new User();
        staff.setId(2L);
        staff.setRole(Role.STAFF);

        department = new Department();
        department.setId(1L);
        department.setName("IT");
    }

    @Test
    void testCreateTask_Success() {
        TaskRequest request = new TaskRequest();
        request.setTitle("Fix Bug");
        request.setDepartmentId(1L);
        request.setAssignedStaffId(2L);
        request.setPriority(TaskPriority.HIGH);
        request.setStatus(TaskStatus.TO_DO);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(10L);
            return t;
        });

        Task createdTask = taskService.createTask(request, admin);

        assertNotNull(createdTask);
        assertEquals("Fix Bug", createdTask.getTitle());
        assertEquals(staff, createdTask.getAssignedStaff());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void testUpdateTaskStatus_Success() {
        Task task = new Task();
        task.setId(10L);
        task.setStatus(TaskStatus.TO_DO);
        task.setAssignedStaff(staff);
        task.setTitle("Fix Bug");

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Task updatedTask = taskService.updateTaskStatus(10L, TaskStatus.IN_PROGRESS, admin);

        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.getStatus());
        verify(taskHistoryRepository).save(any(TaskHistory.class));
        verify(notificationService).sendStatusUpdateNotification(eq(task), anyString(), anyString(), eq(admin));
    }
}
