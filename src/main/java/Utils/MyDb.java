package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDb {
    private Connection connection;
    /** Même logique que com.healthtrack.tools.MyConnection — ne pas confondre avec annonce_db. */
    private final String DB_URL = System.getenv().getOrDefault(
            "HEALTHTRACK_DB_URL",
            "jdbc:mysql://127.0.0.1:3306/healthcare?serverTimezone=UTC");
    private final String USER = System.getenv().getOrDefault("HEALTHTRACK_DB_USER", "root");
    private final String PASS = System.getenv().getOrDefault("HEALTHTRACK_DB_PASSWORD", "");
    private static MyDb instance;

    private MyDb() {
        try {
            this.connection = DriverManager.getConnection(DB_URL, USER, PASS);
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static MyDb getInstance() {
        if (instance == null)
            instance = new MyDb();
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
