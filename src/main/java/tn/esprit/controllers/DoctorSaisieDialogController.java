package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import tn.esprit.entities.Fiche;
import tn.esprit.entities.Medicament;
import tn.esprit.entities.Ordonnance;
import tn.esprit.entities.User;
import tn.esprit.fx.FormValidationStyles;
import tn.esprit.fx.Session;
import tn.esprit.fx.UiTheme;
import tn.esprit.services.ServiceFiche;
import tn.esprit.services.ServiceLigneOrdonnance;
import tn.esprit.services.ServiceMedicament;
import tn.esprit.services.ServiceOrdonnance;
import tn.esprit.services.ServiceUser;
import tn.esprit.tools.ValidationUtil;
import tn.esprit.tools.ValidationUtil.FicheFormInput;
import tn.esprit.tools.ValidationUtil.OrdonnanceFormInput;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Modal formulaire médecin : patient, fiche, ordonnance.
 */
public class DoctorSaisieDialogController {

    @FXML
    private ComboBox<User> patientCombo;
    @FXML
    private TextField fPoids;
    @FXML
    private TextField fTaille;
    @FXML
    private ComboBox<String> fGrpCombo;
    @FXML
    private TextField fAllergie;
    @FXML
    private TextField fChronique;
    @FXML
    private TextField fTension;
    @FXML
    private TextField fGlycemie;
    @FXML
    private DatePicker fDate;
    @FXML
    private TextField fLibelle;
    @FXML
    private ComboBox<String> fGraviteCombo;
    @FXML
    private TextArea fReco;
    @FXML
    private TextArea fSymptomes;
    @FXML
    private ComboBox<Fiche> ficheCombo;
    @FXML
    private ListView<Medicament> medsListView;
    @FXML
    private TextField oPosologie;
    @FXML
    private TextField oFrequence;
    @FXML
    private TextField oDuree;
    @FXML
    private DatePicker oDate;
    @FXML
    private ComboBox<String> oHourCombo;
    @FXML
    private ComboBox<String> oMinCombo;
    @FXML
    private VBox ficheFormSection;
    @FXML
    private Label ficheValidationBanner;
    @FXML
    private VBox ordoFormSection;
    @FXML
    private Label ordoValidationBanner;

    private final ServiceUser userService = new ServiceUser();
    private final ServiceFiche ficheService = new ServiceFiche();
    private final ServiceOrdonnance ordonnanceService = new ServiceOrdonnance();
    private final ServiceMedicament medicamentService = new ServiceMedicament();
    private final ServiceLigneOrdonnance ligneService = new ServiceLigneOrdonnance();

    private Runnable afterSave;

    public void setAfterSave(Runnable afterSave) {
        this.afterSave = afterSave;
    }

