package com.healthtrack.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainLayoutController {

    @FXML private Label pageTitle;
    @FXML private StackPane contentArea;
    @FXML private Button btnRendezVous, btnTableauBord, btnMedicaments, btnStats;
    @FXML private Button btnBenevolat, btnForum, btnAnnonces;

    private final List<Button> navButtons = new ArrayList<>();

    private static final String STYLE_ACTIVE = "-fx-background-color: #eef3ff; -fx-text-fill: #1a73e8; -fx-font-size: 13; -fx-font-weight: bold; -fx-padding: 0 0 0 20; -fx-cursor: hand; -fx-background-radius: 0;";
    private static final String STYLE_INACTIVE = "-fx-background-color: transparent; -fx-text-fill: #555; -fx-font-size: 13; -fx-padding: 0 0 0 20; -fx-cursor: hand;";

    @FXML
    public void initialize() {
        navButtons.addAll(List.of(btnTableauBord, btnRendezVous, btnMedicaments, btnStats, btnBenevolat, btnForum, btnAnnonces));

        btnRendezVous.setOnAction(e -> {
            setActiveButton(btnRendezVous);
            pageTitle.setText("Gestion des Rendez-vous");
            chargerContenu("/fxml/AfficherRdv.fxml");
        });

        btnBenevolat.setOnAction(e -> {
            setActiveButton(btnBenevolat);
            pageTitle.setText("Bénévolat");
            chargerContenu("/fxml/benevolat-page.fxml");
        });

        btnAnnonces.setOnAction(e -> {
            setActiveButton(btnAnnonces);
            pageTitle.setText("Annonces & Dons");
            chargerContenu("/fxml/sponsors-page.fxml");
        });

        btnTableauBord.setOnAction(e -> {
            setActiveButton(btnTableauBord);
            pageTitle.setText("Tableau de Bord");
        });

        // Charger RDV par défaut
        setActiveButton(btnRendezVous);
        chargerContenu("/fxml/AfficherRdv.fxml");

        // Charger le CSS dashboard
        contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                String css = getClass().getResource("/styles/dashboard.css").toExternalForm();
                if (!newScene.getStylesheets().contains(css)) {
                    newScene.getStylesheets().add(css);
                }
            }
        });
    }

    private void setActiveButton(Button active) {
        for (Button btn : navButtons) {
            btn.setStyle(STYLE_INACTIVE);
        }
        active.setStyle(STYLE_ACTIVE);
    }

    public void chargerContenu(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Node content = loader.load();

            Object controller = loader.getController();
            if (controller instanceof PageController pageController) {
                pageController.setMainController(null);
                pageController.onPageShown();
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}