package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import tn.esprit.entities.Fiche;
import tn.esprit.entities.User;
import tn.esprit.fx.FormFieldSlot;
import tn.esprit.fx.FormValidationStyles;
import tn.esprit.fx.UiTheme;
import tn.esprit.services.ServiceFiche;
import tn.esprit.services.ServiceUser;
import tn.esprit.tools.ValidationUtil;
import tn.esprit.tools.ValidationUtil.FicheFormInput;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminFichesController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> graviteFilter;
    @FXML
    private ComboBox<String> grpFilter;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Label countLabel;
    @FXML
    private FlowPane cardsFlow;

    private final ServiceFiche service = new ServiceFiche();
    private final ServiceUser userService = new ServiceUser();
    private final ObservableList<Fiche> master = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Date (récent → ancien)",
                "Date (ancien → récent)",
                "Plus récentes d’abord",
                "Libellé (A → Z)"
        ));
        sortCombo.getSelectionModel().selectFirst();
        graviteFilter.setItems(FXCollections.observableArrayList("Toutes"));
        grpFilter.setItems(FXCollections.observableArrayList("Tous"));

        searchField.textProperty().addListener((o, a, b) -> refreshView());
        graviteFilter.valueProperty().addListener((o, a, b) -> refreshView());
        grpFilter.valueProperty().addListener((o, a, b) -> refreshView());
        sortCombo.valueProperty().addListener((o, a, b) -> refreshView());

        reloadMaster();
    }

    private void reloadMaster() {
        master.setAll(service.listAll());
        ObservableList<String> gravs = FXCollections.observableArrayList("Toutes");
        master.stream().map(Fiche::getGravite).filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).forEach(gravs::add);
        String pg = graviteFilter.getSelectionModel().getSelectedItem();
        graviteFilter.setItems(gravs);
        if (pg != null && gravs.contains(pg)) {
            graviteFilter.getSelectionModel().select(pg);
        } else {
            graviteFilter.getSelectionModel().selectFirst();
        }

        ObservableList<String> grps = FXCollections.observableArrayList("Tous");
        master.stream().map(Fiche::getGrpSanguin).filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).forEach(grps::add);
        String pgr = grpFilter.getSelectionModel().getSelectedItem();
        grpFilter.setItems(grps);
        if (pgr != null && grps.contains(pgr)) {
            grpFilter.getSelectionModel().select(pgr);
        } else {
            grpFilter.getSelectionModel().selectFirst();
        }

        refreshView();
    }

    private void refreshView() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String g = Optional.ofNullable(graviteFilter.getSelectionModel().getSelectedItem()).orElse("Toutes");
        String grp = Optional.ofNullable(grpFilter.getSelectionModel().getSelectedItem()).orElse("Tous");

        Comparator<Fiche> cmp = comparatorForSort();
        var list = master.stream()
                .filter(f -> matchesSearch(f, q))
                .filter(f -> matchesGravite(f, g))
                .filter(f -> matchesGrp(f, grp))
                .sorted(cmp)
                .collect(Collectors.toList());

        cardsFlow.getChildren().clear();
        for (Fiche f : list) {
            cardsFlow.getChildren().add(buildCard(f));
        }
        countLabel.setText(list.size() + " affichée(s) · " + master.size() + " au total");
    }

    private boolean matchesSearch(Fiche f, String q) {
        if (q.isEmpty()) {
            return true;
        }
        String patientLabel = userService.findById(f.getIdUId()).map(UiTheme::userDisplayName).orElse("").toLowerCase(Locale.ROOT);
        return String.valueOf(f.getId()).contains(q)
                || patientLabel.contains(q)
                || contains(f.getLibelleMaladie(), q)
                || contains(f.getGrpSanguin(), q)
                || contains(f.getGravite(), q)
                || contains(f.getAllergie(), q)
                || contains(f.getMaladieChronique(), q)
                || contains(f.getTension(), q)
                || contains(f.getSymptomes(), q)
                || contains(f.getRecommandation(), q)
                || (f.getDate() != null && f.getDate().toString().toLowerCase(Locale.ROOT).contains(q));
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    private boolean matchesGravite(Fiche f, String g) {
        if ("Toutes".equalsIgnoreCase(g)) {
            return true;
        }
        return f.getGravite() != null && f.getGravite().equalsIgnoreCase(g);
    }

    private boolean matchesGrp(Fiche f, String grp) {
        if ("Tous".equalsIgnoreCase(grp)) {
            return true;
        }
        return f.getGrpSanguin() != null && f.getGrpSanguin().equalsIgnoreCase(grp);
    }

    private Comparator<Fiche> comparatorForSort() {
        String s = sortCombo.getSelectionModel().getSelectedItem();
        if (s == null) {
            return Comparator.comparing(Fiche::getDate, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return switch (s) {
            case "Date (ancien → récent)" -> Comparator.comparing(Fiche::getDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Plus récentes d’abord" -> Comparator.comparingInt(Fiche::getId).reversed();
            case "Libellé (A → Z)" -> Comparator.comparing(Fiche::getLibelleMaladie, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Fiche::getDate, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    private VBox buildCard(Fiche f) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        String patient = userService.findById(f.getIdUId()).map(UiTheme::userDisplayName).filter(s -> !s.isBlank()).orElse("Patient");

        String gravStr = f.getGravite() != null && !f.getGravite().isBlank() ? f.getGravite().trim() : "Gravité non précisée";
        Label grav = new Label(gravStr);
        grav.getStyleClass().add("card-badge");
        grav.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label(nullSafe(f.getLibelleMaladie()));
        title.getStyleClass().add("card-title");

        Label who = new Label("Patient · " + patient);
        who.getStyleClass().add("card-sub");
        who.setWrapText(true);

        String medLabel = f.getMedecinUserId() != null
                ? userService.findById(f.getMedecinUserId()).map(UiTheme::userDisplayName).filter(s -> !s.isBlank()).orElse("—")
                : "—";
        Label doc = new Label("Médecin rédacteur · " + medLabel);
        doc.getStyleClass().add("card-sub");
        doc.setWrapText(true);

        Label corp = new Label(
                "Groupe " + nullSafe(f.getGrpSanguin()) + " · Tension " + nullSafe(f.getTension()) + " · Glycémie " + f.getGlycemie()
                        + "\nPoids / taille · " + f.getPoids() + " kg / " + f.getTaille() + " cm"
                        + "\nConsultation du " + (f.getDate() != null ? f.getDate().toString() : "—"));
        corp.setWrapText(true);
        corp.getStyleClass().add("card-meta");

        Label extras = new Label(
                "Allergies · " + nullSafe(f.getAllergie()) + "\nAntécédents · " + nullSafe(f.getMaladieChronique()));
        extras.setWrapText(true);
        extras.getStyleClass().add("card-sub");
        Label sym = new Label(truncate("Symptômes · ", f.getSymptomes(), 200));
        sym.setWrapText(true);
        sym.getStyleClass().add("card-sub");
        Label reco = new Label(truncate("Recommandation · ", f.getRecommandation(), 200));
        reco.setWrapText(true);
        reco.getStyleClass().add("card-sub");

        HBox actions = new HBox(10);
        actions.getStyleClass().add("card-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        Button edit = new Button("Modifier");
        edit.getStyleClass().add("ghost-button");
        edit.setOnAction(e -> showEditorDialog(f));
        Button del = new Button("Supprimer");
        del.getStyleClass().add("danger-button");
        del.setOnAction(e -> confirmDelete(f));
        actions.getChildren().addAll(edit, del);

        card.getChildren().addAll(grav, title, who, doc, corp, extras, sym, reco, actions);
        return card;
    }

    private static String nullSafe(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String truncate(String prefix, String s, int max) {
        if (s == null || s.isBlank()) {
            return prefix + "—";
        }
        String t = s.trim();
        String body = t.length() <= max ? t : t.substring(0, max) + "…";
        return prefix + body;
    }

    @FXML
    private void onAdd() {
        showEditorDialog(null);
    }

    private void confirmDelete(Fiche f) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer définitivement ce dossier ? Les ordonnances liées peuvent empêcher la suppression.");
        UiTheme.applyDialog(a);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            service.supprimer(f);
            reloadMaster();
        }
    }

    private void showEditorDialog(Fiche existing) {
        Window owner = cardsFlow.getScene().getWindow();
        boolean isEdit = existing != null;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        UiTheme.applyDialog(dialog);
        dialog.setTitle(isEdit ? "Modifier une fiche" : "Nouvelle fiche");
        dialog.setHeaderText(isEdit
                ? "Mettre à jour le dossier de « " + userService.findById(existing.getIdUId()).map(UiTheme::userDisplayName).orElse("patient") + " »"
                : "Créer une fiche pour un patient");

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<User> userCombo = new ComboBox<>(FXCollections.observableArrayList(userService.listPatientsRolePatient()));
        userCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : UiTheme.userDisplayName(u));
            }
        });
        userCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : UiTheme.userDisplayName(u));
            }
        });
        ComboBox<User> medecinCombo = new ComboBox<>(FXCollections.observableArrayList(userService.listMedecins()));
        medecinCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : UiTheme.userDisplayName(u));
            }
        });
        medecinCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : UiTheme.userDisplayName(u));
            }
        });
        TextField fPoids = new TextField();
        TextField fTaille = new TextField();
        ComboBox<String> fGrp = new ComboBox<>(FXCollections.observableArrayList(ValidationUtil.GROUPES_SANGUINS));
        TextField fAllergie = new TextField();
        TextField fChronique = new TextField();
        TextField fTension = new TextField();
        TextField fGlycemie = new TextField();
        DatePicker fDate = new DatePicker();
        TextField fLibelle = new TextField();
        ComboBox<String> fGravite = new ComboBox<>(FXCollections.observableArrayList(ValidationUtil.GRAVITES));
        TextArea fReco = new TextArea();
        TextArea fSymptomes = new TextArea();
        fReco.setPrefRowCount(2);
        fSymptomes.setPrefRowCount(2);
        fReco.setWrapText(true);
        fSymptomes.setWrapText(true);

        if (isEdit) {
            userCombo.getItems().stream().filter(u -> u.getId() == existing.getIdUId()).findFirst()
                    .ifPresent(u -> userCombo.getSelectionModel().select(u));
            if (existing.getMedecinUserId() != null) {
                medecinCombo.getItems().stream().filter(u -> u.getId() == existing.getMedecinUserId()).findFirst()
                        .ifPresent(u -> medecinCombo.getSelectionModel().select(u));
            }
            fPoids.setText(String.valueOf(existing.getPoids()));
            fTaille.setText(String.valueOf(existing.getTaille()));
            if (existing.getGrpSanguin() != null && fGrp.getItems().contains(existing.getGrpSanguin())) {
                fGrp.getSelectionModel().select(existing.getGrpSanguin());
            }
            fAllergie.setText(existing.getAllergie());
            fChronique.setText(existing.getMaladieChronique());
            fTension.setText(existing.getTension());
            fGlycemie.setText(String.valueOf(existing.getGlycemie()));
            if (existing.getDate() != null) {
                fDate.setValue(existing.getDate());
            }
            fLibelle.setText(existing.getLibelleMaladie());
            if (existing.getGravite() != null && fGravite.getItems().contains(existing.getGravite())) {
                fGravite.getSelectionModel().select(existing.getGravite());
            } else if (existing.getGravite() != null && !existing.getGravite().isBlank()) {
                fGravite.getItems().add(existing.getGravite());
                fGravite.getSelectionModel().select(existing.getGravite());
            }
            fReco.setText(existing.getRecommandation());
            fSymptomes.setText(existing.getSymptomes());
            fDate.setDisable(false);
        } else {
            fDate.setValue(LocalDate.now());
            fDate.setDisable(true);
        }

        FormFieldSlot slotPatient = new FormFieldSlot(userCombo);
        FormFieldSlot slotMedecin = new FormFieldSlot(medecinCombo);
        FormFieldSlot slotPoids = new FormFieldSlot(fPoids);
        FormFieldSlot slotTaille = new FormFieldSlot(fTaille);
        FormFieldSlot slotGrp = new FormFieldSlot(fGrp);
        FormFieldSlot slotAllergie = new FormFieldSlot(fAllergie);
        FormFieldSlot slotChronique = new FormFieldSlot(fChronique);
        FormFieldSlot slotTension = new FormFieldSlot(fTension);
        FormFieldSlot slotGlycemie = new FormFieldSlot(fGlycemie);
        FormFieldSlot slotDate = new FormFieldSlot(fDate);
        FormFieldSlot slotLibelle = new FormFieldSlot(fLibelle);
        FormFieldSlot slotGravite = new FormFieldSlot(fGravite);
        FormFieldSlot slotReco = new FormFieldSlot(fReco);
        FormFieldSlot slotSymptomes = new FormFieldSlot(fSymptomes);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(4, 0, 8, 0));
        int r = 0;
        grid.add(new Label("Patient *"), 0, r);
        grid.add(slotPatient.getRoot(), 1, r++);
        grid.add(new Label("Médecin rédacteur *"), 0, r);
        grid.add(slotMedecin.getRoot(), 1, r++);
        grid.add(new Label("Poids (kg) *"), 0, r);
        grid.add(slotPoids.getRoot(), 1, r++);
        grid.add(new Label("Taille (cm) *"), 0, r);
        grid.add(slotTaille.getRoot(), 1, r++);
        grid.add(new Label("Groupe sanguin *"), 0, r);
        grid.add(slotGrp.getRoot(), 1, r++);
        grid.add(new Label("Allergies"), 0, r);
        grid.add(slotAllergie.getRoot(), 1, r++);
        grid.add(new Label("Maladies chroniques"), 0, r);
        grid.add(slotChronique.getRoot(), 1, r++);
        grid.add(new Label("Tension *"), 0, r);
        grid.add(slotTension.getRoot(), 1, r++);
        grid.add(new Label("Glycémie *"), 0, r);
        grid.add(slotGlycemie.getRoot(), 1, r++);
        grid.add(new Label("Date de consultation *"), 0, r);
        grid.add(slotDate.getRoot(), 1, r++);
        grid.add(new Label("Motif / libellé *"), 0, r);
        grid.add(slotLibelle.getRoot(), 1, r++);
        grid.add(new Label("Gravité *"), 0, r);
        grid.add(slotGravite.getRoot(), 1, r++);
        grid.add(new Label("Recommandations"), 0, r);
        grid.add(slotReco.getRoot(), 1, r++);
        grid.add(new Label("Symptômes"), 0, r);
        grid.add(slotSymptomes.getRoot(), 1, r);
        GridPane.setHgrow(userCombo, Priority.ALWAYS);
        GridPane.setHgrow(medecinCombo, Priority.ALWAYS);
        GridPane.setHgrow(fLibelle, Priority.ALWAYS);
        UiTheme.tagFormLabels(grid);

        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(520);

        Label validationSummary = new Label();
        validationSummary.getStyleClass().add("form-validation-summary");
        validationSummary.setWrapText(true);
        validationSummary.setMaxWidth(560);
        validationSummary.setVisible(false);
        validationSummary.setManaged(false);

        VBox formShell = new VBox(10, validationSummary, sp);
        dialog.getDialogPane().setContent(formShell);

        Runnable clearValidation = () -> {
            validationSummary.setVisible(false);
            validationSummary.setManaged(false);
            validationSummary.setText("");
            FormValidationStyles.markSection(formShell, false);
            slotPatient.clear();
            slotMedecin.clear();
            slotPoids.clear();
            slotTaille.clear();
            slotGrp.clear();
            slotAllergie.clear();
            slotChronique.clear();
            slotTension.clear();
            slotGlycemie.clear();
            slotDate.clear();
            slotLibelle.clear();
            slotGravite.clear();
            slotReco.clear();
            slotSymptomes.clear();
        };

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            clearValidation.run();
            User u = userCombo.getSelectionModel().getSelectedItem();
            User med = medecinCombo.getSelectionModel().getSelectedItem();
            FicheFormInput input = new FicheFormInput(
                    u != null ? u.getId() : null,
                    med != null ? med.getId() : null,
                    true,
                    fPoids.getText(),
                    fTaille.getText(),
                    fGrp.getSelectionModel().getSelectedItem(),
                    fGravite.getSelectionModel().getSelectedItem(),
                    fAllergie.getText(),
                    fChronique.getText(),
                    fTension.getText(),
                    fGlycemie.getText(),
                    fLibelle.getText(),
                    fReco.getText(),
                    fSymptomes.getText(),
                    fDate.getValue(),
                    isEdit
            );
            LinkedHashMap<String, String> err = ValidationUtil.validateFicheDetailed(input);
            if (!err.isEmpty()) {
                ev.consume();
                for (Map.Entry<String, String> en : err.entrySet()) {
                    switch (en.getKey()) {
                        case "patient" -> slotPatient.setError(en.getValue());
                        case "medecin" -> slotMedecin.setError(en.getValue());
                        case "poids" -> slotPoids.setError(en.getValue());
                        case "taille" -> slotTaille.setError(en.getValue());
                        case "grpSanguin" -> slotGrp.setError(en.getValue());
                        case "allergie" -> slotAllergie.setError(en.getValue());
                        case "chronique" -> slotChronique.setError(en.getValue());
                        case "tension" -> slotTension.setError(en.getValue());
                        case "glycemie" -> slotGlycemie.setError(en.getValue());
                        case "date" -> slotDate.setError(en.getValue());
                        case "libelle" -> slotLibelle.setError(en.getValue());
                        case "gravite" -> slotGravite.setError(en.getValue());
                        case "recommandation" -> slotReco.setError(en.getValue());
                        case "symptomes" -> slotSymptomes.setError(en.getValue());
                        default -> {
                        }
                    }
                }
                validationSummary.setText("Veuillez corriger les champs signalés ci-dessous.");
                validationSummary.setVisible(true);
                validationSummary.setManaged(true);
                FormValidationStyles.markSection(formShell, true);
                return;
            }

            double poids = Double.parseDouble(fPoids.getText().trim().replace(',', '.'));
            double taille = Double.parseDouble(fTaille.getText().trim().replace(',', '.'));
            double glycemie = Double.parseDouble(fGlycemie.getText().trim().replace(',', '.'));
            String grp = fGrp.getSelectionModel().getSelectedItem();
            String grav = fGravite.getSelectionModel().getSelectedItem();
            String lib = fLibelle.getText() != null ? fLibelle.getText().trim() : "";
            String tension = fTension.getText() != null ? fTension.getText().trim() : "";

            Fiche f = new Fiche();
            f.setPoids(poids);
            f.setTaille(taille);
            f.setGrpSanguin(grp);
            f.setAllergie(safe(fAllergie));
            f.setMaladieChronique(safe(fChronique));
            f.setTension(tension);
            f.setGlycemie(glycemie);
            if (isEdit) {
                f.setDate(fDate.getValue());
            } else {
                f.setDate(LocalDate.now());
            }
            f.setLibelleMaladie(lib);
            f.setGravite(grav);
            f.setRecommandation(fReco.getText() != null ? fReco.getText() : "");
            f.setSymptomes(fSymptomes.getText() != null ? fSymptomes.getText() : "");
            f.setIdUId(u.getId());
            f.setMedecinUserId(med.getId());

            if (isEdit) {
                f.setId(existing.getId());
                service.modifier(f);
            } else {
                Optional<Fiche> ex = service.findByUserId(u.getId());
                if (ex.isPresent()) {
                    ev.consume();
                    slotPatient.setError("Ce patient a déjà une fiche. Utilisez « Modifier » sur la carte.");
                    validationSummary.setText("Impossible d’ajouter une deuxième fiche pour ce patient.");
                    validationSummary.setVisible(true);
                    validationSummary.setManaged(true);
                    FormValidationStyles.markSection(formShell, true);
                    return;
                }
                service.ajouter(f);
            }
        });

        dialog.showAndWait();
        reloadMaster();
    }

    private static String safe(TextField t) {
        String s = t.getText();
        return s != null ? s.trim() : "";
    }

    private static void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        UiTheme.applyDialog(a);
        a.showAndWait();
    }
}
