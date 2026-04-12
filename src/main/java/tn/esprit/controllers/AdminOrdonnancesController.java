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
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;
import tn.esprit.entities.Fiche;
import tn.esprit.entities.Medicament;
import tn.esprit.entities.Ordonnance;
import tn.esprit.entities.User;
import tn.esprit.fx.FormFieldSlot;
import tn.esprit.fx.FormValidationStyles;
import tn.esprit.fx.UiTheme;
import tn.esprit.services.ServiceFiche;
import tn.esprit.services.ServiceLigneOrdonnance;
import tn.esprit.services.ServiceMedicament;
import tn.esprit.services.ServiceOrdonnance;
import tn.esprit.services.ServiceUser;
import tn.esprit.tools.ValidationUtil;
import tn.esprit.tools.ValidationUtil.OrdonnanceFormInput;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AdminOrdonnancesController {

    private static final User ALL_PATIENTS = new User(-1, "", "", "Tous les patients", "");

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<User> patientFilter;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Label countLabel;
    @FXML
    private FlowPane cardsFlow;

    private final ServiceOrdonnance service = new ServiceOrdonnance();
    private final ServiceUser userService = new ServiceUser();
    private final ServiceFiche ficheService = new ServiceFiche();
    private final ServiceMedicament medicamentService = new ServiceMedicament();
    private final ServiceLigneOrdonnance ligneService = new ServiceLigneOrdonnance();
    private final ObservableList<Ordonnance> master = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Date (récent → ancien)",
                "Date (ancien → récent)",
                "Plus récentes d’abord",
                "Durée de traitement (croissant)"
        ));
        sortCombo.getSelectionModel().selectFirst();

        patientFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(User u) {
                if (u == null || u.getId() < 0) {
                    return "Tous les patients";
                }
                return UiTheme.userDisplayName(u);
            }

            @Override
            public User fromString(String s) {
                return null;
            }
        });
        reloadPatientFilterItems();
        patientFilter.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((o, a, b) -> refreshView());
        patientFilter.valueProperty().addListener((o, a, b) -> refreshView());
        sortCombo.valueProperty().addListener((o, a, b) -> refreshView());

        reloadMaster();
    }

    private void reloadPatientFilterItems() {
        User sel = patientFilter != null ? patientFilter.getSelectionModel().getSelectedItem() : null;
        ObservableList<User> items = FXCollections.observableArrayList();
        items.add(ALL_PATIENTS);
        items.addAll(userService.listPatientsRolePatient());
        patientFilter.setItems(items);
        if (sel != null && items.stream().anyMatch(u -> u.getId() == sel.getId())) {
            items.stream().filter(u -> u.getId() == sel.getId()).findFirst()
                    .ifPresent(u -> patientFilter.getSelectionModel().select(u));
        } else {
            patientFilter.getSelectionModel().selectFirst();
        }
    }

    private void reloadMaster() {
        master.setAll(service.listAll());
        reloadPatientFilterItems();
        refreshView();
    }

    private void refreshView() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        User pu = patientFilter.getSelectionModel().getSelectedItem();
        int patientId = pu != null && pu.getId() >= 0 ? pu.getId() : -1;

        Comparator<Ordonnance> cmp = comparatorForSort();
        var list = master.stream()
                .filter(o -> matchesSearch(o, q))
                .filter(o -> patientId < 0 || o.getIdUId() == patientId)
                .sorted(cmp)
                .collect(Collectors.toList());

        cardsFlow.getChildren().clear();
        for (Ordonnance o : list) {
            cardsFlow.getChildren().add(buildCard(o));
        }
        countLabel.setText(list.size() + " affichée(s) · " + master.size() + " au total");
    }

    private boolean matchesSearch(Ordonnance o, String q) {
        if (q.isEmpty()) {
            return true;
        }
        String patient = userService.findById(o.getIdUId()).map(UiTheme::userDisplayName).orElse("").toLowerCase(Locale.ROOT);
        String ficheLine = ficheService.findById(o.getIdFicheId()).map(f -> f.getLibelleMaladie() != null ? f.getLibelleMaladie() : "").orElse("").toLowerCase(Locale.ROOT);
        String meds = ligneService.listMedicamentIdsByOrdonnance(o.getId()).stream()
                .map(medicamentService::findById)
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getNomMedicament().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
        return String.valueOf(o.getId()).contains(q)
                || patient.contains(q)
                || ficheLine.contains(q)
                || meds.contains(q)
                || contains(o.getPosologie(), q)
                || contains(o.getFrequence(), q)
                || contains(o.getScanToken(), q)
                || (o.getDateOrdonnance() != null && o.getDateOrdonnance().toString().toLowerCase(Locale.ROOT).contains(q));
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    private Comparator<Ordonnance> comparatorForSort() {
        String s = sortCombo.getSelectionModel().getSelectedItem();
        if (s == null) {
            return Comparator.comparing(Ordonnance::getDateOrdonnance, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return switch (s) {
            case "Date (ancien → récent)" ->
                    Comparator.comparing(Ordonnance::getDateOrdonnance, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Plus récentes d’abord" -> Comparator.comparingInt(Ordonnance::getId).reversed();
            case "Durée de traitement (croissant)" -> Comparator.comparingInt(Ordonnance::getDureeTraitement);
            default -> Comparator.comparing(Ordonnance::getDateOrdonnance, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    private String medicamentsLine(Ordonnance o) {
        List<String> names = ligneService.listMedicamentIdsByOrdonnance(o.getId()).stream()
                .map(medicamentService::findById)
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getNomMedicament())
                .collect(Collectors.toList());
        if (names.isEmpty()) {
            return "Aucun médicament lié";
        }
        return String.join(", ", names);
    }

    private VBox buildCard(Ordonnance o) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        String patient = userService.findById(o.getIdUId()).map(UiTheme::userDisplayName).filter(s -> !s.isBlank()).orElse("Patient");
        String ficheLib = ficheService.findById(o.getIdFicheId()).map(f -> f.getLibelleMaladie() != null ? f.getLibelleMaladie() : "Dossier lié").orElse("Dossier lié");
        String medecin = o.getMedecinUserId() != null
                ? userService.findById(o.getMedecinUserId()).map(UiTheme::userDisplayName).filter(s -> !s.isBlank()).orElse("—")
                : "—";

        Label badge = new Label("Prescription");
        badge.getStyleClass().add("card-badge");

        Label title = new Label(nullSafe(o.getPosologie()));
        title.getStyleClass().add("card-title");

        Label who = new Label("Pour · " + patient);
        who.getStyleClass().add("card-sub");
        who.setWrapText(true);

        Label dossier = new Label("Dossier clinique · " + ficheLib);
        dossier.getStyleClass().add("card-meta");
        dossier.setWrapText(true);

        Label doc = new Label("Prescripteur · " + medecin);
        doc.getStyleClass().add("card-meta");
        doc.setWrapText(true);

        Label meds = new Label("Médicaments · " + truncate(medicamentsLine(o), 180));
        meds.getStyleClass().add("card-meta");
        meds.setWrapText(true);

        Label dt = new Label("Émise le " + (o.getDateOrdonnance() != null ? o.getDateOrdonnance().toString() : "—"));
        dt.getStyleClass().add("card-sub");

        Label fr = new Label("Fréquence · " + nullSafe(o.getFrequence()) + " · Durée " + o.getDureeTraitement() + " jour(s)");
        fr.setWrapText(true);
        fr.getStyleClass().add("card-sub");

        card.getChildren().addAll(badge, title, who, dossier, doc, meds, dt, fr);
        if (o.getScanToken() != null && !o.getScanToken().isBlank()) {
            Label ref = new Label("Référence · " + truncate(o.getScanToken(), 72));
            ref.setWrapText(true);
            ref.getStyleClass().add("card-sub");
            card.getChildren().add(ref);
        }

        HBox actions = new HBox(10);
        actions.getStyleClass().add("card-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        Button edit = new Button("Modifier");
        edit.getStyleClass().add("ghost-button");
        edit.setOnAction(e -> showEditorDialog(o));
        Button del = new Button("Supprimer");
        del.getStyleClass().add("danger-button");
        del.setOnAction(e -> confirmDelete(o));
        actions.getChildren().addAll(edit, del);
        card.getChildren().add(actions);
        return card;
    }

    private static String nullSafe(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "—";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    @FXML
    private void onAdd() {
        showEditorDialog(null);
    }

    private void confirmDelete(Ordonnance o) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer définitivement cette ordonnance ?");
        UiTheme.applyDialog(a);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            service.supprimer(o);
            reloadMaster();
        }
    }

    private static ObservableList<String> hourItems() {
        return FXCollections.observableArrayList(
                IntStream.range(0, 24).mapToObj(i -> String.format("%02d", i)).collect(Collectors.toList()));
    }

    private static ObservableList<String> minuteItems() {
        return FXCollections.observableArrayList("00", "15", "30", "45");
    }

    private void showEditorDialog(Ordonnance existing) {
        Window owner = cardsFlow.getScene().getWindow();
        boolean isEdit = existing != null;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        UiTheme.applyDialog(dialog);
        dialog.setTitle(isEdit ? "Modifier une ordonnance" : "Nouvelle ordonnance");
        dialog.setHeaderText(isEdit
                ? "Mettre à jour la prescription"
                : "Patient, prescripteur, dossier et médicaments");

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<User> patientCombo = new ComboBox<>(FXCollections.observableArrayList(userService.listPatientsRolePatient()));
        bindUserDisplay(patientCombo);
        ComboBox<User> medecinCombo = new ComboBox<>(FXCollections.observableArrayList(userService.listMedecins()));
        bindUserDisplay(medecinCombo);
        ComboBox<Fiche> ficheCombo = new ComboBox<>();
        ficheCombo.setCellFactory(lv -> new FicheListCell());
        ficheCombo.setButtonCell(new FicheListCell());

        ListView<Medicament> medsList = new ListView<>(FXCollections.observableArrayList(medicamentService.listAll()));
        medsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        medsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Medicament m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setText(null);
                } else {
                    setText(m.getNomMedicament() + (m.getDosage() != null && !m.getDosage().isBlank() ? " · " + m.getDosage() : ""));
                }
            }
        });
        medsList.setPrefHeight(160);

        Runnable refreshFiches = () -> {
            User u = patientCombo.getSelectionModel().getSelectedItem();
            ficheCombo.getItems().clear();
            if (u == null) {
                return;
            }
            List<Fiche> fiches = ficheService.listAll().stream()
                    .filter(f -> f.getIdUId() == u.getId())
                    .collect(Collectors.toList());
            ficheCombo.getItems().setAll(fiches);
            if (!fiches.isEmpty()) {
                ficheCombo.getSelectionModel().selectFirst();
            }
        };
        patientCombo.getSelectionModel().selectedItemProperty().addListener((o, a, u) -> refreshFiches.run());

        TextField fPosologie = new TextField();
        TextField fFrequence = new TextField();
        TextField fDuree = new TextField();
        DatePicker fDate = new DatePicker();
        ComboBox<String> hourCombo = new ComboBox<>(hourItems());
        ComboBox<String> minCombo = new ComboBox<>(minuteItems());
        hourCombo.getSelectionModel().select("09");
        minCombo.getSelectionModel().selectFirst();
        TextField fToken = new TextField();
        fToken.setPromptText("Référence optionnelle");

        if (isEdit) {
            patientCombo.getItems().stream().filter(u -> u.getId() == existing.getIdUId()).findFirst()
                    .ifPresent(u -> patientCombo.getSelectionModel().select(u));
            refreshFiches.run();
            ficheCombo.getItems().stream().filter(f -> f.getId() == existing.getIdFicheId()).findFirst()
                    .ifPresent(f -> ficheCombo.getSelectionModel().select(f));
            if (existing.getMedecinUserId() != null) {
                medecinCombo.getItems().stream().filter(u -> u.getId() == existing.getMedecinUserId()).findFirst()
                        .ifPresent(u -> medecinCombo.getSelectionModel().select(u));
            }
            fPosologie.setText(existing.getPosologie());
            fFrequence.setText(existing.getFrequence());
            fDuree.setText(String.valueOf(existing.getDureeTraitement()));
            if (existing.getDateOrdonnance() != null) {
                LocalDateTime dt = existing.getDateOrdonnance();
                fDate.setValue(dt.toLocalDate());
                hourCombo.getSelectionModel().select(String.format("%02d", dt.getHour()));
                int m = dt.getMinute();
                String mm = String.format("%02d", m);
                if (!minCombo.getItems().contains(mm)) {
                    minCombo.getItems().add(mm);
                }
                minCombo.getSelectionModel().select(mm);
            }
            fToken.setText(existing.getScanToken() != null ? existing.getScanToken() : "");
            List<Integer> mids = ligneService.listMedicamentIdsByOrdonnance(existing.getId());
            for (Medicament m : medsList.getItems()) {
                if (mids.contains(m.getId())) {
                    medsList.getSelectionModel().select(m);
                }
            }
        } else if (!patientCombo.getItems().isEmpty()) {
            patientCombo.getSelectionModel().selectFirst();
            refreshFiches.run();
        }

        HBox timeRow = new HBox(8, hourCombo, new Label("h"), minCombo, new Label("min"));
        timeRow.setAlignment(Pos.CENTER_LEFT);

        FormFieldSlot slotPatient = new FormFieldSlot(patientCombo);
        FormFieldSlot slotMedecin = new FormFieldSlot(medecinCombo);
        FormFieldSlot slotFiche = new FormFieldSlot(ficheCombo);
        FormFieldSlot slotMeds = new FormFieldSlot(medsList);
        FormFieldSlot slotPosologie = new FormFieldSlot(fPosologie);
        FormFieldSlot slotFrequence = new FormFieldSlot(fFrequence);
        FormFieldSlot slotDuree = new FormFieldSlot(fDuree);
        FormFieldSlot slotDate = new FormFieldSlot(fDate);
        FormFieldSlot slotHeure = new FormFieldSlot(timeRow, false);
        FormFieldSlot slotToken = new FormFieldSlot(fToken);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(4, 0, 8, 0));
        int r = 0;
        grid.add(new Label("Patient *"), 0, r);
        grid.add(slotPatient.getRoot(), 1, r++);
        grid.add(new Label("Médecin prescripteur *"), 0, r);
        grid.add(slotMedecin.getRoot(), 1, r++);
        grid.add(new Label("Dossier (fiche) *"), 0, r);
        grid.add(slotFiche.getRoot(), 1, r++);
        grid.add(new Label("Médicaments *"), 0, r);
        grid.add(slotMeds.getRoot(), 1, r++);
        grid.add(new Label("Posologie *"), 0, r);
        grid.add(slotPosologie.getRoot(), 1, r++);
        grid.add(new Label("Fréquence *"), 0, r);
        grid.add(slotFrequence.getRoot(), 1, r++);
        grid.add(new Label("Durée (jours) *"), 0, r);
        grid.add(slotDuree.getRoot(), 1, r++);
        grid.add(new Label("Date *"), 0, r);
        grid.add(slotDate.getRoot(), 1, r++);
        grid.add(new Label("Heure *"), 0, r);
        grid.add(slotHeure.getRoot(), 1, r++);
        grid.add(new Label("Référence / jeton"), 0, r);
        grid.add(slotToken.getRoot(), 1, r);
        GridPane.setHgrow(patientCombo, Priority.ALWAYS);
        GridPane.setHgrow(medecinCombo, Priority.ALWAYS);
        GridPane.setHgrow(ficheCombo, Priority.ALWAYS);
        GridPane.setHgrow(medsList, Priority.ALWAYS);
        UiTheme.tagFormLabels(grid);

        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(560);

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
            slotFiche.clear();
            slotMeds.clear();
            slotPosologie.clear();
            slotFrequence.clear();
            slotDuree.clear();
            slotDate.clear();
            slotHeure.clear();
            slotToken.clear();
            FormValidationStyles.clearInvalid(hourCombo);
            FormValidationStyles.clearInvalid(minCombo);
        };

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            clearValidation.run();
            User patient = patientCombo.getSelectionModel().getSelectedItem();
            User medecin = medecinCombo.getSelectionModel().getSelectedItem();
            Fiche fiche = ficheCombo.getSelectionModel().getSelectedItem();
            ObservableList<Medicament> sel = medsList.getSelectionModel().getSelectedItems();
            int medCount = sel != null ? sel.size() : 0;
            String h = hourCombo.getSelectionModel().getSelectedItem();
            String mi = minCombo.getSelectionModel().getSelectedItem();

            OrdonnanceFormInput input = new OrdonnanceFormInput(
                    patient != null ? patient.getId() : null,
                    medecin != null ? medecin.getId() : null,
                    fiche != null ? fiche.getId() : null,
                    medCount,
                    fPosologie.getText(),
                    fFrequence.getText(),
                    fDuree.getText(),
                    fDate.getValue(),
                    h != null ? h : "",
                    mi != null ? mi : "",
                    fToken.getText()
            );
            LinkedHashMap<String, String> err = ValidationUtil.validateOrdonnanceDetailed(input);
            if (patient != null && fiche != null && fiche.getIdUId() != patient.getId()) {
                err.put("fiche", "Ce dossier ne correspond pas au patient sélectionné.");
            }
            if (!err.isEmpty()) {
                ev.consume();
                for (Map.Entry<String, String> en : err.entrySet()) {
                    switch (en.getKey()) {
                        case "patient" -> slotPatient.setError(en.getValue());
                        case "medecin" -> slotMedecin.setError(en.getValue());
                        case "fiche" -> slotFiche.setError(en.getValue());
                        case "medicaments" -> slotMeds.setError(en.getValue());
                        case "posologie" -> slotPosologie.setError(en.getValue());
                        case "frequence" -> slotFrequence.setError(en.getValue());
                        case "duree" -> slotDuree.setError(en.getValue());
                        case "date" -> slotDate.setError(en.getValue());
                        case "heure" -> {
                            slotHeure.setError(en.getValue());
                            FormValidationStyles.markInvalid(hourCombo);
                            FormValidationStyles.markInvalid(minCombo);
                        }
                        case "token" -> slotToken.setError(en.getValue());
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

            int duree = Integer.parseInt(fDuree.getText().trim());
            String pos = fPosologie.getText() != null ? fPosologie.getText().trim() : "";
            String freq = fFrequence.getText() != null ? fFrequence.getText().trim() : "";
            LocalTime lt = LocalTime.of(Integer.parseInt(h), Integer.parseInt(mi));
            LocalDateTime dt = LocalDateTime.of(fDate.getValue(), lt);

            Ordonnance ord = new Ordonnance();
            ord.setPosologie(pos);
            ord.setFrequence(freq);
            ord.setDureeTraitement(duree);
            ord.setDateOrdonnance(dt);
            String tok = fToken.getText() != null ? fToken.getText().trim() : "";
            ord.setScanToken(tok.isEmpty() ? null : tok);
            ord.setIdUId(patient.getId());
            ord.setIdRdvId(null);
            ord.setIdFicheId(fiche.getId());
            ord.setMedecinUserId(medecin.getId());

            if (isEdit) {
                ord.setId(existing.getId());
                service.modifier(ord);
                ligneService.deleteByOrdonnanceId(existing.getId());
                for (Medicament m : sel) {
                    ligneService.insertLigne(existing.getId(), m.getId());
                }
            } else {
                int newId = service.ajouterRetourId(ord);
                if (newId <= 0) {
                    ev.consume();
                    validationSummary.setText("Enregistrement impossible (vérifiez la base et le script SQL).");
                    validationSummary.setVisible(true);
                    validationSummary.setManaged(true);
                    FormValidationStyles.markSection(formShell, true);
                    return;
                }
                for (Medicament m : sel) {
                    ligneService.insertLigne(newId, m.getId());
                }
            }
        });

        dialog.showAndWait();
        reloadMaster();
    }

    private void bindUserDisplay(ComboBox<User> combo) {
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : UiTheme.userDisplayName(u));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : UiTheme.userDisplayName(u));
            }
        });
    }

    private final class FicheListCell extends ListCell<Fiche> {
        @Override
        protected void updateItem(Fiche f, boolean empty) {
            super.updateItem(f, empty);
            if (empty || f == null) {
                setText(null);
            } else {
                String p = userService.findById(f.getIdUId()).map(UiTheme::userDisplayName).orElse("Patient");
                setText(f.getLibelleMaladie() + " · " + p);
            }
        }
    }

    private static void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        UiTheme.applyDialog(a);
        a.showAndWait();
    }
}
