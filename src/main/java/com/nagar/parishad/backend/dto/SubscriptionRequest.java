package com.nagar.parishad.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SubscriptionRequest {
    private Long adminId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String planType;
}
