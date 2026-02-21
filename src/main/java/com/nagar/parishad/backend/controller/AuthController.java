package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ApiResponse;
import com.nagar.parishad.backend.dto.JwtResponse;
import com.nagar.parishad.backend.dto.LoginRequest;
import com.nagar.parishad.backend.dto.SignupRequest;
import com.nagar.parishad.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.util.JwtUtils;
import com.nagar.parishad.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Login Successful", jwtResponse));
    }

    // Endpoint for initial Owner setup (Can be disabled later or secured)
    @PostMapping("/setup-owner")
    public ResponseEntity<ApiResponse<String>> registerOwner(@Valid @RequestBody SignupRequest signUpRequest) {
        authService.registerOwner(signUpRequest);
        return ResponseEntity.ok(ApiResponse.success("Owner registered successfully", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody com.nagar.parishad.backend.dto.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset link sent to your email", null));
    }

    @PostMapping("/magic-login")
    public ResponseEntity<?> magicLogin(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || !jwtUtils.validateJwtToken(token)) {
            return ResponseEntity.badRequest().body("Invalid or expired magic link");
        }

        String username = jwtUtils.getUserNameFromJwtToken(token);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate full session token
        String jwt = jwtUtils.generateJwtToken(user);

        return ResponseEntity.ok(new JwtResponse(jwt,
                user.getId(),
                user.getName(),
                user.getEmail(),
                java.util.Collections.singletonList(user.getRole().name()),
                user.getDepartment() != null ? user.getDepartment().getId() : null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody com.nagar.parishad.backend.dto.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse<String>> validateToken(@RequestParam String token) {
        String email = authService.validatePasswordResetToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token is valid", email));
    }
}
