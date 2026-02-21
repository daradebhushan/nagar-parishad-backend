package com.nagar.parishad.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nagar.parishad.backend.dto.ComplaintDTO;
import com.nagar.parishad.backend.dto.ComplaintSubmissionDTO;
import com.nagar.parishad.backend.dto.TaskRequest;
import com.nagar.parishad.backend.entity.Complaint;
import com.nagar.parishad.backend.entity.ComplaintType;
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.entity.Task;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.ComplaintStatus;
import com.nagar.parishad.backend.repository.ComplaintRepository;
import com.nagar.parishad.backend.repository.ComplaintTypeRepository;
import com.nagar.parishad.backend.repository.DepartmentRepository;

@ExtendWith(MockitoExtension.class)
public class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private ComplaintTypeRepository complaintTypeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TaskService taskService;

    @Mock
    private WhatsappService whatsappService;

    @InjectMocks
    private ComplaintService complaintService;

    private Department department;
    private ComplaintType complaintType;
    private Complaint complaint;
    private User admin;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("Health");

        complaintType = new ComplaintType();
        complaintType.setId(1L);
        complaintType.setNameEn("Garbage");

        complaint = new Complaint();
        complaint.setId(100L);
        complaint.setComplaintNo("CMP-1-1234");
        complaint.setDepartment(department);
        complaint.setComplaintType(complaintType);
        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setCitizenMobile("9999999999");

        admin = new User();
        admin.setId(1L);
    }

    @Test
    void testSubmitComplaint_Success() {
        ComplaintSubmissionDTO dto = new ComplaintSubmissionDTO();
        dto.setDepartmentId(1L);
        dto.setComplaintTypeId(1L);
        dto.setName("Citizen");
        dto.setMobile("1234567890");
        dto.setDescription("Garbage overflow");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(complaintTypeRepository.findById(1L)).thenReturn(Optional.of(complaintType));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint c = invocation.getArgument(0);
            c.setId(101L);
            return c;
        });

        ComplaintDTO result = complaintService.submitComplaint(dto);

        assertNotNull(result);
        assertEquals("Garbage", result.getComplaintTypeName());
        verify(complaintRepository).save(any(Complaint.class));
    }

    @Test
    void testUpdateStatus_Success() {
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        ComplaintDTO result = complaintService.updateStatus(100L, ComplaintStatus.ACCEPTED, "Processing");

        assertEquals(ComplaintStatus.ACCEPTED, result.getStatus());
        verify(complaintRepository).save(complaint);
    }

    @Test
    void testCreateTaskFromComplaint_Success() {
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTitle("Fix Garbage");

        Task createdTask = new Task();
        createdTask.setId(500L);
        createdTask.setTitle("Fix Garbage");

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));
        when(taskService.createTask(any(TaskRequest.class), any(User.class))).thenReturn(createdTask);
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        ComplaintDTO result = complaintService.createTaskFromComplaint(100L, taskRequest, admin);

        assertEquals(ComplaintStatus.CONVERTED_TO_TASK, result.getStatus());
        assertEquals(500L, result.getRelatedTaskId());
        verify(taskService).createTask(any(TaskRequest.class), any(User.class));
    }
}
