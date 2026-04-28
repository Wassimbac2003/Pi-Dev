package Controllers;

import Models.medecin;
import Models.rdv;
import Services.CreneauService;
import Services.CreneauService.Creneau;
import Services.CreneauService.CreneauxResult;
import Services.MedecinService;
import Services.RdvService;
import Services.StripeService;
import Services.RecuPdfService;
import Utils.StripeConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.*;

import java.io.File;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendrierWizardController {

    private int currentStep = 1;
    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private String selectedSpecialite;
    private medecin selectedMedecin;
    private String selectedHeure;
    private String selectedMotif;
    private String selectedMessage;
    private String selectedType = "presentiel";
    private String selectedPaiement = "sur_place";
    private String stripePaymentId;

    private final MedecinService medecinService = new MedecinService();
    private final CreneauService creneauService = new CreneauService();
    private final RdvService rdvService = new RdvService();

    private Stage wizardStage;
    private VBox contentArea;
    private Label titleLabel, subtitleLabel, stepInfoLabel;
    private Button btnNext, btnBack, btnClose;
    private HBox dotsContainer;
    private Runnable onRdvCreated;

    private static final String[] STEP_TITLES = {
            "Choisir une date", "Choisir la specialite", "Choisir un medecin",
            "Choisir un creneau", "Informations", "Mode de paiement"
    };
    private static final String[] STEP_SUBTITLES = {
            "Selectionnez le jour de votre consultation",
            "Quelle specialite medicale vous interesse ?",
            "Votre medecin pour ce rendez-vous",
            "Creneaux disponibles pour la date choisie",
            "Completez les informations du rendez-vous",
            "Choisissez comment regler la consultation"
    };

    private static final Map<String, String> SPECIALITE_COLORS = new HashMap<>() {{
        put("Cardiologie", "#e74c3c"); put("Ophtalmologie", "#3498db"); put("Neurologie", "#9b59b6");
        put("Dermatologie", "#e67e22"); put("Pediatrie", "#2ecc71"); put("Gynecologie", "#e91e63");
        put("Orthopedie", "#795548"); put("Pneumologie", "#00bcd4"); put("Gastroenterologie", "#ff9800");
        put("Dentiste", "#607d8b");
    }};

    private static final Map<String, String> SPECIALITE_ABBR = new HashMap<>() {{
        put("Cardiologie", "CA"); put("Ophtalmologie", "OP"); put("Neurologie", "NE");
        put("Dermatologie", "DE"); put("Pediatrie", "PE"); put("Gynecologie", "GY");
        put("Orthopedie", "OR"); put("Pneumologie", "PN"); put("Gastroenterologie", "GA");
        put("Dentiste", "DN");
    }};

    public void open(Window owner, Runnable onCreated) {
        this.onRdvCreated = onCreated;
        this.currentMonth = YearMonth.now();
        this.currentStep = 1;
        this.stripePaymentId = null;

        wizardStage = new Stage();
        wizardStage.initModality(Modality.APPLICATION_MODAL);
        wizardStage.initOwner(owner);
        wizardStage.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(0);
        root.setPrefWidth(620); root.setPrefHeight(650);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 25, 0, 0, 8);");

        root.getChildren().add(buildHeader());

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        contentArea = new VBox(15);
        contentArea.setPadding(new Insets(25));
        scroll.setContent(contentArea);
        root.getChildren().add(scroll);
        root.getChildren().add(buildFooter());

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        if (getClass().getResource("/css/style.css") != null) scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        wizardStage.setScene(scene);
        goToStep(1);
        wizardStage.showAndWait();
    }

    public void openWithDate(Window owner, LocalDate date, Runnable onCreated) {
        this.selectedDate = date;
        this.currentMonth = YearMonth.from(date);
        open(owner, onCreated);
    }

    // ===== HEADER =====
    private VBox buildHeader() {
        VBox header = new VBox(8);
        header.setPadding(new Insets(20, 25, 15, 25));
        header.setStyle("-fx-background-color: linear-gradient(to right, #4a6cf7, #6a3ced); -fx-background-radius: 20 20 0 0;");

        HBox topRow = new HBox(10); topRow.setAlignment(Pos.CENTER_LEFT);
        btnBack = new Button("<-");
        btnBack.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-font-size: 14; -fx-background-radius: 50; -fx-min-width: 32; -fx-min-height: 32; -fx-cursor: hand;");
        btnBack.setCursor(Cursor.HAND); btnBack.setOnAction(e -> prevStep()); btnBack.setVisible(false);

        VBox titleBox = new VBox(2); HBox.setHgrow(titleBox, Priority.ALWAYS);
        titleLabel = new Label("Choisir une date"); titleLabel.setFont(Font.font("System", FontWeight.BOLD, 17)); titleLabel.setTextFill(Color.WHITE);
        subtitleLabel = new Label("Selectionnez le jour"); subtitleLabel.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.7);");
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        btnClose = new Button("X");
        btnClose.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-font-size: 13; -fx-background-radius: 50; -fx-min-width: 32; -fx-min-height: 32; -fx-cursor: hand;");
        btnClose.setCursor(Cursor.HAND); btnClose.setOnAction(e -> wizardStage.close());
        topRow.getChildren().addAll(btnBack, titleBox, btnClose);

        dotsContainer = new HBox(5); dotsContainer.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 6; i++) {
            Region dot = new Region(); dot.setPrefHeight(5); dot.setPrefWidth(i == 0 ? 22 : 8);
            dot.setStyle("-fx-background-color: " + (i == 0 ? "white" : "rgba(255,255,255,0.3)") + "; -fx-background-radius: 5;");
            dotsContainer.getChildren().add(dot);
        }
        header.getChildren().addAll(topRow, dotsContainer);
        return header;
    }

    // ===== FOOTER =====
    private HBox buildFooter() {
        HBox footer = new HBox(); footer.setPadding(new Insets(12, 25, 15, 25)); footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-border-color: #f1f5f9; -fx-border-width: 1 0 0 0;");
        stepInfoLabel = new Label("Etape 1 / 6"); stepInfoLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        btnNext = new Button("Suivant  ->");
        btnNext.setStyle("-fx-background-color: #4a6cf7; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(74,108,247,0.3), 8, 0, 0, 3);");
        btnNext.setCursor(Cursor.HAND); btnNext.setDisable(true); btnNext.setOnAction(e -> nextStep());
        footer.getChildren().addAll(stepInfoLabel, spacer, btnNext);
        return footer;
    }

    // ===== NAVIGATION =====
    private void goToStep(int step) {
        currentStep = step; contentArea.getChildren().clear();
        switch (step) {
            case 1: contentArea.getChildren().add(buildStep1_Calendar()); break;
            case 2: contentArea.getChildren().add(buildStep2_Specialites()); break;
            case 3: contentArea.getChildren().add(buildStep3_Medecins()); break;
            case 4: contentArea.getChildren().add(buildStep4_Creneaux()); break;
            case 5: contentArea.getChildren().add(buildStep5_Formulaire()); break;
            case 6: contentArea.getChildren().add(buildStep6_Paiement()); break;
        }
        updateUI();
    }

    private void nextStep() { if (currentStep == 6) { confirmerRdv(); return; } if (currentStep < 6) goToStep(currentStep + 1); }
    private void prevStep() { if (currentStep > 1) goToStep(currentStep - 1); }

    private void updateUI() {
        titleLabel.setText(STEP_TITLES[currentStep - 1]); subtitleLabel.setText(STEP_SUBTITLES[currentStep - 1]);
        stepInfoLabel.setText("Etape " + currentStep + " / 6"); btnBack.setVisible(currentStep > 1);
        for (int i = 0; i < 6; i++) {
            Region dot = (Region) dotsContainer.getChildren().get(i);
            if (i < currentStep - 1) { dot.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 5;"); dot.setPrefWidth(8); }
            else if (i == currentStep - 1) { dot.setStyle("-fx-background-color: white; -fx-background-radius: 5;"); dot.setPrefWidth(22); }
            else { dot.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-background-radius: 5;"); dot.setPrefWidth(8); }
        }
        if (currentStep == 5) btnNext.setText("Continuer vers le paiement  ->");
        else if (currentStep == 6) updatePaiementButton();
        else btnNext.setText("Suivant  ->");
        boolean enabled = false;
        switch (currentStep) {
            case 1: enabled = selectedDate != null; break; case 2: enabled = selectedSpecialite != null; break;
            case 3: enabled = selectedMedecin != null; break; case 4: enabled = selectedHeure != null; break;
            case 5: case 6: enabled = true; break;
        }
        btnNext.setDisable(!enabled);
    }

    // ===== ÉTAPE 1 : CALENDRIER =====
    private VBox buildStep1_Calendar() {
        VBox step = new VBox(12);
        HBox nav = new HBox(10); nav.setAlignment(Pos.CENTER);
        Button prevMonth = new Button("<");
        prevMonth.setStyle("-fx-background-color: white; -fx-text-fill: #64748b; -fx-font-size: 16; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-min-width: 40; -fx-min-height: 36; -fx-cursor: hand;");
        prevMonth.setCursor(Cursor.HAND); prevMonth.setOnAction(e -> { currentMonth = currentMonth.minusMonths(1); goToStep(1); });
        String moisNom = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        Label monthLabel = new Label(moisNom.substring(0, 1).toUpperCase() + moisNom.substring(1) + " " + currentMonth.getYear());
        monthLabel.setFont(Font.font("System", FontWeight.BOLD, 16)); monthLabel.setTextFill(Color.web("#1e293b"));
        HBox.setHgrow(monthLabel, Priority.ALWAYS); monthLabel.setAlignment(Pos.CENTER);
        Button nextMonth = new Button(">"); nextMonth.setStyle(prevMonth.getStyle()); nextMonth.setCursor(Cursor.HAND);
        nextMonth.setOnAction(e -> { currentMonth = currentMonth.plusMonths(1); goToStep(1); });
        nav.getChildren().addAll(prevMonth, monthLabel, nextMonth);

        GridPane dayHeaders = new GridPane(); dayHeaders.setHgap(4); dayHeaders.setAlignment(Pos.CENTER);
        String[] jours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(jours[i]); lbl.setPrefWidth(72); lbl.setAlignment(Pos.CENTER);
            lbl.setFont(Font.font("System", FontWeight.BOLD, 12));
            lbl.setTextFill(Color.web(i == 5 ? "#fb923c" : i == 6 ? "#ef4444" : "#64748b"));
            dayHeaders.add(lbl, i, 0);
        }

        GridPane grid = new GridPane(); grid.setHgap(4); grid.setVgap(4); grid.setAlignment(Pos.CENTER);
        LocalDate first = currentMonth.atDay(1); int startDow = first.getDayOfWeek().getValue() - 1;
        int lastDay = currentMonth.lengthOfMonth(); LocalDate today = LocalDate.now();
        int row = 0;
        for (int i = 0; i < startDow; i++) { Label empty = new Label(""); empty.setPrefWidth(72); empty.setPrefHeight(42); grid.add(empty, i, row); }
        for (int d = 1; d <= lastDay; d++) {
            LocalDate date = currentMonth.atDay(d); int col = (startDow + d - 1) % 7; if (col == 0 && d > 1) row++;
            Button dayBtn = new Button(String.valueOf(d)); dayBtn.setPrefWidth(72); dayBtn.setPrefHeight(42);
            dayBtn.setFont(Font.font("System", FontWeight.NORMAL, 14));
            boolean isPast = date.isBefore(today); boolean isToday = date.equals(today); boolean isSelected = date.equals(selectedDate);
            boolean isSaturday = date.getDayOfWeek() == DayOfWeek.SATURDAY; boolean isSunday = date.getDayOfWeek() == DayOfWeek.SUNDAY;

            if (isSelected) { dayBtn.setFont(Font.font("System", FontWeight.BOLD, 14)); dayBtn.setStyle("-fx-background-color: #4a6cf7; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(74,108,247,0.35), 8, 0, 0, 3);"); }
            else if (isPast) { dayBtn.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #c0c8d4; -fx-background-radius: 10;"); dayBtn.setDisable(true); }
            else if (isToday) { dayBtn.setFont(Font.font("System", FontWeight.BOLD, 14)); dayBtn.setStyle("-fx-background-color: #f0f9ff; -fx-text-fill: #0369a1; -fx-background-radius: 10; -fx-border-color: #7dd3fc; -fx-border-radius: 10; -fx-border-width: 2; -fx-cursor: hand;"); }
            else if (isSaturday) { dayBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #fb923c; -fx-background-radius: 10; -fx-cursor: hand;"); }
            else if (isSunday) { dayBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-background-radius: 10; -fx-cursor: hand;"); }
            else { dayBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #1e293b; -fx-background-radius: 10; -fx-cursor: hand;"); }

            if (!isPast) {
                dayBtn.setCursor(Cursor.HAND); final LocalDate clickDate = date;
                dayBtn.setOnAction(e -> { selectedDate = clickDate; selectedHeure = null; goToStep(1); btnNext.setDisable(false); });
                if (!isSelected) { String ns = dayBtn.getStyle();
                    dayBtn.setOnMouseEntered(e -> dayBtn.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #bfdbfe; -fx-border-radius: 10; -fx-cursor: hand;"));
                    dayBtn.setOnMouseExited(e -> dayBtn.setStyle(ns)); }
            }
            grid.add(dayBtn, col, row);
        }
        step.getChildren().addAll(nav, dayHeaders, grid);
        if (selectedDate != null) {
            if (selectedDate.getDayOfWeek() == DayOfWeek.SATURDAY) step.getChildren().add(buildInfoBox("Samedi - horaires reduits", "Consultations de 09h00 a 13h00", "#fff7ed", "#ea580c"));
            else if (selectedDate.getDayOfWeek() == DayOfWeek.SUNDAY) step.getChildren().add(buildInfoBox("Dimanche - disponibilites limitees", "Certains medecins peuvent avoir des creneaux exceptionnels", "#f8fafc", "#64748b"));
        }
        return step;
    }

    // ===== ÉTAPE 2 : SPÉCIALITÉS =====
    private VBox buildStep2_Specialites() {
        VBox step = new VBox(15);
        Label info = new Label("Quelle specialite recherchez-vous ?"); info.getStyleClass().add("wizard-detail-text"); step.getChildren().add(info);
        try {
            List<String> specialites = medecinService.getSpecialites();
            FlowPane grid = new FlowPane(12, 12); grid.setPrefWrapLength(560);
            for (String sp : specialites) {
                boolean selected = sp.equals(selectedSpecialite);
                String color = SPECIALITE_COLORS.getOrDefault(sp, "#4a6cf7");
                String abbr = SPECIALITE_ABBR.getOrDefault(sp, sp.substring(0, 2).toUpperCase());
                HBox card = new HBox(14); card.setAlignment(Pos.CENTER_LEFT); card.setPrefWidth(255); card.setPrefHeight(60);
                card.setPadding(new Insets(12, 16, 12, 16)); card.setCursor(Cursor.HAND);
                String cs = selected ? "-fx-background-color: #eff6ff; -fx-background-radius: 14; -fx-border-color: #4a6cf7; -fx-border-radius: 14; -fx-border-width: 2;"
                        : "-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-border-width: 2;";
                card.setStyle(cs);
                Label iconLabel = new Label(abbr); iconLabel.setPrefWidth(40); iconLabel.setPrefHeight(40); iconLabel.setAlignment(Pos.CENTER);
                iconLabel.setFont(Font.font("System", FontWeight.BOLD, 13)); iconLabel.setTextFill(Color.WHITE);
                iconLabel.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 12;");
                Label nameLabel = new Label(sp); nameLabel.getStyleClass().add("specialite-name");
                card.getChildren().addAll(iconLabel, nameLabel);
                String fsp = sp;
                card.setOnMouseClicked(e -> { selectedSpecialite = fsp; selectedMedecin = null; selectedHeure = null; goToStep(2); btnNext.setDisable(false); });
                if (!selected) { String ncs = cs;
                    card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f0f7ff; -fx-background-radius: 14; -fx-border-color: #93c5fd; -fx-border-radius: 14; -fx-border-width: 2;"));
                    card.setOnMouseExited(e -> card.setStyle(ncs)); }
                grid.getChildren().add(card);
            }
            step.getChildren().add(grid);
        } catch (SQLException e) { step.getChildren().add(new Label("Erreur : " + e.getMessage())); }
        return step;
    }

    // ===== ÉTAPE 3 : MÉDECINS =====
    private VBox buildStep3_Medecins() {
        VBox step = new VBox(12);
        Label info = new Label("Choisissez votre medecin"); info.getStyleClass().add("wizard-detail-text"); step.getChildren().add(info);
        try {
            List<medecin> medecins = medecinService.findBySpecialite(selectedSpecialite);
            for (medecin m : medecins) {
                boolean selected = selectedMedecin != null && selectedMedecin.getId() == m.getId();
                boolean dispo = m.getDisponible() == 1;
                HBox card = new HBox(14); card.setAlignment(Pos.CENTER_LEFT); card.setPadding(new Insets(16));
                card.setCursor(dispo ? Cursor.HAND : Cursor.DEFAULT);
                String cs = selected ? "-fx-background-color: #eff6ff; -fx-background-radius: 14; -fx-border-color: #4a6cf7; -fx-border-radius: 14; -fx-border-width: 2;"
                        : "-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-border-width: 2;";
                card.setStyle(cs); if (!dispo) card.setOpacity(0.5);
                String initiales = ("" + m.getPrenom().charAt(0) + m.getNom().charAt(0)).toUpperCase();
                String ac = SPECIALITE_COLORS.getOrDefault(m.getSpecialite(), "#4a6cf7");
                Label avatar = new Label(initiales); avatar.setPrefWidth(48); avatar.setPrefHeight(48); avatar.setMinWidth(48); avatar.setAlignment(Pos.CENTER);
                avatar.setFont(Font.font("System", FontWeight.BOLD, 16)); avatar.setTextFill(Color.WHITE);
                avatar.setStyle("-fx-background-color: " + ac + "; -fx-background-radius: 50;");
                VBox ib = new VBox(3); HBox.setHgrow(ib, Priority.ALWAYS);
                Label nl = new Label("Dr. " + m.getPrenom() + " " + m.getNom()); nl.getStyleClass().add("medecin-name");
                Label dl = new Label(m.getSpecialite() + "  |  " + m.getType()); dl.getStyleClass().add("medecin-detail");
                ib.getChildren().addAll(nl, dl);
                Label badge = new Label(dispo ? "Disponible" : "Complet");
                badge.setFont(Font.font("System", FontWeight.BOLD, 11)); badge.setPadding(new Insets(5, 12, 5, 12));
                badge.setStyle("-fx-background-radius: 20; -fx-background-color: " + (dispo ? "#dcfce7" : "#fee2e2") + "; -fx-text-fill: " + (dispo ? "#15803d" : "#b91c1c") + ";");
                card.getChildren().addAll(avatar, ib, badge);
                if (dispo) { final medecin cm = m;
                    card.setOnMouseClicked(e -> { selectedMedecin = cm; selectedHeure = null; goToStep(3); btnNext.setDisable(false); });
                    if (!selected) { String ns = cs;
                        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f8fbff; -fx-background-radius: 14; -fx-border-color: #93c5fd; -fx-border-radius: 14; -fx-border-width: 2;"));
                        card.setOnMouseExited(e -> card.setStyle(ns)); } }
                step.getChildren().add(card);
            }
            if (medecins.isEmpty()) { Label empty = new Label("Aucun medecin pour cette specialite"); empty.getStyleClass().add("wizard-detail-text"); empty.setPadding(new Insets(30)); step.getChildren().add(empty); }
        } catch (SQLException e) { step.getChildren().add(new Label("Erreur : " + e.getMessage())); }
        return step;
    }

    // ===== ÉTAPE 4 : CRÉNEAUX =====
    private VBox buildStep4_Creneaux() {
        VBox step = new VBox(12);
        String nomMed = "Dr. " + selectedMedecin.getPrenom() + " " + selectedMedecin.getNom();
        String dateF = selectedDate.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH));
        HBox ih = new HBox(8); ih.setPadding(new Insets(12));
        ih.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 12; -fx-border-color: #bfdbfe; -fx-border-radius: 12;");
        Label il = new Label(dateF + " -- " + nomMed); il.setFont(Font.font("System", FontWeight.BOLD, 13)); il.setTextFill(Color.web("#1d4ed8"));
        ih.getChildren().add(il); step.getChildren().add(ih);
        HBox legend = new HBox(15);
        legend.getChildren().addAll(buildLegendItem("Libre", "white", "#e2e8f0"), buildLegendItem("Bloque", "#f1f5f9", "#e2e8f0"), buildLegendItem("Selectionne", "#4a6cf7", "#4a6cf7"));
        step.getChildren().add(legend);
        try {
            CreneauxResult result = creneauService.getCreneaux(selectedMedecin.getId(), selectedDate.toString(), nomMed);
            if (result.message != null) { Label msg = new Label(result.message); msg.getStyleClass().add("wizard-detail-text"); step.getChildren().add(msg); }
            if (result.creneaux.isEmpty()) {
                VBox eb = new VBox(10); eb.setAlignment(Pos.CENTER); eb.setPadding(new Insets(30));
                Label et = new Label("Aucun creneau disponible"); et.getStyleClass().add("wizard-label");
                Label eh = new Label("Essayez une autre date"); eh.getStyleClass().add("wizard-detail-text");
                eb.getChildren().addAll(et, eh); step.getChildren().add(eb); return step;
            }
            GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(8); grid.setAlignment(Pos.CENTER);
            int col = 0, row = 0;
            for (Creneau c : result.creneaux) {
                Button btn = new Button(c.heure); btn.setPrefWidth(120); btn.setPrefHeight(42);
                btn.setFont(Font.font("System", FontWeight.BOLD, 13));
                boolean isSel = c.heure.equals(selectedHeure);
                if (isSel) btn.setStyle("-fx-background-color: #4a6cf7; -fx-text-fill: white; -fx-background-radius: 10; -fx-border-color: #4a6cf7; -fx-border-radius: 10; -fx-effect: dropshadow(gaussian, rgba(74,108,247,0.3), 6, 0, 0, 2);");
                else if (!c.disponible && c.pris) { btn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #e8a0a0; -fx-background-radius: 10; -fx-border-color: #fecaca; -fx-border-radius: 10;"); btn.setDisable(true); }
                else if (!c.disponible) { btn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #b0bac8; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-radius: 10;"); btn.setDisable(true); }
                else {
                    btn.setStyle("-fx-background-color: white; -fx-text-fill: #1e293b; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-cursor: hand;");
                    btn.setCursor(Cursor.HAND); String fh = c.heure;
                    btn.setOnAction(e -> { selectedHeure = fh; goToStep(4); btnNext.setDisable(false); });
                    btn.setOnMouseEntered(e -> { if (!fh.equals(selectedHeure)) btn.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-background-radius: 10; -fx-border-color: #3b82f6; -fx-border-radius: 10; -fx-cursor: hand;"); });
                    btn.setOnMouseExited(e -> { if (!fh.equals(selectedHeure)) btn.setStyle("-fx-background-color: white; -fx-text-fill: #1e293b; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-cursor: hand;"); });
                }
                grid.add(btn, col, row); col++; if (col == 4) { col = 0; row++; }
            }
            step.getChildren().add(grid);
        } catch (SQLException e) { step.getChildren().add(new Label("Erreur : " + e.getMessage())); }
        return step;
    }

    // ===== ÉTAPE 5 : FORMULAIRE =====
    private VBox buildStep5_Formulaire() {
        VBox step = new VBox(15);
        String dateF = selectedDate.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH));
        String nomMed = "Dr. " + selectedMedecin.getPrenom() + " " + selectedMedecin.getNom();
        VBox recap = new VBox(10); recap.setPadding(new Insets(15));
        recap.setStyle("-fx-background-color: #f0f4ff; -fx-background-radius: 14; -fx-border-color: #bfdbfe; -fx-border-radius: 14;");
        Label rt = new Label("RECAPITULATIF"); rt.getStyleClass().add("wizard-recap-title");
        GridPane rg = new GridPane(); rg.setHgap(20); rg.setVgap(8);
        Label ld = new Label("Date :  " + dateF); ld.getStyleClass().add("wizard-recap-text");
        Label lh = new Label("Heure :  " + selectedHeure); lh.getStyleClass().add("wizard-recap-text");
        Label lm = new Label("Medecin :  " + nomMed); lm.getStyleClass().add("wizard-recap-text");
        Label ls = new Label("Specialite :  " + selectedSpecialite); ls.getStyleClass().add("wizard-recap-text");
        rg.add(ld, 0, 0); rg.add(lh, 1, 0); rg.add(lm, 0, 1); rg.add(ls, 1, 1);
        recap.getChildren().addAll(rt, rg); step.getChildren().add(recap);

        Label lblMotif = new Label("Motif de consultation *"); lblMotif.getStyleClass().add("wizard-label");
        ComboBox<String> motifCombo = new ComboBox<>(); motifCombo.setPromptText("-- Choisir un motif --");
        motifCombo.getItems().addAll("Consultation generale", "Suivi medical", "Urgence", "Bilan de sante", "Renouvellement ordonnance", "Avis specialise", "Autre");
        motifCombo.setMaxWidth(Double.MAX_VALUE); motifCombo.setStyle("-fx-font-size: 13; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e2e8f0;");
        motifCombo.setOnAction(e -> selectedMotif = motifCombo.getValue());
        step.getChildren().addAll(lblMotif, motifCombo);

        Label lblMsg = new Label("Message / Remarques"); lblMsg.getStyleClass().add("wizard-label");
        TextArea msgArea = new TextArea(); msgArea.setPromptText("Decrivez brievement vos symptomes ou questions...");
        msgArea.setPrefRowCount(3); msgArea.setStyle("-fx-font-size: 13; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e2e8f0;");
        msgArea.textProperty().addListener((obs, old, val) -> selectedMessage = val);
        step.getChildren().addAll(lblMsg, msgArea);

        Label lblType = new Label("Type de consultation"); lblType.getStyleClass().add("wizard-label");
        ToggleGroup tg = new ToggleGroup(); HBox typeBox = new HBox(12);
        RadioButton rbP = new RadioButton(); rbP.setToggleGroup(tg); rbP.setSelected(true);
        RadioButton rbE = new RadioButton(); rbE.setToggleGroup(tg);
        HBox cP = buildRadioCard(rbP, "Presentiel", "Au cabinet", true);
        HBox cE = buildRadioCard(rbE, "En ligne", "Teleconsultation", false);
        tg.selectedToggleProperty().addListener((obs, old, val) -> selectedType = val == rbP ? "presentiel" : "en_ligne");
        typeBox.getChildren().addAll(cP, cE); HBox.setHgrow(cP, Priority.ALWAYS); HBox.setHgrow(cE, Priority.ALWAYS);
        step.getChildren().addAll(lblType, typeBox);
        return step;
    }

    // ===== ÉTAPE 6 : PAIEMENT =====
    private VBox buildStep6_Paiement() {
        VBox step = new VBox(12);
        Label info = new Label("Comment souhaitez-vous regler la consultation ?"); info.getStyleClass().add("wizard-detail-text"); step.getChildren().add(info);
        ToggleGroup pg = new ToggleGroup();
        RadioButton r1 = new RadioButton(); r1.setToggleGroup(pg); r1.setSelected(true);
        VBox c1 = buildPaiementCard(r1, "Paiement sur place", "Especes ou carte au cabinet", "En attente", "#f97316", true);
        RadioButton r2 = new RadioButton(); r2.setToggleGroup(pg);
        VBox c2 = buildPaiementCard(r2, "Assurance maladie", "CNAM / Mutuelle - presenter au cabinet", "Remb.", "#3b82f6", false);
        RadioButton r3 = new RadioButton(); r3.setToggleGroup(pg);
        VBox c3 = buildPaiementCard(r3, "Carte bancaire en ligne", "Paiement securise - RDV confirme immediatement", "70 DT", "#7c3aed", false);
        pg.selectedToggleProperty().addListener((obs, old, val) -> {
            if (val == r1) selectedPaiement = "sur_place"; else if (val == r2) selectedPaiement = "assurance"; else if (val == r3) selectedPaiement = "carte_en_ligne";
            updatePaiementButton();
        });
        step.getChildren().addAll(c1, c2, c3);
        return step;
    }

    private void updatePaiementButton() { if (currentStep != 6) return; btnNext.setText("carte_en_ligne".equals(selectedPaiement) ? "Payer 70 DT" : "Confirmer le RDV"); }

    // ===== CONFIRMER =====
    private void confirmerRdv() { if ("carte_en_ligne".equals(selectedPaiement)) { ouvrirStripeWebView(); return; } creerRdvEnBase("En attente", selectedPaiement); }

    private void creerRdvEnBase(String statut, String paiement) {
        try {
            String nm = "Dr. " + selectedMedecin.getPrenom() + " " + selectedMedecin.getNom();
            String hf = calculerHeureFin(selectedHeure);
            String msg = (selectedMessage != null && !selectedMessage.isEmpty()) ? selectedMessage : "Paiement: " + paiement;
            rdv r = new rdv(selectedDate.toString(), selectedHeure, hf, statut, selectedMotif != null ? selectedMotif : "Consultation", nm, msg, 1, selectedMedecin.getUser_id());
            r.setTypeConsultation(selectedType); // ← ajouter cette ligne
            rdvService.insert(r);
            afficherSucces("Confirme".equalsIgnoreCase(statut));
        } catch (SQLException e) { Alert a = new Alert(Alert.AlertType.ERROR); a.setContentText("Erreur : " + e.getMessage()); a.show(); }
    }

    // ===== SUCCÈS =====
    private void afficherSucces(boolean paye) {
        contentArea.getChildren().clear();
        VBox success = new VBox(15); success.setAlignment(Pos.CENTER); success.setPadding(new Insets(20));

        Label icon = new Label(paye ? "OK" : "RDV");
        icon.setFont(Font.font("System", FontWeight.BOLD, 22)); icon.setTextFill(Color.web(paye ? "#15803d" : "#2563eb"));
        icon.setPrefWidth(70); icon.setPrefHeight(70); icon.setAlignment(Pos.CENTER);
        icon.setStyle("-fx-background-color: " + (paye ? "#dcfce7" : "#eff6ff") + "; -fx-background-radius: 50;");

        Label titre = new Label(paye ? "Rendez-vous confirme !" : "Rendez-vous enregistre !"); titre.getStyleClass().add("wizard-success-title");

        Label badge = new Label(paye ? "Confirme - paiement recu" : "En attente de confirmation");
        badge.setFont(Font.font("System", FontWeight.BOLD, 12)); badge.setPadding(new Insets(5, 15, 5, 15));
        badge.setStyle("-fx-background-radius: 20; -fx-background-color: " + (paye ? "#dcfce7" : "#fff7ed") + "; -fx-text-fill: " + (paye ? "#15803d" : "#c2410c") + ";");

        String nomMed = "Dr. " + selectedMedecin.getPrenom() + " " + selectedMedecin.getNom();
        String dateF = selectedDate.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH));

        VBox details = new VBox(8); details.setPadding(new Insets(15));
        details.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 14;");
        Label d1 = new Label("Medecin :  " + nomMed); d1.getStyleClass().add("wizard-detail-text");
        Label d2 = new Label("Date :  " + dateF); d2.getStyleClass().add("wizard-detail-text");
        Label d3 = new Label("Heure :  " + selectedHeure); d3.getStyleClass().add("wizard-detail-text");
        details.getChildren().addAll(d1, d2, d3);

        if (paye && stripePaymentId != null) {
            Label d4 = new Label("Stripe :  " + stripePaymentId); d4.getStyleClass().add("wizard-detail-text");
            d4.setStyle("-fx-font-size: 10; -fx-text-fill: #94a3b8;");
            details.getChildren().add(d4);
        }

        success.getChildren().addAll(icon, titre, badge, details);

        // Bouton reçu PDF si payé
        if (paye && stripePaymentId != null) {
            Button btnRecu = new Button("Telecharger le recu PDF");
            btnRecu.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 10; -fx-cursor: hand;");
            btnRecu.setCursor(Cursor.HAND);
            final String pid = stripePaymentId;
            final String fnm = nomMed;
            final String fdf = dateF;
            btnRecu.setOnAction(ev -> genererRecuPdf(fnm, fdf, pid));
            success.getChildren().add(btnRecu);
        }

        contentArea.getChildren().add(success);
        btnNext.setText("Fermer"); btnNext.setDisable(false);
        btnNext.setOnAction(e -> { wizardStage.close(); if (onRdvCreated != null) onRdvCreated.run(); });
        btnBack.setVisible(false);
        titleLabel.setText(paye ? "Confirme !" : "Enregistre !");
        subtitleLabel.setText("Votre rendez-vous a ete cree");
    }

    // ===== STRIPE WEBVIEW =====
    private void ouvrirStripeWebView() {
        try {
            StripeService stripeService = new StripeService();
            String clientSecret = stripeService.createPaymentIntent();

            java.io.InputStream is = getClass().getResourceAsStream("/stripe_payment.html");
            String html = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            html = html.replace("{{STRIPE_PUBLIC_KEY}}", StripeConfig.PUBLIC_KEY);
            html = html.replace("{{CLIENT_SECRET}}", clientSecret);

            Stage stripeStage = new Stage();
            stripeStage.initModality(Modality.APPLICATION_MODAL);
            stripeStage.initOwner(wizardStage);
            stripeStage.setTitle("Paiement Stripe - VitalTech");

            WebView webView = new WebView();
            webView.setPrefSize(500, 700);
            WebEngine webEngine = webView.getEngine();

            webEngine.setOnAlert(event -> {
                String data = event.getData();
                if (data != null && data.startsWith("STRIPE_SUCCESS:")) {
                    String paymentIntentId = data.substring("STRIPE_SUCCESS:".length());
                    System.out.println("Paiement Stripe OK : " + paymentIntentId);
                    boolean verified = stripeService.verifyPayment(paymentIntentId);
                    if (verified) {
                        javafx.application.Platform.runLater(() -> {
                            stripeStage.close();
                            this.stripePaymentId = paymentIntentId;
                            creerRdvEnBase("Confirme", "carte_en_ligne");
                        });
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            stripeStage.close();
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setContentText("Le paiement n'a pas pu etre verifie.");
                            alert.show();
                        });
                    }
                }
            });

            webEngine.loadContent(html);
            VBox root = new VBox(webView);
            root.setStyle("-fx-background-color: #f8fafc;");
            Scene scene = new Scene(root, 500, 700);
            stripeStage.setScene(scene);
            stripeStage.show();

        } catch (Exception e) {
            System.err.println("Erreur Stripe : " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Erreur Stripe : " + e.getMessage());
            alert.show();
        }
    }

    // ===== REÇU PDF =====
    private void genererRecuPdf(String nomMedecin, String dateRdv, String paymentId) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le recu");
        fileChooser.setInitialFileName("recu-paiement-rdv.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(wizardStage);
        if (file != null) {
            try {
                RecuPdfService recuService = new RecuPdfService();
                recuService.genererRecu(file.getAbsolutePath(), nomMedecin, dateRdv, selectedHeure,
                        calculerHeureFin(selectedHeure), selectedMotif != null ? selectedMotif : "Consultation", paymentId, 0);
                java.awt.Desktop.getDesktop().open(file);
            } catch (Exception ex) {
                System.err.println("Erreur recu PDF : " + ex.getMessage());
            }
        }
    }

    // ===== HELPERS =====
    private String calculerHeureFin(String hdebut) {
        String[] parts = hdebut.split(":"); int h = Integer.parseInt(parts[0]); int m = Integer.parseInt(parts[1]);
        m += 30; if (m >= 60) { m -= 60; h++; } return String.format("%02d:%02d", h, m);
    }

    private HBox buildInfoBox(String title, String subtitle, String bgColor, String textColor) {
        HBox box = new HBox(10); box.setPadding(new Insets(12)); box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; -fx-border-color: " + textColor + "33; -fx-border-radius: 12;");
        VBox tb = new VBox(2); tb.setAlignment(Pos.CENTER);
        Label t = new Label(title); t.setFont(Font.font("System", FontWeight.BOLD, 13)); t.setTextFill(Color.web(textColor));
        Label s = new Label(subtitle); s.setFont(Font.font("System", FontWeight.NORMAL, 11)); s.setTextFill(Color.web(textColor)); s.setOpacity(0.7);
        tb.getChildren().addAll(t, s); box.getChildren().add(tb); return box;
    }

    private HBox buildLegendItem(String text, String bgColor, String borderColor) {
        HBox item = new HBox(6); item.setAlignment(Pos.CENTER_LEFT);
        Region dot = new Region(); dot.setPrefWidth(12); dot.setPrefHeight(12);
        dot.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 3; -fx-border-color: " + borderColor + "; -fx-border-radius: 3; -fx-border-width: 1.5;");
        Label lbl = new Label(text); lbl.getStyleClass().add("wizard-detail-text");
        item.getChildren().addAll(dot, lbl); return item;
    }

    private HBox buildRadioCard(RadioButton rb, String title, String subtitle, boolean selected) {
        HBox card = new HBox(10); card.setAlignment(Pos.CENTER_LEFT); card.setPadding(new Insets(12)); card.setCursor(Cursor.HAND);
        card.setStyle("-fx-background-color: " + (selected ? "#eff6ff" : "white") + "; -fx-background-radius: 12; -fx-border-color: " + (selected ? "#4a6cf7" : "#e2e8f0") + "; -fx-border-radius: 12; -fx-border-width: 2;");
        card.setOnMouseClicked(e -> rb.setSelected(true));
        VBox tb = new VBox(2);
        Label t = new Label(title); t.getStyleClass().add("wizard-radio-title");
        Label s = new Label(subtitle); s.getStyleClass().add("wizard-radio-subtitle");
        tb.getChildren().addAll(t, s); card.getChildren().addAll(rb, tb); return card;
    }

    private VBox buildPaiementCard(RadioButton rb, String title, String subtitle, String badgeText, String badgeColor, boolean selected) {
        VBox card = new VBox();
        HBox content = new HBox(12); content.setAlignment(Pos.CENTER_LEFT); content.setPadding(new Insets(16)); content.setCursor(Cursor.HAND);
        content.setStyle("-fx-background-color: " + (selected ? "#eff6ff" : "white") + "; -fx-background-radius: 14; -fx-border-color: " + (selected ? "#4a6cf7" : "#e2e8f0") + "; -fx-border-radius: 14; -fx-border-width: 2;");
        content.setOnMouseClicked(e -> rb.setSelected(true));
        VBox tb = new VBox(2); HBox.setHgrow(tb, Priority.ALWAYS);
        Label t = new Label(title); t.getStyleClass().add("wizard-paiement-title");
        Label s = new Label(subtitle); s.getStyleClass().add("wizard-paiement-subtitle");
        tb.getChildren().addAll(t, s);
        Label badge = new Label(badgeText); badge.setFont(Font.font("System", FontWeight.BOLD, 11)); badge.setPadding(new Insets(4, 10, 4, 10));
        badge.setStyle("-fx-background-radius: 20; -fx-background-color: " + badgeColor + "22; -fx-text-fill: " + badgeColor + ";");
        content.getChildren().addAll(rb, tb, badge);
        rb.selectedProperty().addListener((obs, old, val) -> content.setStyle("-fx-background-color: " + (val ? "#eff6ff" : "white") + "; -fx-background-radius: 14; -fx-border-color: " + (val ? "#4a6cf7" : "#e2e8f0") + "; -fx-border-radius: 14; -fx-border-width: 2;"));
        card.getChildren().add(content); return card;
    }
}