package com.healthtrack.controllers;

import com.healthtrack.entities.rdv;
import com.healthtrack.services.RdvService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import javafx.scene.layout.StackPane;
public class AfficherRdvController {

    @FXML private VBox rdvListContainer;
    @FXML private TextField searchField;
    @FXML private Button btnAjouter;
    @FXML private HBox prochainRdvCard;
    @FXML private Label prochainMedecin, prochainMotif, prochainMois, prochainJour, prochainHeure;

    private RdvService rdvService = new RdvService();
    private ObservableList<rdv> rdvList;

    @FXML
    public void initialize() {
        chargerDonnees();
        btnAjouter.setOnAction(e -> ouvrirPopupAjouter());
        searchField.textProperty().addListener((obs, old, val) -> afficherCartes(val));
    }

    public void chargerDonnees() {
        try {
            rdvList = FXCollections.observableArrayList(rdvService.findAll());
            mettreAJourProchainRdv();
            afficherCartes(null);
        } catch (SQLException e) {
            afficherErreur("Erreur chargement : " + e.getMessage());
        }
    }

    // ========== PROCHAIN RDV (carte bleue) ==========
    private void mettreAJourProchainRdv() {
        if (rdvList.isEmpty()) {
            prochainMedecin.setText("Aucun RDV");
            prochainMotif.setText("");
            prochainJour.setText("-");
            prochainMois.setText("");
            prochainHeure.setText("");
            return;
        }
        rdv prochain = rdvList.get(0);
        prochainMedecin.setText(prochain.getMedecin());
        prochainMotif.setText(prochain.getMotif());
        prochainHeure.setText(prochain.getHdebut());

        try {
            LocalDate date = LocalDate.parse(prochain.getDate());
            prochainMois.setText(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH).toUpperCase());
            prochainJour.setText(String.valueOf(date.getDayOfMonth()));
        } catch (Exception e) {
            prochainMois.setText("");
            prochainJour.setText(prochain.getDate());
        }
    }

    // ========== AFFICHER LES CARTES RDV ==========
    private void afficherCartes(String recherche) {
        rdvListContainer.getChildren().clear();

        for (rdv r : rdvList) {
            if (recherche != null && !recherche.isEmpty()) {
                String lower = recherche.toLowerCase();
                if (!r.getMedecin().toLowerCase().contains(lower) &&
                        !r.getMotif().toLowerCase().contains(lower) &&
                        !r.getDate().toLowerCase().contains(lower) &&
                        !r.getStatut().toLowerCase().contains(lower)) {
                    continue;
                }
            }
            rdvListContainer.getChildren().add(creerCarteRdv(r));
        }
    }

    private HBox creerCarteRdv(rdv r) {
        HBox carte = new HBox(15);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #eee; -fx-padding: 18 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);");

        // Date badge
        VBox dateBadge = new VBox(2);
        dateBadge.setAlignment(Pos.CENTER);
        dateBadge.setMinWidth(55);
        dateBadge.setStyle("-fx-background-color: #fff5f5; -fx-background-radius: 10; -fx-padding: 10;");

        String moisStr = "";
        String jourStr = "";
        try {
            LocalDate date = LocalDate.parse(r.getDate());
            moisStr = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH).toUpperCase();
            jourStr = String.valueOf(date.getDayOfMonth());
        } catch (Exception e) {
            moisStr = "";
            jourStr = r.getDate();
        }

        Label moisLabel = new Label(moisStr);
        moisLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #e53935;");
        Label jourLabel = new Label(jourStr);
        jourLabel.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #e53935;");
        dateBadge.getChildren().addAll(moisLabel, jourLabel);

        // Infos
        VBox infos = new VBox(4);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label medecinLabel = new Label(r.getMedecin());
        medecinLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label motifLabel = new Label(r.getMotif());
        motifLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #777;");

        Label detailsLabel = new Label("🕐 " + r.getHdebut() + " - " + r.getHfin() + "    💬 " + r.getMessage());
        detailsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");

        infos.getChildren().addAll(medecinLabel, motifLabel, detailsLabel);

        // Statut badge
        Label statutBadge = new Label("● " + r.getStatut());
        String couleurStatut;
        switch (r.getStatut().toLowerCase()) {
            case "confirme": case "confirmé": couleurStatut = "#4caf50"; break;
            case "annule": case "annulé": couleurStatut = "#e53935"; break;
            default: couleurStatut = "#ff9800"; break;
        }
        statutBadge.setStyle("-fx-font-size: 12; -fx-text-fill: " + couleurStatut + "; -fx-background-color: " + couleurStatut + "22; -fx-background-radius: 15; -fx-padding: 5 12;");

        // Boutons actions
        Button btnModifier = new Button("✏");
        btnModifier.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555; -fx-font-size: 14; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");
        btnModifier.setOnAction(e -> ouvrirPopupModifier(r));

        Button btnVoir = new Button("👁");
        btnVoir.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555; -fx-font-size: 14; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");
        btnVoir.setOnAction(e -> ouvrirShowOne(r));

        Button btnSupprimer = new Button("🗑");
        btnSupprimer.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #e53935; -fx-font-size: 14; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");
        btnSupprimer.setOnAction(e -> supprimerRdv(r));

        HBox boutonsBox = new HBox(8, btnModifier, btnVoir, btnSupprimer);
        boutonsBox.setAlignment(Pos.CENTER);

        carte.getChildren().addAll(dateBadge, infos, statutBadge, boutonsBox);
        return carte;
    }

    // ========== POPUP AJOUTER ==========
    private void ouvrirPopupAjouter() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un rendez-vous");

        VBox content = new VBox(18);
        content.setPrefWidth(520);
        content.setStyle("-fx-padding: 25;");

        Label titre = new Label("📅  Ajouter un rendez-vous");
        titre.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Médecin (ComboBox)
        Label lblMedecin = new Label("Médecin *");
        lblMedecin.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        ComboBox<String> medecinCombo = new ComboBox<>();
        medecinCombo.setPromptText("Choisir un médecin");
        medecinCombo.getItems().addAll("Dr. Sarah Amrani", "Dr. Ali Zouhaier", "Dr. M. Kallel");
        medecinCombo.setMaxWidth(Double.MAX_VALUE);
        medecinCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");

        // Motif (ComboBox)
        Label lblMotif = new Label("Motif *");
        lblMotif.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        ComboBox<String> motifCombo = new ComboBox<>();
        motifCombo.setPromptText("Choisir un motif");
        motifCombo.getItems().addAll("Consultation", "Suivi médical", "Urgence", "Contrôle", "Autre");
        motifCombo.setMaxWidth(Double.MAX_VALUE);
        motifCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");

        // Date
        Label lblDate = new Label("Date *");
        lblDate.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("jj/mm/aaaa");
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setStyle("-fx-font-size: 13;");

        // Bloquer les dates passées
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #ccc;");
                }
            }
        });

        // Heure (ComboBox avec créneaux)
        Label lblHeure = new Label("Heure (09:00 - 17:00) *");
        lblHeure.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        ComboBox<String> heureCombo = new ComboBox<>();
        heureCombo.setPromptText("Choisir une heure");
        heureCombo.setMaxWidth(Double.MAX_VALUE);
        heureCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");
        // Créneaux de 09:00 à 17:00 par pas de 30 min
        for (int h = 9; h <= 16; h++) {
            heureCombo.getItems().add(String.format("%02d:00", h));
            heureCombo.getItems().add(String.format("%02d:30", h));
        }
        heureCombo.getItems().add("17:00");

        HBox dateHeureBox = new HBox(15);
        VBox dateBox = new VBox(5, lblDate, datePicker);
        VBox heureBox = new VBox(5, lblHeure, heureCombo);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        HBox.setHgrow(heureBox, Priority.ALWAYS);
        dateHeureBox.getChildren().addAll(dateBox, heureBox);

        // Message
        Label lblMessage = new Label("Message (optionnel — doit commencer par une majuscule)");
        lblMessage.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Entrer votre message ...");
        messageArea.setPrefRowCount(3);
        messageArea.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");

        // Label erreur
        Label lblErreur = new Label();
        lblErreur.setStyle("-fx-text-fill: #e53935; -fx-font-size: 12;");
        lblErreur.setWrapText(true);
        lblErreur.setVisible(false);

        content.getChildren().addAll(
                titre, new Separator(),
                lblMedecin, medecinCombo,
                lblMotif, motifCombo,
                dateHeureBox,
                lblMessage, messageArea,
                lblErreur
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.setText("Confirmer");
        btnOk.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 35; -fx-cursor: hand;");

        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnCancel.setText("Annuler");
        btnCancel.setStyle("-fx-background-radius: 8; -fx-padding: 12 35; -fx-font-size: 14;");

        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // Reset erreur
            lblErreur.setVisible(false);
            medecinCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");
            motifCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");
            datePicker.setStyle("-fx-font-size: 13;");
            heureCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");

            String styleErreur = "-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e53935;";

            // Validation médecin
            if (medecinCombo.getValue() == null) {
                medecinCombo.setStyle(styleErreur);
                lblErreur.setText("❌ Veuillez choisir un médecin");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            // Validation motif
            if (motifCombo.getValue() == null) {
                motifCombo.setStyle(styleErreur);
                lblErreur.setText("❌ Veuillez choisir un motif");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            // Validation date
            if (datePicker.getValue() == null) {
                datePicker.setStyle("-fx-font-size: 13; -fx-border-color: #e53935;");
                lblErreur.setText("❌ Veuillez choisir une date");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            if (datePicker.getValue().isBefore(LocalDate.now())) {
                datePicker.setStyle("-fx-font-size: 13; -fx-border-color: #e53935;");
                lblErreur.setText("❌ La date ne peut pas être dans le passé");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            // Validation heure
            if (heureCombo.getValue() == null) {
                heureCombo.setStyle(styleErreur);
                lblErreur.setText("❌ Veuillez choisir une heure");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            // Validation message (si rempli, doit commencer par majuscule)
            String message = messageArea.getText().trim();
            if (!message.isEmpty() && !Character.isUpperCase(message.charAt(0))) {
                messageArea.setStyle("-fx-font-size: 13; -fx-border-color: #e53935; -fx-border-radius: 8;");
                lblErreur.setText("❌ Le message doit commencer par une majuscule");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            // Tout est valide → insérer
            try {
                String hdebut = heureCombo.getValue();
                // Calculer heure fin (+30 min)
                String[] parts = hdebut.split(":");
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                m += 30;
                if (m >= 60) { m -= 60; h++; }
                String hfin = String.format("%02d:%02d", h, m);

                rdv r = new rdv(
                        datePicker.getValue().toString(),
                        hdebut,
                        hfin,
                        "en_attente",
                        motifCombo.getValue(),
                        medecinCombo.getValue(),
                        message,
                        1,
                        1
                );
                rdvService.insert(r);
                chargerDonnees();
                afficherSucces("RDV ajouté avec succès !");
            } catch (SQLException ex) {
                afficherErreur("Erreur : " + ex.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    // ========== POPUP MODIFIER ==========
    private void ouvrirPopupModifier(rdv r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le rendez-vous");

        VBox content = new VBox(18);
        content.setPrefWidth(520);
        content.setStyle("-fx-padding: 25;");

        Label titre = new Label("✏  Modifier le rendez-vous");
        titre.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label sousTitre = new Label("Modifiez les informations de votre rendez-vous");
        sousTitre.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");

        // Médecin (fixe, non modifiable)
        Label lblMedecin = new Label("Médecin");
        lblMedecin.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        TextField medecinField = new TextField(r.getMedecin());
        medecinField.setEditable(false);
        medecinField.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd; -fx-background-color: #f5f5f5; -fx-text-fill: #888; -fx-padding: 8;");
        medecinField.setMaxWidth(Double.MAX_VALUE);

        // Motif (ComboBox)
        Label lblMotif = new Label("Motif *");
        lblMotif.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        ComboBox<String> motifCombo = new ComboBox<>();
        motifCombo.getItems().addAll("Consultation", "Suivi médical", "Urgence", "Contrôle", "Autre");
        motifCombo.setValue(r.getMotif());
        motifCombo.setMaxWidth(Double.MAX_VALUE);
        motifCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");

        // Date
        Label lblDate = new Label("Date *");
        lblDate.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        DatePicker datePicker = new DatePicker(LocalDate.parse(r.getDate()));
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setStyle("-fx-font-size: 13;");

        // Bloquer les dates passées
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #ccc;");
                }
            }
        });

        // Heure (ComboBox avec créneaux)
        Label lblHeure = new Label("Heure (09:00 - 17:00) *");
        lblHeure.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        ComboBox<String> heureCombo = new ComboBox<>();
        heureCombo.setMaxWidth(Double.MAX_VALUE);
        heureCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");
        for (int h = 9; h <= 16; h++) {
            heureCombo.getItems().add(String.format("%02d:00", h));
            heureCombo.getItems().add(String.format("%02d:30", h));
        }
        heureCombo.getItems().add("17:00");

        // Sélectionner l'heure actuelle du RDV
        String heureActuelle = r.getHdebut();
        if (heureActuelle != null && heureActuelle.length() >= 5) {
            heureCombo.setValue(heureActuelle.substring(0, 5));
        }

        HBox dateHeureBox = new HBox(15);
        VBox dateBox = new VBox(5, lblDate, datePicker);
        VBox heureBox = new VBox(5, lblHeure, heureCombo);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        HBox.setHgrow(heureBox, Priority.ALWAYS);
        dateHeureBox.getChildren().addAll(dateBox, heureBox);

        // Message
        Label lblMessage = new Label("Message (optionnel — doit commencer par une majuscule)");
        lblMessage.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        TextArea messageArea = new TextArea();
        messageArea.setText(r.getMessage());
        messageArea.setPrefRowCount(3);
        messageArea.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");

        // Label erreur
        Label lblErreur = new Label();
        lblErreur.setStyle("-fx-text-fill: #e53935; -fx-font-size: 12;");
        lblErreur.setWrapText(true);
        lblErreur.setVisible(false);

        content.getChildren().addAll(
                titre, sousTitre, new Separator(),
                lblMedecin, medecinField,
                lblMotif, motifCombo,
                dateHeureBox,
                lblMessage, messageArea,
                lblErreur
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.setText("Enregistrer");
        btnOk.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 35; -fx-cursor: hand;");

        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnCancel.setText("Annuler");
        btnCancel.setStyle("-fx-background-radius: 8; -fx-padding: 12 35; -fx-font-size: 14;");

        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // Reset styles
            lblErreur.setVisible(false);
            String styleNormal = "-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;";
            String styleErreurBorder = "-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e53935;";
            motifCombo.setStyle(styleNormal);
            datePicker.setStyle("-fx-font-size: 13;");
            heureCombo.setStyle(styleNormal);
            messageArea.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");

            // Validation motif
            if (motifCombo.getValue() == null) {
                motifCombo.setStyle(styleErreurBorder);
                lblErreur.setText("❌ Veuillez choisir un motif");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            // Validation date
            if (datePicker.getValue() == null) {
                datePicker.setStyle("-fx-font-size: 13; -fx-border-color: #e53935;");
                lblErreur.setText("❌ Veuillez choisir une date");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            if (datePicker.getValue().isBefore(LocalDate.now())) {
                datePicker.setStyle("-fx-font-size: 13; -fx-border-color: #e53935;");
                lblErreur.setText("❌ La date ne peut pas être dans le passé");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            // Validation heure
            if (heureCombo.getValue() == null) {
                heureCombo.setStyle(styleErreurBorder);
                lblErreur.setText("❌ Veuillez choisir une heure");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            // Validation message
            String message = messageArea.getText().trim();
            if (!message.isEmpty() && !Character.isUpperCase(message.charAt(0))) {
                messageArea.setStyle("-fx-font-size: 13; -fx-border-color: #e53935; -fx-border-radius: 8;");
                lblErreur.setText("❌ Le message doit commencer par une majuscule");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            // Tout est valide → modifier
            try {
                String hdebut = heureCombo.getValue();
                String[] parts = hdebut.split(":");
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                m += 30;
                if (m >= 60) { m -= 60; h++; }
                String hfin = String.format("%02d:%02d", h, m);

                r.setDate(datePicker.getValue().toString());
                r.setHdebut(hdebut);
                r.setHfin(hfin);
                r.setMotif(motifCombo.getValue());
                r.setMessage(message);

                rdvService.update(r);
                chargerDonnees();
                afficherSucces("RDV modifié avec succès !");
            } catch (SQLException ex) {
                afficherErreur("Erreur : " + ex.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    // ========== SUPPRIMER ==========
    private void supprimerRdv(rdv r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Supprimer");

        VBox content = new VBox(15);
        content.setPrefWidth(400);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 30 40;");

        // Icône poubelle
        Label icon = new Label("🗑");
        icon.setStyle("-fx-font-size: 32; -fx-background-color: #ffebee; -fx-background-radius: 30; -fx-padding: 15; -fx-text-fill: #e53935;");

        Label titre = new Label("Supprimer ce rendez-vous ?");
        titre.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label sousTitre = new Label("Cette action est irréversible.");
        sousTitre.setStyle("-fx-font-size: 13; -fx-text-fill: #888;");

        content.getChildren().addAll(icon, titre, sousTitre);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnSupprimer = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnSupprimer.setText("Supprimer");
        btnSupprimer.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 40; -fx-cursor: hand;");

        Button btnAnnuler = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnAnnuler.setText("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555; -fx-font-size: 14; -fx-background-radius: 8; -fx-padding: 12 40;");

        btnSupprimer.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                rdvService.delete(r);
                chargerDonnees();
                afficherSucces("RDV supprimé avec succès !");
            } catch (SQLException ex) {
                afficherErreur("Erreur : " + ex.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    // ========== UTILITAIRES ==========
    private TextField creerChamp(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd; -fx-padding: 8;");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private VBox labelEtChamp(String labelText, javafx.scene.Node field) {
        VBox vbox = new VBox(5);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #555;");
        vbox.getChildren().addAll(label, field);
        HBox.setHgrow(vbox, Priority.ALWAYS);
        if (field instanceof TextField) ((TextField) field).setMaxWidth(Double.MAX_VALUE);
        if (field instanceof DatePicker) ((DatePicker) field).setMaxWidth(Double.MAX_VALUE);
        return vbox;
    }
    private void ouvrirShowOne(rdv r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowOneRdv.fxml"));
            javafx.scene.Node content = loader.load();
            ShowOneRdvController controller = loader.getController();
            controller.initData(r);

            StackPane parent = (StackPane) rdvListContainer.getScene().lookup("#contentArea");
            if (parent != null) {
                parent.getChildren().clear();
                parent.getChildren().add(content);
            }
        } catch (IOException ex) {
            afficherErreur("Erreur : " + ex.getMessage());
        }
    }

    private void afficherSucces(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès"); a.setContentText(msg); a.show();
    }

    private void afficherErreur(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setContentText(msg); a.show();
    }
}