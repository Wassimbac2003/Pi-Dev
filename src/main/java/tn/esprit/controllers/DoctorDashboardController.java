package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import tn.esprit.entities.Fiche;
import tn.esprit.entities.Medicament;
import tn.esprit.entities.Ordonnance;
import tn.esprit.entities.User;
import tn.esprit.fx.FormFieldSlot;
import tn.esprit.fx.FormValidationStyles;
import tn.esprit.fx.Session;
import tn.esprit.fx.UiTheme;
import tn.esprit.services.ServiceFiche;
import tn.esprit.services.ServiceLigneOrdonnance;
import tn.esprit.services.ServiceMedicament;
import tn.esprit.services.ServiceOrdonnance;
import tn.esprit.services.ServiceUser;
import tn.esprit.tools.ValidationUtil;
import tn.esprit.tools.ValidationUtil.OrdonnanceFormInput;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DoctorDashboardController {

    private static final DateTimeFormatter DT_CARD = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE);

    @FXML
    private StackPane doctorMainStack;
    @FXML
    private ScrollPane scrollMesFiches;
    @FXML
    private ScrollPane scrollMesOrdos;
    @FXML
    private ToggleButton navFichesBtn;
    @FXML
    private ToggleButton navOrdosBtn;
    @FXML
    private TextField searchMyFiches;
    @FXML
    private TextField searchMyOrdos;
    @FXML
    private Label countMyFiches;
    @FXML
    private Label countMyOrdos;
    @FXML
    private FlowPane myFichesFlow;
    @FXML
    private FlowPane myOrdosFlow;

    private final ServiceUser userService = new ServiceUser();
    private final ServiceFiche ficheService = new ServiceFiche();
    private final ServiceOrdonnance ordonnanceService = new ServiceOrdonnance();
    private final ServiceMedicament medicamentService = new ServiceMedicament();
    private final ServiceLigneOrdonnance ligneService = new ServiceLigneOrdonnance();

    private ToggleGroup doctorNavGroup;
    private final List<Fiche> myFichesMaster = new ArrayList<>();
    private final List<Ordonnance> myOrdosMaster = new ArrayList<>();

    @FXML
    private void initialize() {
        doctorNavGroup = new ToggleGroup();
        navFichesBtn.setToggleGroup(doctorNavGroup);
        navOrdosBtn.setToggleGroup(doctorNavGroup);
        navFichesBtn.setSelected(true);
        navFichesBtn.setOnAction(e -> showDoctorMesFiches());
        navOrdosBtn.setOnAction(e -> showDoctorMesOrdos());
        searchMyFiches.textProperty().addListener((o, a, b) -> refreshMyFichesCards());
        searchMyOrdos.textProperty().addListener((o, a, b) -> refreshMyOrdosCards());
        reloadDoctorMasters();
        refreshMyFichesCards();
        refreshMyOrdosCards();
        showDoctorMesFiches();
    }

    @FXML
    private void onOpenSaisieDialog() {
        Window w = doctorMainStack.getScene().getWindow();
        DoctorSaisieDialogController.show(w, null, this::afterSaisieDataChanged);
    }

    private void afterSaisieDataChanged() {
        reloadDoctorMasters();
        refreshMyFichesCards();
        refreshMyOrdosCards();
    }

    private void showDoctorMesFiches() {
        navFichesBtn.setSelected(true);
        scrollMesFiches.setVisible(true);
        scrollMesFiches.setManaged(true);
        scrollMesOrdos.setVisible(false);
        scrollMesOrdos.setManaged(false);
        refreshMyFichesCards();
    }

    private void showDoctorMesOrdos() {
        navOrdosBtn.setSelected(true);
        scrollMesFiches.setVisible(false);
        scrollMesFiches.setManaged(false);
        scrollMesOrdos.setVisible(true);
        scrollMesOrdos.setManaged(true);
        refreshMyOrdosCards();
    }

    private void reloadDoctorMasters() {
        int mid = Session.getCurrentUserId();
        myFichesMaster.clear();
        myOrdosMaster.clear();
        if (mid > 0) {
            myFichesMaster.addAll(ficheService.listByMedecinUserId(mid));
            myOrdosMaster.addAll(ordonnanceService.listByMedecinUserId(mid));
        }
    }

    private void refreshMyFichesCards() {
        if (myFichesFlow == null) {
            return;
        }
        String q = searchMyFiches.getText() == null ? "" : searchMyFiches.getText().trim().toLowerCase(Locale.ROOT);
        myFichesFlow.getChildren().clear();
        var list = myFichesMaster.stream().filter(f -> matchesMyFicheSearch(f, q)).toList();
        for (Fiche f : list) {
            myFichesFlow.getChildren().add(buildMyFicheCard(f));
        }
        countMyFiches.setText(list.size() + " fiche(s)");
    }

    private boolean matchesMyFicheSearch(Fiche f, String q) {
        if (q.isEmpty()) {
            return true;
        }
        String patient = userService.findById(f.getIdUId()).map(UiTheme::userDisplayName).orElse("").toLowerCase(Locale.ROOT);
        return patient.contains(q)
                || contains(f.getLibelleMaladie(), q)
                || contains(f.getGrpSanguin(), q)
                || contains(f.getGravite(), q)
                || contains(f.getTension(), q)
                || contains(f.getAllergie(), q)
                || contains(f.getMaladieChronique(), q)
                || contains(f.getSymptomes(), q)
                || (f.getDate() != null && f.getDate().toString().toLowerCase(Locale.ROOT).contains(q));
    }

    private void refreshMyOrdosCards() {
        if (myOrdosFlow == null) {
            return;
        }
        String q = searchMyOrdos.getText() == null ? "" : searchMyOrdos.getText().trim().toLowerCase(Locale.ROOT);
        myOrdosFlow.getChildren().clear();
        var list = myOrdosMaster.stream().filter(o -> matchesMyOrdoSearch(o, q)).toList();
        for (Ordonnance o : list) {
            myOrdosFlow.getChildren().add(buildMyOrdoCard(o));
        }
        countMyOrdos.setText(list.size() + " ordonnance(s)");
    }

    private boolean matchesMyOrdoSearch(Ordonnance o, String q) {
        if (q.isEmpty()) {
            return true;
        }
        String patient = userService.findById(o.getIdUId()).map(UiTheme::userDisplayName).orElse("").toLowerCase(Locale.ROOT);
        String meds = ligneService.listMedicamentIdsByOrdonnance(o.getId()).stream()
                .map(medicamentService::findById)
                .filter(Optional::isPresent)
                .map(x -> x.get().getNomMedicament().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
        String ficheTxt = ficheService.findById(o.getIdFicheId())
                .map(f -> f.getLibelleMaladie() != null ? f.getLibelleMaladie() : "").orElse("").toLowerCase(Locale.ROOT);
        String dt = o.getDateOrdonnance() != null ? o.getDateOrdonnance().toString().toLowerCase(Locale.ROOT) : "";
        return patient.contains(q)
                || contains(o.getPosologie(), q)
                || contains(o.getFrequence(), q)
                || meds.contains(q)
                || ficheTxt.contains(q)
                || dt.contains(q);
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    private VBox buildMyFicheCard(Fiche f) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        String patient = userService.findById(f.getIdUId()).map(UiTheme::userDisplayName).filter(s -> !s.isBlank()).orElse("Patient");
        Label badge = new Label(f.getGravite() != null && !f.getGravite().isBlank() ? f.getGravite() : "Fiche");
        badge.getStyleClass().add("card-badge");
        Label title = new Label(patient);
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        Label sub = new Label((f.getLibelleMaladie() != null ? f.getLibelleMaladie() : "—")
                + "\nConsultation du " + (f.getDate() != null ? f.getDate().toString() : "—"));
        sub.getStyleClass().add("card-meta");
        sub.setWrapText(true);
        Label line = new Label("Groupe " + (f.getGrpSanguin() != null ? f.getGrpSanguin() : "—")
                + " · Tension " + (f.getTension() != null ? f.getTension() : "—"));
        line.getStyleClass().add("card-sub");
        line.setWrapText(true);
        Button edit = new Button("Modifier");
        edit.getStyleClass().add("ghost-button");
        edit.setOnAction(e -> openFicheFromListForEdit(f));
        Button del = new Button("Supprimer");
        del.getStyleClass().add("danger-button");
        del.setOnAction(e -> confirmDeleteFiche(f));
        HBox actions = new HBox(10, edit, del);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("card-actions");
        card.getChildren().addAll(badge, title, sub, line, actions);
        return card;
    }

    private VBox buildMyOrdoCard(Ordonnance o) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        String patient = userService.findById(o.getIdUId()).map(UiTheme::userDisplayName).filter(s -> !s.isBlank()).orElse("Patient");
        Label badge = new Label("Ordonnance");
        badge.getStyleClass().add("card-badge");
        String dateStr = o.getDateOrdonnance() != null ? DT_CARD.format(o.getDateOrdonnance()) : "—";
        Label title = new Label("Pour " + patient);
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        Label sub = new Label(dateStr);
        sub.getStyleClass().add("card-sub");
        Label pos = new Label(o.getPosologie());
        pos.setWrapText(true);
        pos.getStyleClass().add("card-meta");
        String medNames = ligneService.listMedicamentIdsByOrdonnance(o.getId()).stream()
                .map(medicamentService::findById)
                .filter(Optional::isPresent)
                .map(x -> x.get().getNomMedicament())
                .collect(Collectors.joining(", "));
        Label meds = new Label(medNames.isBlank() ? "—" : medNames);
        meds.setWrapText(true);
        meds.getStyleClass().add("card-sub");
        Button edit = new Button("Modifier");
        edit.getStyleClass().add("ghost-button");
        edit.setOnAction(e -> showEditOrdonnanceDialog(o));
        Button del = new Button("Supprimer");
        del.getStyleClass().add("danger-button");
        del.setOnAction(e -> confirmDeleteOrdonnance(o));
        HBox actions = new HBox(10, edit, del);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("card-actions");
        card.getChildren().addAll(badge, title, sub, pos, meds, actions);
        return card;
    }

    private void openFicheFromListForEdit(Fiche f) {
        showDoctorMesFiches();
        Window w = doctorMainStack.getScene().getWindow();
        DoctorSaisieDialogController.show(w, f, this::afterSaisieDataChanged);
    }

    private void confirmDeleteFiche(Fiche f) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette fiche ? Les ordonnances liées peuvent empêcher la suppression.", ButtonType.OK, ButtonType.CANCEL);
        UiTheme.applyDialog(a);
        a.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            ficheService.supprimer(f);
            reloadDoctorMasters();
            refreshMyFichesCards();
            refreshMyOrdosCards();
        });
    }

    private void confirmDeleteOrdonnance(Ordonnance o) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer définitivement cette ordonnance ?", ButtonType.OK, ButtonType.CANCEL);
        UiTheme.applyDialog(a);
        a.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            ordonnanceService.supprimer(o);
            reloadDoctorMasters();
            refreshMyOrdosCards();
        });
    }

    private void showEditOrdonnanceDialog(Ordonnance existing) {
        Window owner = doctorMainStack.getScene().getWindow();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        UiTheme.applyDialog(dialog);
        dialog.setTitle("Modifier l’ordonnance");
        dialog.setHeaderText("Mettre à jour la prescription");

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        User patient = userService.findById(existing.getIdUId()).orElse(null);
        Label labPatient = new Label(patient != null ? UiTheme.userDisplayName(patient) : "Patient");
        labPatient.getStyleClass().add("card-title");

        ComboBox<Fiche> ficheComboDlg = new ComboBox<>();
        ficheComboDlg.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Fiche f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    String p = userService.findById(f.getIdUId()).map(UiTheme::userDisplayName).orElse("Patient");
                    setText((f.getLibelleMaladie() != null ? f.getLibelleMaladie() : "Fiche") + " · " + p);
                }
            }
        });
        ficheComboDlg.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Fiche f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    String p = userService.findById(f.getIdUId()).map(UiTheme::userDisplayName).orElse("Patient");
                    setText((f.getLibelleMaladie() != null ? f.getLibelleMaladie() : "Fiche") + " · " + p);
                }
            }
        });
        if (patient != null) {
            List<Fiche> fiches = ficheService.listAll().stream().filter(fi -> fi.getIdUId() == patient.getId()).collect(Collectors.toList());
            ficheComboDlg.setItems(FXCollections.observableArrayList(fiches));
            fiches.stream().filter(fi -> fi.getId() == existing.getIdFicheId()).findFirst()
                    .ifPresent(f -> ficheComboDlg.getSelectionModel().select(f));
        }

        ListView<Medicament> medsDlg = new ListView<>(FXCollections.observableArrayList(medicamentService.listAll()));
        medsDlg.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        medsDlg.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Medicament m, boolean empty) {
                super.updateItem(m, empty);
                setText(empty || m == null ? null : m.getNomMedicament());
            }
        });
        medsDlg.setPrefHeight(140);
        for (Medicament m : medsDlg.getItems()) {
            if (ligneService.listMedicamentIdsByOrdonnance(existing.getId()).contains(m.getId())) {
                medsDlg.getSelectionModel().select(m);
            }
        }

        TextField fPos = new TextField(existing.getPosologie());
        TextField fFreq = new TextField(existing.getFrequence());
        TextField fDur = new TextField(String.valueOf(existing.getDureeTraitement()));
        DatePicker fDt = new DatePicker();
        ComboBox<String> hCb = new ComboBox<>(FXCollections.observableArrayList(
                IntStream.range(0, 24).mapToObj(i -> String.format("%02d", i)).collect(Collectors.toList())));
        ComboBox<String> mCb = new ComboBox<>(FXCollections.observableArrayList("00", "15", "30", "45"));
        if (existing.getDateOrdonnance() != null) {
            LocalDateTime dtx = existing.getDateOrdonnance();
            fDt.setValue(dtx.toLocalDate());
            hCb.getSelectionModel().select(String.format("%02d", dtx.getHour()));
            String mm = String.format("%02d", dtx.getMinute());
            if (!mCb.getItems().contains(mm)) {
                mCb.getItems().add(mm);
            }
            mCb.getSelectionModel().select(mm);
        }

        FormFieldSlot slotFiche = new FormFieldSlot(ficheComboDlg);
        FormFieldSlot slotMeds = new FormFieldSlot(medsDlg);
        FormFieldSlot slotPos = new FormFieldSlot(fPos);
        FormFieldSlot slotFreq = new FormFieldSlot(fFreq);
        FormFieldSlot slotDur = new FormFieldSlot(fDur);
        FormFieldSlot slotDate = new FormFieldSlot(fDt);
        HBox timeRow = new HBox(8, hCb, new Label("h"), mCb, new Label("min"));
        timeRow.setAlignment(Pos.CENTER_LEFT);
        FormFieldSlot slotTime = new FormFieldSlot(timeRow, false);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(4, 0, 8, 0));
        int r = 0;
        grid.add(new Label("Patient"), 0, r);
        grid.add(labPatient, 1, r++);
        grid.add(new Label("Fiche *"), 0, r);
        grid.add(slotFiche.getRoot(), 1, r++);
        grid.add(new Label("Médicaments *"), 0, r);
        grid.add(slotMeds.getRoot(), 1, r++);
        grid.add(new Label("Posologie *"), 0, r);
        grid.add(slotPos.getRoot(), 1, r++);
        grid.add(new Label("Fréquence *"), 0, r);
        grid.add(slotFreq.getRoot(), 1, r++);
        grid.add(new Label("Durée (j) *"), 0, r);
        grid.add(slotDur.getRoot(), 1, r++);
        grid.add(new Label("Date *"), 0, r);
        grid.add(slotDate.getRoot(), 1, r++);
        grid.add(new Label("Heure *"), 0, r);
        grid.add(slotTime.getRoot(), 1, r);
        GridPane.setHgrow(ficheComboDlg, Priority.ALWAYS);
        UiTheme.tagFormLabels(grid);

        Label summary = new Label();
        summary.getStyleClass().add("form-validation-summary");
        summary.setWrapText(true);
        summary.setMaxWidth(520);
        summary.setVisible(false);
        summary.setManaged(false);
        ScrollPane spGrid = new ScrollPane(grid);
        spGrid.setFitToWidth(true);
        spGrid.setPrefViewportHeight(420);
        VBox shell = new VBox(10, summary, spGrid);
        dialog.getDialogPane().setContent(shell);

        Runnable clearVal = () -> {
            summary.setVisible(false);
            summary.setManaged(false);
            summary.setText("");
            slotFiche.clear();
            slotMeds.clear();
            slotPos.clear();
            slotFreq.clear();
            slotDur.clear();
            slotDate.clear();
            slotTime.clear();
            FormValidationStyles.clearInvalid(hCb);
            FormValidationStyles.clearInvalid(mCb);
        };

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            clearVal.run();
            Fiche fic = ficheComboDlg.getSelectionModel().getSelectedItem();
            var sel = medsDlg.getSelectionModel().getSelectedItems();
            String hh = hCb.getSelectionModel().getSelectedItem();
            String mi = mCb.getSelectionModel().getSelectedItem();
            OrdonnanceFormInput in = new OrdonnanceFormInput(
                    patient != null ? patient.getId() : null,
                    Session.getCurrentUserId() > 0 ? Session.getCurrentUserId() : null,
                    fic != null ? fic.getId() : null,
                    sel != null ? sel.size() : 0,
                    fPos.getText(),
                    fFreq.getText(),
                    fDur.getText(),
                    fDt.getValue(),
                    hh != null ? hh : "",
                    mi != null ? mi : "",
                    ""
            );
            LinkedHashMap<String, String> err = ValidationUtil.validateOrdonnanceDetailed(in);
            if (!err.isEmpty()) {
                ev.consume();
                for (Map.Entry<String, String> en : err.entrySet()) {
                    switch (en.getKey()) {
                        case "fiche" -> slotFiche.setError(en.getValue());
                        case "medicaments" -> slotMeds.setError(en.getValue());
                        case "posologie" -> slotPos.setError(en.getValue());
                        case "frequence" -> slotFreq.setError(en.getValue());
                        case "duree" -> slotDur.setError(en.getValue());
                        case "date" -> slotDate.setError(en.getValue());
                        case "heure" -> {
                            slotTime.setError(en.getValue());
                            FormValidationStyles.markInvalid(hCb);
                            FormValidationStyles.markInvalid(mCb);
                        }
                        default -> {
                        }
                    }
                }
                summary.setText("Corrigez les champs signalés.");
                summary.setVisible(true);
                summary.setManaged(true);
                return;
            }
            Ordonnance ord = new Ordonnance();
            ord.setId(existing.getId());
            ord.setPosologie(fPos.getText() != null ? fPos.getText().trim() : "");
            ord.setFrequence(fFreq.getText() != null ? fFreq.getText().trim() : "");
            ord.setDureeTraitement(Integer.parseInt(fDur.getText().trim()));
            ord.setDateOrdonnance(LocalDateTime.of(fDt.getValue(), LocalTime.of(Integer.parseInt(hh), Integer.parseInt(mi))));
            ord.setScanToken(existing.getScanToken());
            ord.setIdUId(existing.getIdUId());
            ord.setIdRdvId(null);
            ord.setIdFicheId(fic.getId());
            ord.setMedecinUserId(Session.getCurrentUserId() > 0 ? Session.getCurrentUserId() : null);
            ordonnanceService.modifier(ord);
            ligneService.deleteByOrdonnanceId(existing.getId());
            for (Medicament m : sel) {
                ligneService.insertLigne(existing.getId(), m.getId());
            }
        });

        dialog.showAndWait();
        reloadDoctorMasters();
        refreshMyOrdosCards();
    }

    @FXML
    private void onLogout() throws Exception {
        Session.clear();
        Stage stage = (Stage) doctorMainStack.getScene().getWindow();
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/tn/esprit/fxml/login.fxml")));
        Scene scene = new Scene(root, 1080, 700);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/tn/esprit/styles/app.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Clinique — Connexion");
    }
}
