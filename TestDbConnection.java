
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestDbConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/nagar_parishad_db_final?useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = args.length > 0 ? args[0] : "Bbd@1415";

        System.out.println("Testing connection to: " + url);
        System.out.println("User: " + user);
        System.out.println("Password: " + password); // Be careful with logs in real prod

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("SUCCESS: Connection established!");
        } catch (SQLException e) {
            System.out.println("FAILURE: Could not connect.");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
