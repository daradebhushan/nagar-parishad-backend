package com.nagar.parishad.backend.dto;

import com.nagar.parishad.backend.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class TaskStatusUpdateRequest {
    @NotNull
    private String status; // Assuming String or TaskStatus, original was String in my read

    // Manual Getter
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
