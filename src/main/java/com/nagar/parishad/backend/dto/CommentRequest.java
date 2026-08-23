package com.nagar.parishad.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class CommentRequest {
    @NotBlank
    private String text;

    private boolean hasAttachments;

    // Manual Getter
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean getHasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }
}
