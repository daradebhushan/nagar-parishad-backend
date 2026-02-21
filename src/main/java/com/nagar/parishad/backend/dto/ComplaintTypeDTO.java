package com.nagar.parishad.backend.dto;

import lombok.Data;

@Data
public class ComplaintTypeDTO {
    private Long id;
    private Long departmentId;
    private String departmentName;
    private String nameMr;
    private String nameEn;
    private boolean active;
}
