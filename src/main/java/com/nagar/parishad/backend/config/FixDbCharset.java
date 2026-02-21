package com.nagar.parishad.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FixDbCharset implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Running DB Charset Fixer...");
        try {
            // Alter Database default
            jdbcTemplate
                    .execute("ALTER DATABASE nagar_parishad_db_final CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

            // Alter specific tables that store Marathi text
            jdbcTemplate.execute("ALTER TABLE departments CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            jdbcTemplate
                    .execute("ALTER TABLE complaint_types CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            jdbcTemplate.execute("ALTER TABLE complaints CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

            System.out.println("DB Charset Fixer Completed Successfully.");
        } catch (Exception e) {
            System.err.println("DB Charset Fixer Failed (might be already fixed or permissions): " + e.getMessage());
        }
    }
}
