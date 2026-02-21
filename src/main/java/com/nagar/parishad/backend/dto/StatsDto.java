package com.nagar.parishad.backend.dto;

import lombok.Builder;
import lombok.Data;

public class StatsDto {
    private long totalDepartments;
    private long totalUsers;
    private long totalTasks;
    private long toDoTasks;
    private long inProgressTasks;
    private long onHoldTasks;
    private long completedTasks;
    private long criticalTasks;
    private long directToDoTasks;
    private long myAssignedTasks;
    private long tasksFromCo;
    private java.util.List<DepartmentStatDto> departmentStats;
    private java.util.List<EmployeeTaskStatDto> employeeStats;

    // Manual Getters and Setters
    public java.util.List<DepartmentStatDto> getDepartmentStats() {
        return departmentStats;
    }

    public void setDepartmentStats(java.util.List<DepartmentStatDto> departmentStats) {
        this.departmentStats = departmentStats;
    }

    public long getTotalDepartments() {
        return totalDepartments;
    }

    public void setTotalDepartments(long totalDepartments) {
        this.totalDepartments = totalDepartments;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(long totalTasks) {
        this.totalTasks = totalTasks;
    }

    public long getToDoTasks() {
        return toDoTasks;
    }

    public void setToDoTasks(long toDoTasks) {
        this.toDoTasks = toDoTasks;
    }

    public long getInProgressTasks() {
        return inProgressTasks;
    }

    public void setInProgressTasks(long inProgressTasks) {
        this.inProgressTasks = inProgressTasks;
    }

    public long getOnHoldTasks() {
        return onHoldTasks;
    }

    public void setOnHoldTasks(long onHoldTasks) {
        this.onHoldTasks = onHoldTasks;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(long completedTasks) {
        this.completedTasks = completedTasks;
    }

    public long getCriticalTasks() {
        return criticalTasks;
    }

    public void setCriticalTasks(long criticalTasks) {
        this.criticalTasks = criticalTasks;
    }

    public long getDirectToDoTasks() {
        return directToDoTasks;
    }

    public void setDirectToDoTasks(long directToDoTasks) {
        this.directToDoTasks = directToDoTasks;
    }

    public long getMyAssignedTasks() {
        return myAssignedTasks;
    }

    public void setMyAssignedTasks(long myAssignedTasks) {
        this.myAssignedTasks = myAssignedTasks;
    }

    public long getTasksFromCo() {
        return tasksFromCo;
    }

    public void setTasksFromCo(long tasksFromCo) {
        this.tasksFromCo = tasksFromCo;
    }

    public java.util.List<EmployeeTaskStatDto> getEmployeeStats() {
        return employeeStats;
    }

    public void setEmployeeStats(java.util.List<EmployeeTaskStatDto> employeeStats) {
        this.employeeStats = employeeStats;
    }
}
