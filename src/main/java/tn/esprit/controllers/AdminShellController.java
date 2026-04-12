package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.fx.Session;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class AdminShellController {

    @FXML
    private StackPane contentPane;

    @FXML
    private Button navMedicaments;

    @FXML
    private Button navFiches;

    @FXML
    private Button navOrdonnances;

    @FXML
    private void initialize() throws IOException {
        setActiveNav(navMedicaments);
        loadCenter("/tn/esprit/fxml/admin_medicaments.fxml");
    }

    @FXML
    private void showMedicaments(ActionEvent e) throws IOException {
        setActiveNav(navMedicaments);
        loadCenter("/tn/esprit/fxml/admin_medicaments.fxml");
    }

    @FXML
    private void showFiches(ActionEvent e) throws IOException {
        setActiveNav(navFiches);
        loadCenter("/tn/esprit/fxml/admin_fiches.fxml");
    }

    @FXML
    private void showOrdonnances(ActionEvent e) throws IOException {
        setActiveNav(navOrdonnances);
        loadCenter("/tn/esprit/fxml/admin_ordonnances.fxml");
    }

    private void setActiveNav(Button active) {
        for (Button b : List.of(navMedicaments, navFiches, navOrdonnances)) {
            b.getStyleClass().remove("nav-active");
        }
        if (active != null) {
            if (!active.getStyleClass().contains("nav-active")) {
                active.getStyleClass().add("nav-active");
            }
        }
    }

    private void loadCenter(String path) throws IOException {
        Parent p = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(path)));
        contentPane.getChildren().setAll(p);
    }

    @FXML
    private void onLogout(ActionEvent e) throws IOException {
        Session.clear();
        Stage stage = (Stage) contentPane.getScene().getWindow();
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/tn/esprit/fxml/login.fxml")));
        Scene scene = new Scene(root, 1080, 700);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/tn/esprit/styles/app.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Clinique — Connexion");
    }
}
