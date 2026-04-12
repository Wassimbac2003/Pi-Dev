package com.healthtrack.controllers;

import com.healthtrack.entities.rdv;
import com.healthtrack.services.RdvService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class ShowOneRdvController {

    @FXML private Label lblMedecin, lblMotif;
    @FXML private Label lblDateBadge, lblHoraireBadge, lblStatutBadge;
    @FXML private Label lblMois, lblJour, lblHeure;
    @FXML private Label lblInfoMedecin, lblInfoMotif, lblInfoDate, lblInfoHoraire, lblInfoStatut;
    @FXML private Label lblMessage, lblDateCreation;
    @FXML private Button btnRetour, btnModifier, btnModifierBas, btnAnnuler;

    private rdv rdvActuel;
    private RdvService rdvService = new RdvService();

    public void initData(rdv r) {
        this.rdvActuel = r;

        // Carte bleue
        lblMedecin.setText(r.getMedecin());
        lblMotif.setText(r.getMotif());
        lblHeure.setText(r.getHdebut());

        try {
            LocalDate date = LocalDate.parse(r.getDate());
            lblMois.setText(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH).toUpperCase());
            lblJour.setText(String.valueOf(date.getDayOfMonth()));
            lblDateBadge.setText("📅 " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            lblInfoDate.setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            lblDateCreation.setText("ℹ️ Rendez-vous créé le " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } catch (Exception e) {
            lblMois.setText("");
            lblJour.setText(r.getDate());
            lblDateBadge.setText("📅 " + r.getDate());
            lblInfoDate.setText(r.getDate());
        }

        lblHoraireBadge.setText("🕐 " + r.getHdebut() + " - " + r.getHfin());
        lblStatutBadge.setText("⏳ " + r.getStatut());

        // Style statut
        String couleur;
        switch (r.getStatut().toLowerCase()) {
            case "confirme": case "confirmé": couleur = "#4caf50"; break;
            case "annule": case "annulé": couleur = "#e53935"; break;
            default: couleur = "#ff9800"; break;
        }
        lblStatutBadge.setStyle("-fx-font-size: 12; -fx-text-fill: white; -fx-background-color: " + couleur + "; -fx-background-radius: 15; -fx-padding: 6 14;");
        lblInfoStatut.setText("⏳ " + r.getStatut());
        lblInfoStatut.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + couleur + ";");

        // Infos
        lblInfoMedecin.setText(r.getMedecin());
        lblInfoMotif.setText(r.getMotif());
        lblInfoHoraire.setText(r.getHdebut() + " - " + r.getHfin());

        // Message
        lblMessage.setText(r.getMessage() != null && !r.getMessage().isEmpty() ? r.getMessage() : "Aucun message");
    }

    @FXML
    public void initialize() {
        btnRetour.setOnAction(e -> retourListe());
        btnModifier.setOnAction(e -> modifierRdv());
        btnModifierBas.setOnAction(e -> modifierRdv());
        btnAnnuler.setOnAction(e -> annulerRdv());
    }

    private void retourListe() {
        try {
            Node content = FXMLLoader.load(getClass().getResource("/fxml/AfficherRdv.fxml"));
            StackPane parent = (StackPane) btnRetour.getScene().lookup("#contentArea");
            if (parent != null) {
                parent.getChildren().clear();
                parent.getChildren().add(content);
            }
        } catch (IOException e) {
            System.err.println("Erreur retour : " + e.getMessage());
        }
    }

    private void modifierRdv() {
        // Ouvre le même popup modifier que AfficherRdvController
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le rendez-vous");

        javafx.scene.layout.VBox contentBox = new javafx.scene.layout.VBox(18);
        contentBox.setPrefWidth(520);
        contentBox.setStyle("-fx-padding: 25;");

        Label titre = new Label("✏  Modifier le rendez-vous");
        titre.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label sousTitre = new Label("Modifiez les informations de votre rendez-vous");
        sousTitre.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");

        ComboBox<String> medecinCombo = new ComboBox<>();
        medecinCombo.getItems().addAll("Dr. Sarah Amrani", "Dr. Ali Zouhaier", "Dr. M. Kallel");
        medecinCombo.setValue(rdvActuel.getMedecin());
        medecinCombo.setMaxWidth(Double.MAX_VALUE);
        medecinCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");

        ComboBox<String> motifCombo = new ComboBox<>();
        motifCombo.getItems().addAll("Consultation", "Suivi médical", "Urgence", "Contrôle", "Autre");
        motifCombo.setValue(rdvActuel.getMotif());
        motifCombo.setMaxWidth(Double.MAX_VALUE);
        motifCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");

        DatePicker datePicker = new DatePicker(LocalDate.parse(rdvActuel.getDate()));
        datePicker.setMaxWidth(Double.MAX_VALUE);

        TextField heureField = new TextField(rdvActuel.getHdebut());
        heureField.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd; -fx-padding: 8;");

        TextArea messageArea = new TextArea(rdvActuel.getMessage());
        messageArea.setPrefRowCount(3);
        messageArea.setStyle("-fx-font-size: 13; -fx-border-color: #ddd; -fx-border-radius: 8;");

        javafx.scene.layout.HBox dateHeureBox = new javafx.scene.layout.HBox(15);
        javafx.scene.layout.VBox dateBox = new javafx.scene.layout.VBox(5, new Label("Date"), datePicker);
        javafx.scene.layout.VBox heureBox = new javafx.scene.layout.VBox(5, new Label("Heure"), heureField);
        javafx.scene.layout.HBox.setHgrow(dateBox, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(heureBox, javafx.scene.layout.Priority.ALWAYS);
        dateHeureBox.getChildren().addAll(dateBox, heureBox);

        contentBox.getChildren().addAll(
                titre, sousTitre, new Separator(),
                new Label("Médecin"), medecinCombo,
                new Label("Motif"), motifCombo,
                dateHeureBox,
                new Label("Message"), messageArea
        );

        dialog.getDialogPane().setContent(contentBox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.setText("Enregistrer");
        btnOk.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 35; -fx-cursor: hand;");

        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                rdvActuel.setDate(datePicker.getValue().toString());
                rdvActuel.setHdebut(heureField.getText());
                rdvActuel.setHfin(heureField.getText());
                rdvActuel.setMotif(motifCombo.getValue());
                rdvActuel.setMedecin(medecinCombo.getValue());
                rdvActuel.setMessage(messageArea.getText());

                rdvService.update(rdvActuel);
                initData(rdvActuel); // Rafraîchir la page
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Succès");
                a.setContentText("RDV modifié !");
                a.show();
            } catch (SQLException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Erreur");
                a.setContentText(ex.getMessage());
                a.show();
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    private void annulerRdv() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler le RDV");
        confirm.setContentText("Voulez-vous vraiment annuler ce rendez-vous ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    rdvActuel.setStatut("annulé");
                    rdvService.update(rdvActuel);
                    initData(rdvActuel);
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle("Succès");
                    a.setContentText("RDV annulé !");
                    a.show();
                } catch (SQLException e) {
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("Erreur");
                    a.setContentText(e.getMessage());
                    a.show();
                }
            }
        });
    }
}