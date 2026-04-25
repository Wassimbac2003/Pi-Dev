package Controllers;

import Models.rdv;
import Services.RdvService;
import Services.SmsService;
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
    @FXML private Label lblSmsInfo;
    @FXML private Button btnRetour, btnModifier, btnModifierBas, btnAnnuler;
    @FXML private Button btnSmsRappel;

    private rdv rdvActuel;
    private RdvService rdvService = new RdvService();
    private SmsService smsService = new SmsService();

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

        // ── Configurer le bouton SMS selon la date ──
        configurerBoutonSms(r);
    }

    /**
     * Configure le bouton SMS :
     * - Vert + actif si le RDV est aujourd'hui
     * - Gris + désactivé sinon (avec message explicatif)
     */
    private void configurerBoutonSms(rdv r) {
        // ── MODE DEMO : bouton toujours actif pour tester devant le prof ──
        // TODO: remettre la vérification estAujourdhui() après la démo
        btnSmsRappel.setDisable(false);
        btnSmsRappel.setText("📱 Envoyer SMS Rappel");
        btnSmsRappel.setStyle(
                "-fx-background-color: linear-gradient(to right, #16a34a, #22c55e); " +
                        "-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold; " +
                        "-fx-background-radius: 10; -fx-padding: 14; -fx-cursor: hand;"
        );
        btnSmsRappel.setOnMouseEntered(e -> btnSmsRappel.setStyle(
                "-fx-background-color: linear-gradient(to right, #15803d, #16a34a); " +
                        "-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold; " +
                        "-fx-background-radius: 10; -fx-padding: 14; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(22,163,74,0.4), 10, 0, 0, 3);"
        ));
        btnSmsRappel.setOnMouseExited(e -> btnSmsRappel.setStyle(
                "-fx-background-color: linear-gradient(to right, #16a34a, #22c55e); " +
                        "-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold; " +
                        "-fx-background-radius: 10; -fx-padding: 14; -fx-cursor: hand;"
        ));
        lblSmsInfo.setText("✅ Cliquez pour recevoir un SMS de rappel.");
        lblSmsInfo.setStyle("-fx-font-size: 11; -fx-text-fill: #16a34a;");
    }

    @FXML
    public void initialize() {
        btnRetour.setOnAction(e -> retourListe());
        btnModifier.setOnAction(e -> modifierRdv());
        btnModifierBas.setOnAction(e -> modifierRdv());
        btnAnnuler.setOnAction(e -> annulerRdv());
        btnSmsRappel.setOnAction(e -> envoyerSmsRappel());
    }

    // ========== ENVOI SMS TWILIO ==========
    private void envoyerSmsRappel() {
        if (rdvActuel == null) return;

        // MODE DEMO : pas de vérification de date
        // TODO: remettre la vérification estAujourdhui() après la démo

        // Confirmation avant envoi
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Envoyer SMS de rappel");
        confirm.setHeaderText("📱 Confirmer l'envoi du SMS");
        confirm.setContentText(
                "Un SMS de rappel sera envoyé pour :\n\n" +
                        "👨‍⚕️ " + rdvActuel.getMedecin() + "\n" +
                        "🕐 Aujourd'hui à " + rdvActuel.getHdebut() + "\n" +
                        "📋 " + rdvActuel.getMotif() + "\n\n" +
                        "Confirmer l'envoi ?"
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Désactiver le bouton pendant l'envoi
                btnSmsRappel.setDisable(true);
                btnSmsRappel.setText("⏳ Envoi en cours...");
                btnSmsRappel.setStyle(
                        "-fx-background-color: #fbbf24; -fx-text-fill: white; -fx-font-size: 13; " +
                                "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 14;"
                );

                // Envoyer le SMS
                boolean ok = smsService.envoyerRappel(
                        rdvActuel.getMedecin(),
                        rdvActuel.getDate(),
                        rdvActuel.getHdebut(),
                        rdvActuel.getMotif()
                );

                if (ok) {
                    // Succès → bouton vert "Envoyé ✓"
                    btnSmsRappel.setText("✅ SMS envoyé avec succès !");
                    btnSmsRappel.setStyle(
                            "-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 13; " +
                                    "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 14;"
                    );
                    btnSmsRappel.setDisable(true);
                    lblSmsInfo.setText("✅ SMS de rappel envoyé avec succès !");
                    lblSmsInfo.setStyle("-fx-font-size: 11; -fx-text-fill: #16a34a; -fx-font-weight: bold;");

                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Succès");
                    success.setHeaderText(null);
                    success.setContentText("✅ SMS de rappel envoyé avec succès !");
                    success.show();
                } else {
                    // Erreur → rétablir le bouton
                    btnSmsRappel.setDisable(false);
                    btnSmsRappel.setText("📱 Réessayer l'envoi");
                    btnSmsRappel.setStyle(
                            "-fx-background-color: linear-gradient(to right, #16a34a, #22c55e); " +
                                    "-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold; " +
                                    "-fx-background-radius: 10; -fx-padding: 14; -fx-cursor: hand;"
                    );
                    lblSmsInfo.setText("❌ Erreur lors de l'envoi. Réessayez.");
                    lblSmsInfo.setStyle("-fx-font-size: 11; -fx-text-fill: #e53935;");

                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Erreur");
                    error.setHeaderText(null);
                    error.setContentText("❌ Erreur lors de l'envoi du SMS. Vérifiez votre connexion internet.");
                    error.show();
                }
            }
        });
    }

    private void retourListe() {
        try {
            Node content = FXMLLoader.load(getClass().getResource("/AfficherRdv.fxml"));
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
                initData(rdvActuel);
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