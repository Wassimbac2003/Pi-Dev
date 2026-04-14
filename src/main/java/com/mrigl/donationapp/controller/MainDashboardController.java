package com.mrigl.donationapp.controller;

import com.mrigl.donationapp.model.Annonce;
import com.mrigl.donationapp.model.Donation;
import com.mrigl.donationapp.service.DataRepository;
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
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainDashboardController implements Initializable {
    private final DataRepository store = DataRepository.getInstance();
    private boolean suppressSelectionDialogs;
    private boolean backOfficeMode;

    @FXML private Button btnShowAll;
    @FXML private Button btnShowAnnonces;
    @FXML private Button btnShowDonations;
    @FXML private Button btnFrontMode;
    @FXML private Button btnBackMode;
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
    @FXML private VBox annoncesPanel;
    @FXML private VBox donationsPanel;
    @FXML private ListView<Annonce> listAnnonces;
    @FXML private ListView<Donation> listDonations;

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
    @FXML private void switchToFront() { backOfficeMode = false; applyMode(); }
    @FXML private void switchToBack() { backOfficeMode = true; applyMode(); }
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
        TextArea desc = new TextArea(safe(a.getDescription())); desc.setWrapText(true); desc.setEditable(false); desc.setPrefRowCount(6);
        Label title = new Label("Titre : " + safe(a.getTitreAnnonce())); title.getStyleClass().add("dialog-title");
        VBox box = new VBox(10, title, new Label("Description :"), desc,
                new Label("Date publication : " + a.getDatePublication()), new Label("Urgence : " + safe(a.getUrgence())), new Label("État : " + safe(a.getEtatAnnonce())));
        box.setPadding(new Insets(12)); box.getStyleClass().add(backOfficeMode ? "back-detail-card" : "panel-card");
        Button btnSupprimer = new Button("Supprimer"); btnSupprimer.getStyleClass().add("btn-danger");
        Button btnProposerDon = new Button("Proposer un don"); btnProposerDon.getStyleClass().add("btn-secondary");
        Button btnModifier = new Button("Modifier"); btnModifier.getStyleClass().add("btn-primary");
        Button btnFermer = new Button("Fermer"); btnFermer.getStyleClass().add("btn-ghost");
        HBox actions = new HBox(10, btnSupprimer, btnProposerDon, btnModifier, btnFermer);
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
        btnSupprimer.setOnAction(e -> { store.deleteAnnonce(a.getId()); stage.close(); });
        btnFermer.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private void openDonationDetails(Donation dn) {
        FormStage form = buildFormStage("Détail donation", "Consultez et gérez cette donation");
        Stage stage = form.stage();
        String annonce = dn.getAnnonceId() == null ? "Aucune" : store.findAnnonce(dn.getAnnonceId()).map(Annonce::getTitreAnnonce).orElse("Annonce supprimée");
        Label title = new Label("Type de don : " + safe(dn.getTypeDon())); title.getStyleClass().add("dialog-title");
        VBox box = new VBox(10, title, new Label("Quantité : " + dn.getQuantite()),
                new Label("Date : " + dn.getDateDonation()), new Label("Statut : " + safe(dn.getStatut())), new Label("Annonce liée : " + annonce));
        box.setPadding(new Insets(12)); box.getStyleClass().add(backOfficeMode ? "back-detail-card" : "panel-card");
        Button btnSupprimer = new Button("Supprimer"); btnSupprimer.getStyleClass().add("btn-danger");
        Button btnModifier = new Button("Modifier"); btnModifier.getStyleClass().add("btn-primary");
        Button btnFermer = new Button("Fermer"); btnFermer.getStyleClass().add("btn-ghost");
        HBox actions = new HBox(10, btnSupprimer, btnModifier, btnFermer);
        actions.setPadding(new Insets(12));
        VBox content = new VBox(10, box, actions);
        form.body().getChildren().add(content);

        btnModifier.setOnAction(e -> {
            stage.close();
            Platform.runLater(() -> openDonationEditor(dn, false));
        });
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
        Label err = new Label(); err.setStyle("-fx-text-fill:#dc2626;"); err.setWrapText(true);
        Label lTitre = formLabel("Titre * :");
        Label lDesc = formLabel("Description * :");
        Label lDate = formLabel("Date * :");
        Label lUrg = formLabel("Urgence * :");
        Label lEtat = formLabel("État * :");

        VBox fields = new VBox(10);
        fields.setPadding(new Insets(12));
        fields.getStyleClass().add(backOfficeMode ? "back-form-card" : "pro-form-card");
        fields.getChildren().addAll(
                formRow(lTitre, titre),
                formRowTopAligned(lDesc, desc),
                formRow(lDate, date),
                formRow(lUrg, urg),
                formRow(lEtat, etat));
        Button saveBtn = new Button("Enregistrer"); saveBtn.getStyleClass().add(backOfficeMode ? "btn-secondary" : "btn-primary");
        Button cancelBtn = new Button("Annuler"); cancelBtn.getStyleClass().add("btn-ghost");
        HBox actions = new HBox(10, saveBtn, cancelBtn); actions.setStyle("-fx-padding: 12;");
        VBox root = new VBox(10, fields, err, actions); root.getStyleClass().add(backOfficeMode ? "back-form-root" : "pro-form-root");
        root.setMinHeight(Region.USE_PREF_SIZE);
        form.body().getChildren().add(root);

        saveBtn.setOnAction(ev -> {
            Annonce a = new Annonce(); a.setId(base.getId()); a.setTitreAnnonce(titre.getText()); a.setDescription(desc.getText()); a.setDatePublication(date.getValue()); a.setUrgence(urg.getValue()); a.setEtatAnnonce(etat.getValue());
            List<String> es = ValidationService.validateAnnonce(a);
            if (!es.isEmpty()) { err.setText(String.join("\n", es)); return; }
            if (isCreate || a.getId() == null) store.createAnnonce(a); else store.updateAnnonce(a);
            stage.close();
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
        ComboBox<Annonce> ann = new ComboBox<>(store.annoncesProperty());
        bindAnnonceComboToId(ann, base.getAnnonceId());
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
        HBox actions = new HBox(10, saveBtn, cancelBtn); actions.setStyle("-fx-padding: 12;");
        VBox root = new VBox(10, fields, err, actions); root.getStyleClass().add(backOfficeMode ? "back-form-root" : "pro-form-root");
        root.setMinHeight(Region.USE_PREF_SIZE);
        form.body().getChildren().add(root);

        saveBtn.setOnAction(ev -> {
            Donation dn = new Donation(); dn.setId(base.getId()); dn.setTypeDon(type.getText()); dn.setQuantite(parseInt(q.getText())); dn.setDateDonation(date.getValue()); dn.setStatut(st.getValue()); dn.setAnnonceId(ann.getValue()==null?base.getAnnonceId():ann.getValue().getId());
            List<String> es = ValidationService.validateDonation(dn);
            if (dn.getQuantite()==null && q.getText()!=null && !q.getText().isBlank()) { es = new java.util.ArrayList<>(es); es.add(0,"La quantité doit être un nombre entier valide."); }
            if (!es.isEmpty()) { err.setText(String.join("\n", es)); return; }
            if (isCreate || dn.getId()==null) store.createDonation(dn); else store.updateDonation(dn);
            stage.close();
        });
        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private void refreshStats() {
        lblTotalAnnonces.setText(String.valueOf(store.annoncesProperty().size()));
        lblPendingDons.setText(String.valueOf(store.donationsProperty().stream().filter(d -> "en attente".equals(d.getStatut())).count()));
        lblAcceptedDons.setText(String.valueOf(store.donationsProperty().stream().filter(d -> "accepté".equals(d.getStatut())).count()));
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
            btnFrontMode.getStyleClass().setAll("btn-ghost");
            btnBackMode.getStyleClass().setAll("btn-secondary");
            listAnnonces.refresh();
            listDonations.refresh();
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
