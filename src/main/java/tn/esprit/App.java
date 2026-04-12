package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.tools.MyConnection;

import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        MyConnection.getInstance();
        Parent root = FXMLLoader.load(Objects.requireNonNull(
                getClass().getResource("/tn/esprit/fxml/login.fxml")));
        Scene scene = new Scene(root, 1080, 700);
        stage.setTitle("Clinique — Connexion");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
