package com.healthtrack.controllers;

import com.healthtrack.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AdminLayoutController {

    @FXML private Label pageTitle, lblDateAujourdhui;
    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard, btnUtilisateurs, btnAdminRdv, btnBenevolat, btnForum;

    private final List<Button> navButtons = new ArrayList<>();

    private static final String STYLE_ACTIVE = "-fx-background-color: #2d6ff2; -fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold; -fx-padding: 0 0 0 20; -fx-cursor: hand; -fx-background-radius: 0;";
    private static final String STYLE_INACTIVE = "-fx-background-color: transparent; -fx-text-fill: #8a8fa8; -fx-font-size: 13; -fx-padding: 0 0 0 20; -fx-cursor: hand;";

    @FXML
    public void initialize() {
        lblDateAujourdhui.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        navButtons.addAll(List.of(btnDashboard, btnUtilisateurs, btnAdminRdv, btnBenevolat, btnForum));

        btnAdminRdv.setOnAction(e -> {
            setActiveButton(btnAdminRdv);
            chargerContenu("/fxml/AdminRdv.fxml");
        });

        btnBenevolat.setOnAction(e -> {
            setActiveButton(btnBenevolat);
            chargerContenu("/fxml/admin-missions-page.fxml");
        });

        // Charger RDV par défaut
        setActiveButton(btnAdminRdv);
        chargerContenu("/fxml/AdminRdv.fxml");
    }

    private void setActiveButton(Button active) {
        for (Button btn : navButtons) {
            btn.setStyle(STYLE_INACTIVE);
        }
        active.setStyle(STYLE_ACTIVE);
    }

    public void chargerContenu(String fxml) {
        try {
            Node content = FXMLLoader.load(getClass().getResource(fxml));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            System.err.println("Erreur chargement " + fxml + " : " + e.getMessage());
        }
    }
}