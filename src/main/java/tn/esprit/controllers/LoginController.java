package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.fx.Session;
import tn.esprit.services.ServiceUser;

import java.util.List;
import java.util.Objects;

public class LoginController {

    @FXML
    private ChoiceBox<String> portalChoice;

    @FXML
    private ComboBox<User> userCombo;

    @FXML
    private Label errorLabel;

    private final ServiceUser serviceUser = new ServiceUser();

    @FXML
    private void initialize() {
        portalChoice.setItems(FXCollections.observableArrayList(
                "Administration",
                "Médecin",
                "Patient"
        ));
        portalChoice.getSelectionModel().selectFirst();
        userCombo.setDisable(true);
        refreshUsersForPortal();

        portalChoice.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refreshUsersForPortal());
    }

    private void refreshUsersForPortal() {
        String p = portalChoice.getSelectionModel().getSelectedItem();
        errorLabel.setText("");
        if ("Administration".equals(p)) {
            userCombo.setDisable(true);
            userCombo.getItems().clear();
            return;
        }
        userCombo.setDisable(false);
        List<User> users;
        if ("Médecin".equals(p)) {
            users = serviceUser.listMedecins();
            if (users.isEmpty()) {
                users = serviceUser.listAll();
            }
        } else {
            users = serviceUser.listSelectablePatients();
        }
        userCombo.setItems(FXCollections.observableArrayList(users));
        if (!users.isEmpty()) {
            userCombo.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onEnter() {
        errorLabel.setText("");
        String portal = portalChoice.getSelectionModel().getSelectedItem();
        if (portal == null) {
            errorLabel.setText("Choisissez un portail.");
            return;
        }
        try {
            Stage stage = (Stage) portalChoice.getScene().getWindow();
            if ("Administration".equals(portal)) {
                Session.setAdmin();
                load(stage, "/tn/esprit/fxml/admin_shell.fxml", "Administration");
            } else if ("Médecin".equals(portal)) {
                User u = userCombo.getSelectionModel().getSelectedItem();
                if (u == null) {
                    errorLabel.setText("Sélectionnez un médecin.");
                    return;
                }
                Session.setMedecin(u);
                Parent root = loadRoot("/tn/esprit/fxml/doctor_dashboard.fxml");
                Scene scene = new Scene(root, 1100, 720);
                applyCss(scene);
                stage.setScene(scene);
                stage.setTitle("Espace médecin — " + u.getPrenom());
            } else {
                User u = userCombo.getSelectionModel().getSelectedItem();
                if (u == null) {
                    errorLabel.setText("Sélectionnez un patient.");
                    return;
                }
                Session.setPatient(u);
                Parent root = loadRoot("/tn/esprit/fxml/patient_dashboard.fxml");
                Scene scene = new Scene(root, 1000, 680);
                applyCss(scene);
                stage.setScene(scene);
                stage.setTitle("Espace patient — " + u.getPrenom());
            }
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
            e.printStackTrace();
        }
    }

    private void load(Stage stage, String fxml, String title) throws Exception {
        Parent root = loadRoot(fxml);
        Scene scene = new Scene(root, 1200, 760);
        applyCss(scene);
        stage.setScene(scene);
        stage.setTitle(title);
    }

    private static Parent loadRoot(String fxml) throws Exception {
        return FXMLLoader.load(Objects.requireNonNull(
                LoginController.class.getResource(fxml)));
    }

    private static void applyCss(Scene scene) {
        String css = Objects.requireNonNull(LoginController.class.getResource("/tn/esprit/styles/app.css")).toExternalForm();
        scene.getStylesheets().add(css);
    }
}
