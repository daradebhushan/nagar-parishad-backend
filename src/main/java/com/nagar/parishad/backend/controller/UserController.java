package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ApiResponse;
import com.nagar.parishad.backend.dto.UpdateUserRequest;
import com.nagar.parishad.backend.dto.UserUpdateResponse;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.service.AuthService;
import com.nagar.parishad.backend.util.JwtUtils;
import com.nagar.parishad.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    AuthService authService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtUtils jwtUtils;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> getProfile(@AuthenticationPrincipal User user) {
        // Refresh user from DB to get latest state
        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", currentUser));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserUpdateResponse>> updateProfile(
            @Valid @RequestBody UpdateUserRequest updateRequest,
            @AuthenticationPrincipal User user) {
        // User updating their own profile
        User updatedUser = authService.updateUser(user.getId(), updateRequest, user);

        // Generate new token for the updated user
        String newToken = jwtUtils.generateJwtToken(updatedUser);

        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully",
                new UserUpdateResponse(updatedUser, newToken)));
    }

    @Autowired
    com.nagar.parishad.backend.service.FileStorageService fileStorageService;

    @PostMapping("/profile-photo")
    public ResponseEntity<ApiResponse<String>> uploadProfilePhoto(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal User user) {

        String fileName = fileStorageService.storeProfilePhoto(file);

        // Update User
        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        currentUser.setProfilePhoto(fileName);
        userRepository.save(currentUser);

        return ResponseEntity.ok(ApiResponse.success("Profile photo uploaded successfully", fileName));
    }
}
