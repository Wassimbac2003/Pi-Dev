package com.healthtrack.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class AppNavigator {
    private static Stage stage;

    private AppNavigator() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void switchRoot(String fxmlPath) {
        if (stage == null) {
            throw new IllegalStateException("Primary stage not initialized.");
        }

        try {
            FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1400, 860);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            String stylesheet = AppNavigator.class.getResource("/styles/dashboard.css").toExternalForm();
            if (!scene.getStylesheets().contains(stylesheet)) {
                scene.getStylesheets().add(stylesheet);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de charger la vue " + fxmlPath, e);
        }
    }
}
