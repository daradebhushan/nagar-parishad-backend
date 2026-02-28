package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ApiResponse;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.service.EmailService;
import com.nagar.parishad.backend.service.PdfReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/report")

public class ReportController {

    @Autowired
    private PdfReportService pdfReportService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/email")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<ApiResponse<String>> emailAdminReport(@AuthenticationPrincipal User admin) {
        try {
            byte[] pdfBytes = pdfReportService.generateAdminReport(admin);

            String subject = "Nagar Parishad - Admin Executive Report";
            String body = "<h3>Admin Executive Report</h3><p>Please find attached the latest executive report containing task summaries and staff performance metrics.</p>";

            emailService.sendEmailWithAttachment(
                    admin.getEmail(),
                    subject,
                    body,
                    pdfBytes,
                    "Admin_Report_" + java.time.LocalDate.now() + ".pdf");

            return ResponseEntity.ok(ApiResponse.success("Report sent successfully to " + admin.getEmail(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate report: " + e.getMessage()));
        }
    }
}
