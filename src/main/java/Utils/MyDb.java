package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDb {
    private Connection connection;
    private final String DB_URL = "jdbc:mysql://localhost:3306/health_track";
    private final String USER = "root";
    private final String PASS = "";
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
