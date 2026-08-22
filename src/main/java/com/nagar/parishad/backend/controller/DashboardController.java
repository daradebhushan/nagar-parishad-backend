package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ApiResponse;
import com.nagar.parishad.backend.dto.StatsDto;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/stats")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'NAGARADHYAKSHA', 'DEPARTMENT_HEAD', 'STAFF')")
    public ResponseEntity<ApiResponse<StatsDto>> getDashboardStats(@AuthenticationPrincipal User user) {
        StatsDto stats = dashboardService.getStats(user);
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats fetched", stats));
    }
}