    @FXML
    private void initialize() {
        patientCombo.setItems(FXCollections.observableArrayList(userService.listPatientsRolePatient()));
        patientCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : UiTheme.userDisplayName(u));
            }
        });
        patientCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : UiTheme.userDisplayName(u));
            }
        });

        fGrpCombo.setItems(FXCollections.observableArrayList(ValidationUtil.GROUPES_SANGUINS));
        fGraviteCombo.setItems(FXCollections.observableArrayList(ValidationUtil.GRAVITES));

        ficheCombo.setCellFactory(lv -> new ListCell<>() {
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
        ficheCombo.setButtonCell(new ListCell<>() {
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

        medsListView.setItems(FXCollections.observableArrayList(medicamentService.listAll()));
        medsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        medsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Medicament m, boolean empty) {
                super.updateItem(m, empty);
                setText(empty || m == null ? null : m.getNomMedicament());
            }
        });

        oHourCombo.setItems(FXCollections.observableArrayList(
                IntStream.range(0, 24).mapToObj(i -> String.format("%02d", i)).collect(Collectors.toList())));
        oMinCombo.setItems(FXCollections.observableArrayList("00", "15", "30", "45"));
        oHourCombo.getSelectionModel().select("09");
        oMinCombo.getSelectionModel().selectFirst();

        patientCombo.getSelectionModel().selectedItemProperty().addListener((o, a, u) -> onPatientChanged(u));
    }

    /**
     * Affiche le dialogue. {@code prefillFiche} non nul : ouverture depuis « Modifier » sur une fiche listée.
     */
    public static void show(Window owner, Fiche prefillFiche, Runnable afterSave) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    DoctorSaisieDialogController.class.getResource("/tn/esprit/fxml/doctor_saisie_dialog.fxml")));
            Parent root = loader.load();
            DoctorSaisieDialogController c = loader.getController();
            c.setAfterSave(afterSave != null ? afterSave : () -> {
            });
            if (prefillFiche != null) {
                c.applyPrefillFromFiche(prefillFiche);
            } else {
                c.resetToBlank();
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initOwner(owner);
            dialog.setTitle("Nouvelle saisie");
            dialog.setHeaderText(null);
            UiTheme.applyDialog(dialog);
            dialog.getDialogPane().setContent(root);
            dialog.getDialogPane().getButtonTypes().setAll(
                    new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
            dialog.getDialogPane().setPrefWidth(940);
            dialog.getDialogPane().setPrefHeight(780);
            dialog.getDialogPane().setMinWidth(680);
            dialog.getDialogPane().setMinHeight(560);
            dialog.setResizable(true);
            dialog.showAndWait();
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Impossible d’ouvrir la saisie : " + e.getMessage(), ButtonType.OK);
            UiTheme.applyDialog(a);
            a.showAndWait();
        }
    }

    private void resetToBlank() {
        patientCombo.getSelectionModel().clearSelection();
        onPatientChanged(null);
    }

    private void applyPrefillFromFiche(Fiche f) {
        User patient = userService.findById(f.getIdUId()).orElse(null);
        if (patient == null) {
            resetToBlank();
            return;
        }
        patientCombo.getItems().stream().filter(u -> u.getId() == patient.getId()).findFirst()
                .ifPresentOrElse(u -> patientCombo.getSelectionModel().select(u),
                        () -> {
                            patientCombo.getItems().add(patient);
                            patientCombo.getSelectionModel().select(patient);
                        });
        onPatientChanged(patient);
        loadFicheIntoForm(f);
    }

    private void onPatientChanged(User u) {
        clearFicheValidation();
        clearOrdoValidation();
        clearFicheForm();
        clearOrdoForm();
        ficheCombo.getItems().clear();
        if (u == null) {
            return;
        }
        Optional<Fiche> f = ficheService.findByUserId(u.getId());
        f.ifPresent(this::loadFicheIntoForm);
        List<Fiche> fiches = ficheService.listAll().stream()
                .filter(fi -> fi.getIdUId() == u.getId())
                .collect(Collectors.toList());
        ficheCombo.setItems(FXCollections.observableArrayList(fiches));
        if (!fiches.isEmpty()) {
            ficheCombo.getSelectionModel().selectFirst();
        }
    }

    private void loadFicheIntoForm(Fiche f) {
        fPoids.setText(String.valueOf(f.getPoids()));
        fTaille.setText(String.valueOf(f.getTaille()));
        if (f.getGrpSanguin() != null && fGrpCombo.getItems().contains(f.getGrpSanguin())) {
            fGrpCombo.getSelectionModel().select(f.getGrpSanguin());
        } else if (f.getGrpSanguin() != null && !f.getGrpSanguin().isBlank()) {
            fGrpCombo.getItems().add(f.getGrpSanguin());
            fGrpCombo.getSelectionModel().select(f.getGrpSanguin());
        }
        fAllergie.setText(f.getAllergie());
        fChronique.setText(f.getMaladieChronique());
        fTension.setText(f.getTension());
        fGlycemie.setText(String.valueOf(f.getGlycemie()));
        fDate.setValue(f.getDate() != null ? f.getDate() : LocalDate.now());
        fLibelle.setText(f.getLibelleMaladie());
        if (f.getGravite() != null && fGraviteCombo.getItems().contains(f.getGravite())) {
            fGraviteCombo.getSelectionModel().select(f.getGravite());
        } else if (f.getGravite() != null && !f.getGravite().isBlank()) {
            fGraviteCombo.getItems().add(f.getGravite());
            fGraviteCombo.getSelectionModel().select(f.getGravite());
        }
        fReco.setText(f.getRecommandation());
        fSymptomes.setText(f.getSymptomes());
    }

    private void clearFicheForm() {
        fPoids.clear();
        fTaille.clear();
        fGrpCombo.getSelectionModel().clearSelection();
        fAllergie.clear();
        fChronique.clear();
        fTension.clear();
        fGlycemie.clear();
        fDate.setValue(LocalDate.now());
        fLibelle.clear();
        fGraviteCombo.getSelectionModel().clearSelection();
        fReco.clear();
        fSymptomes.clear();
        clearFicheValidation();
    }

    private void clearOrdoForm() {
        oPosologie.clear();
        oFrequence.clear();
        oDuree.clear();
        oDate.setValue(null);
        oHourCombo.getSelectionModel().select("09");
        oMinCombo.getSelectionModel().selectFirst();
        medsListView.getSelectionModel().clearSelection();
        clearOrdoValidation();
    }

    @FXML
    private void onSaveFiche() {
        clearFicheValidation();
        User patient = patientCombo.getSelectionModel().getSelectedItem();
        Optional<Fiche> existingFiche = patient != null
                ? ficheService.findByUserId(patient.getId())
                : Optional.empty();
        FicheFormInput input = new FicheFormInput(
                patient != null ? patient.getId() : null,
                null,
                false,
                fPoids.getText(),
                fTaille.getText(),
                fGrpCombo.getSelectionModel().getSelectedItem(),
                fGraviteCombo.getSelectionModel().getSelectedItem(),
                fAllergie.getText(),
                fChronique.getText(),
                fTension.getText(),
                fGlycemie.getText(),
                fLibelle.getText(),
                fReco.getText(),
                fSymptomes.getText(),
                fDate.getValue(),
                existingFiche.isPresent()
        );
        LinkedHashMap<String, String> err = ValidationUtil.validateFicheDetailed(input);
        if (!err.isEmpty()) {
            applyFicheFieldErrors(err);
            return;
        }
        double poids = Double.parseDouble(fPoids.getText().trim().replace(',', '.'));
        double taille = Double.parseDouble(fTaille.getText().trim().replace(',', '.'));
        double glycemie = Double.parseDouble(fGlycemie.getText().trim().replace(',', '.'));
        String grp = fGrpCombo.getSelectionModel().getSelectedItem();
        String grav = fGraviteCombo.getSelectionModel().getSelectedItem();
        String lib = fLibelle.getText() != null ? fLibelle.getText().trim() : "";
        String tension = fTension.getText() != null ? fTension.getText().trim() : "";
        int medId = Session.getCurrentUserId();
        LocalDate consultDate = existingFiche.isPresent()
                ? (fDate.getValue() != null ? fDate.getValue()
                : (existingFiche.get().getDate() != null ? existingFiche.get().getDate() : LocalDate.now()))
                : LocalDate.now();
        Fiche f = new Fiche();
        f.setPoids(poids);
        f.setTaille(taille);
        f.setGrpSanguin(grp);
        f.setAllergie(safe(fAllergie));
        f.setMaladieChronique(safe(fChronique));
        f.setTension(tension);
        f.setGlycemie(glycemie);
        f.setDate(consultDate);
        f.setLibelleMaladie(lib);
        f.setGravite(grav);
        f.setRecommandation(fReco.getText() != null ? fReco.getText() : "");
        f.setSymptomes(fSymptomes.getText() != null ? fSymptomes.getText() : "");
        f.setIdUId(patient.getId());
        f.setMedecinUserId(medId > 0 ? medId : null);

        if (existingFiche.isPresent()) {
            f.setId(existingFiche.get().getId());
            ficheService.modifier(f);
        } else {
            ficheService.ajouter(f);
        }
        clearFicheValidation();
        onPatientChanged(patient);
        afterSave.run();
        alert(Alert.AlertType.INFORMATION, "Fiche enregistrée.");
    }

    @FXML
    private void onSaveOrdonnance() {
        clearOrdoValidation();
        User patient = patientCombo.getSelectionModel().getSelectedItem();
        Fiche fiche = ficheCombo.getSelectionModel().getSelectedItem();
        var sel = medsListView.getSelectionModel().getSelectedItems();
        int medCount = sel != null ? sel.size() : 0;
        int mid = Session.getCurrentUserId();
        String h = oHourCombo.getSelectionModel().getSelectedItem();
        String mi = oMinCombo.getSelectionModel().getSelectedItem();
        OrdonnanceFormInput input = new OrdonnanceFormInput(
                patient != null ? patient.getId() : null,
                mid > 0 ? mid : null,
                fiche != null ? fiche.getId() : null,
                medCount,
                oPosologie.getText(),
                oFrequence.getText(),
                oDuree.getText(),
                oDate.getValue(),
                h != null ? h : "",
                mi != null ? mi : "",
                ""
        );
        LinkedHashMap<String, String> err = ValidationUtil.validateOrdonnanceDetailed(input);
        if (patient != null && fiche != null && fiche.getIdUId() != patient.getId()) {
            err.put("fiche", "La fiche doit appartenir au patient sélectionné.");
        }
        if (!err.isEmpty()) {
            applyOrdoFieldErrors(err);
            return;
        }
        int duree = Integer.parseInt(oDuree.getText().trim());
        String pos = safe(oPosologie);
        String freq = safe(oFrequence);
        LocalTime lt = LocalTime.of(Integer.parseInt(h), Integer.parseInt(mi));
        LocalDateTime dt = LocalDateTime.of(oDate.getValue(), lt);

        Ordonnance o = new Ordonnance();
        o.setPosologie(pos);
        o.setFrequence(freq);
        o.setDureeTraitement(duree);
        o.setDateOrdonnance(dt);
        o.setScanToken(null);
        o.setIdUId(patient.getId());
        o.setIdRdvId(null);
        o.setIdFicheId(fiche.getId());
        o.setMedecinUserId(mid > 0 ? mid : null);

        int newId = ordonnanceService.ajouterRetourId(o);
        if (newId <= 0) {
            ordoValidationBanner.setText("Impossible d’enregistrer l’ordonnance (vérifiez la base / script SQL).");
            ordoValidationBanner.setVisible(true);
            ordoValidationBanner.setManaged(true);
            FormValidationStyles.markSection(ordoFormSection, true);
            return;
        }
        for (Medicament m : sel) {
            ligneService.insertLigne(newId, m.getId());
        }
        clearOrdoForm();
        afterSave.run();
        alert(Alert.AlertType.INFORMATION, "Ordonnance enregistrée.");
    }

    private static String safe(TextField t) {
        return t.getText() != null ? t.getText().trim() : "";
    }

    private static void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        UiTheme.applyDialog(a);
        a.showAndWait();
    }

    private void clearFicheValidation() {
        if (ficheValidationBanner != null) {
            ficheValidationBanner.setVisible(false);
            ficheValidationBanner.setManaged(false);
            ficheValidationBanner.setText("");
        }
        if (ficheFormSection != null) {
            FormValidationStyles.markSection(ficheFormSection, false);
        }
        FormValidationStyles.clearInvalid(patientCombo);
        FormValidationStyles.clearInvalid(fPoids);
        FormValidationStyles.clearInvalid(fTaille);
        FormValidationStyles.clearInvalid(fGrpCombo);
        FormValidationStyles.clearInvalid(fAllergie);
        FormValidationStyles.clearInvalid(fChronique);
        FormValidationStyles.clearInvalid(fTension);
        FormValidationStyles.clearInvalid(fGlycemie);
        FormValidationStyles.clearInvalid(fDate);
        FormValidationStyles.clearInvalid(fLibelle);
        FormValidationStyles.clearInvalid(fGraviteCombo);
        FormValidationStyles.clearInvalid(fReco);
        FormValidationStyles.clearInvalid(fSymptomes);
    }

    private void applyFicheFieldErrors(LinkedHashMap<String, String> err) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> en : err.entrySet()) {
            sb.append("• ").append(en.getValue()).append("\n");
        }
        ficheValidationBanner.setText(sb.toString().trim());
        ficheValidationBanner.setVisible(true);
        ficheValidationBanner.setManaged(true);
        FormValidationStyles.markSection(ficheFormSection, true);
        for (String k : err.keySet()) {
            switch (k) {
                case "patient" -> FormValidationStyles.markInvalid(patientCombo);
                case "poids" -> FormValidationStyles.markInvalid(fPoids);
                case "taille" -> FormValidationStyles.markInvalid(fTaille);
                case "grpSanguin" -> FormValidationStyles.markInvalid(fGrpCombo);
                case "allergie" -> FormValidationStyles.markInvalid(fAllergie);
                case "chronique" -> FormValidationStyles.markInvalid(fChronique);
                case "tension" -> FormValidationStyles.markInvalid(fTension);
                case "glycemie" -> FormValidationStyles.markInvalid(fGlycemie);
                case "date" -> FormValidationStyles.markInvalid(fDate);
                case "libelle" -> FormValidationStyles.markInvalid(fLibelle);
                case "gravite" -> FormValidationStyles.markInvalid(fGraviteCombo);
                case "recommandation" -> FormValidationStyles.markInvalid(fReco);
                case "symptomes" -> FormValidationStyles.markInvalid(fSymptomes);
                default -> {
                }
            }
        }
    }

    private void clearOrdoValidation() {
        if (ordoValidationBanner != null) {
            ordoValidationBanner.setVisible(false);
            ordoValidationBanner.setManaged(false);
            ordoValidationBanner.setText("");
        }
        if (ordoFormSection != null) {
            FormValidationStyles.markSection(ordoFormSection, false);
        }
        FormValidationStyles.clearInvalid(patientCombo);
        FormValidationStyles.clearInvalid(ficheCombo);
        FormValidationStyles.clearInvalid(medsListView);
        FormValidationStyles.clearInvalid(oPosologie);
        FormValidationStyles.clearInvalid(oFrequence);
        FormValidationStyles.clearInvalid(oDuree);
        FormValidationStyles.clearInvalid(oDate);
        FormValidationStyles.clearInvalid(oHourCombo);
        FormValidationStyles.clearInvalid(oMinCombo);
        if (oHourCombo.getParent() instanceof HBox hb) {
            FormValidationStyles.clearInvalid(hb);
        }
    }

    private void applyOrdoFieldErrors(LinkedHashMap<String, String> err) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> en : err.entrySet()) {
            sb.append("• ").append(en.getValue()).append("\n");
        }
        ordoValidationBanner.setText(sb.toString().trim());
        ordoValidationBanner.setVisible(true);
        ordoValidationBanner.setManaged(true);
        FormValidationStyles.markSection(ordoFormSection, true);
        for (String k : err.keySet()) {
            switch (k) {
                case "patient" -> FormValidationStyles.markInvalid(patientCombo);
                case "medecin" -> {
                }
                case "fiche" -> FormValidationStyles.markInvalid(ficheCombo);
                case "medicaments" -> FormValidationStyles.markInvalid(medsListView);
                case "posologie" -> FormValidationStyles.markInvalid(oPosologie);
                case "frequence" -> FormValidationStyles.markInvalid(oFrequence);
                case "duree" -> FormValidationStyles.markInvalid(oDuree);
                case "date" -> FormValidationStyles.markInvalid(oDate);
                case "heure" -> {
                    FormValidationStyles.markInvalid(oHourCombo);
                    FormValidationStyles.markInvalid(oMinCombo);
                    if (oHourCombo.getParent() instanceof HBox hb) {
                        FormValidationStyles.markInvalid(hb);
                    }
                }
                case "token" -> {
                }
                default -> {
                }
            }
        }
    }
}
