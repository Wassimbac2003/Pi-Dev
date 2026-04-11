package Controllers;

import Models.rdv;
import Services.RdvService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import Utils.MyDb;

public class AdminRdvController {

    @FXML private VBox rdvTableContainer;
    @FXML private Label lblTodayCount, lblPendingCount, lblDoneCount;
    @FXML private Button btnGererDispo;

    private RdvService rdvService = new RdvService();

    @FXML
    public void initialize() {
        chargerDonnees();
        btnGererDispo.setOnAction(e -> naviguerVersDispo());
    }

    private void chargerDonnees() {
        try {
            List<rdv> rdvs = rdvService.findAll();
            calculerStats(rdvs);
            afficherLignes(rdvs);
        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    private void calculerStats(List<rdv> rdvs) {
        String today = LocalDate.now().toString();
        long todayCount = rdvs.stream().filter(r -> r.getDate().equals(today)).count();
        long pendingCount = rdvs.stream().filter(r -> r.getStatut().toLowerCase().contains("attente")).count();
        long doneCount = rdvs.stream().filter(r -> r.getStatut().toLowerCase().contains("confirm")).count();

        lblTodayCount.setText(todayCount + " RDV");
        lblPendingCount.setText(pendingCount + " Demandes");
        lblDoneCount.setText(doneCount + " Consultations");
    }

    private void afficherLignes(List<rdv> rdvs) {
        rdvTableContainer.getChildren().clear();

        for (rdv r : rdvs) {
            HBox ligne = new HBox();
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.setStyle("-fx-padding: 15 25; -fx-border-color: #f5f5f5; -fx-border-width: 0 0 1 0;");

            // Réf
            Label ref = new Label("#RDV-" + r.getId());
            ref.setPrefWidth(100);
            ref.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");

            // Patient
            HBox patientBox = new HBox(8);
            patientBox.setAlignment(Pos.CENTER_LEFT);
            patientBox.setPrefWidth(250);
            Label avatar = new Label("DR");
            avatar.setMinWidth(32);
            avatar.setMinHeight(32);
            avatar.setAlignment(Pos.CENTER);
            avatar.setStyle("-fx-background-color: #1a73e8; -fx-background-radius: 16; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold;");
            VBox patientInfo = new VBox(2);
            Label patientNom = new Label(r.getMedecin());
            patientNom.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
            Label patientMotif = new Label(r.getMotif());
            patientMotif.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");
            patientInfo.getChildren().addAll(patientNom, patientMotif);
            patientBox.getChildren().addAll(avatar, patientInfo);

            // Médecin
            HBox medecinBox = new HBox(5);
            medecinBox.setAlignment(Pos.CENTER_LEFT);
            medecinBox.setPrefWidth(220);
            Label medecinIcon = new Label("👨‍⚕️");
            Label medecinNom = new Label(r.getMedecin());
            medecinNom.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");
            medecinBox.getChildren().addAll(medecinIcon, medecinNom);

            // Date & Heure
            VBox dateBox = new VBox(2);
            dateBox.setPrefWidth(200);
            String dateFormatee = r.getDate();
            try {
                LocalDate date = LocalDate.parse(r.getDate());
                dateFormatee = String.format("%02d", date.getDayOfMonth()) + " " +
                        date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " +
                        date.getYear();
            } catch (Exception ignored) {}
            Label dateLabel = new Label(dateFormatee);
            dateLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
            Label heureLabel = new Label("🕐 " + r.getHdebut());
            heureLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");
            dateBox.getChildren().addAll(dateLabel, heureLabel);

            // Statut
            Label statut = new Label();
            statut.setPrefWidth(150);
            String couleur;
            String statutText;
            switch (r.getStatut().toLowerCase()) {
                case "confirme": case "confirmé":
                    couleur = "#4caf50";
                    statutText = "✓ Confirmé";
                    break;
                case "annule": case "annulé":
                    couleur = "#e53935";
                    statutText = "✕ Annulé";
                    break;
                default:
                    couleur = "#ff9800";
                    statutText = "⏳ En attente";
                    break;
            }
            statut.setText(statutText);
            statut.setStyle("-fx-font-size: 11; -fx-text-fill: " + couleur + "; -fx-background-color: " + couleur + "22; -fx-background-radius: 12; -fx-padding: 4 10; -fx-font-weight: bold;");

            // Actions
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_LEFT);

            // Confirmer (seulement si en attente)
            if (r.getStatut().toLowerCase().contains("attente")) {
                Button btnConfirmer = new Button("✓");
                btnConfirmer.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #4caf50; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;");
                btnConfirmer.setOnAction(e -> {
                    try {
                        rdvService.updateStatut(r.getId(), "confirmé");
                        chargerDonnees();
                    } catch (SQLException ex) {
                        System.err.println(ex.getMessage());
                    }
                });
                actions.getChildren().add(btnConfirmer);
            }

            Button btnEdit = new Button("✏");
            btnEdit.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;");
            btnEdit.setOnAction(e -> ouvrirPopupModifierAdmin(r));

            Button btnDelete = new Button("🗑");
            btnDelete.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #e53935; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;");
            btnDelete.setOnAction(e -> supprimerRdv(r));

            actions.getChildren().addAll(btnEdit, btnDelete);

            ligne.getChildren().addAll(ref, patientBox, medecinBox, dateBox, statut, actions);
            rdvTableContainer.getChildren().add(ligne);
        }
    }

    private void supprimerRdv(rdv r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Annuler le RDV");

        VBox content = new VBox(15);
        content.setPrefWidth(400);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 30 40;");

        Label icon = new Label("🗑");
        icon.setStyle("-fx-font-size: 32; -fx-background-color: #ffebee; -fx-background-radius: 30; -fx-padding: 15; -fx-text-fill: #e53935;");

        Label titre = new Label("Annuler ce rendez-vous ?");
        titre.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label sousTitre = new Label("Le statut sera changé en Annulé.");
        sousTitre.setStyle("-fx-font-size: 13; -fx-text-fill: #888;");

        content.getChildren().addAll(icon, titre, sousTitre);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnAnnulerRdv = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnAnnulerRdv.setText("Annuler le RDV");
        btnAnnulerRdv.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 40; -fx-cursor: hand;");

        Button btnRetour = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnRetour.setText("Retour");
        btnRetour.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555; -fx-font-size: 14; -fx-background-radius: 8; -fx-padding: 12 40;");

        btnAnnulerRdv.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                rdvService.updateStatut(r.getId(), "annulé");
                chargerDonnees();
            } catch (SQLException ex) {
                System.err.println(ex.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    private void ouvrirPopupModifierAdmin(rdv r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier Date & Heure");

        VBox content = new VBox(20);
        content.setPrefWidth(450);
        content.setStyle("-fx-padding: 25;");

        Label titre = new Label("Modifier Date & Heure");
        titre.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Date
        Label lblDate = new Label("Date");
        lblDate.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        DatePicker datePicker = new DatePicker(LocalDate.parse(r.getDate()));
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setStyle("-fx-font-size: 13;");

        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate dateOriginale = LocalDate.parse(r.getDate());
                if (date.isBefore(LocalDate.now()) && !date.equals(dateOriginale)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #ccc;");
                }
            }
        });

        // Heure
        Label lblHeure = new Label("Heure");
        lblHeure.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        ComboBox<String> heureCombo = new ComboBox<>();
        heureCombo.setMaxWidth(Double.MAX_VALUE);
        heureCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");
        for (int h = 9; h <= 16; h++) {
            heureCombo.getItems().add(String.format("%02d:00", h));
            heureCombo.getItems().add(String.format("%02d:30", h));
        }
        heureCombo.getItems().add("17:00");

        String heureActuelle = r.getHdebut();
        if (heureActuelle != null && heureActuelle.length() >= 5) {
            String heureCourte = heureActuelle.substring(0, 5);
            if (!heureCombo.getItems().contains(heureCourte)) {
                heureCombo.getItems().add(heureCourte);
            }
            heureCombo.setValue(heureCourte);
        }

        HBox dateHeureBox = new HBox(15);
        VBox dateBox = new VBox(5, lblDate, datePicker);
        VBox heureBox = new VBox(5, lblHeure, heureCombo);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        HBox.setHgrow(heureBox, Priority.ALWAYS);
        dateHeureBox.getChildren().addAll(dateBox, heureBox);

        // Label erreur
        Label lblErreur = new Label();
        lblErreur.setStyle("-fx-text-fill: #e53935; -fx-font-size: 12;");
        lblErreur.setWrapText(true);
        lblErreur.setVisible(false);

        content.getChildren().addAll(titre, dateHeureBox, lblErreur);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnEnregistrer = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnEnregistrer.setText("Enregistrer");
        btnEnregistrer.setStyle("-fx-background-color: #ff6b35; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 40; -fx-cursor: hand;");

        Button btnAnnuler = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnAnnuler.setText("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555; -fx-font-size: 14; -fx-background-radius: 8; -fx-padding: 12 40;");

        btnEnregistrer.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            lblErreur.setVisible(false);

            if (datePicker.getValue() == null) {
                lblErreur.setText("❌ Veuillez choisir une date");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            LocalDate dateOriginale = LocalDate.parse(r.getDate());
            if (datePicker.getValue().isBefore(LocalDate.now()) && !datePicker.getValue().equals(dateOriginale)) {
                lblErreur.setText("❌ La date ne peut pas être dans le passé");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

            if (heureCombo.getValue() == null) {
                lblErreur.setText("❌ Veuillez choisir une heure");
                lblErreur.setVisible(true);
                event.consume();
                return;
            }

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

                String sql = "UPDATE rdv SET date=?, hdebut=?, hfin=? WHERE id=?";
                java.sql.PreparedStatement ps = Utils.MyDb.getInstance().getConnection().prepareStatement(sql);
                ps.setString(1, r.getDate());
                ps.setString(2, r.getHdebut());
                ps.setString(3, r.getHfin());
                ps.setInt(4, r.getId());
                ps.executeUpdate();
                chargerDonnees();
            } catch (SQLException ex) {
                lblErreur.setText("❌ Erreur : " + ex.getMessage());
                lblErreur.setVisible(true);
                event.consume();
            }
        });

        dialog.showAndWait();
    }
    private void naviguerVersDispo() {
        try {
            Node content = FXMLLoader.load(getClass().getResource("/AdminDispo.fxml"));
            StackPane parent = (StackPane) rdvTableContainer.getScene().lookup("#contentArea");
            if (parent != null) {
                parent.getChildren().clear();
                parent.getChildren().add(content);
            }
        } catch (IOException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}