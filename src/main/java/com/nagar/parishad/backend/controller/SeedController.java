package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.entity.ComplaintType;
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.repository.ComplaintTypeRepository;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/seeder")
public class SeedController {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ComplaintTypeRepository complaintTypeRepository;

    @PostMapping("/seed-types")
    public String seedTypes() {
        List<Department> departments = departmentRepository.findAll();
        StringBuilder log = new StringBuilder();
        int added = 0;

        for (Department dept : departments) {
            // Check if types exist for this dept
            List<ComplaintType> existing = complaintTypeRepository.findByDepartmentId(dept.getId());
            if (existing.isEmpty()) {
                // Determine default types based on Dept Name (rough match)
                if (dept.getName().toLowerCase().contains("sanitation")
                        || dept.getName().toLowerCase().contains("waste")) {
                    addType(dept, "Garbage Issue", "कचरा समस्या");
                    addType(dept, "Drainage Issue", "गटार समस्या");
                    added += 2;
                } else if (dept.getName().toLowerCase().contains("water")) {
                    addType(dept, "No Water Supply", "पाणी पुरवठा नाही");
                    addType(dept, "Leakage", "गळती");
                    added += 2;
                } else if (dept.getName().toLowerCase().contains("electric")) {
                    addType(dept, "Street Light Off", "पथदिवे बंद");
                    added++;
                } else {
                    addType(dept, "General Issue", "सामान्य समस्या");
                    added++;
                }
                log.append("Seeded types for Dept: ").append(dept.getName()).append("\n");
            } else {
                log.append("Dept ").append(dept.getName()).append(" already has types.\n");
            }
        }
        return "Seeding Complete. Added: " + added + "\n" + log.toString();
    }

    @PostMapping("/seed-sub-questions")
    public String seedSubQuestions() {
        List<Department> departments = departmentRepository.findAll();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        int updated = 0;

        for (Department dept : departments) {
            if (dept.getName().toLowerCase().contains("electric") || dept.getName().toLowerCase().contains("light")) {
                try {
                    List<String> questions = java.util.Arrays.asList(
                            "विद्युत पोलवरील दिवा बंद",
                            "पोल दिवा सतत चालू",
                            "हायमास्ट बंद असल्याबाबत");
                    dept.setSubQuestions(mapper.writeValueAsString(questions));
                    departmentRepository.save(dept);
                    updated++;

                    // ALSO SEED AS COMPLAINT TYPES
                    for (String q : questions) {
                        addType(dept, q, q); // Use same string for En and Mr for now to ensure match
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return "Sub-Questions Seeded for " + updated + " departments.";
    }

    private void addType(Department dept, String nameEn, String nameMr) {
        ComplaintType type = new ComplaintType();
        type.setDepartment(dept);
        type.setNameEn(nameEn);
        type.setNameMr(nameMr);
        type.setActive(true);
        complaintTypeRepository.save(type);
    }
}
