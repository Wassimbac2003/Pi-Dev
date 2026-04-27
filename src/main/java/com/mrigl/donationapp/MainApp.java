package com.mrigl.donationapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainApp extends Application {

    public static final double SCENE_WIDTH = 1400;
    public static final double SCENE_HEIGHT = 860;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                MainApp.class.getResource("/com/mrigl/donationapp/main.fxml")));
        Scene scene = new Scene(loader.load(), SCENE_WIDTH, SCENE_HEIGHT);
        scene.getStylesheets().add(Objects.requireNonNull(
                MainApp.class.getResource("/com/mrigl/donationapp/app.css")).toExternalForm());
        stage.setTitle("Annonces et donations");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}