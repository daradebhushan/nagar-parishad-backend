package com.nagar.parishad.backend.dto;

import com.nagar.parishad.backend.enums.Role;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private String mobile;
    private String email;
    private String password;
    private Role role;
    private Long departmentId;
    private String designation;
    private Long adminId; // Allow updating supervisor
    private Boolean active;
    private String organizationName;
    private String organizationLogo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    private Boolean emailNotifications;

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    private Boolean clearDepartment;
    private Boolean clearDesignation;

    public Boolean getClearDepartment() {
        return clearDepartment;
    }

    public void setClearDepartment(Boolean clearDepartment) {
        this.clearDepartment = clearDepartment;
    }

    public Boolean getClearDesignation() {
        return clearDesignation;
    }

    public void setClearDesignation(Boolean clearDesignation) {
        this.clearDesignation = clearDesignation;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationLogo() {
        return organizationLogo;
    }

    public void setOrganizationLogo(String organizationLogo) {
        this.organizationLogo = organizationLogo;
    }
}
