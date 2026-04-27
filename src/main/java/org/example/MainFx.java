package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MainFx extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // ✅ PAS DE SERVEUR ICI - Patient est JUSTE un client

        FXMLLoader fxmlLoader = new FXMLLoader(MainFx.class.getResource("/MainLayout.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("VitaTech Medical Center - Patient");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}