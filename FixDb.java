import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class FixDb {
    public static void main(String[] args) {
        // Updated to correct database name from application.properties
        String url = "jdbc:mysql://localhost:3306/nagar_parishad_db_final?useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "root";

        try (Connection con = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to DB: nagar_parishad_db_final");

            // 1. Add missing 'type' column to chatbot_config
            try {
                String sql = "ALTER TABLE chatbot_config ADD COLUMN type VARCHAR(50) DEFAULT 'TEXT' NOT NULL";
                try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                    pstmt.executeUpdate();
                    System.out.println("SUCCESS: Added 'type' column to chatbot_config");
                }
            } catch (Exception e) {
                System.out.println("Column 'type' likely already exists or error: " + e.getMessage());
            }

            // 2. Fix admin_id default value in chatbot_sessions if needed (altering to
            // allow null or default)
            // Or just clean up old sessions
            String sqlCleanup = "DELETE FROM chatbot_sessions";
            try (PreparedStatement pstmt = con.prepareStatement(sqlCleanup)) {
                int affected = pstmt.executeUpdate();
                System.out.println("Cleaned up " + affected + " old chatbot sessions");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
