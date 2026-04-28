package Controllers;

import Models.rdv;
import Models.medecin;
import Services.RdvService;
import Services.MedecinService;
import Services.SocketClient;
import Services.SocketServer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.animation.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class AfficherRdvController {

    @FXML private VBox rdvListContainer;
    @FXML private TextField searchField;
    @FXML private Button btnAjouter;
    @FXML private Button btnHistorique;
    @FXML private Button btnLancerVisio;       // ← NOUVEAU
    @FXML private HBox carteProchainRdv;
    @FXML private Label prochainMedecin, prochainMotif, prochainMois, prochainJour, prochainHeure;
    @FXML private Label tabAVenir, tabPasses, tabAnnules;
    @FXML private Label calendarMonthLabel;
    @FXML private GridPane calendarGrid;
    @FXML private Button btnCalPrev, btnCalNext;

    @FXML private VBox medecinListContainer;
    @FXML private ScrollPane medecinScrollPane;
    @FXML private Label lblVoirTout;
    @FXML private TextField searchMedecinField;

    private RdvService rdvService = new RdvService();
    private MedecinService medecinService = new MedecinService();
    private ObservableList<rdv> rdvList;
    private List<medecin> tousLesMedecins = new ArrayList<>();
    private String ongletActif = "avenir";
    private YearMonth currentCalendarMonth = YearMonth.now();
    private VBox searchDropdown;
    private javafx.stage.Popup searchPopup;
    private rdv prochainRdv; // ← NOUVEAU : stocker le prochain RDV

    private static final int PATIENT_ID_COURANT = 1;

    @FXML
    public void initialize() {
        SocketServer serveur = SocketServer.getInstance();
        if (!serveur.isRunning()) serveur.demarrer();

        chargerDonnees();
        btnAjouter.setOnAction(e -> ouvrirPopupAjouter());
        btnHistorique.setOnAction(e -> ouvrirHistorique());

        initSearchDropdown();
        searchField.textProperty().addListener((obs, old, val) -> {
            afficherCartes(val);
            afficherSearchDropdown(val);
        });
        searchField.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) new Timeline(new KeyFrame(Duration.millis(200), e -> fermerDropdown())).play();
            else if (searchField.getText() != null && !searchField.getText().isEmpty()) afficherSearchDropdown(searchField.getText());
        });

        tabAVenir.setOnMouseClicked(e -> changerOnglet("avenir"));
        tabPasses.setOnMouseClicked(e -> changerOnglet("passes"));
        tabAnnules.setOnMouseClicked(e -> changerOnglet("annules"));

        animerCarteBleue();

        btnCalPrev.setOnAction(e -> { currentCalendarMonth = currentCalendarMonth.minusMonths(1); buildCalendar(); });
        btnCalNext.setOnAction(e -> { currentCalendarMonth = currentCalendarMonth.plusMonths(1); buildCalendar(); });
        buildCalendar();

        chargerMedecins();
        lblVoirTout.setOnMouseClicked(e -> ouvrirPopupTousLesMedecins());
        searchMedecinField.textProperty().addListener((obs, old, val) -> filtrerMedecins(val));
    }

    // ══════════════════════════════════════════════
    //  ANIMATION CARTE BLEUE
    // ══════════════════════════════════════════════
    private void animerCarteBleue() {
        carteProchainRdv.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(800), carteProchainRdv);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(800), carteProchainRdv);
        slide.setFromY(30); slide.setToY(0);
        ParallelTransition entree = new ParallelTransition(fade, slide);

        TranslateTransition flottement = new TranslateTransition(Duration.millis(2000), carteProchainRdv);
        flottement.setFromY(0); flottement.setToY(-6);
        flottement.setCycleCount(Animation.INDEFINITE); flottement.setAutoReverse(true);
        flottement.setInterpolator(Interpolator.EASE_BOTH);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#4a6cf7", 0.4)); glow.setRadius(15);
        carteProchainRdv.setEffect(glow);
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 15)),
                new KeyFrame(Duration.millis(1500), new KeyValue(glow.radiusProperty(), 25))
        );
        pulse.setCycleCount(Animation.INDEFINITE); pulse.setAutoReverse(true);
        entree.setOnFinished(e -> { flottement.play(); pulse.play(); });
        entree.play();
    }

    // ══════════════════════════════════════════════
    //  CALENDRIER
    // ══════════════════════════════════════════════
    private void buildCalendar() {
        calendarGrid.getChildren().clear();
        String moisNom = currentCalendarMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        calendarMonthLabel.setText(moisNom.substring(0, 1).toUpperCase() + moisNom.substring(1) + " " + currentCalendarMonth.getYear());
        LocalDate first = currentCalendarMonth.atDay(1);
        int startDow = first.getDayOfWeek().getValue() - 1;
        int lastDay = currentCalendarMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();
        int row = 0;
        for (int d = 1; d <= lastDay; d++) {
            LocalDate date = currentCalendarMonth.atDay(d);
            int col = (startDow + d - 1) % 7; if (col == 0 && d > 1) row++;
            Button btn = new Button(String.valueOf(d)); btn.setPrefWidth(36); btn.setPrefHeight(30);
            boolean isPast = date.isBefore(today); boolean isToday = date.equals(today);
            boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (isPast) { btn.setStyle("-fx-background-color: transparent; -fx-font-size: 12; -fx-text-fill: #ccc; -fx-background-radius: 8;"); btn.setDisable(true); }
            else if (isToday) btn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
            else btn.setStyle("-fx-background-color: transparent; -fx-font-size: 12; -fx-background-radius: 8; -fx-cursor: hand; -fx-text-fill: " + (isWeekend ? "#e53935" : "#333") + ";");
            if (!isPast) {
                btn.setCursor(javafx.scene.Cursor.HAND);
                LocalDate clickDate = date;
                btn.setOnAction(e -> { CalendrierWizardController wizard = new CalendrierWizardController(); activerBlur(); wizard.openWithDate(rdvListContainer.getScene().getWindow(), clickDate, () -> chargerDonnees()); desactiverBlur(); });
                if (!isToday) { String ns = btn.getStyle(); btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1a73e8; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;")); btn.setOnMouseExited(e -> btn.setStyle(ns)); }
            }
            calendarGrid.add(btn, col, row);
        }
    }

    // ══════════════════════════════════════════════
    //  CHARGER DONNÉES
    // ══════════════════════════════════════════════
    public void chargerDonnees() {
        try {
            rdvList = FXCollections.observableArrayList(rdvService.findAll());
            gererStatutsAutomatiques();
            mettreAJourProchainRdv();
            afficherCartes(null);
        } catch (SQLException e) { afficherErreur("Erreur chargement : " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════
    //  GESTION STATUTS AUTOMATIQUES
    // ══════════════════════════════════════════════
    private void gererStatutsAutomatiques() {
        LocalDateTime now = LocalDateTime.now();
        for (rdv r : rdvList) {
            try {
                LocalDate dateRdv = LocalDate.parse(r.getDate());
                String hfinStr = r.getHfin();
                if (hfinStr == null || hfinStr.length() < 5) continue;
                LocalTime hfinTime = LocalTime.parse(hfinStr.substring(0, 5));
                LocalDateTime rdvFinDateTime = LocalDateTime.of(dateRdv, hfinTime);
                String statut = r.getStatut().toLowerCase().trim();
                if ((statut.equals("confirme") || statut.equals("confirmé")) && now.isAfter(rdvFinDateTime)) { r.setStatut("Termine"); rdvService.updateStatut(r.getId(), "Termine"); }
                if (statut.equals("termine") && now.isAfter(rdvFinDateTime.plusHours(24))) { r.setStatut("passe"); rdvService.updateStatut(r.getId(), "passe"); }
                if (statut.contains("attente") && dateRdv.isBefore(LocalDate.now())) { r.setStatut("expiré"); rdvService.updateStatut(r.getId(), "expiré"); }
            } catch (Exception e) { System.err.println("Erreur statut RDV #" + r.getId() + " : " + e.getMessage()); }
        }
    }

    // ══════════════════════════════════════════════
    //  PROCHAIN RDV + BOUTON VISIO
    // ══════════════════════════════════════════════
    private void mettreAJourProchainRdv() {
        LocalDateTime now = LocalDateTime.now();
        prochainRdv = rdvList.stream()
                .filter(r -> {
                    try {
                        String statut = r.getStatut().toLowerCase().trim();
                        if (statut.contains("annul") || statut.contains("expir") || statut.equals("passe") || statut.equals("termine")) return false;
                        LocalDate dateRdv = LocalDate.parse(r.getDate());
                        String hdebut = r.getHdebut();
                        if (hdebut == null || hdebut.length() < 5) return false;
                        LocalDateTime rdvDebut = LocalDateTime.of(dateRdv, LocalTime.parse(hdebut.substring(0, 5)));
                        return rdvDebut.isAfter(now);
                    } catch (Exception e) { return false; }
                })
                .findFirst().orElse(null);

        if (prochainRdv == null) {
            prochainMedecin.setText("Aucun RDV à venir");
            prochainMotif.setText(""); prochainJour.setText("-"); prochainMois.setText(""); prochainHeure.setText("");
            configurerBoutonVisio(null);
            return;
        }

        prochainMedecin.setText(prochainRdv.getMedecin());
        prochainMotif.setText(prochainRdv.getMotif());
        prochainHeure.setText(prochainRdv.getHdebut());
        try {
            LocalDate date = LocalDate.parse(prochainRdv.getDate());
            prochainMois.setText(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH).toUpperCase());
            prochainJour.setText(String.valueOf(date.getDayOfMonth()));
        } catch (Exception e) { prochainMois.setText(""); prochainJour.setText(prochainRdv.getDate()); }

        configurerBoutonVisio(prochainRdv);
    }

    // ══════════════════════════════════════════════
    //  LOGIQUE BOUTON VISIO
    // ══════════════════════════════════════════════
    private void configurerBoutonVisio(rdv r) {
        if (btnLancerVisio == null) return;

        // Arrêter toute animation en cours
        btnLancerVisio.setScaleX(1.0);
        btnLancerVisio.setScaleY(1.0);

        if (r == null) {
            // Aucun RDV → cacher
            btnLancerVisio.setVisible(false);
            btnLancerVisio.setManaged(false);
            return;
        }

        boolean estEnLigne  = "en_ligne".equalsIgnoreCase(r.getTypeConsultation());
        boolean estAujourdHui = LocalDate.parse(r.getDate()).equals(LocalDate.now());
        boolean estConfirme = r.getStatut().toLowerCase().contains("confirm");

        if (!estEnLigne) {
            // Présentiel → cacher le bouton
            btnLancerVisio.setVisible(false);
            btnLancerVisio.setManaged(false);
            return;
        }

        // C'est en ligne → toujours visible
        btnLancerVisio.setVisible(true);
        btnLancerVisio.setManaged(true);

        if (estAujourdHui && estConfirme) {
            // ✅ Jour J + confirmé → ROUGE + pulsation
            btnLancerVisio.setStyle(
                    "-fx-background-color: #dc2626; -fx-text-fill: white; " +
                            "-fx-font-size: 12; -fx-font-weight: bold; " +
                            "-fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;"
            );
            btnLancerVisio.setDisable(false);
            btnLancerVisio.setText("📹 Lancer Visio");

            ScaleTransition pulse = new ScaleTransition(Duration.millis(800), btnLancerVisio);
            pulse.setFromX(1.0); pulse.setFromY(1.0);
            pulse.setToX(1.1); pulse.setToY(1.1);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();

            btnLancerVisio.setOnAction(e -> lancerVisio(r));

        } else if (!estConfirme) {
            // En attente → grisé
            btnLancerVisio.setStyle(
                    "-fx-background-color: #e2e8f0; -fx-text-fill: #94a3b8; " +
                            "-fx-font-size: 12; -fx-background-radius: 8; -fx-padding: 8 15;"
            );
            btnLancerVisio.setDisable(true);
            btnLancerVisio.setText("📹 En attente de confirmation");

        } else {
            // Confirmé mais pas encore aujourd'hui → bleu grisé
            btnLancerVisio.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: rgba(255,255,255,0.6); " +
                            "-fx-font-size: 12; -fx-background-radius: 8; " +
                            "-fx-padding: 8 15; -fx-border-color: rgba(255,255,255,0.4); -fx-border-radius: 8;"
            );
            btnLancerVisio.setDisable(true);
            btnLancerVisio.setText("📹 Visio le " + LocalDate.parse(r.getDate())
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")));
        }
    }

    // ══════════════════════════════════════════════
    //  LANCER LA VISIO
    // ══════════════════════════════════════════════
    private void lancerVisio(rdv r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VideoCall.fxml"));
            VBox root = loader.load();
            VideoCallController ctrl = loader.getController();
            ctrl.initCall(r, r.getMedecin());

            Stage stage = new Stage();
            stage.setTitle("📹 Consultation Vidéo — " + r.getMedecin());
            stage.setScene(new Scene(root, 900, 650));
            stage.setMinWidth(700);
            stage.setMinHeight(500);
            stage.initOwner(rdvListContainer.getScene().getWindow());
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            afficherErreur("Impossible de lancer la visio : " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════
    //  ONGLETS
    // ══════════════════════════════════════════════
    private void changerOnglet(String onglet) {
        ongletActif = onglet;
        String styleActif   = "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1a73e8; -fx-border-color: #1a73e8; -fx-border-width: 0 0 2 0; -fx-padding: 0 5 8 5; -fx-cursor: hand;";
        String styleInactif = "-fx-font-size: 14; -fx-text-fill: #999; -fx-padding: 0 5 8 5; -fx-cursor: hand;";
        tabAVenir.setStyle(onglet.equals("avenir")   ? styleActif : styleInactif);
        tabPasses.setStyle(onglet.equals("passes")   ? styleActif : styleInactif);
        tabAnnules.setStyle(onglet.equals("annules") ? styleActif : styleInactif);
        afficherCartes(searchField.getText());
    }

    // ══════════════════════════════════════════════
    //  AFFICHER CARTES
    // ══════════════════════════════════════════════
    private void afficherCartes(String recherche) {
        rdvListContainer.getChildren().clear();
        for (rdv r : rdvList) {
            boolean afficher = false;
            try {
                LocalDate dateRdv = LocalDate.parse(r.getDate());
                String statut = r.getStatut().toLowerCase().trim();
                boolean estPasse   = dateRdv.isBefore(LocalDate.now());
                boolean estAnnule  = statut.contains("annul");
                boolean estExpire  = statut.contains("expir");
                boolean estTermine = statut.equals("termine");
                boolean estPasseDB = statut.equals("passe");
                switch (ongletActif) {
                    case "avenir":  afficher = !estPasse && !estAnnule && !estExpire && !estTermine && !estPasseDB; break;
                    case "passes":  afficher = estPasse || estExpire || estTermine || estPasseDB; break;
                    case "annules": afficher = estAnnule; break;
                }
            } catch (Exception e) { afficher = ongletActif.equals("avenir"); }
            if (!afficher) continue;
            if (recherche != null && !recherche.isEmpty()) {
                String lower = recherche.toLowerCase();
                if (!r.getMedecin().toLowerCase().contains(lower) && !r.getMotif().toLowerCase().contains(lower) &&
                        !r.getDate().toLowerCase().contains(lower) && !r.getStatut().toLowerCase().contains(lower)) continue;
            }
            rdvListContainer.getChildren().add(creerCarteRdv(r));
        }
        if (rdvListContainer.getChildren().isEmpty()) {
            String msg; switch (ongletActif) {
                case "passes":  msg = "Aucun rendez-vous passé";   break;
                case "annules": msg = "Aucun rendez-vous annulé";  break;
                default:        msg = "Aucun rendez-vous à venir"; break;
            }
            Label vide = new Label(msg); vide.setStyle("-fx-font-size: 14; -fx-text-fill: #999; -fx-font-style: italic; -fx-padding: 30;");
            rdvListContainer.getChildren().add(vide);
        }
    }

    // ══════════════════════════════════════════════
    //  CRÉER CARTE RDV
    // ══════════════════════════════════════════════
    private HBox creerCarteRdv(rdv r) {
        String statut = r.getStatut().toLowerCase().trim();
        HBox carte = new HBox(15); carte.setAlignment(Pos.CENTER_LEFT);
        boolean estTermineSansFeedback = statut.equals("termine") && r.getFeedbackNote() == null;
        String borderColor = estTermineSansFeedback ? "#fb923c" : "#eee";
        carte.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: " + borderColor + "; -fx-padding: 18 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        carte.setOnMouseEntered(e -> carte.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #fed7aa; -fx-padding: 18 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 12, 0, 0, 4); -fx-scale-x: 1.01; -fx-scale-y: 1.01;"));
        carte.setOnMouseExited(e -> carte.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: " + borderColor + "; -fx-padding: 18 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"));

        // Date badge
        VBox dateBadge = new VBox(2); dateBadge.setAlignment(Pos.CENTER); dateBadge.setMinWidth(55);
        dateBadge.setStyle("-fx-background-color: #fff5f5; -fx-background-radius: 10; -fx-padding: 10;");
        String moisStr = "", jourStr = "";
        try { LocalDate date = LocalDate.parse(r.getDate()); moisStr = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH).toUpperCase(); jourStr = String.valueOf(date.getDayOfMonth()); }
        catch (Exception e) { moisStr = ""; jourStr = r.getDate(); }
        Label moisLabel = new Label(moisStr); moisLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #e53935;");
        Label jourLabel = new Label(jourStr); jourLabel.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #e53935;");
        dateBadge.getChildren().addAll(moisLabel, jourLabel);

        // Infos
        VBox infos = new VBox(4); HBox.setHgrow(infos, Priority.ALWAYS);
        Label medecinLabel = new Label(r.getMedecin()); medecinLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #333;");
        Label motifLabel = new Label(r.getMotif()); motifLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #777;");

        // Badge type consultation dans la carte
        String typeIcon = "en_ligne".equalsIgnoreCase(r.getTypeConsultation()) ? "📹 En ligne" : "🏥 Présentiel";
        String typeBg   = "en_ligne".equalsIgnoreCase(r.getTypeConsultation()) ? "#eff6ff" : "#f0fdf4";
        String typeColor = "en_ligne".equalsIgnoreCase(r.getTypeConsultation()) ? "#1d4ed8" : "#15803d";
        Label typeLabel = new Label(typeIcon);
        typeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: " + typeColor + "; -fx-background-color: " + typeBg + "; -fx-background-radius: 8; -fx-padding: 3 8;");

        Label detailsLabel = new Label("🕐 " + r.getHdebut() + " - " + r.getHfin() + "    💬 " + r.getMessage());
        detailsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
        infos.getChildren().addAll(medecinLabel, motifLabel, typeLabel, detailsLabel);

        if (r.getFeedbackNote() != null) {
            String emoji = obtenirEmojisNote(r.getFeedbackNote());
            HBox feedbackRow = new HBox(6); feedbackRow.setAlignment(Pos.CENTER_LEFT);
            Label feedbackLabel = new Label(emoji + "  " + (r.getFeedbackCommentaire() != null && !r.getFeedbackCommentaire().isEmpty() ? "\"" + r.getFeedbackCommentaire() + "\"" : "Avis donné ✓"));
            feedbackLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #f97316; -fx-font-style: italic; -fx-background-color: #fff7ed; -fx-background-radius: 8; -fx-padding: 3 8;");
            feedbackRow.getChildren().add(feedbackLabel); infos.getChildren().add(feedbackRow);
        }

        // Statut badge
        String couleurStatut, statutText;
        switch (statut) {
            case "confirme": case "confirmé": couleurStatut = "#4caf50"; statutText = "✓ Confirmé"; break;
            case "annule": case "annulé":     couleurStatut = "#e53935"; statutText = "✕ Annulé"; break;
            case "expiré": case "expire":     couleurStatut = "#9e9e9e"; statutText = "⏰ Expiré"; break;
            case "termine":                   couleurStatut = "#f97316"; statutText = "✅ Terminé"; break;
            case "passe":                     couleurStatut = "#607d8b"; statutText = "📁 Passé"; break;
            default:                          couleurStatut = "#ff9800"; statutText = "● En attente"; break;
        }
        Label statutBadge = new Label(statutText);
        statutBadge.setStyle("-fx-font-size: 12; -fx-text-fill: " + couleurStatut + "; -fx-background-color: " + couleurStatut + "22; -fx-background-radius: 15; -fx-padding: 5 12;");

        // Boutons
        Button btnModifier = new Button("✏");
        btnModifier.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555; -fx-font-size: 14; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");
        btnModifier.setOnAction(e -> ouvrirPopupModifier(r));
        Button btnVoir = new Button("👁");
        btnVoir.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555; -fx-font-size: 14; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");
        btnVoir.setOnAction(e -> ouvrirShowOne(r));
        Button btnSupprimer = new Button("🗑");
        btnSupprimer.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #e53935; -fx-font-size: 14; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");
        btnSupprimer.setOnAction(e -> supprimerRdv(r));
        HBox boutonsBox = new HBox(8, btnModifier, btnVoir, btnSupprimer); boutonsBox.setAlignment(Pos.CENTER);

        if (statut.contains("expir") || statut.contains("annul") || statut.equals("passe") || statut.equals("termine")) {
            btnModifier.setDisable(true); btnModifier.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #ccc; -fx-font-size: 14; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36;");
            btnSupprimer.setDisable(true); btnSupprimer.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #ccc; -fx-font-size: 14; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36;");
        }

        if ((statut.equals("termine") || statut.equals("passe")) && r.getFeedbackNote() == null) {
            Button btnFeedback = new Button("⭐ Donner mon avis");
            btnFeedback.setStyle("-fx-background-color: linear-gradient(to right, #f97316, #fb923c); -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 7 16; -fx-cursor: hand;");
            btnFeedback.setOnMouseEntered(e -> btnFeedback.setStyle("-fx-background-color: linear-gradient(to right, #ea6c00, #f97316); -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 7 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(249,115,22,0.4), 10, 0, 0, 3);"));
            btnFeedback.setOnMouseExited(e -> btnFeedback.setStyle("-fx-background-color: linear-gradient(to right, #f97316, #fb923c); -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 7 16; -fx-cursor: hand;"));
            ScaleTransition pulse = new ScaleTransition(Duration.millis(900), btnFeedback);
            pulse.setFromX(1.0); pulse.setFromY(1.0); pulse.setToX(1.06); pulse.setToY(1.06);
            pulse.setCycleCount(Animation.INDEFINITE); pulse.setAutoReverse(true); pulse.play();
            btnFeedback.setOnAction(e -> ouvrirFeedbackPopup(r));
            boutonsBox.getChildren().add(0, btnFeedback);
        }

        carte.getChildren().addAll(dateBadge, infos, statutBadge, boutonsBox);
        return carte;
    }

    // ══════════════════════════════════════════════
    //  FEEDBACK
    // ══════════════════════════════════════════════
    private void ouvrirFeedbackPopup(rdv r) {
        activerBlur();
        new FeedbackPopupController().show(r, () -> { desactiverBlur(); chargerDonnees(); }, () -> desactiverBlur());
    }

    private String obtenirEmojisNote(int note) {
        switch (note) { case 1: return "😞"; case 2: return "😕"; case 3: return "😐"; case 4: return "😊"; case 5: return "🤩"; default: return "⭐"; }
    }

    // ══════════════════════════════════════════════
    //  POPUP AJOUTER
    // ══════════════════════════════════════════════
    private void ouvrirPopupAjouter() {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Ajouter un rendez-vous");
        VBox content = new VBox(18); content.setPrefWidth(520); content.setStyle("-fx-padding: 25;");
        Label titre = new Label("📅  Ajouter un rendez-vous"); titre.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #333;");
        Label lblMedecin = new Label("Médecin *"); lblMedecin.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        ComboBox<String> medecinCombo = new ComboBox<>(); medecinCombo.setPromptText("Choisir un médecin");
        medecinCombo.getItems().addAll("Dr. Sarah Amrani", "Dr. Ali Zouhaier", "Dr. M. Kallel");
        medecinCombo.setMaxWidth(Double.MAX_VALUE); medecinCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");
        Label lblMotif = new Label("Motif *"); lblMotif.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        ComboBox<String> motifCombo = new ComboBox<>(); motifCombo.setPromptText("Choisir un motif");
        motifCombo.getItems().addAll("Consultation", "Suivi médical", "Urgence", "Contrôle", "Autre");
        motifCombo.setMaxWidth(Double.MAX_VALUE); motifCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");
        Label lblDate = new Label("Date *"); lblDate.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        DatePicker datePicker = new DatePicker(); datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setDayCellFactory(picker -> new DateCell() { @Override public void updateItem(LocalDate date, boolean empty) { super.updateItem(date, empty); if (date.isBefore(LocalDate.now())) { setDisable(true); setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #ccc;"); } } });
        Label lblHeure = new Label("Heure *"); lblHeure.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        ComboBox<String> heureCombo = new ComboBox<>(); heureCombo.setMaxWidth(Double.MAX_VALUE);
        for (int h = 9; h <= 16; h++) { heureCombo.getItems().add(String.format("%02d:00", h)); heureCombo.getItems().add(String.format("%02d:30", h)); } heureCombo.getItems().add("17:00");
        HBox dateHeureBox = new HBox(15); VBox dateBox = new VBox(5, lblDate, datePicker); VBox heureBox = new VBox(5, lblHeure, heureCombo);
        HBox.setHgrow(dateBox, Priority.ALWAYS); HBox.setHgrow(heureBox, Priority.ALWAYS); dateHeureBox.getChildren().addAll(dateBox, heureBox);
        Label lblMessage = new Label("Message (optionnel)"); lblMessage.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        TextArea messageArea = new TextArea(); messageArea.setPrefRowCount(3); messageArea.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");
        Label lblErreur = new Label(); lblErreur.setStyle("-fx-text-fill: #e53935; -fx-font-size: 12;"); lblErreur.setVisible(false);
        content.getChildren().addAll(titre, new Separator(), lblMedecin, medecinCombo, lblMotif, motifCombo, dateHeureBox, lblMessage, messageArea, lblErreur);
        dialog.getDialogPane().setContent(content); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK); btnOk.setText("Confirmer");
        btnOk.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 35; -fx-cursor: hand;");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Annuler");
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (medecinCombo.getValue() == null) { lblErreur.setText("❌ Veuillez choisir un médecin"); lblErreur.setVisible(true); event.consume(); return; }
            if (motifCombo.getValue() == null)   { lblErreur.setText("❌ Veuillez choisir un motif");   lblErreur.setVisible(true); event.consume(); return; }
            if (datePicker.getValue() == null)   { lblErreur.setText("❌ Veuillez choisir une date");   lblErreur.setVisible(true); event.consume(); return; }
            if (heureCombo.getValue() == null)   { lblErreur.setText("❌ Veuillez choisir une heure");  lblErreur.setVisible(true); event.consume(); return; }
            try {
                String hdebut = heureCombo.getValue(); String[] parts = hdebut.split(":"); int h = Integer.parseInt(parts[0]); int m = Integer.parseInt(parts[1]);
                m += 30; if (m >= 60) { m -= 60; h++; } String hfin = String.format("%02d:%02d", h, m);
                rdv rv = new rdv(datePicker.getValue().toString(), hdebut, hfin, "en_attente", motifCombo.getValue(), medecinCombo.getValue(), messageArea.getText().trim(), 1, 1);
                rv.setTypeConsultation("presentiel");
                rdvService.insert(rv); chargerDonnees(); afficherSucces("RDV ajouté avec succès !");
            } catch (SQLException ex) { afficherErreur("Erreur : " + ex.getMessage()); event.consume(); }
        });
        activerBlur(); dialog.showAndWait(); desactiverBlur();
    }

    // ══════════════════════════════════════════════
    //  POPUP MODIFIER
    // ══════════════════════════════════════════════
    private void ouvrirPopupModifier(rdv r) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Modifier le rendez-vous");
        VBox content = new VBox(18); content.setPrefWidth(520); content.setStyle("-fx-padding: 25;");
        Label titre = new Label("✏  Modifier le rendez-vous"); titre.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #333;");
        Label lblMedecin = new Label("Médecin"); lblMedecin.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        TextField medecinField = new TextField(r.getMedecin()); medecinField.setEditable(false);
        medecinField.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd; -fx-background-color: #f5f5f5; -fx-text-fill: #888; -fx-padding: 8;"); medecinField.setMaxWidth(Double.MAX_VALUE);
        Label lblMotif = new Label("Motif *"); lblMotif.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        ComboBox<String> motifCombo = new ComboBox<>(); motifCombo.getItems().addAll("Consultation", "Suivi médical", "Urgence", "Contrôle", "Autre");
        if (r.getMotif() != null && !motifCombo.getItems().contains(r.getMotif())) motifCombo.getItems().add(r.getMotif());
        motifCombo.setValue(r.getMotif()); motifCombo.setMaxWidth(Double.MAX_VALUE);
        LocalDate dateOriginale = LocalDate.parse(r.getDate());
        DatePicker datePicker = new DatePicker(dateOriginale); datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setDayCellFactory(picker -> new DateCell() { @Override public void updateItem(LocalDate date, boolean empty) { super.updateItem(date, empty); if (date.isBefore(LocalDate.now()) && !date.equals(dateOriginale)) { setDisable(true); setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #ccc;"); } } });
        ComboBox<String> heureCombo = new ComboBox<>(); heureCombo.setMaxWidth(Double.MAX_VALUE);
        for (int h = 9; h <= 16; h++) { heureCombo.getItems().add(String.format("%02d:00", h)); heureCombo.getItems().add(String.format("%02d:30", h)); } heureCombo.getItems().add("17:00");
        String heureActuelle = r.getHdebut(); if (heureActuelle != null && heureActuelle.length() >= 5) { String hc = heureActuelle.substring(0, 5); if (!heureCombo.getItems().contains(hc)) heureCombo.getItems().add(hc); heureCombo.setValue(hc); }
        HBox dateHeureBox = new HBox(15); VBox dateBox = new VBox(5, new Label("Date *"), datePicker); VBox heureBox = new VBox(5, new Label("Heure *"), heureCombo);
        HBox.setHgrow(dateBox, Priority.ALWAYS); HBox.setHgrow(heureBox, Priority.ALWAYS); dateHeureBox.getChildren().addAll(dateBox, heureBox);
        TextArea messageArea = new TextArea(); messageArea.setText(r.getMessage()); messageArea.setPrefRowCount(3);
        Label lblErreur = new Label(); lblErreur.setStyle("-fx-text-fill: #e53935; -fx-font-size: 12;"); lblErreur.setVisible(false);
        content.getChildren().addAll(titre, new Separator(), lblMedecin, medecinField, lblMotif, motifCombo, dateHeureBox, new Label("Message"), messageArea, lblErreur);
        dialog.getDialogPane().setContent(content); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK); btnOk.setText("Enregistrer");
        btnOk.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 35; -fx-cursor: hand;");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Annuler");
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (motifCombo.getValue() == null) { lblErreur.setText("❌ Veuillez choisir un motif"); lblErreur.setVisible(true); event.consume(); return; }
            if (datePicker.getValue() == null) { lblErreur.setText("❌ Veuillez choisir une date"); lblErreur.setVisible(true); event.consume(); return; }
            if (heureCombo.getValue() == null) { lblErreur.setText("❌ Veuillez choisir une heure"); lblErreur.setVisible(true); event.consume(); return; }
            try {
                String hdebut = heureCombo.getValue(); String[] parts = hdebut.split(":"); int h = Integer.parseInt(parts[0]); int m = Integer.parseInt(parts[1]);
                m += 30; if (m >= 60) { m -= 60; h++; }
                r.setDate(datePicker.getValue().toString()); r.setHdebut(hdebut); r.setHfin(String.format("%02d:%02d", h, m));
                r.setMotif(motifCombo.getValue()); r.setMessage(messageArea.getText().trim());
                rdvService.update(r); chargerDonnees(); afficherSucces("RDV modifié avec succès !");
            } catch (SQLException ex) { afficherErreur("Erreur : " + ex.getMessage()); event.consume(); }
        });
        activerBlur(); dialog.showAndWait(); desactiverBlur();
    }

    // ══════════════════════════════════════════════
    //  SUPPRIMER
    // ══════════════════════════════════════════════
    private void supprimerRdv(rdv r) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Supprimer");
        VBox content = new VBox(15); content.setPrefWidth(400); content.setAlignment(Pos.CENTER); content.setStyle("-fx-padding: 30 40;");
        Label icon = new Label("🗑"); icon.setStyle("-fx-font-size: 32; -fx-background-color: #ffebee; -fx-background-radius: 30; -fx-padding: 15;");
        Label titre = new Label("Supprimer ce rendez-vous ?"); titre.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #333;");
        Label sousTitre = new Label("Cette action est irréversible."); sousTitre.setStyle("-fx-font-size: 13; -fx-text-fill: #888;");
        content.getChildren().addAll(icon, titre, sousTitre);
        dialog.getDialogPane().setContent(content); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button btnSup = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK); btnSup.setText("Supprimer");
        btnSup.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 40; -fx-cursor: hand;");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Annuler");
        btnSup.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try { rdvService.delete(r); chargerDonnees(); afficherSucces("RDV supprimé !"); }
            catch (SQLException ex) { afficherErreur("Erreur : " + ex.getMessage()); event.consume(); }
        });
        activerBlur(); dialog.showAndWait(); desactiverBlur();
    }

    // ══════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════
    private void ouvrirShowOne(rdv r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ShowOneRdv.fxml"));
            javafx.scene.Node content = loader.load();
            ShowOneRdvController controller = loader.getController();
            controller.initData(r);
            StackPane parent = (StackPane) rdvListContainer.getScene().lookup("#contentArea");
            if (parent != null) { parent.getChildren().clear(); parent.getChildren().add(content); }
        } catch (IOException ex) { afficherErreur("Erreur : " + ex.getMessage()); }
    }

    private void ouvrirHistorique() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HistoriqueRdv.fxml"));
            javafx.scene.Node content = loader.load();
            StackPane parent = (StackPane) rdvListContainer.getScene().lookup("#contentArea");
            if (parent != null) { parent.getChildren().clear(); parent.getChildren().add(content); }
        } catch (IOException ex) { afficherErreur("Erreur : " + ex.getMessage()); }
    }

    // ══════════════════════════════════════════════
    //  RECHERCHE DROPDOWN
    // ══════════════════════════════════════════════
    private void initSearchDropdown() {
        searchDropdown = new VBox();
        searchDropdown.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e2e8f0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 15, 0, 0, 5); -fx-padding: 6 0;");
        searchDropdown.setMaxWidth(360); searchDropdown.setMinWidth(300);
        searchPopup = new javafx.stage.Popup(); searchPopup.getContent().add(searchDropdown); searchPopup.setAutoHide(true);
    }

    private void afficherSearchDropdown(String query) {
        if (query == null || query.trim().length() < 1) { fermerDropdown(); return; }
        String lower = query.trim().toLowerCase(); List<rdv> resultats = new ArrayList<>();
        for (rdv r : rdvList) {
            if ((r.getMedecin() != null && r.getMedecin().toLowerCase().contains(lower)) || (r.getMotif() != null && r.getMotif().toLowerCase().contains(lower)) ||
                    (r.getDate() != null && r.getDate().toLowerCase().contains(lower)) || (r.getStatut() != null && r.getStatut().toLowerCase().contains(lower))) resultats.add(r);
            if (resultats.size() >= 5) break;
        }
        searchDropdown.getChildren().clear();
        Label header = new Label("   🔍  " + resultats.size() + " résultat" + (resultats.size() > 1 ? "s" : ""));
        header.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 6 12;"); header.setMaxWidth(Double.MAX_VALUE);
        searchDropdown.getChildren().add(header);
        if (resultats.isEmpty()) { Label vide = new Label("   😕  Aucun résultat pour \"" + query.trim() + "\""); vide.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8; -fx-padding: 12 16;"); searchDropdown.getChildren().add(vide); }
        else for (rdv r : resultats) searchDropdown.getChildren().add(creerDropdownItem(r, lower));
        if (!searchPopup.isShowing()) { javafx.geometry.Bounds bounds = searchField.localToScreen(searchField.getBoundsInLocal()); if (bounds != null) searchPopup.show(searchField.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY() + 5); }
    }

    private HBox creerDropdownItem(rdv r, String query) {
        HBox item = new HBox(12); item.setAlignment(Pos.CENTER_LEFT); item.setStyle("-fx-padding: 10 16; -fx-cursor: hand;"); item.setMaxWidth(Double.MAX_VALUE);
        String initiale = (r.getMedecin() != null && !r.getMedecin().isEmpty()) ? r.getMedecin().substring(0, 1).toUpperCase() : "?";
        Label avatar = new Label(initiale); avatar.setMinSize(34, 34); avatar.setMaxSize(34, 34); avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 17; -fx-text-fill: #1d4ed8; -fx-font-weight: bold; -fx-font-size: 13;");
        VBox infos = new VBox(2); HBox.setHgrow(infos, Priority.ALWAYS);
        Label nom = new Label(r.getMedecin() != null ? r.getMedecin() : ""); nom.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label detail = new Label((r.getMotif() != null ? r.getMotif() : "") + " · " + (r.getDate() != null ? r.getDate() : "")); detail.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");
        infos.getChildren().addAll(nom, detail);
        String sl = r.getStatut() != null ? r.getStatut().toLowerCase().trim() : "";
        String bgC = sl.contains("confirm") ? "#dcfce7" : sl.contains("annul") ? "#fee2e2" : "#ffedd5";
        String txC = sl.contains("confirm") ? "#15803d" : sl.contains("annul") ? "#b91c1c" : "#c2410c";
        Label statutBadge = new Label(r.getStatut()); statutBadge.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 10; -fx-background-color: " + bgC + "; -fx-text-fill: " + txC + ";");
        item.getChildren().addAll(avatar, infos, statutBadge);
        item.setOnMouseEntered(e -> item.setStyle("-fx-padding: 10 16; -fx-cursor: hand; -fx-background-color: #eff6ff;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-padding: 10 16; -fx-cursor: hand;"));
        item.setOnMouseClicked(e -> { fermerDropdown(); ouvrirShowOne(r); });
        return item;
    }

    private void fermerDropdown() { if (searchPopup != null && searchPopup.isShowing()) searchPopup.hide(); }

    // ══════════════════════════════════════════════
    //  MES MÉDECINS
    // ══════════════════════════════════════════════
    private void chargerMedecins() {
        try { tousLesMedecins = medecinService.findAll(); afficherMedecinsListe(tousLesMedecins); }
        catch (SQLException e) { System.err.println("Erreur médecins : " + e.getMessage()); }
    }

    private void afficherMedecinsListe(List<medecin> medecins) {
        medecinListContainer.getChildren().clear();
        for (medecin m : medecins) medecinListContainer.getChildren().add(creerCarteMedecin(m, false));
        if (medecins.isEmpty()) { Label vide = new Label("Aucun médecin trouvé"); vide.setStyle("-fx-font-size: 12; -fx-text-fill: #999; -fx-padding: 15;"); medecinListContainer.getChildren().add(vide); }
    }

    private void filtrerMedecins(String recherche) {
        if (recherche == null || recherche.trim().isEmpty()) { afficherMedecinsListe(tousLesMedecins); return; }
        String lower = recherche.trim().toLowerCase(); List<medecin> filtres = new ArrayList<>();
        for (medecin m : tousLesMedecins) { String nom = (m.getNom() + " " + m.getPrenom()).toLowerCase(); String spec = m.getSpecialite() != null ? m.getSpecialite().toLowerCase() : ""; if (nom.contains(lower) || spec.contains(lower)) filtres.add(m); }
        afficherMedecinsListe(filtres);
    }

    private HBox creerCarteMedecin(medecin m, boolean showQrButton) {
        HBox carte = new HBox(12); carte.setAlignment(Pos.CENTER_LEFT); carte.setStyle("-fx-padding: 8 5; -fx-cursor: hand;");
        carte.setOnMouseEntered(e -> carte.setStyle("-fx-padding: 8 5; -fx-cursor: hand; -fx-background-color: #f0f7ff; -fx-background-radius: 8;"));
        carte.setOnMouseExited(e -> carte.setStyle("-fx-padding: 8 5; -fx-cursor: hand;"));
        String initiales = ""; if (m.getPrenom() != null && !m.getPrenom().isEmpty()) initiales += m.getPrenom().substring(0, 1).toUpperCase(); if (m.getNom() != null && !m.getNom().isEmpty()) initiales += m.getNom().substring(0, 1).toUpperCase(); if (initiales.isEmpty()) initiales = "?";
        String[] colors = {"#1a73e8", "#e53935", "#f59e0b", "#16a34a", "#8b5cf6", "#ec4899", "#06b6d4", "#f97316"};
        String avatarColor = colors[Math.abs((m.getNom() + m.getPrenom()).hashCode()) % colors.length];
        Label avatar = new Label(initiales); avatar.setMinWidth(40); avatar.setMinHeight(40); avatar.setMaxWidth(40); avatar.setMaxHeight(40); avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: " + avatarColor + "; -fx-background-radius: 20; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");
        VBox infos = new VBox(2); HBox.setHgrow(infos, Priority.ALWAYS);
        Label nomLabel = new Label("Dr. " + m.getPrenom().substring(0, 1) + ". " + m.getNom()); nomLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");
        Label specLabel = new Label(m.getSpecialite() != null ? m.getSpecialite() : ""); specLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");
        if (m.getDisponible() == 1) { Label dispo = new Label("●"); dispo.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 8;"); HBox nomRow = new HBox(5, nomLabel, dispo); nomRow.setAlignment(Pos.CENTER_LEFT); infos.getChildren().addAll(nomRow, specLabel); }
        else infos.getChildren().addAll(nomLabel, specLabel);
        Button btnChat = new Button("💬"); btnChat.setTooltip(new Tooltip("Envoyer un message"));
        btnChat.setStyle("-fx-background-color: #e3f2fd; -fx-font-size: 14; -fx-cursor: hand; -fx-padding: 5 8; -fx-background-radius: 8;");
        btnChat.setOnMouseEntered(e -> btnChat.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 14; -fx-cursor: hand; -fx-padding: 5 8; -fx-background-radius: 8;"));
        btnChat.setOnMouseExited(e -> btnChat.setStyle("-fx-background-color: #e3f2fd; -fx-font-size: 14; -fx-cursor: hand; -fx-padding: 5 8; -fx-background-radius: 8;"));
        btnChat.setOnAction(e -> ouvrirChatAvecMedecin(m));
        Button btnQr = new Button("📱"); btnQr.setTooltip(new Tooltip("Voir QR Code"));
        btnQr.setStyle("-fx-background-color: transparent; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 4;");
        btnQr.setOnMouseEntered(e -> btnQr.setStyle("-fx-background-color: #e0f2fe; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 4; -fx-background-radius: 8;"));
        btnQr.setOnMouseExited(e -> btnQr.setStyle("-fx-background-color: transparent; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 4;"));
        btnQr.setOnAction(e -> ouvrirPopupQrMedecin(m));
        carte.getChildren().addAll(avatar, infos, btnChat, btnQr);
        carte.setOnMouseClicked(e -> { if (e.getTarget() != btnQr && e.getTarget() != btnChat) ouvrirPopupQrMedecin(m); });
        return carte;
    }

    // ══════════════════════════════════════════════
    //  CHAT AVEC MÉDECIN
    // ══════════════════════════════════════════════
    private void ouvrirChatAvecMedecin(medecin m) {
        try {
            SocketServer serveur = SocketServer.getInstance(); if (!serveur.isRunning()) { serveur.demarrer(); Thread.sleep(400); }
            String patientUserId = SocketClient.userIdPatient(PATIENT_ID_COURANT);
            String medecinUserId = SocketClient.slugMedecin(m.getPrenom() + " " + m.getNom());
            String medecinNom    = "Dr. " + m.getPrenom() + " " + m.getNom();
            SocketClient clientPatient = SocketClient.getInstance("patient", patientUserId);
            if (!clientPatient.isConnecte()) { clientPatient.connecter(); Thread.sleep(300); }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Chat.fxml"));
            VBox root = loader.load(); ChatController chatController = loader.getController();
            chatController.initChat(clientPatient, medecinUserId, medecinNom);
            Stage chatStage = new Stage(); chatStage.initModality(Modality.APPLICATION_MODAL); chatStage.initStyle(StageStyle.DECORATED);
            chatStage.setTitle("💬 Chat — " + medecinNom); chatStage.setScene(new Scene(root, 480, 580));
            chatStage.setMinWidth(380); chatStage.setMinHeight(450); chatStage.initOwner(rdvListContainer.getScene().getWindow()); chatStage.show();
        } catch (Exception ex) { ex.printStackTrace(); afficherErreur("Impossible d'ouvrir le chat : " + ex.getMessage()); }
    }

    // ══════════════════════════════════════════════
    //  POPUP VOIR TOUT MÉDECINS
    // ══════════════════════════════════════════════
    private void ouvrirPopupTousLesMedecins() {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Tous les Médecins");
        VBox content = new VBox(15); content.setPrefWidth(500); content.setPrefHeight(500); content.setStyle("-fx-padding: 20;");
        Label titre = new Label("👨‍⚕️ Tous les Médecins"); titre.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label compteur = new Label(tousLesMedecins.size() + " médecin(s)"); compteur.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8;");
        TextField searchPopupField = new TextField(); searchPopupField.setPromptText("🔍 Rechercher...");
        searchPopupField.setStyle("-fx-font-size: 13; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #e2e8f0; -fx-padding: 10 16;");
        VBox listeContainer = new VBox(6); ScrollPane scrollListe = new ScrollPane(listeContainer);
        scrollListe.setFitToWidth(true); scrollListe.setPrefHeight(380);
        scrollListe.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;"); VBox.setVgrow(scrollListe, Priority.ALWAYS);
        Runnable remplirListe = () -> {
            listeContainer.getChildren().clear(); String query = searchPopupField.getText(); String lower = (query != null) ? query.trim().toLowerCase() : "";
            for (medecin m : tousLesMedecins) { String nom = (m.getNom() + " " + m.getPrenom()).toLowerCase(); String spec = m.getSpecialite() != null ? m.getSpecialite().toLowerCase() : ""; if (lower.isEmpty() || nom.contains(lower) || spec.contains(lower)) listeContainer.getChildren().add(creerCarteMedecinPopup(m)); }
            if (listeContainer.getChildren().isEmpty()) { Label vide = new Label("😕 Aucun médecin trouvé"); vide.setStyle("-fx-font-size: 13; -fx-text-fill: #94a3b8; -fx-padding: 30;"); listeContainer.getChildren().add(vide); }
            compteur.setText(listeContainer.getChildren().size() + " résultat(s)");
        };
        remplirListe.run(); searchPopupField.textProperty().addListener((obs, old, val) -> remplirListe.run());
        content.getChildren().addAll(titre, compteur, searchPopupField, scrollListe);
        dialog.getDialogPane().setContent(content); dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).setText("Fermer");
        activerBlur(); dialog.showAndWait(); desactiverBlur();
    }

    private HBox creerCarteMedecinPopup(medecin m) {
        HBox carte = new HBox(15); carte.setAlignment(Pos.CENTER_LEFT); carte.setStyle("-fx-padding: 12 15; -fx-background-color: #f8fafc; -fx-background-radius: 10;");
        carte.setOnMouseEntered(e -> carte.setStyle("-fx-padding: 12 15; -fx-background-color: #eff6ff; -fx-background-radius: 10;"));
        carte.setOnMouseExited(e -> carte.setStyle("-fx-padding: 12 15; -fx-background-color: #f8fafc; -fx-background-radius: 10;"));
        String initiales = ""; if (m.getPrenom() != null && !m.getPrenom().isEmpty()) initiales += m.getPrenom().substring(0, 1).toUpperCase(); if (m.getNom() != null && !m.getNom().isEmpty()) initiales += m.getNom().substring(0, 1).toUpperCase();
        String[] colors = {"#1a73e8", "#e53935", "#f59e0b", "#16a34a", "#8b5cf6", "#ec4899", "#06b6d4", "#f97316"};
        String avatarColor = colors[Math.abs((m.getNom() + m.getPrenom()).hashCode()) % colors.length];
        Label avatar = new Label(initiales); avatar.setMinWidth(45); avatar.setMinHeight(45); avatar.setMaxWidth(45); avatar.setMaxHeight(45); avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: " + avatarColor + "; -fx-background-radius: 22; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15;");
        VBox infos = new VBox(3); HBox.setHgrow(infos, Priority.ALWAYS);
        Label nomLabel = new Label("Dr. " + m.getPrenom() + " " + m.getNom()); nomLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label specLabel = new Label(m.getSpecialite() != null ? m.getSpecialite() : ""); specLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
        Label dispoLabel = new Label(m.getDisponible() == 1 ? "● Disponible" : "○ Indisponible"); dispoLabel.setStyle("-fx-font-size: 11; -fx-text-fill: " + (m.getDisponible() == 1 ? "#16a34a" : "#9ca3af") + "; -fx-font-weight: bold;");
        infos.getChildren().addAll(nomLabel, specLabel, dispoLabel);
        Button btnChat = new Button("💬 Chat"); btnChat.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1a73e8; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 14; -fx-cursor: hand;");
        btnChat.setOnMouseEntered(e -> btnChat.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 14; -fx-cursor: hand;"));
        btnChat.setOnMouseExited(e -> btnChat.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1a73e8; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 14; -fx-cursor: hand;"));
        btnChat.setOnAction(e -> ouvrirChatAvecMedecin(m));
        Button btnQr = new Button("📱 QR Code"); btnQr.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 14; -fx-cursor: hand;");
        btnQr.setOnMouseEntered(e -> btnQr.setStyle("-fx-background-color: #0369a1; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 14; -fx-cursor: hand;"));
        btnQr.setOnMouseExited(e -> btnQr.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 14; -fx-cursor: hand;"));
        btnQr.setOnAction(e -> ouvrirPopupQrMedecin(m));
        carte.getChildren().addAll(avatar, infos, btnChat, btnQr); return carte;
    }

    private void ouvrirPopupQrMedecin(medecin m) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("QR Code — Dr. " + m.getPrenom() + " " + m.getNom());
        VBox content = new VBox(15); content.setPrefWidth(420); content.setAlignment(Pos.CENTER); content.setStyle("-fx-padding: 25;");
        String initiales = ""; if (m.getPrenom() != null && !m.getPrenom().isEmpty()) initiales += m.getPrenom().substring(0, 1).toUpperCase(); if (m.getNom() != null && !m.getNom().isEmpty()) initiales += m.getNom().substring(0, 1).toUpperCase();
        String[] colors = {"#1a73e8", "#e53935", "#f59e0b", "#16a34a", "#8b5cf6", "#ec4899", "#06b6d4", "#f97316"};
        String avatarColor = colors[Math.abs((m.getNom() + m.getPrenom()).hashCode()) % colors.length];
        Label avatarBig = new Label(initiales); avatarBig.setMinWidth(70); avatarBig.setMinHeight(70); avatarBig.setMaxWidth(70); avatarBig.setMaxHeight(70); avatarBig.setAlignment(Pos.CENTER);
        avatarBig.setStyle("-fx-background-color: " + avatarColor + "; -fx-background-radius: 35; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 24;");
        Label nomLabel = new Label("Dr. " + m.getPrenom() + " " + m.getNom()); nomLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label specLabel = new Label(m.getSpecialite() != null ? m.getSpecialite() : ""); specLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #64748b;");
        String qrData = "Dr. " + m.getPrenom() + " " + m.getNom() + "\nSpécialité: " + (m.getSpecialite() != null ? m.getSpecialite() : "N/A") + "\nVitalTech Medical Center\nTel: +216 29 254 485";
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + java.net.URLEncoder.encode(qrData, java.nio.charset.StandardCharsets.UTF_8);
        ImageView qrImage = new ImageView(); qrImage.setFitWidth(200); qrImage.setFitHeight(200);
        try { qrImage.setImage(new Image(qrUrl, true)); } catch (Exception e) { System.err.println("Erreur QR : " + e.getMessage()); }
        VBox qrBox = new VBox(8, qrImage); qrBox.setAlignment(Pos.CENTER); qrBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-border-color: #e2e8f0; -fx-border-radius: 12;");
        content.getChildren().addAll(avatarBig, nomLabel, specLabel, qrBox);
        dialog.getDialogPane().setContent(content); dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).setText("Fermer");
        activerBlur(); dialog.showAndWait(); desactiverBlur();
    }

    // ══════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════
    private void afficherSucces(String msg) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle("Succès"); a.setContentText(msg); a.show(); }
    private void afficherErreur(String msg)  { Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle("Erreur"); a.setContentText(msg); a.show(); }
    private void activerBlur()    { rdvListContainer.getScene().getRoot().setEffect(new GaussianBlur(10)); }
    private void desactiverBlur() { rdvListContainer.getScene().getRoot().setEffect(null); }
}