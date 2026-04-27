package org.example;

import Services.SocketServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainAdmin extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // ✅ DÉMARRER LE SERVEUR (ADMIN SEULEMENT)
        SocketServer serveur = SocketServer.getInstance();
        if (!serveur.isRunning()) {
            serveur.demarrer();
            Thread.sleep(500); // Attendre démarrage
            System.out.println("✅ [MAIN ADMIN] Serveur socket démarré");
        }

        Parent root = FXMLLoader.load(getClass().getResource("/AdminLayout.fxml"));
        Scene scene = new Scene(root, 1400, 860);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setTitle("VitalTech - Admin");
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            SocketServer.getInstance().arreter();
            System.out.println("🛑 [MAIN ADMIN] Serveur socket arrêté");
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}