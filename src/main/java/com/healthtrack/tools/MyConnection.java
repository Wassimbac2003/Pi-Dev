package com.healthtrack.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static MyConnection instance;
    private final Connection connection;

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/health_track?serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private MyConnection() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connexion etablie");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur de connexion a la base de donnees", e);
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
