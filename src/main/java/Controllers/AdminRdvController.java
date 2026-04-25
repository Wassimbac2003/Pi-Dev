package Controllers;

import Models.rdv;
import Services.RdvService;
import Services.EmailService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AdminRdvController {

    @FXML private VBox rdvTableContainer;
    @FXML private Label lblTodayCount, lblPendingCount, lblDoneCount;
    @FXML private Button btnGererDispo, btnReset;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatut;
    @FXML private ComboBox<String> filterDate;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label lblResultCount, lblEmptyMsg;
    @FXML private VBox emptyState;

    private RdvService rdvService = new RdvService();
    private EmailService emailService = new EmailService();
    private List<rdv> tousLesRdvs = new ArrayList<>();

    @FXML
    public void initialize() {
        initFiltres();
        chargerDonnees();

        searchField.textProperty().addListener((obs, old, val) -> appliquerFiltres());
        filterStatut.valueProperty().addListener((obs, old, val) -> appliquerFiltres());
        filterDate.valueProperty().addListener((obs, old, val) -> appliquerFiltres());
        sortCombo.valueProperty().addListener((obs, old, val) -> appliquerFiltres());

        btnGererDispo.setOnAction(e -> naviguerVersDispo());
        btnReset.setOnAction(e -> resetFiltres());
    }

    private void initFiltres() {
        filterStatut.getItems().addAll("Tous les statuts", "⏳ En attente", "✓ Confirmé", "✕ Annulé", "⏰ Expiré", "📁 Passé");
        filterStatut.setValue("Tous les statuts");
        filterDate.getItems().addAll("Toutes les dates", "📅 Aujourd'hui", "📅 Cette semaine", "📅 Ce mois", "📅 Passés", "📅 À venir");
        filterDate.setValue("Toutes les dates");
        sortCombo.getItems().addAll("📆 Date ↓ (récent)", "📆 Date ↑ (ancien)", "👨‍⚕️ Médecin A→Z", "👨‍⚕️ Médecin Z→A", "📊 Statut A→Z");
        sortCombo.setValue("📆 Date ↓ (récent)");
    }

    private void resetFiltres() {
        searchField.clear();
        filterStatut.setValue("Tous les statuts");
        filterDate.setValue("Toutes les dates");
        sortCombo.setValue("📆 Date ↓ (récent)");
    }

    private void chargerDonnees() {
        try {
            tousLesRdvs = rdvService.findAll();
            calculerStats(tousLesRdvs);
            appliquerFiltres();
        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    private void calculerStats(List<rdv> rdvs) {
        String today = LocalDate.now().toString();
        LocalDate todayDate = LocalDate.now();
        long todayCount = rdvs.stream().filter(r -> r.getDate().equals(today)).count();
        long pendingCount = rdvs.stream().filter(r -> {
            boolean passe = false;
            try { passe = LocalDate.parse(r.getDate()).isBefore(todayDate); } catch (Exception ignored) {}
            return r.getStatut().toLowerCase().contains("attente") && !passe;
        }).count();
        long doneCount = rdvs.stream().filter(r -> r.getStatut().toLowerCase().contains("confirm")).count();
        lblTodayCount.setText(todayCount + " RDV");
        lblPendingCount.setText(pendingCount + " Demandes");
        lblDoneCount.setText(doneCount + " Consultations");
    }

    private void appliquerFiltres() {
        List<rdv> resultats = new ArrayList<>(tousLesRdvs);

        String recherche = searchField.getText();
        if (recherche != null && !recherche.trim().isEmpty()) {
            String lower = recherche.trim().toLowerCase();
            resultats = resultats.stream().filter(r -> {
                String medecin = r.getMedecin() != null ? r.getMedecin().toLowerCase() : "";
                String motif = r.getMotif() != null ? r.getMotif().toLowerCase() : "";
                String message = r.getMessage() != null ? r.getMessage().toLowerCase() : "";
                String statut = r.getStatut() != null ? r.getStatut().toLowerCase() : "";
                String date = r.getDate() != null ? r.getDate().toLowerCase() : "";
                return medecin.contains(lower) || motif.contains(lower) || message.contains(lower) || statut.contains(lower) || date.contains(lower);
            }).collect(Collectors.toList());
        }

        String statutFiltre = filterStatut.getValue();
        if (statutFiltre != null && !statutFiltre.equals("Tous les statuts")) {
            LocalDate todayFiltre = LocalDate.now();
            resultats = resultats.stream().filter(r -> {
                String s = r.getStatut().toLowerCase();

                if (statutFiltre.contains("Expiré")) return s.contains("expir");
                if (statutFiltre.contains("Passé")) return s.contains("passe") || s.contains("passé");
                if (statutFiltre.contains("attente")) return s.contains("attente");
                if (statutFiltre.contains("Confirmé")) return s.contains("confirm");
                if (statutFiltre.contains("Annulé")) return s.contains("annul");
                return true;
            }).collect(Collectors.toList());
        }

        String dateFiltre = filterDate.getValue();
        if (dateFiltre != null && !dateFiltre.equals("Toutes les dates")) {
            LocalDate today = LocalDate.now();
            resultats = resultats.stream().filter(r -> {
                try {
                    LocalDate dateRdv = LocalDate.parse(r.getDate());
                    if (dateFiltre.contains("Aujourd'hui")) return dateRdv.equals(today);
                    else if (dateFiltre.contains("Cette semaine")) {
                        LocalDate debut = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                        LocalDate fin = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                        return !dateRdv.isBefore(debut) && !dateRdv.isAfter(fin);
                    } else if (dateFiltre.contains("Ce mois")) return dateRdv.getMonth() == today.getMonth() && dateRdv.getYear() == today.getYear();
                    else if (dateFiltre.contains("Passés")) return dateRdv.isBefore(today);
                    else if (dateFiltre.contains("À venir")) return !dateRdv.isBefore(today);
                } catch (Exception e) { }
                return true;
            }).collect(Collectors.toList());
        }

        String tri = sortCombo.getValue();
        if (tri != null) {
            if (tri.contains("Date ↓")) resultats.sort((a, b) -> { try { return LocalDate.parse(b.getDate()).compareTo(LocalDate.parse(a.getDate())); } catch (Exception e) { return 0; } });
            else if (tri.contains("Date ↑")) resultats.sort((a, b) -> { try { return LocalDate.parse(a.getDate()).compareTo(LocalDate.parse(b.getDate())); } catch (Exception e) { return 0; } });
            else if (tri.contains("Médecin A→Z")) resultats.sort(Comparator.comparing(r -> r.getMedecin() != null ? r.getMedecin().toLowerCase() : ""));
            else if (tri.contains("Médecin Z→A")) resultats.sort((a, b) -> (b.getMedecin() != null ? b.getMedecin() : "").compareToIgnoreCase(a.getMedecin() != null ? a.getMedecin() : ""));
            else if (tri.contains("Statut")) resultats.sort(Comparator.comparing(r -> r.getStatut() != null ? r.getStatut().toLowerCase() : ""));
        }

        afficherLignes(resultats);
        lblResultCount.setText(resultats.size() + " résultat" + (resultats.size() > 1 ? "s" : ""));

        if (resultats.isEmpty()) {
            emptyState.setVisible(true); emptyState.setManaged(true);
            lblEmptyMsg.setText(recherche != null && !recherche.trim().isEmpty() ? "Aucun résultat pour \"" + recherche.trim() + "\"" : "Aucun rendez-vous trouvé pour ces filtres");
        } else {
            emptyState.setVisible(false); emptyState.setManaged(false);
        }
    }

    // ========== AFFICHER LIGNES ==========
    private void afficherLignes(List<rdv> rdvs) {
        rdvTableContainer.getChildren().clear();
        for (int i = 0; i < rdvs.size(); i++) {
            rdv r = rdvs.get(i);
            boolean isEven = i % 2 == 0;
            HBox ligne = new HBox();
            ligne.setAlignment(Pos.CENTER_LEFT);
            String bgColor = isEven ? "white" : "#f8fafc";
            ligne.setStyle("-fx-padding: 15 25; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0; -fx-background-color: " + bgColor + ";");
            String normalStyle = ligne.getStyle();
            ligne.setOnMouseEntered(e -> ligne.setStyle("-fx-padding: 15 25; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0; -fx-background-color: #eff6ff;"));
            ligne.setOnMouseExited(e -> ligne.setStyle(normalStyle));

            Label ref = new Label("#RDV-" + r.getId());
            ref.setPrefWidth(100);
            ref.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

            HBox patientBox = new HBox(8);
            patientBox.setAlignment(Pos.CENTER_LEFT); patientBox.setPrefWidth(250);
            Label avatar = new Label(getInitiales(r.getMedecin()));
            avatar.setMinWidth(34); avatar.setMinHeight(34); avatar.setMaxWidth(34); avatar.setMaxHeight(34);
            avatar.setAlignment(Pos.CENTER);
            avatar.setStyle("-fx-background-color: " + getAvatarColor(r.getMedecin()) + "; -fx-background-radius: 17; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold;");
            VBox patientInfo = new VBox(2);
            Label patientNom = new Label(r.getMedecin());
            patientNom.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            Label patientMotif = new Label(r.getMotif());
            patientMotif.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");
            patientInfo.getChildren().addAll(patientNom, patientMotif);
            patientBox.getChildren().addAll(avatar, patientInfo);

            HBox medecinBox = new HBox(6);
            medecinBox.setAlignment(Pos.CENTER_LEFT); medecinBox.setPrefWidth(220);
            Label medecinIcon = new Label("👨‍⚕️"); medecinIcon.setStyle("-fx-font-size: 14;");
            Label medecinNom = new Label(r.getMedecin());
            medecinNom.setStyle("-fx-font-size: 12; -fx-text-fill: #475569;");
            medecinBox.getChildren().addAll(medecinIcon, medecinNom);

            VBox dateBox = new VBox(2); dateBox.setPrefWidth(200);
            String dateFormatee = formaterDateAffichage(r.getDate());
            Label dateLabel = new Label(dateFormatee);
            dateLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            String heureAffichee = r.getHdebut() != null ? r.getHdebut() : "";
            if (heureAffichee.length() > 5) heureAffichee = heureAffichee.substring(0, 5);
            Label heureLabel = new Label("🕐 " + heureAffichee);
            heureLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");
            dateBox.getChildren().addAll(dateLabel, heureLabel);

            Label statut = new Label(); statut.setPrefWidth(150);
            String couleur, statutText, statutLower = r.getStatut().toLowerCase();

            // ── Déterminer si le RDV est dans le passé ──
            boolean estPasse = false;
            try {
                String dateStr = r.getDate();
                LocalDate dateRdv;

                // Essayer format ISO (2026-04-25)
                if (dateStr.contains("-") && dateStr.indexOf("-") == 4) {
                    dateRdv = LocalDate.parse(dateStr);
                }
                // Essayer format français (25/04/2026)
                else if (dateStr.contains("/")) {
                    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    dateRdv = LocalDate.parse(dateStr, fmt);
                }
                // Essayer format SQL date (25-04-2026)
                else {
                    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    dateRdv = LocalDate.parse(dateStr, fmt);
                }

                estPasse = dateRdv.isBefore(LocalDate.now());

                // DEBUG : afficher pour chaque RDV
                System.out.println("DEBUG: id=" + r.getId() + " date='" + dateStr + "' statut='" + r.getStatut() + "' estPasse=" + estPasse);
            } catch (Exception e) {
                System.err.println("⚠️ Date id=" + r.getId() + " raw='" + r.getDate() + "' len=" + r.getDate().length() + " → " + e.getMessage());
            }

            // ── Statut dynamique selon la date ──
            if (statutLower.contains("annul")) {
                couleur = "#dc2626"; statutText = "✕ Annulé";
            } else if (statutLower.contains("expir")) {
                couleur = "#9ca3af"; statutText = "⏰ Expiré";
            } else if (statutLower.contains("passe") || statutLower.contains("passé")) {
                couleur = "#6366f1"; statutText = "📁 Passé";
            } else if (estPasse && statutLower.contains("attente")) {
                couleur = "#9ca3af"; statutText = "⏰ Expiré";
            } else if (estPasse && statutLower.contains("confirm")) {
                couleur = "#6366f1"; statutText = "📁 Passé";
            } else if (statutLower.contains("confirm")) {
                couleur = "#16a34a"; statutText = "✓ Confirmé";
            } else if (statutLower.contains("attente")) {
                couleur = "#f59e0b"; statutText = "⏳ En attente";
            } else {
                couleur = "#64748b"; statutText = r.getStatut();
            }
            statut.setText(statutText);
            statut.setStyle("-fx-font-size: 11; -fx-text-fill: " + couleur + "; -fx-background-color: " + couleur + "18; -fx-background-radius: 12; -fx-padding: 5 12; -fx-font-weight: bold;");

            HBox actions = new HBox(6); actions.setAlignment(Pos.CENTER_LEFT);

            // ── Actions disponibles selon le statut ──
            boolean estAnnule = statutLower.contains("annul");
            boolean estExpire = statutLower.contains("expir");
            boolean estPasseStatut = statutLower.contains("passe") || statutLower.contains("passé");
            boolean aucuneAction = estAnnule || estExpire || estPasseStatut || estPasse;

            // Bouton Confirmer : seulement si en attente ET pas expiré/passé
            if (statutLower.contains("attente") && !estPasse) {
                Button btnConfirmer = new Button("✓");
                btnConfirmer.setTooltip(new Tooltip("Confirmer"));
                btnConfirmer.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;");
                btnConfirmer.setOnMouseEntered(ev -> btnConfirmer.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
                btnConfirmer.setOnMouseExited(ev -> btnConfirmer.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
                btnConfirmer.setOnAction(ev -> ouvrirPopupConfirmation(r));
                actions.getChildren().add(btnConfirmer);
            }

            // Boutons Modifier et Annuler : seulement si PAS annulé/expiré/passé
            if (!aucuneAction) {

                Button btnEdit = new Button("✏");
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnEdit.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;");
                btnEdit.setOnMouseEntered(ev -> btnEdit.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
                btnEdit.setOnMouseExited(ev -> btnEdit.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
                btnEdit.setOnAction(ev -> ouvrirPopupModifierAdmin(r));

                Button btnDelete = new Button("🗑");
                btnDelete.setTooltip(new Tooltip("Annuler le RDV"));
                btnDelete.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;");
                btnDelete.setOnMouseEntered(ev -> btnDelete.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
                btnDelete.setOnMouseExited(ev -> btnDelete.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
                btnDelete.setOnAction(ev -> ouvrirPopupAnnulation(r));

                actions.getChildren().addAll(btnEdit, btnDelete);
            } else if (aucuneAction) {
                // Pas d'actions pour les RDV annulés, expirés ou passés
                Label lblNoAction = new Label("—");
                lblNoAction.setStyle("-fx-font-size: 12; -fx-text-fill: #cbd5e1;");
                actions.getChildren().add(lblNoAction);
            }
            ligne.getChildren().addAll(ref, patientBox, medecinBox, dateBox, statut, actions);
            rdvTableContainer.getChildren().add(ligne);
        }
    }

    private String getInitiales(String nom) {
        if (nom == null || nom.isEmpty()) return "?";
        String[] parts = nom.replace("Dr.", "").replace("Dr", "").trim().split("\\s+");
        if (parts.length >= 2) return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
    }

    private String getAvatarColor(String nom) {
        if (nom == null) return "#64748b";
        int hash = Math.abs(nom.hashCode());
        String[] colors = {"#1a73e8", "#e53935", "#f59e0b", "#16a34a", "#8b5cf6", "#ec4899", "#06b6d4", "#f97316"};
        return colors[hash % colors.length];
    }

    // ═══════════════════════════════════════════════════════════════
    //  POPUP CONFIRMER RDV + EMAIL
    // ═══════════════════════════════════════════════════════════════

    private void ouvrirPopupConfirmation(rdv r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirmer le RDV");

        VBox content = new VBox(15);
        content.setPrefWidth(450);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 30 40;");

        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 36; -fx-background-color: #dcfce7; -fx-background-radius: 30; -fx-padding: 15;");

        Label titre = new Label("Confirmer ce rendez-vous ?");
        titre.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        VBox infoCard = new VBox(8);
        infoCard.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-padding: 15; -fx-border-color: #e2e8f0; -fx-border-radius: 10;");
        infoCard.getChildren().addAll(
                creerLigneInfo("👨‍⚕️ Médecin", r.getMedecin()),
                creerLigneInfo("📅 Date", formaterDateAffichage(r.getDate())),
                creerLigneInfo("🕐 Heure", r.getHdebut() != null ? r.getHdebut().substring(0, 5) : "N/A"),
                creerLigneInfo("📋 Motif", r.getMotif())
        );

        String emailPatient = obtenirEmailPatient(r);
        CheckBox checkEmail = new CheckBox("📧 Envoyer un email de confirmation à :");
        checkEmail.setSelected(true);
        checkEmail.setStyle("-fx-font-size: 13; -fx-text-fill: #334155;");
        Label lblEmail = new Label("   " + emailPatient);
        lblEmail.setStyle("-fx-font-size: 12; -fx-text-fill: #16a34a; -fx-font-weight: bold;");

        content.getChildren().addAll(icon, titre, infoCard, checkEmail, lblEmail);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.setText("✓ Confirmer le RDV");
        btnOk.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 30; -fx-cursor: hand;");
        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnCancel.setText("Annuler");
        btnCancel.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-size: 14; -fx-background-radius: 8; -fx-padding: 12 30;");

        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                rdvService.updateStatut(r.getId(), "confirmé");
                if (checkEmail.isSelected()) {
                    envoyerEmailEnArrierePlan("confirmation", r, emailPatient, null, null);
                }
                chargerDonnees();
            } catch (SQLException ex) {
                System.err.println(ex.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    //  POPUP ANNULER RDV + EMAIL
    // ═══════════════════════════════════════════════════════════════

    private void ouvrirPopupAnnulation(rdv r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Annuler le RDV");

        VBox content = new VBox(15);
        content.setPrefWidth(450);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 30 40;");

        Label icon = new Label("🗑");
        icon.setStyle("-fx-font-size: 36; -fx-background-color: #fef2f2; -fx-background-radius: 30; -fx-padding: 15;");
        Label titre = new Label("Annuler ce rendez-vous ?");
        titre.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label sousTitre = new Label("Le statut sera changé en Annulé.");
        sousTitre.setStyle("-fx-font-size: 13; -fx-text-fill: #94a3b8;");

        VBox infoCard = new VBox(8);
        infoCard.setStyle("-fx-background-color: #fef2f2; -fx-background-radius: 10; -fx-padding: 15; -fx-border-color: #fecaca; -fx-border-radius: 10;");
        infoCard.getChildren().addAll(
                creerLigneInfo("👨‍⚕️ Médecin", r.getMedecin()),
                creerLigneInfo("📅 Date", formaterDateAffichage(r.getDate())),
                creerLigneInfo("🕐 Heure", r.getHdebut() != null ? r.getHdebut().substring(0, 5) : "N/A"),
                creerLigneInfo("📋 Motif", r.getMotif())
        );

        String emailPatient = obtenirEmailPatient(r);
        CheckBox checkEmail = new CheckBox("📧 Envoyer un email d'annulation à :");
        checkEmail.setSelected(true);
        checkEmail.setStyle("-fx-font-size: 13; -fx-text-fill: #334155;");
        Label lblEmail = new Label("   " + emailPatient);
        lblEmail.setStyle("-fx-font-size: 12; -fx-text-fill: #dc2626; -fx-font-weight: bold;");

        content.getChildren().addAll(icon, titre, sousTitre, infoCard, checkEmail, lblEmail);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.setText("🗑 Annuler le RDV");
        btnOk.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 30; -fx-cursor: hand;");
        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnCancel.setText("Retour");
        btnCancel.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-size: 14; -fx-background-radius: 8; -fx-padding: 12 30;");

        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                rdvService.updateStatut(r.getId(), "annulé");
                if (checkEmail.isSelected()) {
                    envoyerEmailEnArrierePlan("annulation", r, emailPatient, null, null);
                }
                chargerDonnees();
            } catch (SQLException ex) {
                System.err.println(ex.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    //  POPUP MODIFIER RDV + EMAIL
    // ═══════════════════════════════════════════════════════════════

    private void ouvrirPopupModifierAdmin(rdv r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier Date & Heure");

        VBox content = new VBox(20);
        content.setPrefWidth(500);
        content.setStyle("-fx-padding: 25;");

        Label titre = new Label("✏ Modifier Date & Heure");
        titre.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label lblDate = new Label("Date");
        lblDate.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #334155;");
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

        Label lblHeure = new Label("Heure");
        lblHeure.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #334155;");
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
            if (!heureCombo.getItems().contains(heureCourte)) heureCombo.getItems().add(heureCourte);
            heureCombo.setValue(heureCourte);
        }

        HBox dateHeureBox = new HBox(15);
        VBox dateBox = new VBox(5, lblDate, datePicker);
        VBox heureBox = new VBox(5, lblHeure, heureCombo);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        HBox.setHgrow(heureBox, Priority.ALWAYS);
        dateHeureBox.getChildren().addAll(dateBox, heureBox);

        String emailPatient = obtenirEmailPatient(r);
        CheckBox checkEmail = new CheckBox("📧 Envoyer un email de modification à :");
        checkEmail.setSelected(true);
        checkEmail.setStyle("-fx-font-size: 13; -fx-text-fill: #334155;");
        Label lblEmail = new Label("   " + emailPatient);
        lblEmail.setStyle("-fx-font-size: 12; -fx-text-fill: #f97316; -fx-font-weight: bold;");

        Label lblErreur = new Label();
        lblErreur.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12;");
        lblErreur.setWrapText(true);
        lblErreur.setVisible(false);

        content.getChildren().addAll(titre, dateHeureBox, checkEmail, lblEmail, lblErreur);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnEnregistrer = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnEnregistrer.setText("✏ Enregistrer");
        btnEnregistrer.setStyle("-fx-background-color: #f97316; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 30; -fx-cursor: hand;");
        Button btnAnnuler = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnAnnuler.setText("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-size: 14; -fx-background-radius: 8; -fx-padding: 12 30;");

        btnEnregistrer.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            lblErreur.setVisible(false);
            if (datePicker.getValue() == null) { lblErreur.setText("❌ Veuillez choisir une date"); lblErreur.setVisible(true); event.consume(); return; }
            LocalDate dateOriginale = LocalDate.parse(r.getDate());
            if (datePicker.getValue().isBefore(LocalDate.now()) && !datePicker.getValue().equals(dateOriginale)) { lblErreur.setText("❌ La date ne peut pas être dans le passé"); lblErreur.setVisible(true); event.consume(); return; }
            if (heureCombo.getValue() == null) { lblErreur.setText("❌ Veuillez choisir une heure"); lblErreur.setVisible(true); event.consume(); return; }

            try {
                String ancienneDate = r.getDate();
                String ancienneHeure = r.getHdebut() != null ? r.getHdebut().substring(0, 5) : "";

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

                if (checkEmail.isSelected()) {
                    envoyerEmailEnArrierePlan("modification", r, emailPatient, ancienneDate, ancienneHeure);
                }

                chargerDonnees();
            } catch (SQLException ex) {
                lblErreur.setText("❌ Erreur : " + ex.getMessage());
                lblErreur.setVisible(true);
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    //  ENVOI EMAIL EN ARRIÈRE-PLAN
    // ═══════════════════════════════════════════════════════════════

    private void envoyerEmailEnArrierePlan(String type, rdv r, String emailPatient, String ancienneDate, String ancienneHeure) {
        new Thread(() -> {
            boolean succes;
            switch (type) {
                case "confirmation":
                    succes = emailService.envoyerEmailConfirmation(r, emailPatient);
                    break;
                case "annulation":
                    succes = emailService.envoyerEmailAnnulation(r, emailPatient);
                    break;
                case "modification":
                    succes = emailService.envoyerEmailModification(r, emailPatient, ancienneDate, ancienneHeure);
                    break;
                default:
                    succes = false;
            }

            boolean finalSucces = succes;
            javafx.application.Platform.runLater(() -> {
                Alert alert;
                if (finalSucces) {
                    alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("✅ Email envoyé");
                    alert.setHeaderText(null);
                    alert.setContentText("📧 Email de " + type + " envoyé avec succès à\n" + emailPatient);
                } else {
                    alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("⚠️ Erreur email");
                    alert.setHeaderText(null);
                    alert.setContentText("L'action a été effectuée mais l'email n'a pas pu être envoyé.\nVérifiez la connexion internet.");
                }
                alert.show();
            });
        }).start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private HBox creerLigneInfo(String label, String valeur) {
        HBox ligne = new HBox(10);
        ligne.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
        lbl.setMinWidth(110);
        Label val = new Label(valeur != null ? valeur : "N/A");
        val.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        ligne.getChildren().addAll(lbl, val);
        return ligne;
    }

    private String formaterDateAffichage(String dateISO) {
        try {
            LocalDate date = LocalDate.parse(dateISO);
            return String.format("%02d", date.getDayOfMonth()) + " " +
                    date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + date.getYear();
        } catch (Exception e) { return dateISO; }
    }

    private String obtenirEmailPatient(rdv rdv) {
        return "hmaied.nada1@gmail.com";
    }

    private void naviguerVersDispo() {
        try {
            Node content = FXMLLoader.load(getClass().getResource("/AdminDispo.fxml"));
            StackPane parent = (StackPane) rdvTableContainer.getScene().lookup("#contentArea");
            if (parent != null) { parent.getChildren().clear(); parent.getChildren().add(content); }
        } catch (IOException e) { System.err.println("Erreur : " + e.getMessage()); }
    }
}