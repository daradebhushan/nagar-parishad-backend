package com.nagar.parishad.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeTaskStatDto {
    private Long id;
    private String name;
    private String designation;
    private long totalTasks;
    private long completedTasks;

}
