package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.entity.Complaint;
import com.nagar.parishad.backend.entity.ComplaintType;
import com.nagar.parishad.backend.repository.ComplaintRepository;
import com.nagar.parishad.backend.repository.ComplaintTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/migration")
public class FixTypeController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintTypeRepository complaintTypeRepository;

    @PostMapping("/fix-types")
    public String fixTypes() {
        List<Complaint> complaints = complaintRepository.findAll();
        int updatedCount = 0;
        StringBuilder log = new StringBuilder();

        for (Complaint c : complaints) {
            if (c.getComplaintType() == null || c.getComplaintType().getId() == 1L) { // Assuming 1L might be phantom
                // Find valid type for this Dept
                if (c.getDepartment() != null) {
                    List<ComplaintType> types = complaintTypeRepository.findByDepartmentId(c.getDepartment().getId());
                    if (!types.isEmpty()) {
                        c.setComplaintType(types.get(0));
                        complaintRepository.save(c);
                        updatedCount++;
                        log.append("Fixed Type for CMP ").append(c.getComplaintNo()).append(" -> ")
                                .append(types.get(0).getNameEn()).append("\n");
                    } else {
                        log.append("No types found for Dept ").append(c.getDepartment().getName()).append(" (CMP: ")
                                .append(c.getComplaintNo()).append(")\n");
                    }
                }
            }
        }
        return "Type Fix Completed. Updated: " + updatedCount + "\n" + log.toString();
    }
}
