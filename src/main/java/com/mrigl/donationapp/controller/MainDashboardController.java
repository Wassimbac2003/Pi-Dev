package com.mrigl.donationapp.controller;

import com.mrigl.donationapp.model.Annonce;
import com.mrigl.donationapp.model.Donation;
import com.mrigl.donationapp.model.FraudNotification;
import com.mrigl.donationapp.service.DataRepository;
import com.mrigl.donationapp.service.FraudDetectionService;
import com.mrigl.donationapp.service.HybridFraudDetectionService;
import com.mrigl.donationapp.service.HybridMatchingService;
import com.mrigl.donationapp.service.MatchingService;
import com.mrigl.donationapp.service.TextToSpeechService;
import com.mrigl.donationapp.service.TranslationService;
import com.mrigl.donationapp.service.ValidationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainDashboardController implements Initializable {
    private final DataRepository store = DataRepository.getInstance();
    private final HybridMatchingService matchingService = new HybridMatchingService();
    private final TranslationService translationService = new TranslationService();
    private final HybridFraudDetectionService fraudDetectionService = new HybridFraudDetectionService();
    private final TextToSpeechService textToSpeechService = new TextToSpeechService();
    private boolean suppressSelectionDialogs;
    private boolean backOfficeMode;

    @FXML private Button btnShowAll;
    @FXML private Button btnShowAnnonces;
    @FXML private Button btnShowDonations;
    @FXML private Button btnFrontMode;
    @FXML private Button btnBackMode;
    @FXML private Button btnSpamNotifications;
    @FXML private Button btnNav1;
    @FXML private Button btnNav2;
    @FXML private Button btnNav3;
    @FXML private Button btnNav4;
    @FXML private Button btnNav5;
    @FXML private Button btnNav6;
    @FXML private Button btnAddAnnonceTop;
    @FXML private Button btnAddDonationTop;
    @FXML private VBox subMenuDonAnnonce;
    @FXML private VBox sidebarPane;
    @FXML private VBox workspacePane;
    @FXML private VBox statsCard1;
    @FXML private VBox statsCard2;
    @FXML private VBox statsCard3;
    @FXML private BorderPane rootPane;
    @FXML private TextField tfGlobalSearch;
    @FXML private Label lblMenuMain;
    @FXML private Label lblMenuCommunity;
    @FXML private Label lblWorkspaceTitle;
    @FXML private Label lblHeroTitle;
    @FXML private Label lblHeroSubtitle;
    @FXML private Label lblStat1Title;
    @FXML private Label lblStat2Title;
    @FXML private Label lblStat3Title;
    @FXML private Label lblAnnoncesSectionTitle;
    @FXML private Label lblDonationsSectionTitle;
    @FXML private Label lblTotalAnnonces;
    @FXML private Label lblPendingDons;
    @FXML private Label lblAcceptedDons;
    @FXML private Label lblSpamNotifCount;
    @FXML private VBox annoncesPanel;
    @FXML private VBox donationsPanel;
    @FXML private HBox backOfficeInsightsRow;
    @FXML private ListView<Annonce> listAnnonces;
    @FXML private ListView<Donation> listDonations;
    @FXML private Label lblInsight1Title;
    @FXML private Label lblInsight1Value;
    @FXML private Label lblInsight1Sub;
    @FXML private Label lblInsight2Title;
    @FXML private Label lblInsight2Value;
    @FXML private Label lblInsight2Sub;
    @FXML private Label lblInsight3Title;
    @FXML private Label lblInsight3Value;
    @FXML private Label lblInsight3Sub;
    @FXML private Label lblInsight4Title;
    @FXML private Label lblInsight4Value;
    @FXML private Label lblInsight4Sub;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        subMenuDonAnnonce.setVisible(false);
        subMenuDonAnnonce.setManaged(false);
        activate(btnShowAll);
        switchToFront();

        store.getStartupDbError().ifPresent(err -> Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Connexion base de données");
            a.setHeaderText("Connexion MySQL indisponible");
            a.setContentText("L'interface démarre, mais la base n'est pas accessible pour le moment.\n" + err);
            a.showAndWait();
        }));

        FilteredList<Annonce> annonces = new FilteredList<>(store.annoncesProperty(), a -> true);
        FilteredList<Donation> dons = new FilteredList<>(store.donationsProperty(), d -> true);
        listAnnonces.setItems(annonces);
        listDonations.setItems(dons);
        refreshStats();
        refreshSpamNotificationBadge();

        tfGlobalSearch.textProperty().addListener((obs, ov, term) -> {
            String t = term == null ? "" : term.trim().toLowerCase();
            annonces.setPredicate(a -> t.isBlank() || safe(a.getTitreAnnonce()).toLowerCase().contains(t));
            dons.setPredicate(d -> t.isBlank() || safe(d.getTypeDon()).toLowerCase().contains(t));
        });

        store.annoncesProperty().addListener((javafx.collections.ListChangeListener.Change<? extends Annonce> c) -> refreshStats());
        store.donationsProperty().addListener((javafx.collections.ListChangeListener.Change<? extends Donation> c) -> refreshStats());

        listAnnonces.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Annonce a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setGraphic(null); return; }
                Label title = new Label(safe(a.getTitreAnnonce())); title.setStyle("-fx-font-weight: 700; -fx-font-size: 14px;");
                Label desc = new Label(safe(a.getDescription())); desc.setWrapText(true); desc.setStyle(backOfficeMode ? "-fx-text-fill: #6b7280;" : "-fx-text-fill: #475569;");
                Label date = new Label((backOfficeMode ? "Date & heure: " : "Publié le ") + a.getDatePublication()); date.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
                Label urgency = new Label("Urgence " + safe(a.getUrgence())); urgency.setStyle(badgeUrgence(a.getUrgence()));
                Label etat = new Label("active".equals(a.getEtatAnnonce()) ? "Active" : "Clôturée");
                etat.setStyle("active".equals(a.getEtatAnnonce()) ? badgeGreen() : badgeGray());
                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                HBox top = new HBox(8, title, sp, urgency);
                Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
                HBox foot = new HBox(8, date, sp2, etat);
                VBox card = new VBox(8, top, desc, foot);
                card.setPadding(new Insets(backOfficeMode ? 10 : 12));
                card.setStyle(backOfficeMode
                        ? "-fx-background-color: #ffffff; -fx-border-color: #eef2f7; -fx-border-width: 0 0 1 0; -fx-border-radius: 0; -fx-background-radius: 0;"
                        : "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12;");
                setGraphic(card);
            }
        });

        listDonations.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Donation d, boolean empty) {
                super.updateItem(d, empty);
                if (empty || d == null) { setGraphic(null); return; }
                Label title = new Label(safe(d.getTypeDon())); title.setStyle("-fx-font-weight: 700; -fx-font-size: 14px;");
                Label qty = new Label("Qté: " + (d.getQuantite() == null ? "—" : d.getQuantite())); qty.setStyle(badgeGray());
                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                HBox top = new HBox(8, title, sp, qty);
                Label date = new Label((backOfficeMode ? "Date & heure: " : "Donation du ") + d.getDateDonation()); date.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
                String annonce = d.getAnnonceId() == null ? "Aucune" : store.findAnnonce(d.getAnnonceId()).map(Annonce::getTitreAnnonce).orElse("Annonce supprimée");
                Label ann = new Label("Annonce: " + annonce); ann.setStyle(backOfficeMode ? "-fx-text-fill: #6b7280;" : "-fx-text-fill: #475569;");
                Label statut = new Label(formatStatus(d.getStatut())); statut.setStyle(statusStyle(d.getStatut()));
                Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
                HBox foot = new HBox(8, ann, sp2, statut);
                VBox card = new VBox(8, top, date, foot);
                card.setPadding(new Insets(backOfficeMode ? 10 : 12));
                card.setStyle(backOfficeMode
                        ? "-fx-background-color: #ffffff; -fx-border-color: #eef2f7; -fx-border-width: 0 0 1 0; -fx-border-radius: 0; -fx-background-radius: 0;"
                        : "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12;");
                setGraphic(card);
            }
        });

        listAnnonces.getSelectionModel().selectedItemProperty().addListener((obs, ov, s) -> {
            if (suppressSelectionDialogs || s == null) return;
            openAnnonceDetails(s);
            clearSelection();
        });
        listDonations.getSelectionModel().selectedItemProperty().addListener((obs, ov, s) -> {
            if (suppressSelectionDialogs || s == null) return;
            openDonationDetails(s);
            clearSelection();
        });
    }

    @FXML private void showAll() { annoncesPanel.setManaged(true); annoncesPanel.setVisible(true); donationsPanel.setManaged(true); donationsPanel.setVisible(true); activate(btnShowAll); }
    @FXML private void showAnnoncesOnly() { annoncesPanel.setManaged(true); annoncesPanel.setVisible(true); donationsPanel.setManaged(false); donationsPanel.setVisible(false); activate(btnShowAnnonces); }
    @FXML private void showDonationsOnly() { annoncesPanel.setManaged(false); annoncesPanel.setVisible(false); donationsPanel.setManaged(true); donationsPanel.setVisible(true); activate(btnShowDonations); }
    @FXML private void switchToFront() {
        backOfficeMode = false;
        applyMode();
        refreshSpamNotificationBadge();
    }
    @FXML private void switchToBack() {
        backOfficeMode = true;
        applyMode();
        refreshSpamNotificationBadge();
    }
    @FXML private void onAddAnnonceTop() { openAnnonceEditor(new Annonce(), true); }
    @FXML private void onAddDonationTop() { openDonationEditor(new Donation(), true); }
    @FXML private void toggleDonAnnoncesMenu() {
        boolean visible = subMenuDonAnnonce.isVisible();
        subMenuDonAnnonce.setVisible(!visible);
        subMenuDonAnnonce.setManaged(!visible);
        if (!visible) {
            showAll();
        }
    }

    private void openAnnonceDetails(Annonce a) {
        FormStage form = buildFormStage("Détail annonce", "Consultez et gérez cette annonce");
        Stage stage = form.stage();
        String rawTitre = safe(a.getTitreAnnonce());
        String rawDesc = safe(a.getDescription());
        String rawDate = String.valueOf(a.getDatePublication());
        String rawUrgence = safe(a.getUrgence());
        String rawEtat = safe(a.getEtatAnnonce());

        TextArea desc = new TextArea(rawDesc); desc.setWrapText(true); desc.setEditable(false); desc.setPrefRowCount(6);
        Label title = new Label("Titre : " + rawTitre); title.getStyleClass().add("dialog-title");
        Label descLabel = new Label("Description :");
        Label dateLabel = new Label("Date publication : " + rawDate);
        Label urgenceLabel = new Label("Urgence : " + rawUrgence);
        Label etatLabel = new Label("État : " + rawEtat);

        ComboBox<String> langChoice = new ComboBox<>(FXCollections.observableArrayList("fr", "en", "ar"));
        langChoice.setValue("fr");
        Button btnTranslate = new Button("Traduire");
        btnTranslate.getStyleClass().add("btn-ghost");
        btnTranslate.setOnAction(e -> {
            String lang = langChoice.getValue();
            title.setText(tr("Titre : " + rawTitre, lang));
            descLabel.setText(tr("Description :", lang));
            desc.setText(tr(rawDesc, lang));
            dateLabel.setText(tr("Date publication : " + rawDate, lang));
            urgenceLabel.setText(tr("Urgence : " + rawUrgence, lang));
            etatLabel.setText(tr("État : " + rawEtat, lang));
        });
        HBox translateBar = new HBox(10, new Label("Langue :"), langChoice, btnTranslate);
        translateBar.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, translateBar, title, descLabel, desc, dateLabel, urgenceLabel, etatLabel);
        box.setPadding(new Insets(12)); box.getStyleClass().add(backOfficeMode ? "back-detail-card" : "panel-card");
        Button btnSupprimer = new Button("Supprimer"); btnSupprimer.getStyleClass().add("btn-danger");
        Button btnProposerDon = new Button("Proposer un don"); btnProposerDon.getStyleClass().add("btn-secondary");
        Button btnRead = new Button("Lire à voix haute"); btnRead.getStyleClass().add("btn-ghost");
        Button btnStopRead = new Button("Arrêter la lecture"); btnStopRead.getStyleClass().add("btn-ghost");
        Button btnModifier = new Button("Modifier"); btnModifier.getStyleClass().add("btn-primary");
        Button btnFermer = new Button("Fermer"); btnFermer.getStyleClass().add("btn-ghost");
        HBox actions = new HBox(10, btnSupprimer, btnProposerDon, btnRead, btnStopRead, btnModifier, btnFermer);
        actions.setPadding(new Insets(12));
        VBox content = new VBox(10, box, actions);
        form.body().getChildren().add(content);

        btnModifier.setOnAction(e -> {
            stage.close();
            Platform.runLater(() -> openAnnonceEditor(a, false));
        });
        btnProposerDon.setOnAction(e -> {
            stage.close();
            Platform.runLater(() -> {
                Donation dn = new Donation();
                dn.setAnnonceId(a.getId());
                openDonationEditor(dn, true);
            });
        });
        btnRead.setOnAction(e -> speakText(
                "Annonce " + rawTitre + ". Description. " + rawDesc + ". Date publication " + rawDate
                        + ". Urgence " + rawUrgence + ". Etat " + rawEtat + "."
        ));
        btnStopRead.setOnAction(e -> stopSpeak());
        btnSupprimer.setOnAction(e -> { store.deleteAnnonce(a.getId()); stage.close(); });
        btnFermer.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private void openDonationDetails(Donation dn) {
        FormStage form = buildFormStage("Détail donation", "Consultez et gérez cette donation");
        Stage stage = form.stage();
        String annonce = dn.getAnnonceId() == null ? "Aucune" : store.findAnnonce(dn.getAnnonceId()).map(Annonce::getTitreAnnonce).orElse("Annonce supprimée");
        String rawType = safe(dn.getTypeDon());
        String rawQty = String.valueOf(dn.getQuantite());
        String rawDate = String.valueOf(dn.getDateDonation());
        String rawStatut = safe(dn.getStatut());
        String rawAnnonce = annonce;

        Label title = new Label("Type de don : " + rawType); title.getStyleClass().add("dialog-title");
        Label qtyLabel = new Label("Quantité : " + rawQty);
        Label dateLabel = new Label("Date : " + rawDate);
        Label statutLabel = new Label("Statut : " + rawStatut);
        Label annonceLabel = new Label("Annonce liée : " + rawAnnonce);

        ComboBox<String> langChoice = new ComboBox<>(FXCollections.observableArrayList("fr", "en", "ar"));
        langChoice.setValue("fr");
        Button btnTranslate = new Button("Traduire");
        btnTranslate.getStyleClass().add("btn-ghost");
        btnTranslate.setOnAction(e -> {
            String lang = langChoice.getValue();
            title.setText(tr("Type de don : " + rawType, lang));
            qtyLabel.setText(tr("Quantité : " + rawQty, lang));
            dateLabel.setText(tr("Date : " + rawDate, lang));
            statutLabel.setText(tr("Statut : " + rawStatut, lang));
            annonceLabel.setText(tr("Annonce liée : " + rawAnnonce, lang));
        });
        HBox translateBar = new HBox(10, new Label("Langue :"), langChoice, btnTranslate);
        translateBar.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, translateBar, title, qtyLabel, dateLabel, statutLabel, annonceLabel);
        box.setPadding(new Insets(12)); box.getStyleClass().add(backOfficeMode ? "back-detail-card" : "panel-card");
        Button btnSupprimer = new Button("Supprimer"); btnSupprimer.getStyleClass().add("btn-danger");
        Button btnRead = new Button("Lire à voix haute"); btnRead.getStyleClass().add("btn-ghost");
        Button btnStopRead = new Button("Arrêter la lecture"); btnStopRead.getStyleClass().add("btn-ghost");
        Button btnModifier = new Button("Modifier"); btnModifier.getStyleClass().add("btn-primary");
        Button btnFermer = new Button("Fermer"); btnFermer.getStyleClass().add("btn-ghost");
        HBox actions = new HBox(10, btnSupprimer, btnRead, btnStopRead, btnModifier, btnFermer);
        actions.setPadding(new Insets(12));
        VBox content = new VBox(10, box, actions);
        form.body().getChildren().add(content);

        btnModifier.setOnAction(e -> {
            stage.close();
            Platform.runLater(() -> openDonationEditor(dn, false));
        });
        btnRead.setOnAction(e -> speakText(
                "Donation. Type de don " + rawType + ". Quantite " + rawQty + ". Date " + rawDate
                        + ". Statut " + rawStatut + ". Annonce liee " + rawAnnonce + "."
        ));
        btnStopRead.setOnAction(e -> stopSpeak());
        btnSupprimer.setOnAction(e -> { store.deleteDonation(dn.getId()); stage.close(); });
        btnFermer.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private void openAnnonceEditor(Annonce base, boolean isCreate) {
        FormStage form = buildFormStage(isCreate ? "Nouvelle annonce" : "Modifier annonce", "Formulaire annonce");
        Stage stage = form.stage();
        TextField titre = new TextField(txt(base.getTitreAnnonce()));
        titre.setEditable(true);
        TextArea desc = new TextArea(txt(base.getDescription()));
        desc.setEditable(true);
        desc.setWrapText(true);
        desc.setPrefRowCount(4);
        DatePicker date = new DatePicker(base.getDatePublication() == null ? LocalDate.now() : base.getDatePublication());
        ComboBox<String> urg = new ComboBox<>(FXCollections.observableArrayList("faible", "moyenne", "élevée"));
        setStringCombo(urg, base.getUrgence());
        ComboBox<String> etat = new ComboBox<>(FXCollections.observableArrayList("active", "clôturée"));
        setStringCombo(etat, base.getEtatAnnonce());
        ComboBox<String> sourceLang = new ComboBox<>(FXCollections.observableArrayList("auto", "fr", "en", "ar"));
        sourceLang.setValue("auto");
        ComboBox<String> targetLang = new ComboBox<>(FXCollections.observableArrayList("fr", "en", "ar"));
        targetLang.setValue("fr");
        sourceLang.setPrefWidth(120);
        targetLang.setPrefWidth(120);
        Label translateInfo = new Label();
        translateInfo.setStyle("-fx-text-fill:#0f766e; -fx-font-size: 12px;");
        translateInfo.setWrapText(true);
        Label err = new Label(); err.setStyle("-fx-text-fill:#dc2626;"); err.setWrapText(true);
        Label lTitre = formLabel("Titre * :");
        Label lDesc = formLabel("Description * :");
        Label lDate = formLabel("Date * :");
        Label lUrg = formLabel("Urgence * :");
        Label lEtat = formLabel("État * :");
        Label lTranslate = formLabel("Traduction :");

        VBox fields = new VBox(10);
        fields.setPadding(new Insets(12));
        fields.getStyleClass().add(backOfficeMode ? "back-form-card" : "pro-form-card");
        Button translateBtn = new Button("Traduire");
        translateBtn.getStyleClass().add("btn-ghost");
        translateBtn.setOnAction(e -> {
            try {
                String translatedTitle = translationService.translate(titre.getText(), sourceLang.getValue(), targetLang.getValue());
                String translatedDesc = translationService.translate(desc.getText(), sourceLang.getValue(), targetLang.getValue());
                titre.setText(translatedTitle);
                desc.setText(translatedDesc);
                translateInfo.setText("Traduction appliquée vers '" + targetLang.getValue() + "'.");
                err.setText("");
            } catch (RuntimeException ex) {
                translateInfo.setText("");
                err.setText("Impossible de traduire pour le moment. Vérifiez votre connexion Internet.");
            }
        });
        HBox translateControls = new HBox(8,
                new Label("Source"), sourceLang,
                new Label("Cible"), targetLang,
                translateBtn);
        translateControls.setAlignment(Pos.CENTER_LEFT);

        fields.getChildren().addAll(
                formRow(lTitre, titre),
                formRowTopAligned(lDesc, desc),
                formRow(lDate, date),
                formRow(lUrg, urg),
                formRow(lEtat, etat),
                formRow(lTranslate, translateControls));
        Button saveBtn = new Button("Enregistrer"); saveBtn.getStyleClass().add(backOfficeMode ? "btn-secondary" : "btn-primary");
        Button cancelBtn = new Button("Annuler"); cancelBtn.getStyleClass().add("btn-ghost");
        HBox actions = new HBox(10, saveBtn, cancelBtn); actions.setStyle("-fx-padding: 12;");
        VBox root = new VBox(10, fields, translateInfo, err, actions); root.getStyleClass().add(backOfficeMode ? "back-form-root" : "pro-form-root");
        root.setMinHeight(Region.USE_PREF_SIZE);
        form.body().getChildren().add(root);

        saveBtn.setOnAction(ev -> {
            Annonce a = new Annonce(); a.setId(base.getId()); a.setTitreAnnonce(titre.getText()); a.setDescription(desc.getText()); a.setDatePublication(date.getValue()); a.setUrgence(urg.getValue()); a.setEtatAnnonce(etat.getValue());

            List<String> es = ValidationService.validateAnnonce(a);
            if (!es.isEmpty()) { err.setText(String.join("\n", es)); return; }

            FraudDetectionService.FraudReport fraud = fraudDetectionService.analyze(a, store.annoncesProperty());
            boolean adminReviewRequired = false;
            if (fraud.suspicious()) {
                boolean proceed = showFraudReviewDialog(fraud);
                if (!proceed) {
                    err.setText("Enregistrement annulé après alerte anti-fraude.");
                    return;
                }
                if (fraud.riskScore() > 0.69d) {
                    adminReviewRequired = true;
                }
            }
            suppressSelectionDialogs = true;
            if (isCreate || a.getId() == null) {
                store.createAnnonce(a);
            } else {
                store.updateAnnonce(a);
            }

            boolean shouldShowAdminPopupNow = false;
            if (adminReviewRequired) {
                Annonce persisted = (isCreate || a.getId() == null)
                        ? store.findLastAnnonceByContent(a).orElse(a)
                        : a;
                store.createFraudNotification(persisted, fraud.riskScore(), fraud.reasons());
                refreshSpamNotificationBadge();
                shouldShowAdminPopupNow = false;
            }
            stage.close();
            boolean finalShouldShowAdminPopupNow = shouldShowAdminPopupNow;
            Platform.runLater(() -> {
                clearSelection();
                suppressSelectionDialogs = false;
                if (finalShouldShowAdminPopupNow) {
                    showPendingFraudNotificationsForAdmin();
                }
            });
        });
        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private void openDonationEditor(Donation base, boolean isCreate) {
        FormStage form = buildFormStage(isCreate ? "Nouveau don" : "Modifier donation", "Formulaire donation");
        Stage stage = form.stage();
        TextField type = new TextField(txt(base.getTypeDon()));
        type.setEditable(true);
        TextField q = new TextField(base.getQuantite() == null ? "" : base.getQuantite().toString());
        q.setEditable(true);
        DatePicker date = new DatePicker(base.getDateDonation() == null ? LocalDate.now() : base.getDateDonation());
        ComboBox<String> st = new ComboBox<>(FXCollections.observableArrayList("en attente", "accepté", "refusé"));
        setStringCombo(st, base.getStatut() == null ? "en attente" : base.getStatut());
        ComboBox<Annonce> ann = new ComboBox<>(FXCollections.observableArrayList(store.annoncesProperty()));
        bindAnnonceComboToId(ann, base.getAnnonceId());
        Label suggestion = new Label();
        suggestion.setStyle("-fx-text-fill: #0369a1; -fx-font-size: 12px;");
        suggestion.setWrapText(true);
        Label err = new Label(); err.setStyle("-fx-text-fill:#dc2626;"); err.setWrapText(true);
        Label lType = formLabel("Type de don * :");
        Label lQte = formLabel("Quantité * :");
        Label lDate = formLabel("Date * :");
        Label lStatut = formLabel("Statut * :");
        Label lAnnonce = formLabel("Annonce :");

        VBox fields = new VBox(10);
        fields.setPadding(new Insets(12));
        fields.getStyleClass().add(backOfficeMode ? "back-form-card" : "pro-form-card");
        fields.getChildren().addAll(
                formRow(lType, type),
                formRow(lQte, q),
                formRow(lDate, date),
                formRow(lStatut, st),
                formRow(lAnnonce, ann));
        Button saveBtn = new Button("Enregistrer"); saveBtn.getStyleClass().add(backOfficeMode ? "btn-secondary" : "btn-primary");
        Button cancelBtn = new Button("Annuler"); cancelBtn.getStyleClass().add("btn-ghost");
        Button suggestBtn = new Button("Suggérer annonce IA");
        suggestBtn.getStyleClass().add("btn-ghost");
        suggestBtn.setOnAction(e -> applyDonationSuggestion(type.getText(), ann, suggestion, true));
        HBox actions = new HBox(10, saveBtn, cancelBtn); actions.setStyle("-fx-padding: 12;");
        HBox aiActions = new HBox(10, suggestBtn);
        aiActions.setPadding(new Insets(0, 12, 0, 12));
        VBox root = new VBox(10, fields, suggestion, err, aiActions, actions); root.getStyleClass().add(backOfficeMode ? "back-form-root" : "pro-form-root");
        root.setMinHeight(Region.USE_PREF_SIZE);
        form.body().getChildren().add(root);

        type.textProperty().addListener((obs, oldV, newV) -> applyDonationSuggestion(newV, ann, suggestion, false));
        applyDonationSuggestion(type.getText(), ann, suggestion, false);

        saveBtn.setOnAction(ev -> {
            Donation dn = new Donation(); dn.setId(base.getId()); dn.setTypeDon(type.getText()); dn.setQuantite(parseInt(q.getText())); dn.setDateDonation(date.getValue()); dn.setStatut(st.getValue()); dn.setAnnonceId(ann.getValue()==null?base.getAnnonceId():ann.getValue().getId());
            List<String> es = ValidationService.validateDonation(dn);
            if (dn.getQuantite()==null && q.getText()!=null && !q.getText().isBlank()) { es = new java.util.ArrayList<>(es); es.add(0,"La quantité doit être un nombre entier valide."); }
            if (!es.isEmpty()) { err.setText(String.join("\n", es)); return; }
            suppressSelectionDialogs = true;
            if (isCreate || dn.getId()==null) store.createDonation(dn); else store.updateDonation(dn);
            stage.close();
            Platform.runLater(() -> {
                clearSelection();
                suppressSelectionDialogs = false;
            });
        });
        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private void refreshStats() {
        int totalAnnonces = store.annoncesProperty().size();
        long pendingDons = store.donationsProperty().stream().filter(d -> "en attente".equals(d.getStatut())).count();
        long acceptedDons = store.donationsProperty().stream().filter(d -> "accepté".equals(d.getStatut())).count();
        lblTotalAnnonces.setText(String.valueOf(totalAnnonces));
        lblPendingDons.setText(String.valueOf(pendingDons));
        lblAcceptedDons.setText(String.valueOf(acceptedDons));
        refreshBackOfficeInsights();
    }

    private void activate(Button active) {
        btnShowAll.getStyleClass().remove("sidebar-btn-active");
        btnShowAnnonces.getStyleClass().remove("sidebar-btn-active");
        btnShowDonations.getStyleClass().remove("sidebar-btn-active");
        if (!active.getStyleClass().contains("sidebar-btn-active")) active.getStyleClass().add("sidebar-btn-active");
    }

    private void clearSelection() {
        suppressSelectionDialogs = true;
        listAnnonces.getSelectionModel().clearSelection();
        listDonations.getSelectionModel().clearSelection();
        Platform.runLater(() -> suppressSelectionDialogs = false);
    }

    private void applyMode() {
        rootPane.getStyleClass().remove("back-office-root");
        sidebarPane.getStyleClass().remove("back-office-sidebar");
        workspacePane.getStyleClass().remove("back-workspace");
        annoncesPanel.getStyleClass().remove("back-panel-card");
        donationsPanel.getStyleClass().remove("back-panel-card");
        statsCard1.getStyleClass().remove("back-stats-card");
        statsCard2.getStyleClass().remove("back-stats-card");
        statsCard3.getStyleClass().remove("back-stats-card");

        if (backOfficeMode) {
            rootPane.getStyleClass().add("back-office-root");
            sidebarPane.getStyleClass().add("back-office-sidebar");
            workspacePane.getStyleClass().add("back-workspace");
            annoncesPanel.getStyleClass().add("back-panel-card");
            donationsPanel.getStyleClass().add("back-panel-card");
            statsCard1.getStyleClass().add("back-stats-card");
            statsCard2.getStyleClass().add("back-stats-card");
            statsCard3.getStyleClass().add("back-stats-card");
            lblWorkspaceTitle.setText("Annonces et dons");
            lblHeroTitle.setText("Annonces et Donations");
            lblHeroSubtitle.setText("Gérez les annonces et les dons depuis ce tableau.");
            lblStat1Title.setText("AUJOURD'HUI");
            lblStat2Title.setText("EN ATTENTE");
            lblStat3Title.setText("TERMINÉS");
            lblAnnoncesSectionTitle.setText("Consultations / Annonces");
            lblDonationsSectionTitle.setText("Demandes de dons");
            lblMenuMain.setText("ADMIN");
            lblMenuCommunity.setText("GESTION");
            btnNav1.setText("◧  Dashboard");
            btnNav2.setText("◫  Utilisateurs");
            btnNav3.setText("◈  Rendez-vous");
            btnNav4.setText("◉  Modération");
            btnNav5.setText("◍  Bénévolat");
            btnNav6.setText("◔  Forum");
            btnAddAnnonceTop.setText("Ajouter une annonce");
            btnAddDonationTop.setText("Ajouter un don");
            btnSpamNotifications.setVisible(true);
            btnSpamNotifications.setManaged(true);
            lblSpamNotifCount.setVisible(true);
            lblSpamNotifCount.setManaged(true);
            backOfficeInsightsRow.setVisible(true);
            backOfficeInsightsRow.setManaged(true);
            btnFrontMode.getStyleClass().setAll("btn-ghost");
            btnBackMode.getStyleClass().setAll("btn-secondary");
            listAnnonces.refresh();
            listDonations.refresh();
            refreshBackOfficeInsights();
            return;
        }

        lblWorkspaceTitle.setText("Espace Solidarité");
        lblHeroTitle.setText("Gérez vos dons et annonces");
        lblHeroSubtitle.setText("Consultez les besoins, ajoutez vos annonces et proposez des dons rapidement.");
        lblMenuMain.setText("MENU PRINCIPAL");
        lblMenuCommunity.setText("COMMUNAUTÉ");
        btnNav1.setText("◧  Tableau de bord");
        btnNav2.setText("▣  Rendez-vous");
        btnNav3.setText("◌  Médicaments");
        btnNav4.setText("▤  Mes statistiques");
        btnNav5.setText("❤  Bénévolat");
        btnNav6.setText("◔  Forum");
        lblStat1Title.setText("Annonces");
        lblStat2Title.setText("Dons en attente");
        lblStat3Title.setText("Dons acceptés");
        lblAnnoncesSectionTitle.setText("Annonces récentes");
        lblDonationsSectionTitle.setText("Mes propositions de dons");
        btnAddAnnonceTop.setText("Ajouter une annonce");
        btnAddDonationTop.setText("Ajouter un don");
        btnSpamNotifications.setVisible(false);
        btnSpamNotifications.setManaged(false);
        lblSpamNotifCount.setVisible(false);
        lblSpamNotifCount.setManaged(false);
        backOfficeInsightsRow.setVisible(false);
        backOfficeInsightsRow.setManaged(false);
        btnFrontMode.getStyleClass().setAll("btn-secondary");
        btnBackMode.getStyleClass().setAll("btn-ghost");
        listAnnonces.refresh();
        listDonations.refresh();
    }

    private void styleDialog(Dialog<ButtonType> dialog, ButtonType primary, ButtonType secondary, ButtonType danger, ButtonType ghost) {
        DialogPane pane = dialog.getDialogPane();
        if (!pane.getStyleClass().contains("pro-dialog")) {
            pane.getStyleClass().add("pro-dialog");
        }
        pane.setPrefWidth(760);
        pane.setMinWidth(700);
        pane.setPrefHeight(460);
        pane.lookupButton(ghost).getStyleClass().add("btn-ghost");
        if (primary != null) {
            pane.lookupButton(primary).getStyleClass().add("btn-primary");
        }
        if (secondary != null) {
            pane.lookupButton(secondary).getStyleClass().add("btn-secondary");
        }
        if (danger != null) {
            pane.lookupButton(danger).getStyleClass().add("btn-danger");
        }
    }

    /**
     * Modal form window: body is the center region (no ScrollPane) so TextField/TextArea receive mouse and
     * keyboard reliably on all platforms — nested ScrollPane can steal hit-testing.
     */
    private record FormStage(Stage stage, VBox body) {}

    private FormStage buildFormStage(String title, String subtitle) {
        Stage stage = new Stage();
        stage.setTitle(title);
        Window owner = listAnnonces != null && listAnnonces.getScene() != null ? listAnnonces.getScene().getWindow() : null;
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);

        Label headerTitle = new Label(title);
        headerTitle.getStyleClass().add("dialog-title");
        Label headerSubtitle = new Label(subtitle);
        headerSubtitle.getStyleClass().add("muted-label");
        VBox header = new VBox(4, headerTitle, headerSubtitle);
        header.getStyleClass().add(backOfficeMode ? "back-dialog-header" : "pro-dialog-hero");
        header.setMaxWidth(Double.MAX_VALUE);

        VBox body = new VBox();
        body.setPadding(new Insets(8, 12, 12, 12));
        body.setMaxWidth(Double.MAX_VALUE);

        BorderPane root = new BorderPane();
        root.getStyleClass().add(backOfficeMode ? "back-dialog-root" : "pro-dialog-root");
        root.setTop(header);
        root.setCenter(body);
        BorderPane.setAlignment(body, Pos.TOP_LEFT);

        Scene scene = new Scene(root, 860, 560);
        var cssUrl = MainDashboardController.class.getResource("/com/mrigl/donationapp/app.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        stage.setMinWidth(520);
        stage.setMinHeight(420);
        stage.setScene(scene);
        return new FormStage(stage, body);
    }

    private Integer parseInt(String s) { try { return s == null || s.isBlank() ? null : Integer.parseInt(s.trim()); } catch (Exception e) { return null; } }

    private String tr(String text, String targetLang) {
        try {
            return translationService.translate(text, "auto", targetLang);
        } catch (RuntimeException ex) {
            return text;
        }
    }

    private boolean showFraudReviewDialog(FraudDetectionService.FraudReport fraud) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Vérification anti-fraude");
        dialog.initOwner(listAnnonces.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Label title = new Label("Annonce potentiellement suspecte");
        title.getStyleClass().add("dialog-title");
        int pct = (int) Math.round(fraud.riskScore() * 100d);
        Label score = new Label("Risque " + pct + "%");
        score.setStyle(pct >= 70
                ? "-fx-background-color:#fee2e2; -fx-text-fill:#991b1b; -fx-font-weight:700; -fx-padding:4 10; -fx-background-radius:999;"
                : "-fx-background-color:#fef3c7; -fx-text-fill:#92400e; -fx-font-weight:700; -fx-padding:4 10; -fx-background-radius:999;");

        VBox reasonsBox = new VBox(6);
        if (fraud.reasons() == null || fraud.reasons().isEmpty()) {
            reasonsBox.getChildren().add(new Label("- Anomalie détectée"));
        } else {
            for (String reason : fraud.reasons()) {
                reasonsBox.getChildren().add(new Label("- " + reason));
            }
        }
        Label confirm = new Label("Continuer l'enregistrement ?");
        confirm.setStyle("-fx-font-weight: 700;");
        VBox content = new VBox(10,
                new HBox(10, title, score),
                new Label("Signaux détectés :"),
                reasonsBox,
                confirm);
        content.setPadding(new Insets(12));
        content.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12;");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(560);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        okBtn.getStyleClass().add("btn-primary");
        cancelBtn.getStyleClass().add("btn-ghost");

        return dialog.showAndWait().filter(bt -> bt == ButtonType.OK).isPresent();
    }

    private void showPendingFraudNotificationsForAdmin() {
        List<FraudNotification> pending = new ArrayList<>(store.findPendingFraudNotifications());
        if (pending.isEmpty()) {
            return;
        }

        for (FraudNotification notification : pending) {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Alerte modération anti-spam");
            dialog.initOwner(listAnnonces.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);

            ButtonType keepType = new ButtonType("Garder l'annonce");
            ButtonType deleteType = new ButtonType("Supprimer l'annonce", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(keepType, deleteType);

            int pct = (int) Math.round(notification.getRiskScore() * 100d);
            String titre = notification.getTitreAnnonce() == null || notification.getTitreAnnonce().isBlank()
                    ? "Annonce sans titre"
                    : notification.getTitreAnnonce();
            String reasons = notification.getReasons() == null || notification.getReasons().isBlank()
                    ? "- Aucun détail disponible"
                    : "- " + notification.getReasons().replace(" | ", "\n- ");

            Label title = new Label("Risque spam donation détecté");
            title.getStyleClass().add("dialog-title");
            Label risk = new Label("Risque " + pct + "%");
            risk.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#991b1b; -fx-font-weight:700; -fx-padding:4 10; -fx-background-radius:999;");

            Label body = new Label(
                    "Une annonce à haut risque a été publiée malgré l'alerte utilisateur.\n\n"
                            + "Titre: " + titre + "\n"
                            + "ID annonce: " + (notification.getAnnonceId() == null ? "inconnu" : notification.getAnnonceId()) + "\n\n"
                            + "Signaux:\n" + reasons + "\n\n"
                            + "Décision administrateur:"
            );
            body.setWrapText(true);

            VBox content = new VBox(10, new HBox(10, title, risk), body);
            content.setPadding(new Insets(12));
            content.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12;");
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefWidth(620);

            Button keepBtn = (Button) dialog.getDialogPane().lookupButton(keepType);
            Button deleteBtn = (Button) dialog.getDialogPane().lookupButton(deleteType);
            keepBtn.getStyleClass().add("btn-ghost");
            deleteBtn.getStyleClass().add("btn-danger");

            ButtonType decision = dialog.showAndWait().orElse(keepType);
            if (decision == deleteType && notification.getAnnonceId() != null) {
                store.deleteAnnonce(notification.getAnnonceId());
                store.resolveFraudNotification(notification.getId(), "deleted_by_admin");
            } else if (decision == deleteType) {
                store.resolveFraudNotification(notification.getId(), "delete_requested_but_missing_annonce_id");
            } else {
                store.resolveFraudNotification(notification.getId(), "kept_by_admin");
            }
        }
    }

    @FXML
    private void openSpamNotifications() {
        if (!backOfficeMode) {
            return;
        }

        List<FraudNotification> pending = new ArrayList<>(store.findPendingFraudNotifications());
        refreshSpamNotificationBadge();
        if (pending.isEmpty()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Notifications anti-spam");
            info.setHeaderText("Aucune alerte en attente");
            info.setContentText("Il n'y a actuellement aucune annonce à risque > 69% en attente de revue.");
            info.showAndWait();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Notifications anti-spam");
        dialog.initOwner(listAnnonces.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        ListView<FraudNotification> notifList = new ListView<>();
        notifList.setItems(FXCollections.observableArrayList(pending));
        notifList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FraudNotification item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                int pct = (int) Math.round(item.getRiskScore() * 100d);
                Label title = new Label((item.getTitreAnnonce() == null || item.getTitreAnnonce().isBlank())
                        ? "Annonce sans titre"
                        : item.getTitreAnnonce());
                title.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
                Label sub = new Label("Risque " + pct + "%  •  cliquez pour modérer");
                sub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                VBox box = new VBox(3, title, sub);
                box.setPadding(new Insets(6));
                setGraphic(box);
            }
        });

        notifList.setOnMouseClicked(e -> {
            if (e.getClickCount() < 2) {
                return;
            }
            FraudNotification selected = notifList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            boolean resolved = openFraudNotificationReview(selected);
            if (resolved) {
                notifList.getItems().remove(selected);
                refreshSpamNotificationBadge();
                if (notifList.getItems().isEmpty()) {
                    dialog.close();
                }
            }
        });

        Label hint = new Label("Double-cliquez une notification pour voir l'annonce et décider garder/supprimer.");
        hint.setStyle("-fx-text-fill: #64748b;");
        VBox content = new VBox(10, hint, notifList);
        content.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(620);
        dialog.getDialogPane().setPrefHeight(460);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).getStyleClass().add("btn-ghost");
        dialog.showAndWait();
    }

    private boolean openFraudNotificationReview(FraudNotification notification) {
        if (notification == null) {
            return false;
        }
        Annonce annonce = notification.getAnnonceId() == null
                ? null
                : store.findAnnonce(notification.getAnnonceId()).orElse(null);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Revue annonce signalée");
        dialog.initOwner(listAnnonces.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        ButtonType keepType = new ButtonType("Garder");
        ButtonType deleteType = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(keepType, deleteType, ButtonType.CANCEL);
        dialog.getDialogPane().getStyleClass().add("pro-dialog");
        dialog.getDialogPane().setPrefWidth(760);

        int pct = (int) Math.round(notification.getRiskScore() * 100d);
        Label risk = new Label("Risque " + pct + "%");
        risk.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#991b1b; -fx-font-weight:700; -fx-padding:5 12; -fx-background-radius:999;");
        Label title = new Label("Annonce signalée");
        title.getStyleClass().add("dialog-title");
        Label subtitle = new Label("Modérez cette annonce signalée par l'anti-spam.");
        subtitle.getStyleClass().add("muted-label");

        String details = annonce == null
                ? "Annonce introuvable (peut-être déjà supprimée)\n"
                : "Titre: " + safe(annonce.getTitreAnnonce()) + "\n"
                + "Description: " + safe(annonce.getDescription()) + "\n"
                + "Date: " + annonce.getDatePublication() + "\n"
                + "Urgence: " + safe(annonce.getUrgence()) + "\n"
                + "État: " + safe(annonce.getEtatAnnonce()) + "\n";

        String reasons = notification.getReasons() == null || notification.getReasons().isBlank()
                ? "- Aucun signal détaillé"
                : "- " + notification.getReasons().replace(" | ", "\n- ");
        Label annonceSection = new Label("Détails de l'annonce");
        annonceSection.setStyle("-fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label detailsLabel = new Label(details);
        detailsLabel.setWrapText(true);
        VBox annonceCard = new VBox(8, annonceSection, detailsLabel);
        annonceCard.setPadding(new Insets(12));
        annonceCard.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label reasonsSection = new Label("Signaux anti-spam");
        reasonsSection.setStyle("-fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label reasonsLabel = new Label(reasons);
        reasonsLabel.setWrapText(true);
        VBox reasonsCard = new VBox(8, reasonsSection, reasonsLabel);
        reasonsCard.setPadding(new Insets(12));
        reasonsCard.setStyle("-fx-background-color: #fff7ed; -fx-border-color: #fed7aa; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label decisionHint = new Label("Décision admin");
        decisionHint.setStyle("-fx-font-weight: 700; -fx-text-fill: #334155;");

        VBox content = new VBox(12,
                new VBox(4, new HBox(10, title, risk), subtitle),
                annonceCard,
                reasonsCard,
                decisionHint);
        content.setPadding(new Insets(14));
        content.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-background-radius: 14;");
        dialog.getDialogPane().setContent(content);

        ((Button) dialog.getDialogPane().lookupButton(keepType)).getStyleClass().add("btn-ghost");
        ((Button) dialog.getDialogPane().lookupButton(deleteType)).getStyleClass().add("btn-danger");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).getStyleClass().add("btn-secondary");

        ButtonType decision = dialog.showAndWait().orElse(ButtonType.CANCEL);
        if (decision == keepType) {
            store.resolveFraudNotification(notification.getId(), "kept_by_admin");
            return true;
        }
        if (decision == deleteType) {
            if (notification.getAnnonceId() != null) {
                store.deleteAnnonce(notification.getAnnonceId());
                store.resolveFraudNotification(notification.getId(), "deleted_by_admin");
            } else {
                store.resolveFraudNotification(notification.getId(), "delete_requested_but_missing_annonce_id");
            }
            return true;
        }
        return false;
    }

    private void refreshSpamNotificationBadge() {
        int pending = store.findPendingFraudNotifications().size();
        lblSpamNotifCount.setText(String.valueOf(pending));
        lblSpamNotifCount.setVisible(backOfficeMode);
        lblSpamNotifCount.setManaged(backOfficeMode);
        if (pending == 0) {
            lblSpamNotifCount.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 3 8 3 8; -fx-alignment: center;");
        } else {
            lblSpamNotifCount.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 3 8 3 8; -fx-alignment: center;");
        }
    }

    private void refreshBackOfficeInsights() {
        if (lblInsight1Value == null) {
            return;
        }

        int totalAnnonces = store.annoncesProperty().size();
        long activeAnnonces = store.annoncesProperty().stream().filter(a -> "active".equals(a.getEtatAnnonce())).count();
        long closedAnnonces = store.annoncesProperty().stream().filter(a -> "clôturée".equals(a.getEtatAnnonce())).count();
        long highUrgence = store.annoncesProperty().stream().filter(a -> "élevée".equals(a.getUrgence())).count();

        long linkedDonations = store.donationsProperty().stream().filter(d -> d.getAnnonceId() != null).count();
        long pendingDons = store.donationsProperty().stream().filter(d -> "en attente".equals(d.getStatut())).count();
        long acceptedDons = store.donationsProperty().stream().filter(d -> "accepté".equals(d.getStatut())).count();
        long refusedDons = store.donationsProperty().stream().filter(d -> "refusé".equals(d.getStatut())).count();

        long annoncesWithDonation = store.annoncesProperty().stream()
                .filter(a -> a.getId() != null)
                .filter(a -> store.donationsProperty().stream().anyMatch(d -> a.getId().equals(d.getAnnonceId())))
                .count();
        int coveragePct = totalAnnonces == 0 ? 0 : (int) Math.round((annoncesWithDonation * 100.0d) / totalAnnonces);

        long totalDons = store.donationsProperty().size();
        int acceptedRate = totalDons == 0 ? 0 : (int) Math.round((acceptedDons * 100.0d) / totalDons);

        int spamPending = store.findPendingFraudNotifications().size();

        lblInsight1Title.setText("Activité annonces");
        lblInsight1Value.setText(activeAnnonces + " / " + totalAnnonces);
        lblInsight1Sub.setText("Clôturées: " + closedAnnonces + "  •  Urgence élevée: " + highUrgence);

        lblInsight2Title.setText("Couverture des besoins");
        lblInsight2Value.setText(coveragePct + "%");
        lblInsight2Sub.setText("Annonces avec don: " + annoncesWithDonation + "/" + totalAnnonces);

        lblInsight3Title.setText("Pipeline des dons");
        lblInsight3Value.setText(acceptedRate + "% acceptés");
        lblInsight3Sub.setText("En attente: " + pendingDons + "  •  Refusés: " + refusedDons + "  •  Liés: " + linkedDonations);

        lblInsight4Title.setText("Conformité anti-spam");
        lblInsight4Value.setText(spamPending + " alertes");
        lblInsight4Sub.setText("Notifications en attente de revue admin");
    }

    private void speakText(String text) {
        if (!textToSpeechService.isSupported()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Lecture vocale");
            info.setHeaderText("Lecture vocale indisponible");
            info.setContentText("Cette fonctionnalité est disponible sur Windows.");
            info.showAndWait();
            return;
        }
        textToSpeechService.speakAsync(text);
    }

    private void stopSpeak() {
        textToSpeechService.stop();
    }

    private static String txt(String s) {
        return s == null ? "" : s;
    }

    /** ComboBox value must match list items; DB values may differ in case or spacing. */
    private static void setStringCombo(ComboBox<String> cb, String value) {
        if (value == null || value.isBlank()) {
            cb.getSelectionModel().clearSelection();
            return;
        }
        String trimmed = value.trim();
        if (cb.getItems().contains(trimmed)) {
            cb.setValue(trimmed);
            return;
        }
        for (String item : cb.getItems()) {
            if (item != null && item.equalsIgnoreCase(trimmed)) {
                cb.setValue(item);
                return;
            }
        }
        cb.getSelectionModel().clearSelection();
    }

    /** List items are identity instances from the store; match by id when setting selection. */
    private void bindAnnonceComboToId(ComboBox<Annonce> ann, Long annonceId) {
        if (annonceId == null) {
            return;
        }
        store.findAnnonce(annonceId).ifPresent(found ->
                ann.getItems().stream()
                        .filter(x -> Objects.equals(x.getId(), found.getId()))
                        .findFirst()
                        .ifPresent(ann::setValue));
    }

    private void applyDonationSuggestion(String donationText, ComboBox<Annonce> ann, Label suggestion, boolean forceSelect) {
        List<MatchingService.MatchResult> ranked = matchingService.rankAnnoncesForDonationText(
                donationText,
                store.annoncesProperty(),
                5
        );
        if (ranked.isEmpty()) {
            ann.setItems(FXCollections.observableArrayList(store.annoncesProperty()));
            if (forceSelect) {
                ann.getSelectionModel().clearSelection();
            }
            suggestion.setText("IA: aucune correspondance pertinente pour le moment.");
            return;
        }

        List<Annonce> ordered = ranked.stream().map(MatchingService.MatchResult::annonce).toList();
        ann.setItems(FXCollections.observableArrayList(ordered));
        MatchingService.MatchResult top = ranked.get(0);
        if (forceSelect || ann.getValue() == null) {
            ann.setValue(top.annonce());
        }
        suggestion.setText(String.format(
                "IA: meilleure correspondance '%s' (score %.0f%%).",
                safe(top.annonce().getTitreAnnonce()),
                top.score() * 100d
        ));
    }

    private String safe(String s) { return s == null || s.isBlank() ? "—" : s; }
    private String formatStatus(String s) { return s == null ? "Inconnu" : ("en attente".equals(s) ? "En attente" : ("accepté".equals(s) ? "Accepté" : s)); }
    private String statusStyle(String s) { return "en attente".equals(s) ? badgeAmber() : ("accepté".equals(s) ? badgeGreen() : badgeGray()); }
    private String badgeUrgence(String u) { return "élevée".equals(u) ? badgeRed() : ("moyenne".equals(u) ? badgeAmber() : badgeGreen()); }
    private String badgeGreen() { return "-fx-font-size: 11px; -fx-font-weight: 700; -fx-background-color:#dcfce7; -fx-text-fill:#15803d; -fx-padding:4 8; -fx-background-radius:999;"; }
    private String badgeAmber() { return "-fx-font-size: 11px; -fx-font-weight: 700; -fx-background-color:#fef3c7; -fx-text-fill:#b45309; -fx-padding:4 8; -fx-background-radius:999;"; }
    private String badgeRed() { return "-fx-font-size: 11px; -fx-font-weight: 700; -fx-background-color:#fee2e2; -fx-text-fill:#b91c1c; -fx-padding:4 8; -fx-background-radius:999;"; }
    private String badgeGray() { return "-fx-font-size: 11px; -fx-font-weight: 700; -fx-background-color:#e2e8f0; -fx-text-fill:#334155; -fx-padding:4 8; -fx-background-radius:999;"; }

    private Label formLabel(String text) {
        Label label = new Label(text);
        label.setMinWidth(140);
        label.setStyle(backOfficeMode
                ? "-fx-text-fill: #374151; -fx-font-weight: 700;"
                : "-fx-text-fill: #334155; -fx-font-weight: 700;");
        return label;
    }

    /** Horizontal label + control row — avoids GridPane hit-testing issues on Windows with expanded rows. */
    private HBox formRow(Label label, Node field) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        label.setMinWidth(140);
        label.setPrefWidth(160);
        label.setMaxWidth(200);
        row.getChildren().addAll(label, field);
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    private HBox formRowTopAligned(Label label, TextArea area) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        label.setMinWidth(140);
        label.setPrefWidth(160);
        label.setMaxWidth(200);
        label.setPadding(new Insets(6, 0, 0, 0));
        row.getChildren().addAll(label, area);
        HBox.setHgrow(area, Priority.ALWAYS);
        return row;
    }
}
