package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtil {

    private static final String DATABASE_URL = "jdbc:sqlite:C:/Program Files/Apache Software Foundation/Tomcat 9.0/simple_bank.db";

    private static boolean driverLoaded = false;

    public static Connection getConnection() throws SQLException {
        loadDriver(); // Ensure driver is loaded
        return DriverManager.getConnection(DATABASE_URL);
    }

    private static void loadDriver() throws SQLException {
        if (!driverLoaded) {
            try {
                // The newInstance() call is deprecated in modern JDBC drivers
                // Class.forName() is sufficient to register the driver
                Class.forName("org.sqlite.JDBC");
                driverLoaded = true;
                System.out.println("SQLite JDBC Driver Loaded Successfully.");
            } catch (ClassNotFoundException e) {
                System.err.println("CRITICAL: SQLite JDBC driver class not found!");
                System.err
                        .println("Ensure sqlite-jdbc-*.jar is in Tomcat's lib directory or the webapp's WEB-INF/lib.");
                throw new SQLException("SQLite JDBC Driver (org.sqlite.JDBC) not found.", e);
            }
        }
    }

    // Helper to close resources quietly
    public static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                // Log or ignore
                System.err.println("Warn: Failed to close resource: " + e.getMessage());
            }
        }
    }
}