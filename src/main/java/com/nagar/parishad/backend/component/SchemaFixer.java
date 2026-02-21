package com.nagar.parishad.backend.component;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Component
public class SchemaFixer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Running Schema Fixer...");
        try {
            // 1. Fix Chatbot Config Constraints (PRIORITY)
            try {
                jdbcTemplate.execute("ALTER TABLE chatbot_config DROP INDEX UK_ojtao1lpyoxh333blqw7q5re0");
                System.out.println("Schema Fixed: Dropped incorrect constraint UK_ojtao1lpyoxh333blqw7q5re0");
            } catch (Exception e) {
                System.out.println("Constraint drop skipped: " + e.getMessage());
            }

            try {
                // Ensure unique on (admin_id, conf_key)
                jdbcTemplate.execute(
                        "ALTER TABLE chatbot_config ADD CONSTRAINT uk_chatbot_config_admin_key UNIQUE (admin_id, conf_key)");
                System.out.println("Schema Fixed: Added correct constraint uk_chatbot_config_admin_key");
            } catch (Exception e) {
                System.out.println("Constraint add skipped: " + e.getMessage());
            }

            // 2. Fix Chatbot Sessions
            jdbcTemplate.execute("ALTER TABLE chatbot_sessions MODIFY state VARCHAR(100)");
            System.out.println("Schema Fixed: chatbot_sessions.state resized to 100");

            // 3. Verification Insert
            jdbcTemplate.update("INSERT INTO chatbot_sessions (mobile_number, state, language) VALUES (?, ?, ?)",
                    "+91000TEST", "LANGUAGE_SELECTION_TEST_LONG_STRING_VERIFICATION", "en");
            System.out.println("Schema Verification: Test Insert SUCCESS");

        } catch (Exception e) {
            System.err.println("Schema Fixer Error: " + e.getMessage());
        }
    }
}
