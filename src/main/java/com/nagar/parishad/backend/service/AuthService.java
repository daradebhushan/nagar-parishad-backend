package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.dto.JwtResponse;
import com.nagar.parishad.backend.dto.LoginRequest;
import com.nagar.parishad.backend.dto.SignupRequest;
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.Role;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import com.nagar.parishad.backend.repository.UserRepository;
import com.nagar.parishad.backend.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public User getUserFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (jwtUtils.validateJwtToken(token)) {
            String email = jwtUtils.getUserNameFromJwtToken(token);
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        throw new RuntimeException("Invalid Token");
    }

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    DepartmentService departmentService;

    @Autowired
    com.nagar.parishad.backend.repository.DesignationRepository designationRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    EmailService emailService;

    @Autowired
    EmailTemplateService emailTemplateService;

    @Autowired
    private NotificationService notificationService;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            User userDetails = (User) authentication.getPrincipal();

            return new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getName(),
                    userDetails.getEmail(),
                    java.util.Collections.singletonList(userDetails.getRole().name()),
                    userDetails.getDepartment() != null ? userDetails.getDepartment().getId() : null);
        } catch (org.springframework.security.core.AuthenticationException e) {
            System.out.println("DEBUG: Authentication failed for " + loginRequest.getEmail());
            // ... existing debug logic ...
            throw e;
        } catch (Exception e) {
            System.err.println("CRITICAL: Exception in authenticateUser:");
            e.printStackTrace();
            throw e;
        }
    }

    // Helper to get System Owner
    private User getSystemOwner() {
        return userRepository.findByRole(Role.OWNER).stream().findFirst().orElse(null);
    }

    private void sendEmailToUserAndOwner(String userEmail, String subject, String body) {
        // Send to User
        emailService.sendEmail(userEmail, subject, body);

        // Send copy to Owner
        User owner = getSystemOwner();
        if (owner != null && !owner.getEmail().equals(userEmail)) {
            try {
                emailService.sendEmail(owner.getEmail(), "COPY: " + subject, body);
            } catch (Exception e) {
                System.err.println("Failed to send copy email to owner: " + e.getMessage());
            }
        }
    }

    public void forgotPassword(com.nagar.parishad.backend.dto.ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Error: User not found with email: " + request.getEmail()));

        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        // Save OTP to user (reusing resetPasswordToken field)
        user.setResetPasswordToken(otp);
        user.setResetPasswordTokenExpiry(java.time.LocalDateTime.now().plusMinutes(10)); // 10 min validity
        userRepository.save(user);

        // Send email with OTP
        System.out.println("DEBUG: OTP for " + user.getEmail() + ": " + otp);

        String subject = "Password Reset OTP - Nagar Parishad Task Management";
        String body = "<p>Hello " + user.getName() + ",</p>"
                + "<p>You have requested to reset your password.</p>"
                + "<p>Your One-Time Password (OTP) is:</p>"
                + "<h2 style='color: #d97706;'>" + otp + "</h2>"
                + "<p>This OTP is valid for 10 minutes.</p>";

        emailService.sendEmail(user.getEmail(), subject, body);
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public void resetPassword(com.nagar.parishad.backend.dto.ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getResetPasswordToken() == null || !user.getResetPasswordToken().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        if (user.getResetPasswordTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        user.setPassword(encoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        // Send confirmation email
        String subject = "Password Reset Successful - Nagar Parishad Task Management";
        String body = "<p>Hello " + user.getName() + ",</p>"
                + "<p>Your password has been successfully reset.</p>"
                + "<p>If you did not perform this action, please contact the administrator immediately.</p>";

        emailService.sendEmail(user.getEmail(), subject, body);
    }

    public String validatePasswordResetToken(String token) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (user.getResetPasswordTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        return user.getEmail();
    }

    public User registerUser(SignupRequest signUpRequest, User adminUser) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User user = new User();
        user.setName(signUpRequest.getName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setRole(signUpRequest.getRole());
        user.setMobile(signUpRequest.getMobile());
        user.setAdmin(adminUser);

        if (signUpRequest.getDepartmentId() != null) {
            Department department = departmentRepository.findById(signUpRequest.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Error: Department not found."));
            user.setDepartment(department);

            // HIERARCHY LOGIC:
            // If creating STAFF, assign the DEPARTMENT_HEAD of this department as 'admin'
            // (supervisor).
            // If creating DEPT_HEAD, assign the CHIEF_OFFICER (Role.ADMIN) as 'admin'.
            // If creating ADMIN, no supervisor (or Owner).

            if (signUpRequest.getRole() == Role.STAFF) {
                // Find Dept Head for this department
                // Assuming One Dept Head per Department active
                User deptHead = userRepository.findByDepartmentIdAndRole(department.getId(), Role.DEPARTMENT_HEAD)
                        .stream().findFirst().orElse(adminUser); // Fallback to creating Admin if no Head found
                user.setAdmin(deptHead);
            } else if (signUpRequest.getRole() == Role.DEPARTMENT_HEAD) {
                // Head reports to Chief Officer (Who is creating them, likely 'adminUser')
                user.setAdmin(adminUser);
            }
        }

        if (signUpRequest.getDesignationId() != null) {
            com.nagar.parishad.backend.entity.Designation designation = designationRepository
                    .findById(signUpRequest.getDesignationId())
                    .orElseThrow(() -> new RuntimeException("Error: Designation not found"));
            user.setDesignation(designation);
        }

        User savedUser = userRepository.save(user);

        // Auto-seed Departments for new Admins
        if (savedUser.getRole() == Role.ADMIN) {
            try {
                departmentService.createDefaultDepartments(savedUser);
            } catch (Exception e) {
                System.err.println("Failed to seed default departments: " + e.getMessage());
            }
        }

        // Send Welcome Email
        try {
            String subject = "Welcome to Nagar Parishad Staff Management";
            String body = emailTemplateService.getWelcomeEmail(savedUser, signUpRequest.getPassword());
            sendEmailToUserAndOwner(savedUser.getEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        // Notify Admin and Dept Head
        notificationService.sendUserManagementNotification(savedUser, "CREATED", adminUser);

        return savedUser;
    }

    public void registerOwner(SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User user = new User();
        user.setName(signUpRequest.getName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setRole(Role.OWNER);
        user.setMobile(signUpRequest.getMobile());

        userRepository.save(user);
    }

    public void seedOwnerAccount() {
        if (!userRepository.existsByEmail("owner@govt.in")) {
            User user = new User();
            user.setName("System Owner");
            user.setEmail("owner@govt.in");
            user.setPassword(encoder.encode("password"));
            user.setRole(Role.OWNER);
            user.setMobile("0000000000");
            user.setActive(true);
            userRepository.save(user);
            System.out.println("SEED: Owner account created (owner@govt.in).");
        }
    }

    public User updateUser(Long userId, com.nagar.parishad.backend.dto.UpdateUserRequest request, User modifier) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        // Capture Old State for Notification
        User oldUser = new User();
        oldUser.setName(user.getName());
        oldUser.setEmail(user.getEmail());
        oldUser.setMobile(user.getMobile());
        oldUser.setRole(user.getRole());
        oldUser.setActive(user.isActive());
        oldUser.setDepartment(user.getDepartment());
        oldUser.setDesignation(user.getDesignation());

        if (request.getName() != null)
            user.setName(request.getName());
        if (request.getMobile() != null)
            user.setMobile(request.getMobile());
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Error: Email is already in use!");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(encoder.encode(request.getPassword()));
        }
        if (request.getActive() != null)
            user.setActive(request.getActive());

        if (request.getRole() != null)
            user.setRole(request.getRole());

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Error: Department not found"));
            user.setDepartment(department);

            // Re-evaluate hierarchy if department changed (Simplified logic)
            // Ideally we need to check if Role changed too.
            // For now, if role is STAFF, assign Head of this new Dept as admin.
            if (user.getRole() == Role.STAFF) {
                User deptHead = userRepository.findByDepartmentIdAndRole(department.getId(), Role.DEPARTMENT_HEAD)
                        .stream().findFirst().orElse(null);
                if (deptHead != null)
                    user.setAdmin(deptHead);
            }
        }

        if (request.getDesignationId() != null) {
            com.nagar.parishad.backend.entity.Designation designation = designationRepository
                    .findById(request.getDesignationId())
                    .orElseThrow(() -> new RuntimeException("Error: Designation not found"));
            user.setDesignation(designation);
        }

        if (request.getAdminId() != null) {
            User newAdmin = userRepository.findById(request.getAdminId())
                    .orElseThrow(() -> new RuntimeException("Error: New Admin User not found"));
            user.setAdmin(newAdmin);
        }

        if (request.getEmailNotifications() != null) {
            user.setEmailNotifications(request.getEmailNotifications());
        }

        User updatedUser = userRepository.save(user);

        // Send Update Notification
        try {
            String subject = "Your Profile has been Updated";
            String body = emailTemplateService.getUserUpdateEmail(oldUser, updatedUser);

            // 1. Send to User (New Email)
            emailService.sendEmail(updatedUser.getEmail(), subject, body);

            // 1b. Send to User (Old Email) - Security Notification
            if (!oldUser.getEmail().equals(updatedUser.getEmail())) {
                emailService.sendEmail(oldUser.getEmail(), "Security Alert: Email Address Changed", body);
            }

            // 2. Send to Modifier (Admin) if different
            if (modifier != null && !modifier.getEmail().equals(updatedUser.getEmail())) {
                emailService.sendEmail(modifier.getEmail(), "Admin Notification: User Profile Updated", body);
            }

            // 3. Send Copy to Owner (Access via Repository or Service if needed, keeping
            // existing logic if any)
            User owner = getSystemOwner();
            if (owner != null && !owner.getEmail().equals(updatedUser.getEmail())
                    && (modifier == null || !owner.getEmail().equals(modifier.getEmail()))) {
                // Avoid duplicate if Owner IS the modifier
                emailService.sendEmail(owner.getEmail(), "COPY: " + subject, body);
            }

            // 4. Send to Supervisor (Assigned Admin) if User updated themselves
            // If modifier IS the user, then notify their supervisor
            if (modifier != null && modifier.getId().equals(updatedUser.getId())) {
                User supervisor = updatedUser.getAdmin();
                if (supervisor != null && !supervisor.getEmail().equals(updatedUser.getEmail()) &&
                        (owner == null || !supervisor.getEmail().equals(owner.getEmail()))) {
                    // Check if supervisor is NOT owner (already sent copy)
                    emailService.sendEmail(supervisor.getEmail(), "Staff Profile Updated: " + updatedUser.getName(),
                            body);
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to send user update email: " + e.getMessage());
            e.printStackTrace();
        }

        // Notify Admin and Dept Head (App + Email)
        notificationService.sendUserManagementNotification(updatedUser, "UPDATED", modifier);

        return updatedUser;
    }
}
