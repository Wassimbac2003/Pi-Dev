package com.healthtrack.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static MyConnection instance;
    private final Connection connection;

    /**
     * Base dédiée à l’appli HealthTrack — pas la même que annonce_db (donations).
     * Surcharge : variable d’environnement HEALTHTRACK_DB_URL.
     */
    private static final String URL = System.getenv().getOrDefault(
            "HEALTHTRACK_DB_URL",
            "jdbc:mysql://127.0.0.1:3306/healthcare?serverTimezone=UTC");
    private static final String USERNAME = System.getenv().getOrDefault("HEALTHTRACK_DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("HEALTHTRACK_DB_PASSWORD", "");

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
