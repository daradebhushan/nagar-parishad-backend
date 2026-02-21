package com.nagar.parishad.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class DepartmentRequest {
    @NotBlank
    private String name;

    private String nameMr;
    private String nameHi;
    private boolean chatbotEnabled;

    // Manual Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameMr() {
        return nameMr;
    }

    public void setNameMr(String nameMr) {
        this.nameMr = nameMr;
    }

    public String getNameHi() {
        return nameHi;
    }

    public void setNameHi(String nameHi) {
        this.nameHi = nameHi;
    }

    public boolean isChatbotEnabled() {
        return chatbotEnabled;
    }

    public void setChatbotEnabled(boolean chatbotEnabled) {
        this.chatbotEnabled = chatbotEnabled;
    }

    private String subQuestions;

    private Boolean active;

    public String getSubQuestions() {
        return subQuestions;
    }

    public void setSubQuestions(String subQuestions) {
        this.subQuestions = subQuestions;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
