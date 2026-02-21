package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.dto.ChatbotConfigDTO;
import com.nagar.parishad.backend.dto.ComplaintDTO;
import com.nagar.parishad.backend.dto.ComplaintSubmissionDTO;
import com.nagar.parishad.backend.dto.ComplaintTypeDTO;
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import com.nagar.parishad.backend.service.ChatbotConfigService;
import com.nagar.parishad.backend.service.ComplaintService;
import com.nagar.parishad.backend.service.ComplaintTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")

public class PublicController {

    @Autowired
    private ChatbotConfigService chatbotConfigService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ComplaintTypeService complaintTypeService;

    @Autowired
    private ComplaintService complaintService;

    @GetMapping("/config")
    public ResponseEntity<List<ChatbotConfigDTO>> getChatbotConfig() {
        return ResponseEntity.ok(chatbotConfigService.getAllConfigs());
    }

    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getActiveDepartments() {
        // Assuming we want all departments for now. Ideally filter by active.
        // Department entity has 'active' field.
        // For simplicity returning all, front-end (chatbot) can filter or we add repo
        // method.
        // Let's rely on standard findAll for now or filter in stream if needed.
        // Actually best to filter active=true.
        // Convert to simplified DTO if needed to hide Admin info?
        // Department entity has @JsonIgnoreProperties covering sensitive fields.
        return ResponseEntity.ok(departmentRepository.findAll().stream()
                .filter(Department::isActive)
                .collect(java.util.stream.Collectors.toList()));
    }

    @GetMapping("/complaint-types/{deptId}")
    public ResponseEntity<List<ComplaintTypeDTO>> getComplaintTypes(@PathVariable Long deptId) {
        // Retrieve active types only
        List<ComplaintTypeDTO> types = complaintTypeService.getComplaintTypesByDepartment(deptId);
        return ResponseEntity
                .ok(types.stream().filter(ComplaintTypeDTO::isActive).collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping("/complaints")
    public ResponseEntity<ComplaintDTO> submitComplaint(@RequestBody ComplaintSubmissionDTO dto) {
        return ResponseEntity.ok(complaintService.submitComplaint(dto));
    }
}
