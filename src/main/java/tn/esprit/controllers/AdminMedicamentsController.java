package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import tn.esprit.entities.Medicament;
import tn.esprit.fx.FormFieldSlot;
import tn.esprit.fx.FormValidationStyles;
import tn.esprit.fx.UiTheme;
import tn.esprit.services.ServiceMedicament;
import tn.esprit.tools.ValidationUtil;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminMedicamentsController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Label countLabel;
    @FXML
    private FlowPane cardsFlow;

    private final ServiceMedicament service = new ServiceMedicament();
    private final ObservableList<Medicament> master = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Nom (A → Z)",
                "Nom (Z → A)",
                "Expiration (plus proche)",
                "Expiration (plus lointain)"
        ));
        sortCombo.getSelectionModel().selectFirst();
        categoryFilter.setItems(FXCollections.observableArrayList("Toutes"));
        categoryFilter.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((o, a, b) -> refreshView());
        categoryFilter.valueProperty().addListener((o, a, b) -> refreshView());
        sortCombo.valueProperty().addListener((o, a, b) -> refreshView());

        reloadMaster();
    }

    private void reloadMaster() {
        master.setAll(service.listAll());
        ObservableList<String> cats = FXCollections.observableArrayList("Toutes");
        master.stream()
                .map(Medicament::getCategorie)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .forEach(cats::add);
        String prev = categoryFilter.getSelectionModel().getSelectedItem();
        categoryFilter.setItems(cats);
        if (prev != null && cats.contains(prev)) {
            categoryFilter.getSelectionModel().select(prev);
        } else {
            categoryFilter.getSelectionModel().selectFirst();
        }
        refreshView();
    }

    private void refreshView() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        final String cat = Optional.ofNullable(categoryFilter.getSelectionModel().getSelectedItem()).orElse("Toutes");

        var stream = master.stream().filter(m -> matchesSearch(m, q)).filter(m -> matchesCategory(m, cat));
        Comparator<Medicament> cmp = comparatorForSort();
        var list = stream.sorted(cmp).collect(Collectors.toList());

        cardsFlow.getChildren().clear();
        for (Medicament m : list) {
            cardsFlow.getChildren().add(buildCard(m));
        }
        countLabel.setText(list.size() + " affiché(s) · " + master.size() + " au total");
    }

    private boolean matchesSearch(Medicament m, String q) {
        if (q.isEmpty()) {
            return true;
        }
        return contains(m.getNomMedicament(), q)
                || contains(m.getCategorie(), q)
                || contains(m.getDosage(), q)
                || contains(m.getForme(), q)
                || String.valueOf(m.getId()).contains(q);
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    private boolean matchesCategory(Medicament m, String cat) {
        if ("Toutes".equalsIgnoreCase(cat)) {
            return true;
        }
        return m.getCategorie() != null && m.getCategorie().equalsIgnoreCase(cat);
    }

    private Comparator<Medicament> comparatorForSort() {
        String s = sortCombo.getSelectionModel().getSelectedItem();
        if (s == null) {
            return Comparator.comparing(Medicament::getNomMedicament, String.CASE_INSENSITIVE_ORDER);
        }
        return switch (s) {
            case "Nom (Z → A)" -> Comparator.comparing(Medicament::getNomMedicament, String.CASE_INSENSITIVE_ORDER).reversed();
            case "Expiration (plus proche)" -> Comparator.comparing(Medicament::getDateExpiration, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Expiration (plus lointain)" -> Comparator.comparing(Medicament::getDateExpiration, Comparator.nullsFirst(Comparator.naturalOrder())).reversed();
            default -> Comparator.comparing(Medicament::getNomMedicament, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private VBox buildCard(Medicament m) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label badge = new Label(nullSafe(m.getCategorie()));
        badge.getStyleClass().add("card-badge");
        badge.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label(m.getNomMedicament());
        title.getStyleClass().add("card-title");

        Label dos = new Label("Dosage · " + nullSafe(m.getDosage()));
        dos.getStyleClass().add("card-meta");
        dos.setWrapText(true);
        Label forme = new Label("Forme · " + nullSafe(m.getForme()));
        forme.getStyleClass().add("card-meta");
        forme.setWrapText(true);
        Label exp = new Label("Expire le " + (m.getDateExpiration() != null ? m.getDateExpiration().toString() : "—"));
        exp.getStyleClass().add("card-sub");
        exp.setWrapText(true);

        HBox actions = new HBox(10);
        actions.getStyleClass().add("card-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        Button edit = new Button("Modifier");
        edit.getStyleClass().add("ghost-button");
        edit.setOnAction(e -> showEditorDialog(m));
        Button del = new Button("Supprimer");
        del.getStyleClass().add("danger-button");
        del.setOnAction(e -> confirmDelete(m));
        actions.getChildren().addAll(edit, del);

        card.getChildren().addAll(badge, title, dos, forme, exp, actions);
        return card;
    }

    private static String nullSafe(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    @FXML
    private void onAdd() {
        showEditorDialog(null);
    }

    private void confirmDelete(Medicament m) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer définitivement « " + m.getNomMedicament() + " » ?");
        UiTheme.applyDialog(a);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            service.supprimer(m);
            reloadMaster();
        }
    }

    private void showEditorDialog(Medicament existing) {
        Window owner = cardsFlow.getScene().getWindow();
        boolean isEdit = existing != null;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        UiTheme.applyDialog(dialog);
        dialog.setTitle(isEdit ? "Modifier un médicament" : "Ajouter un médicament");
        dialog.setHeaderText(isEdit
                ? "Mettre à jour « " + existing.getNomMedicament() + " »"
                : "Renseignez les informations du médicament");

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField fNom = new TextField();
        TextField fCategorie = new TextField();
        TextField fDosage = new TextField();
        TextField fForme = new TextField();
        DatePicker fExp = new DatePicker();
        fNom.setPromptText("Ex. Paracétamol");
        fCategorie.setPromptText("Ex. Antalgique");
        fDosage.setPromptText("Ex. 500 mg");
        fForme.setPromptText("Ex. Comprimé");

        FormFieldSlot slotNom = new FormFieldSlot(fNom);
        FormFieldSlot slotCat = new FormFieldSlot(fCategorie);
        FormFieldSlot slotDosage = new FormFieldSlot(fDosage);
        FormFieldSlot slotForme = new FormFieldSlot(fForme);
        FormFieldSlot slotExp = new FormFieldSlot(fExp);

        if (isEdit) {
            fNom.setText(existing.getNomMedicament());
            fCategorie.setText(existing.getCategorie());
            fDosage.setText(existing.getDosage());
            fForme.setText(existing.getForme());
            if (existing.getDateExpiration() != null) {
                fExp.setValue(existing.getDateExpiration());
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(4, 0, 8, 0));
        int r = 0;
        grid.add(new Label("Nom *"), 0, r);
        grid.add(slotNom.getRoot(), 1, r++);
        grid.add(new Label("Catégorie"), 0, r);
        grid.add(slotCat.getRoot(), 1, r++);
        grid.add(new Label("Dosage"), 0, r);
        grid.add(slotDosage.getRoot(), 1, r++);
        grid.add(new Label("Forme"), 0, r);
        grid.add(slotForme.getRoot(), 1, r++);
        grid.add(new Label("Date d’expiration *"), 0, r);
        grid.add(slotExp.getRoot(), 1, r);
        GridPane.setHgrow(fNom, Priority.ALWAYS);
        UiTheme.tagFormLabels(grid);

        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(360);

        Label validationSummary = new Label();
        validationSummary.getStyleClass().add("form-validation-summary");
        validationSummary.setWrapText(true);
        validationSummary.setMaxWidth(520);
        validationSummary.setVisible(false);
        validationSummary.setManaged(false);

        VBox formShell = new VBox(10, validationSummary, sp);
        dialog.getDialogPane().setContent(formShell);

        Runnable clearValidation = () -> {
            validationSummary.setVisible(false);
            validationSummary.setManaged(false);
            validationSummary.setText("");
            FormValidationStyles.markSection(formShell, false);
            slotNom.clear();
            slotCat.clear();
            slotDosage.clear();
            slotForme.clear();
            slotExp.clear();
        };

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            clearValidation.run();
            Medicament med = new Medicament();
            med.setNomMedicament(fNom.getText() != null ? fNom.getText().trim() : "");
            med.setCategorie(safe(fCategorie));
            med.setDosage(safe(fDosage));
            med.setForme(safe(fForme));
            med.setDateExpiration(fExp.getValue());
            LinkedHashMap<String, String> err = ValidationUtil.validateMedicamentDetailed(med);
            if (!err.isEmpty()) {
                ev.consume();
                for (Map.Entry<String, String> en : err.entrySet()) {
                    switch (en.getKey()) {
                        case "nom" -> slotNom.setError(en.getValue());
                        case "categorie" -> slotCat.setError(en.getValue());
                        case "dosage" -> slotDosage.setError(en.getValue());
                        case "forme" -> slotForme.setError(en.getValue());
                        case "dateExpiration" -> slotExp.setError(en.getValue());
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
            if (isEdit) {
                med.setId(existing.getId());
                service.modifier(med);
            } else {
                service.ajouter(med);
            }
        });

        dialog.showAndWait();
        reloadMaster();
    }

