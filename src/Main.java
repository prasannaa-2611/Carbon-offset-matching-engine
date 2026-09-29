import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Main {

    // AIVEN DATABASE DETAILS
    static String host = "mysql-d45df12-prasanna07abburi-3c62.a.aivencloud.com";
    static String port = "13973";
    static String database = "defaultdb";
    static String username = "avnadmin";
    static String password = System.getenv("DB_PASSWORD");

    public static void main(String[] args) {

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                   + "?sslMode=REQUIRED";

        try {
            Connection con = DriverManager.getConnection(url, username, password);

            System.out.println("Connected to Aiven MySQL successfully!");

            Statement stmt = con.createStatement();

            // Create carbon projects table
            String createTable =
                    "CREATE TABLE IF NOT EXISTS carbon_projects (" +
                    "project_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "project_name VARCHAR(100) NOT NULL," +
                    "project_type VARCHAR(50) NOT NULL," +
                    "location VARCHAR(50) NOT NULL," +
                    "available_credits INT NOT NULL," +
                    "price_per_credit DOUBLE NOT NULL," +
                    "verification_standard VARCHAR(100)," +
                    "vintage_year INT" +
                    ")";

            stmt.executeUpdate(createTable);

            System.out.println("carbon_projects table created!");

            // Insert sample projects
            String insertData =
                    "INSERT INTO carbon_projects " +
                    "(project_name, project_type, location, available_credits, " +
                    "price_per_credit, verification_standard, vintage_year) VALUES " +

                    "('Solar Gujarat', 'Renewable Energy', 'India', 1000, 450, 'Gold Standard', 2024)," +
                    "('Wind Tamil Nadu', 'Renewable Energy', 'India', 2000, 420, 'Verra', 2023)," +
                    "('Forest Karnataka', 'Forestry', 'India', 500, 380, 'Verra', 2024)," +
                    "('Solar Rajasthan', 'Renewable Energy', 'India', 800, 470, 'Gold Standard', 2023)," +
                    "('Wind Brazil', 'Renewable Energy', 'Brazil', 1500, 400, 'Verra', 2024)";

            stmt.executeUpdate(insertData);

            System.out.println("Sample carbon projects inserted!");

            con.close();

            System.out.println("Database setup completed!");

        } catch (Exception e) {
            System.out.println("Something went wrong!");
            e.printStackTrace();
        }
    }
}