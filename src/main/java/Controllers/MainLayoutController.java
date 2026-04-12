package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainLayoutController {

    @FXML private Label pageTitle;
    @FXML private StackPane contentArea;
    @FXML private Button btnRendezVous, btnTableauBord;

    @FXML
    public void initialize() {
        // Charger la page RDV par défaut
        chargerContenu("/AfficherRdv.fxml");

        // Navigation sidebar
        btnRendezVous.setOnAction(e -> {
            pageTitle.setText("Gestion des Rendez-vous");
            chargerContenu("/AfficherRdv.fxml");
        });

        btnTableauBord.setOnAction(e -> {
            pageTitle.setText("Tableau de Bord");
            // Plus tard tu pourras charger un autre FXML ici
        });
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