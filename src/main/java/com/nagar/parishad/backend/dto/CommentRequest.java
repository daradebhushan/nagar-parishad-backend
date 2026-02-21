package com.nagar.parishad.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class CommentRequest {
    @NotBlank
    private String text;

    // Manual Getter
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
