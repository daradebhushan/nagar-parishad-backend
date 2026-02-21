package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.dto.DepartmentRequest;
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    @Autowired
    DepartmentRepository departmentRepository;

    public Department createDepartment(DepartmentRequest request, User admin) {
        Department department = new Department();
        department.setName(request.getName());
        department.setNameMr(request.getNameMr());
        department.setNameHi(request.getNameHi());
        department.setChatbotEnabled(request.isChatbotEnabled());
        department.setSubQuestions(request.getSubQuestions());
        department.setAdmin(admin);
        return departmentRepository.save(department);
    }

    public Page<Department> getDepartmentsByAdmin(Long adminId, Pageable pageable) {
        return departmentRepository.findByAdminId(adminId, pageable);
    }

    public List<Department> getAllDepartmentsByAdmin(Long adminId) {
        return departmentRepository.findByAdminId(adminId);
    }

    public Department updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        department.setName(request.getName());
        department.setNameMr(request.getNameMr());
        department.setNameHi(request.getNameHi());
        department.setChatbotEnabled(request.isChatbotEnabled());
        department.setChatbotEnabled(request.isChatbotEnabled());
        department.setSubQuestions(request.getSubQuestions());
        if (request.getActive() != null) {
            department.setActive(request.getActive());
        }
        return departmentRepository.save(department);
    }

    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
    }

    @Autowired
    com.nagar.parishad.backend.repository.ComplaintTypeRepository complaintTypeRepository;

    @Autowired
    com.nagar.parishad.backend.repository.TaskRepository taskRepository;

    @Autowired
    com.nagar.parishad.backend.repository.UserRepository userRepository;

    public void deleteDepartment(Long id) {
        // Check 1: Users assigned to this department
        long userCount = userRepository.countByDepartmentId(id);
        if (userCount > 0) {
            throw new RuntimeException(
                    "Cannot delete department. There are " + userCount + " staff members assigned to it.");
        }

        // Check 2: Tasks assigned to this department
        long taskCount = taskRepository.countByDepartmentId(id);
        if (taskCount > 0) {
            throw new RuntimeException("Cannot delete department. There are " + taskCount + " tasks linked to it.");
        }

        // Safe to delete:
        // 1. Delete Linked Complaint Types (Cascade logic manually)
        List<com.nagar.parishad.backend.entity.ComplaintType> types = complaintTypeRepository.findByDepartmentId(id);
        complaintTypeRepository.deleteAll(types);

        // 2. Delete Department
        departmentRepository.deleteById(id);
    }

    public java.util.Map<String, Integer> deleteInactiveDepartments(Long adminId) {
        List<Department> inactiveDepts = departmentRepository.findByAdminId(adminId).stream()
                .filter(d -> !d.isActive())
                .toList();

        int deleted = 0;
        int skipped = 0;

        for (Department dept : inactiveDepts) {
            try {
                deleteDepartment(dept.getId());
                System.out.println("Deleted inactive department: " + dept.getName());
                deleted++;
            } catch (Exception e) {
                System.err.println("Skipping inactive department " + dept.getName() + ": " + e.getMessage());
                skipped++;
            }
        }

        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        stats.put("deleted", deleted);
        stats.put("skipped", skipped);
        return stats;
    }

    public void createDefaultDepartments(User admin) {
        // 1. City Cleanliness (शहर स्वच्छता)
        createDept(admin, "City Cleanliness", "शहर स्वच्छता",
                "[\"स्वच्छता होत नाही लवकर\", \"मेलेले जनावर तक्रार\", \"नाली गटार स्वच्छता तक्रार\", \"रस्त्यावर कचरा तक्रार\", \"रस्त्यावरील बांधकाम\", \"कचरा जाळणे तक्रार\", \"इतर तक्रार\"]");

        // 2. Water Supply (पाणी पुरवठा)
        createDept(admin, "Water Supply", "पाणी पुरवठा",
                "[\"पाणी येत नाही तक्रार\", \"पाईपलाईन लिकेज तक्रार\", \"पाणी दबाव तक्रार\", \"अनधिकृत नळ तक्रार\", \"पाणी खराब येत असल्याबाबत\"]");

        // 3. Electricity (विद्युत)
        createDept(admin, "Electricity", "विद्युत",
                "[\"विद्युत पोलवरील दिवा बंद\", \"पोल दिवा सतत चालू\", \"हायमास्ट बंद असल्याबाबत\"]");

        // 4. Public Works (सार्वजनिक बांधकाम)
        createDept(admin, "Public Works", "सार्वजनिक बांधकाम",
                "[\"चेंबर / ढापा / मॅनहोल तुटलेले\"]");

        // 5. Waste Vehicle (घंटागाडी)
        createDept(admin, "Waste Vehicle (Ghanta Gadi)", "घंटागाडी",
                "[\"घंटागाडी येत नाही\", \"मेलेले प्राणी उचलणे बाबत\", \"गटार साफ / चोक अप दुरुस्ती\", \"साचलेला कचरा उचलणे\"]");
    }

    private void createDept(User admin, String name, String nameMr, String subQuestions) {
        // Idempotency Check: Existing check
        List<Department> existingDepts = departmentRepository.findByAdminId(admin.getId());
        Department existing = existingDepts.stream()
                .filter(d -> d.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            Department dept = new Department();
            dept.setName(name);
            dept.setNameMr(nameMr);
            dept.setSubQuestions(subQuestions);
            dept.setAdmin(admin);
            dept.setChatbotEnabled(true);
            dept.setActive(true);
            departmentRepository.save(dept);
        } else {
            // Update existing to ensure Marathi text is fixed
            System.out.println("Updating existing department: " + name);
            existing.setNameMr(nameMr);
            existing.setSubQuestions(subQuestions);
            departmentRepository.save(existing);
        }
    }
}
