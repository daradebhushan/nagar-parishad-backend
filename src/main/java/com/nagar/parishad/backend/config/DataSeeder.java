package com.nagar.parishad.backend.config;

import com.nagar.parishad.backend.entity.ComplaintType;
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.Role;
import com.nagar.parishad.backend.repository.ComplaintTypeRepository;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import com.nagar.parishad.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.nagar.parishad.backend.entity.TenantTwilioConfig;
import com.nagar.parishad.backend.repository.TenantTwilioConfigRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataSeeder implements CommandLineRunner {

        @Autowired
        private DepartmentRepository departmentRepository;

        @Autowired
        private ComplaintTypeRepository complaintTypeRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private TenantTwilioConfigRepository tenantTwilioConfigRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) throws Exception {
                // Enforce Single Admin: daradebhushan15+admin@gmail.com
                String email = "daradebhushan15+admin@gmail.com";
                User admin = userRepository.findByEmail(email).orElse(null);

                if (admin == null) {
                        admin = new User();
                        admin.setEmail(email);
                        admin.setPassword(passwordEncoder.encode("Bbd@123"));
                        admin.setName("Bhushan Admin");
                        admin.setRole(Role.ADMIN);
                        admin.setMobile("9999999999");
                        admin.setActive(true);
                        admin = userRepository.save(admin);
                } else {
                        // FORCE UPDATE PASSWORD
                        admin.setPassword(passwordEncoder.encode("Bbd@123"));
                        userRepository.save(admin);
                }

                // Seed Departments for this admin
                seedDepartment(admin, "City Sanitation", "शहर स्वच्छता",
                                new String[] {
                                                "Garbage not picked up", "स्वच्छता होत नाही लवकर",
                                                "Dead animal", "मेलेले जनावर तक्रार",
                                                "Drain cleaning", "नाली गटार स्वच्छता तक्रार",
                                                "Trash on road", "रस्त्यावर कचरा तक्रार",
                                                "Construction material on road", "रस्त्यावरील बांधकाम",
                                                "Burning garbage", "कचरा जाळणे तक्रार",
                                                "Other", "इतर तक्रार"
                                });

                seedDepartment(admin, "Water Supply", "पाणी पुरवठा",
                                new String[] {
                                                "No water", "पाणी येत नाही तक्रार",
                                                "Pipeline leakage", "पाईपलाईन लिकेज तक्रार",
                                                "Low pressure", "पाणी दबाव तक्रार",
                                                "Illegal connection", "अनधिकृत नळ तक्रार",
                                                "Dirty water", "पाणी खराब येत असल्याबाबत"
                                });

                seedDepartment(admin, "Electricity", "विद्युत",
                                new String[] {
                                                "Street light off", "विद्युत पोलवरील दिवा बंद",
                                                "Street light always on", "पोल दिवा सतत चालू",
                                                "Highmast off", "हायमास्ट बंद असल्याबाबत"
                                });

                seedDepartment(admin, "Public Works", "सार्वजनिक बांधकाम",
                                new String[] {
                                                "Damaged Chamber/Manhole", "चेंबर / ढापा / मॅनहोल तुटलेले"
                                });

                // Seed Twilio Config for this admin ONLY IF they don't have one and sandbox is
                // free
                TenantTwilioConfig existingAdminConfig = tenantTwilioConfigRepository.findByAdminId(admin.getId())
                                .orElse(null);
                if (existingAdminConfig == null) {
                        // check if sandbox is free
                        String sandboxNum = "whatsapp:+14155238886";
                        boolean isSandboxUsed = tenantTwilioConfigRepository.findByPhoneNumber(sandboxNum).isPresent();
                        if (!isSandboxUsed) {
                                TenantTwilioConfig config = new TenantTwilioConfig();
                                config.setAdmin(admin);
                                config.setAccountSid("AC349613979568d34da4845c4a4d28f13d");
                                config.setAuthToken("ea718eeea53c3c746007655e9f22d3f0");
                                config.setPhoneNumber(sandboxNum);
                                config.setActive(true);
                                tenantTwilioConfigRepository.save(config);
                        }
                }
        }

        private void seedDepartment(User admin, String nameEn, String nameMr, String[] types) {
                // Find by nameEn to avoid duplicates
                Optional<Department> existing = departmentRepository.findAll().stream()
                                .filter(d -> d.getName().equalsIgnoreCase(nameEn))
                                .findFirst();

                Department dept;
                if (existing.isPresent()) {
                        dept = existing.get();
                        if (dept.getNameMr() == null) {
                                dept.setNameMr(nameMr);
                                departmentRepository.save(dept);
                        }
                } else {
                        dept = new Department();
                        dept.setName(nameEn);
                        dept.setNameMr(nameMr);
                        dept.setAdmin(admin);
                        dept.setActive(true);
                        dept = departmentRepository.save(dept);
                }

                // Seed Types
                for (int i = 0; i < types.length; i += 2) {
                        String tNameEn = types[i];
                        String tNameMr = types[i + 1];

                        // Check if exists in this dept
                        boolean typeExists = complaintTypeRepository.findByDepartmentId(dept.getId()).stream()
                                        .anyMatch(t -> t.getNameEn().equalsIgnoreCase(tNameEn));

                        if (!typeExists) {
                                ComplaintType type = new ComplaintType();
                                type.setDepartment(dept);
                                type.setNameEn(tNameEn);
                                type.setNameMr(tNameMr);
                                type.setActive(true);
                                complaintTypeRepository.save(type);
                        }
                }
        }
}
