package com.mrigl.donationapp.controller;

import com.mrigl.donationapp.model.Annonce;
import com.mrigl.donationapp.model.Donation;
import com.mrigl.donationapp.service.DataRepository;
import com.mrigl.donationapp.service.ValidationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class AnnonceController implements Initializable {

    private final DataRepository store = DataRepository.getInstance();

    @FXML
    private ListView<Annonce> listAnnonces;
    @FXML
    private TextField tfSearchAnnonce;
    @FXML
    private ComboBox<String> cbSortUrgence;
    @FXML
    private TextField tfTitre;
    @FXML
    private TextArea taDescription;
    @FXML
    private DatePicker dpDatePublication;
    @FXML
    private ComboBox<String> cbUrgence;
    @FXML
    private ComboBox<String> cbEtatAnnonce;
    @FXML
    private Label lblErreursAnnonce;
    @FXML
    private Button btnAjouterAnnonce;
    @FXML
    private Button btnModifierAnnonce;
    @FXML
    private Button btnSupprimerAnnonce;
    @FXML
    private Button btnEffacerAnnonce;

    private Annonce editing;
    private boolean suppressSelectionDialogs;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbUrgence.setItems(FXCollections.observableArrayList("faible", "moyenne", "élevée"));
        cbEtatAnnonce.setItems(FXCollections.observableArrayList("active", "clôturée"));
        cbSortUrgence.setItems(FXCollections.observableArrayList(
                "Aucun tri", "Urgence élevée -> faible", "Urgence faible -> élevée"));
        cbSortUrgence.setValue("Aucun tri");

        FilteredList<Annonce> filtered = new FilteredList<>(store.annoncesProperty(), a -> true);
        SortedList<Annonce> sorted = new SortedList<>(filtered);
        listAnnonces.setItems(sorted);

        tfSearchAnnonce.textProperty().addListener((obs, oldV, term) ->
                filtered.setPredicate(a -> {
                    if (term == null || term.isBlank()) {
                        return true;
                    }
                    String t = a.getTitreAnnonce() == null ? "" : a.getTitreAnnonce().toLowerCase();
                    return t.contains(term.toLowerCase().trim());
                }));

        cbSortUrgence.valueProperty().addListener((obs, oldV, selected) -> {
            if ("Urgence élevée -> faible".equals(selected)) {
                sorted.setComparator(Comparator.comparingInt(this::urgenceWeight).reversed());
            } else if ("Urgence faible -> élevée".equals(selected)) {
                sorted.setComparator(Comparator.comparingInt(this::urgenceWeight));
            } else {
                sorted.setComparator(null);
            }
        });

        listAnnonces.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Annonce item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                Label title = new Label(item.getTitreAnnonce());
                title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                Label urgenceBadge = new Label(item.getUrgence() == null ? "Urgence ?" : "Urgence " + item.getUrgence());
                urgenceBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8 4 8; -fx-background-radius: 999;");
                styleUrgenceBadge(urgenceBadge, item.getUrgence());
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox titleRow = new HBox(8, title, spacer, urgenceBadge);

                Label desc = new Label(item.getDescription());
                desc.setWrapText(true);
                desc.setStyle("-fx-text-fill: #455a64;");
                Label statusChip = new Label("active".equals(item.getEtatAnnonce()) ? "Active" : "Cloturee");
                statusChip.setStyle("active".equals(item.getEtatAnnonce())
                        ? "-fx-font-size: 11px; -fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 4 8 4 8; -fx-background-radius: 999;"
                        : "-fx-font-size: 11px; -fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-padding: 4 8 4 8; -fx-background-radius: 999;");
                Label meta = new Label("Publie le " + item.getDatePublication());
                meta.setStyle("-fx-text-fill: #607d8b; -fx-font-size: 12px;");
                Region metaSpacer = new Region();
                HBox.setHgrow(metaSpacer, Priority.ALWAYS);
                HBox footer = new HBox(8, meta, metaSpacer, statusChip);

                VBox card = new VBox(8, titleRow, desc, footer);
                card.setPadding(new Insets(10));
                card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10;");
                setGraphic(card);
            }
        });

        listAnnonces.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            if (suppressSelectionDialogs) {
                return;
            }
            if (sel != null) {
                openAnnonceDetails(sel);
                clearSelectionSilently();
            } else {
                editing = null;
            }
        });

        btnAjouterAnnonce.setOnAction(e -> onAjouter());
        btnModifierAnnonce.setOnAction(e -> onModifier());
        btnSupprimerAnnonce.setOnAction(e -> onSupprimer());
        btnEffacerAnnonce.setOnAction(e -> clearForm());

        dpDatePublication.setValue(LocalDate.now());
    }

    private int urgenceWeight(Annonce a) {
        if (a == null || a.getUrgence() == null) {
            return 0;
        }
        return switch (a.getUrgence()) {
            case "élevée" -> 3;
            case "moyenne" -> 2;
            case "faible" -> 1;
            default -> 0;
        };
    }

    private void styleUrgenceBadge(Label badge, String urgence) {
        if ("élevée".equals(urgence)) {
            badge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 4 8 4 8; -fx-background-radius: 999;");
        } else if ("moyenne".equals(urgence)) {
            badge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-padding: 4 8 4 8; -fx-background-radius: 999;");
        } else {
            badge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 4 8 4 8; -fx-background-radius: 999;");
        }
    }

    private void onAjouter() {
        lblErreursAnnonce.setText("");
        Annonce a = readFormWithoutId();
        List<String> err = ValidationService.validateAnnonce(a);
        if (!err.isEmpty()) {
            lblErreursAnnonce.setText(String.join("\n", err));
            return;
        }
        store.createAnnonce(a);
        clearForm();
    }

    private void onModifier() {
        lblErreursAnnonce.setText("");
        Annonce selected = listAnnonces.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getId() == null) {
            lblErreursAnnonce.setText("Sélectionnez une annonce à modifier.");
            return;
        }
        openAnnonceEditor(selected);
    }

    private void onSupprimer() {
        lblErreursAnnonce.setText("");
        Annonce sel = listAnnonces.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getId() == null) {
            lblErreursAnnonce.setText("Sélectionnez une annonce à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette annonce ? Les donations liées seront aussi retirées.",
                ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Confirmation");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                store.deleteAnnonce(sel.getId());
                clearForm();
                clearSelectionSilently();
            }
        });
    }

    private Annonce readFormWithoutId() {
        return readFormWithId(null);
    }

    private Annonce readFormWithId(Long id) {
        Annonce a = new Annonce();
        a.setId(id);
        a.setTitreAnnonce(tfTitre.getText());
        a.setDescription(taDescription.getText());
        a.setDatePublication(dpDatePublication.getValue());
        a.setUrgence(cbUrgence.getValue());
        a.setEtatAnnonce(cbEtatAnnonce.getValue());
        return a;
    }

    private void fillForm(Annonce a) {
        tfTitre.setText(a.getTitreAnnonce());
        taDescription.setText(a.getDescription());
        dpDatePublication.setValue(a.getDatePublication());
        cbUrgence.setValue(a.getUrgence());
        cbEtatAnnonce.setValue(a.getEtatAnnonce());
    }

    private void clearForm() {
        editing = null;
        listAnnonces.getSelectionModel().clearSelection();
        tfTitre.clear();
        taDescription.clear();
        dpDatePublication.setValue(LocalDate.now());
        cbUrgence.getSelectionModel().clearSelection();
        cbEtatAnnonce.getSelectionModel().clearSelection();
        lblErreursAnnonce.setText("");
    }

    private void openAnnonceDetails(Annonce annonce) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Détail annonce");
        dialog.setHeaderText("Consultez et gérez cette annonce");
        dialog.initOwner(listAnnonces.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType modifyType = new ButtonType("Modifier", ButtonBar.ButtonData.OK_DONE);
        ButtonType addDonationType = new ButtonType("Proposer un don", ButtonBar.ButtonData.OTHER);
        ButtonType deleteType = new ButtonType("Supprimer", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(modifyType, addDonationType, deleteType, ButtonType.CLOSE);

        Label titre = new Label("Titre : " + safe(annonce.getTitreAnnonce()));
        titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        titre.setWrapText(true);
        titre.setMaxWidth(620);

        Label descTitle = new Label("Description :");
        descTitle.setStyle("-fx-font-weight: bold;");
        TextArea description = new TextArea(safe(annonce.getDescription()));
        description.setWrapText(true);
        description.setEditable(false);
        description.setFocusTraversable(false);
        description.setPrefRowCount(6);
        description.setMaxWidth(620);

        Label date = new Label("Date publication : " + (annonce.getDatePublication() == null ? "—" : annonce.getDatePublication()));
        Label urgence = new Label("Urgence : " + safe(annonce.getUrgence()));
        Label etat = new Label("État : " + safe(annonce.getEtatAnnonce()));

        VBox content = new VBox(10, titre, descTitle, description, date, urgence, etat);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 14;");
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefViewportWidth(650);
        scrollPane.setPrefViewportHeight(320);
        dialog.getDialogPane().setContent(scrollPane);

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setMinWidth(720);
        stage.setMinHeight(420);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == modifyType) {
                openAnnonceEditor(annonce);
            } else if (bt == addDonationType) {
                openAddDonationForAnnonceDialog(annonce);
            } else if (bt == deleteType) {
                store.deleteAnnonce(annonce.getId());
                clearSelectionSilently();
            }
        });
    }

    private void openAnnonceEditor(Annonce annonce) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier une annonce");
        dialog.setHeaderText("Mettez à jour les informations");
        dialog.initOwner(listAnnonces.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField titre = new TextField(annonce.getTitreAnnonce());
        titre.setEditable(true);
        TextArea description = new TextArea(annonce.getDescription());
        description.setEditable(true);
        description.setWrapText(true);
        description.setPrefRowCount(4);
        DatePicker date = new DatePicker(annonce.getDatePublication());
        ComboBox<String> urgence = new ComboBox<>(FXCollections.observableArrayList("faible", "moyenne", "élevée"));
        urgence.setValue(annonce.getUrgence());
        ComboBox<String> etat = new ComboBox<>(FXCollections.observableArrayList("active", "clôturée"));
        etat.setValue(annonce.getEtatAnnonce());
        Label error = new Label();
        error.setStyle("-fx-text-fill: #c62828;");
        error.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 14;");
        grid.addRow(0, new Label("Titre *"), titre);
        grid.addRow(1, new Label("Description *"), description);
        grid.addRow(2, new Label("Date publication *"), date);
        grid.addRow(3, new Label("Urgence *"), urgence);
        grid.addRow(4, new Label("État annonce *"), etat);
        grid.add(error, 0, 5, 2, 1);
        dialog.getDialogPane().setContent(grid);

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setMinWidth(650);

        final Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Annonce updated = new Annonce();
            updated.setId(annonce.getId());
            updated.setTitreAnnonce(titre.getText());
            updated.setDescription(description.getText());
            updated.setDatePublication(date.getValue());
            updated.setUrgence(urgence.getValue());
            updated.setEtatAnnonce(etat.getValue());
            List<String> errors = ValidationService.validateAnnonce(updated);
            if (!errors.isEmpty()) {
                error.setText(String.join("\n", errors));
                event.consume();
                return;
            }
            store.updateAnnonce(updated);
        });

        dialog.setOnShown(e -> Platform.runLater(() -> {
            titre.requestFocus();
            String t = titre.getText();
            titre.positionCaret(t != null ? t.length() : 0);
        }));
        dialog.showAndWait();
        clearSelectionSilently();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private void openAddDonationForAnnonceDialog(Annonce annonce) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Proposer un don pour cette annonce");
        dialog.setHeaderText("Créer une donation liée à cette annonce");
        dialog.initOwner(listAnnonces.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        Label annonceInfo = new Label("Annonce sélectionnée: " + safe(annonce.getTitreAnnonce()));
        annonceInfo.setStyle("-fx-font-weight: bold;");
        TextField typeDon = new TextField();
        typeDon.setPromptText("Type de don");
        TextField quantite = new TextField();
        quantite.setPromptText("Quantité");
        DatePicker dateDonation = new DatePicker(LocalDate.now());
        ComboBox<String> statut = new ComboBox<>(FXCollections.observableArrayList("en attente", "accepté", "refusé"));
        statut.setValue("en attente");
        Label error = new Label();
        error.setStyle("-fx-text-fill: #c62828;");
        error.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 14;");
        grid.add(annonceInfo, 0, 0, 2, 1);
        grid.addRow(1, new Label("Type de don *"), typeDon);
        grid.addRow(2, new Label("Quantité *"), quantite);
        grid.addRow(3, new Label("Date du don *"), dateDonation);
        grid.addRow(4, new Label("Statut *"), statut);
        grid.add(error, 0, 5, 2, 1);
        dialog.getDialogPane().setContent(grid);

        final Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Donation donation = new Donation();
            donation.setTypeDon(typeDon.getText());
            donation.setQuantite(parsePositiveInt(quantite.getText()));
            donation.setDateDonation(dateDonation.getValue());
            donation.setStatut(statut.getValue());
            donation.setAnnonceId(annonce.getId());

            List<String> errors = ValidationService.validateDonation(donation);
            if (donation.getQuantite() == null && quantite.getText() != null && !quantite.getText().trim().isEmpty()) {
                errors = new java.util.ArrayList<>(errors);
                errors.add(0, "La quantité doit être un nombre entier valide.");
            }
            if (!errors.isEmpty()) {
                error.setText(String.join("\n", errors));
                event.consume();
                return;
            }
            store.createDonation(donation);
        });

        dialog.showAndWait();
    }

    private Integer parsePositiveInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void clearSelectionSilently() {
        suppressSelectionDialogs = true;
        listAnnonces.getSelectionModel().clearSelection();
        Platform.runLater(() -> suppressSelectionDialogs = false);
    }
}
