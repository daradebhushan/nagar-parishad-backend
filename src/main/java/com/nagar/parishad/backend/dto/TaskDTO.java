package com.nagar.parishad.backend.dto;

import com.nagar.parishad.backend.enums.TaskPriority;
import com.nagar.parishad.backend.enums.TaskStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private String type;
    private LocalDateTime createdDate;
    private LocalDateTime dueDate;

    private Long departmentId;
    private String departmentName;

    private Long assignedStaffId;
    private String assignedStaffName;

    private Long relatedComplaintId;
    private String relatedComplaintNo;
}
