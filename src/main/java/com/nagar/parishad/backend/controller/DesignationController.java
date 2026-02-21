package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ApiResponse;
import com.nagar.parishad.backend.entity.Designation;
import com.nagar.parishad.backend.service.DesignationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/designations")
public class DesignationController {

    @Autowired
    DesignationService designationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Designation>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Fetched designations", designationService.getAll()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Designation>> create(@RequestBody Map<String, String> body) {
        return ResponseEntity
                .ok(ApiResponse.success("Created designation", designationService.createDesignation(body.get("name"))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        designationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted designation", null));
    }
}
