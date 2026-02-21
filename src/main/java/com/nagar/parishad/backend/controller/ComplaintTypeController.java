package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ComplaintTypeDTO;
import com.nagar.parishad.backend.service.ComplaintTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/complaint-types")

public class ComplaintTypeController {

    @Autowired
    private ComplaintTypeService complaintTypeService;

    @GetMapping
    public ResponseEntity<List<ComplaintTypeDTO>> getAllComplaintTypes() {
        return ResponseEntity.ok(complaintTypeService.getAllComplaintTypes());
    }

    @GetMapping("/department/{deptId}")
    public ResponseEntity<List<ComplaintTypeDTO>> getByDepartment(@PathVariable Long deptId) {
        return ResponseEntity.ok(complaintTypeService.getComplaintTypesByDepartment(deptId));
    }

    @PostMapping
    public ResponseEntity<ComplaintTypeDTO> createComplaintType(@RequestBody ComplaintTypeDTO dto) {
        return ResponseEntity.ok(complaintTypeService.createComplaintType(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComplaintTypeDTO> updateComplaintType(@PathVariable Long id,
            @RequestBody ComplaintTypeDTO dto) {
        return ResponseEntity.ok(complaintTypeService.updateComplaintType(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplaintType(@PathVariable Long id) {
        complaintTypeService.deleteComplaintType(id);
        return ResponseEntity.ok().build();
    }
}
