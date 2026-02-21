package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ApiResponse;
import com.nagar.parishad.backend.dto.SignupRequest;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.service.AuthService;
import com.nagar.parishad.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AdminController {

    @Autowired
    AuthService authService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    private com.nagar.parishad.backend.service.NotificationService notificationService;

    @Autowired
    private com.nagar.parishad.backend.repository.TaskRepository taskRepository;

    // OWNER creates ADMIN
    @PostMapping("/owner/create-admin")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<User>> createAdmin(@Valid @RequestBody SignupRequest signupRequest,
            @AuthenticationPrincipal User user) {
        // Validation could be stricter here
        if (signupRequest.getRole() != com.nagar.parishad.backend.enums.Role.ADMIN) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only ADMIN role can be created via this endpoint"));
        }
        User newUser = authService.registerUser(signupRequest, user);
        return ResponseEntity.ok(ApiResponse.success("Admin created successfully", newUser));
    }

    // ADMIN creates Department Head & Staff
    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody SignupRequest signupRequest,
            @AuthenticationPrincipal User admin) {
        User newUser = authService.registerUser(signupRequest, admin);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", newUser));
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'DEPARTMENT_HEAD')")
    public ResponseEntity<ApiResponse<Page<User>>> getUsers(
            @AuthenticationPrincipal User admin,
            @RequestParam(required = false) Long departmentId,
            Pageable pageable) {

        Page<User> users;
        if (departmentId != null) {
            users = userRepository.findByDepartment_Id(departmentId, pageable);
        } else {
            if (admin.getRole() == com.nagar.parishad.backend.enums.Role.OWNER ||
                    admin.getRole() == com.nagar.parishad.backend.enums.Role.ADMIN) {
                // For dashboard visibility, we want to see ALL users currently.
                users = userRepository.findAll(pageable);
            } else {
                // Restrict others (e.g. Dept Head) to see only their users
                users = userRepository.findByAdminId(admin.getId(), pageable);
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users));
    }

    @GetMapping("/admin/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'DEPARTMENT_HEAD')")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", user));
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id, @AuthenticationPrincipal User admin) {
        User userToDelete = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        // Notify before delete (so we have user data)
        notificationService.sendUserManagementNotification(userToDelete, "DELETED", admin);

        userRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    // Using a simpler Map approach for update for speed, or strict DTO.
    // Ideally UpdateUserRequest. Using DTO is safer.
    @PutMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    // Allowing Owner too, and ideally the user themselves if checking ID match (but
    // requirement says Admin edits all)
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id,
            @RequestBody com.nagar.parishad.backend.dto.UpdateUserRequest updateRequest,
            @AuthenticationPrincipal User admin) {
        User updatedUser = authService.updateUser(id, updateRequest, admin);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
    }

    @GetMapping("/admin/users/{id}/report")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getEmployeeReport(@PathVariable Long id) {
        long total = taskRepository.countByAssignedStaffId(id);
        long completed = taskRepository.countByAssignedStaffIdAndStatus(id,
                com.nagar.parishad.backend.enums.TaskStatus.COMPLETED);
        double rate = total > 0 ? (double) completed / total * 100 : 0;

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("totalTasks", total);
        data.put("completedTasks", completed);
        data.put("completionRate", Math.round(rate));

        return ResponseEntity.ok(ApiResponse.success("Report fetched", data));
    }
}
