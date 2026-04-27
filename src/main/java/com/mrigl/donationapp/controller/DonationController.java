package com.mrigl.donationapp.controller;

import com.mrigl.donationapp.model.Annonce;
import com.mrigl.donationapp.model.Donation;
import com.mrigl.donationapp.service.DataRepository;
import com.mrigl.donationapp.service.TextToSpeechService;
import com.mrigl.donationapp.service.ValidationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DonationController implements Initializable {

    private final DataRepository store = DataRepository.getInstance();
    private final TextToSpeechService textToSpeechService = new TextToSpeechService();

    @FXML
    private ListView<Donation> listDonations;
    @FXML
    private Label lblTotalDons;
    @FXML
    private Label lblPendingDons;
    @FXML
    private Label lblAcceptedDons;
    @FXML
    private TextField tfTypeDon;
    @FXML
    private TextField tfQuantite;
    @FXML
    private DatePicker dpDateDonation;
    @FXML
    private ComboBox<String> cbStatut;
    @FXML
    private ComboBox<Annonce> cbAnnonce;
    @FXML
    private Label lblErreursDonation;
    @FXML
    private Button btnAjouterDonation;
    @FXML
    private Button btnModifierDonation;
    @FXML
    private Button btnSupprimerDonation;
    @FXML
    private Button btnEffacerDonation;

    private Donation editing;
    private boolean suppressSelectionDialogs;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbStatut.setItems(FXCollections.observableArrayList("en attente", "accepté", "refusé"));
        cbAnnonce.setItems(store.annoncesProperty());
        cbAnnonce.setPromptText("— Aucune annonce —");

        listDonations.setItems(store.donationsProperty());
        refreshStats();
        listDonations.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Donation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                String annonceTxt = "Aucune";
                if (item.getAnnonceId() != null) {
                    annonceTxt = store.findAnnonce(item.getAnnonceId())
                            .map(Annonce::getTitreAnnonce)
                            .orElse("Annonce supprimée");
                }

                Label title = new Label(item.getTypeDon());
                title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                Label quantityChip = new Label("Qte: " + item.getQuantite());
                quantityChip.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-padding: 3 8 3 8; -fx-background-radius: 999;");
                Region topSpacer = new Region();
                HBox.setHgrow(topSpacer, Priority.ALWAYS);
                HBox titleRow = new HBox(8, title, topSpacer, quantityChip);

                Label meta = new Label("Donation du " + item.getDateDonation());
                meta.setStyle("-fx-text-fill: #607d8b; -fx-font-size: 12px;");
                Label annonce = new Label("Annonce: " + annonceTxt);
                annonce.setStyle("-fx-text-fill: #455a64;");
                Label statusChip = new Label(formatStatus(item.getStatut()));
                statusChip.setStyle(statusStyle(item.getStatut()));
                Region statusSpacer = new Region();
                HBox.setHgrow(statusSpacer, Priority.ALWAYS);
                HBox footer = new HBox(8, annonce, statusSpacer, statusChip);

                VBox card = new VBox(6, titleRow, meta, footer);
                card.setPadding(new Insets(10));
                card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10;");
                setGraphic(card);
            }
        });

        listDonations.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            if (suppressSelectionDialogs) {
                return;
            }
            if (sel != null) {
                openDonationDetails(sel);
                clearSelectionSilently();
            } else {
                editing = null;
            }
        });

        btnAjouterDonation.setOnAction(e -> onAjouter());
        btnModifierDonation.setOnAction(e -> onModifier());
        btnSupprimerDonation.setOnAction(e -> onSupprimer());
        btnEffacerDonation.setOnAction(e -> clearForm());

        store.donationsProperty().addListener((javafx.collections.ListChangeListener.Change<? extends Donation> c) -> refreshStats());
    }

    private void refreshStats() {
        int total = store.donationsProperty().size();
        int pending = (int) store.donationsProperty().stream().filter(d -> "en attente".equals(d.getStatut())).count();
        int accepted = (int) store.donationsProperty().stream().filter(d -> "accepté".equals(d.getStatut())).count();
        lblTotalDons.setText(String.valueOf(total));
        lblPendingDons.setText(String.valueOf(pending));
        lblAcceptedDons.setText(String.valueOf(accepted));
    }

    private String formatStatus(String status) {
        if ("en attente".equals(status)) {
            return "En attente";
        }
        if ("accepté".equals(status)) {
            return "Accepté";
        }
        return status == null ? "Inconnu" : status;
    }

    private String statusStyle(String status) {
        if ("en attente".equals(status)) {
            return "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-padding: 4 8 4 8; -fx-background-radius: 999;";
        }
        if ("accepté".equals(status)) {
            return "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 4 8 4 8; -fx-background-radius: 999;";
        }
        return "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-padding: 4 8 4 8; -fx-background-radius: 999;";
    }

    private void onAjouter() {
        lblErreursDonation.setText("");
        if (quantiteTexteInvalide()) {
            return;
        }
        Donation d = readFormWithoutId();
        List<String> err = ValidationService.validateDonation(d);
        if (!err.isEmpty()) {
            lblErreursDonation.setText(String.join("\n", err));
            return;
        }
        store.createDonation(d);
        clearForm();
        refreshStats();
    }

    private void onModifier() {
        lblErreursDonation.setText("");
        Donation selected = listDonations.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getId() == null) {
            lblErreursDonation.setText("Sélectionnez une donation à modifier.");
            return;
        }
        openDonationEditor(selected);
    }

    private void onSupprimer() {
        lblErreursDonation.setText("");
        Donation sel = listDonations.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getId() == null) {
            lblErreursDonation.setText("Sélectionnez une donation à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette donation ?",
                ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Confirmation");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                store.deleteDonation(sel.getId());
                clearForm();
                refreshStats();
                clearSelectionSilently();
            }
        });
    }

    private Donation readFormWithoutId() {
        return readFormWithId(null);
    }

    private Donation readFormWithId(Long id) {
        Donation d = new Donation();
        d.setId(id);
        d.setTypeDon(tfTypeDon.getText());
        d.setQuantite(parseQuantite(tfQuantite.getText()));
        d.setDateDonation(dpDateDonation.getValue());
        d.setStatut(cbStatut.getValue());
        Annonce ann = cbAnnonce.getValue();
        d.setAnnonceId(ann == null ? null : ann.getId());
        return d;
    }

    private boolean quantiteTexteInvalide() {
        String raw = tfQuantite.getText();
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        if (parseQuantite(raw) == null) {
            lblErreursDonation.setText("La quantité doit être un nombre entier valide.");
            return true;
        }
        return false;
    }

    private static Integer parseQuantite(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void fillForm(Donation d) {
        tfTypeDon.setText(d.getTypeDon());
        tfQuantite.setText(d.getQuantite() == null ? "" : d.getQuantite().toString());
        dpDateDonation.setValue(d.getDateDonation());
        cbStatut.setValue(d.getStatut());
        if (d.getAnnonceId() == null) {
            cbAnnonce.getSelectionModel().clearSelection();
        } else {
            store.findAnnonce(d.getAnnonceId()).ifPresentOrElse(
                    a -> cbAnnonce.getSelectionModel().select(a),
                    () -> cbAnnonce.getSelectionModel().clearSelection());
        }
    }

    private void clearForm() {
        editing = null;
        listDonations.getSelectionModel().clearSelection();
        tfTypeDon.clear();
        tfQuantite.clear();
        dpDateDonation.setValue(null);
        cbStatut.getSelectionModel().clearSelection();
        cbAnnonce.getSelectionModel().clearSelection();
        lblErreursDonation.setText("");
    }

    private void openDonationEditor(Donation donation) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier une donation");
        dialog.setHeaderText("Mettez à jour les détails du don");
        dialog.initOwner(listDonations.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField typeDon = new TextField(donation.getTypeDon());
        TextField quantite = new TextField(donation.getQuantite() == null ? "" : donation.getQuantite().toString());
        DatePicker date = new DatePicker(donation.getDateDonation());
        ComboBox<String> statut = new ComboBox<>(FXCollections.observableArrayList("en attente", "accepté", "refusé"));
        statut.setValue(donation.getStatut());
        ComboBox<Annonce> annonce = new ComboBox<>(store.annoncesProperty());
        annonce.setPromptText("— Aucune annonce —");
        if (donation.getAnnonceId() != null) {
            store.findAnnonce(donation.getAnnonceId()).ifPresent(annonce::setValue);
        }
        Label error = new Label();
        error.setStyle("-fx-text-fill: #c62828;");
        error.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 14;");
        grid.addRow(0, new Label("Type de don *"), typeDon);
        grid.addRow(1, new Label("Quantité *"), quantite);
        grid.addRow(2, new Label("Date du don *"), date);
        grid.addRow(3, new Label("Statut *"), statut);
        grid.addRow(4, new Label("Annonce (optionnel)"), annonce);
        grid.add(error, 0, 5, 2, 1);
        dialog.getDialogPane().setContent(grid);

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setMinWidth(650);

        final Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Donation updated = new Donation();
            updated.setId(donation.getId());
            updated.setTypeDon(typeDon.getText());
            updated.setQuantite(parseQuantite(quantite.getText()));
            updated.setDateDonation(date.getValue());
            updated.setStatut(statut.getValue());
            updated.setAnnonceId(annonce.getValue() == null ? null : annonce.getValue().getId());
            List<String> errors = ValidationService.validateDonation(updated);
            if (updated.getQuantite() == null && quantite.getText() != null && !quantite.getText().trim().isEmpty()) {
                errors = new java.util.ArrayList<>(errors);
                errors.add(0, "La quantité doit être un nombre entier valide.");
            }
            if (!errors.isEmpty()) {
                error.setText(String.join("\n", errors));
                event.consume();
                return;
            }
            store.updateDonation(updated);
            refreshStats();
        });

        dialog.showAndWait();
        clearSelectionSilently();
    }

    private void openDonationDetails(Donation donation) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Détail donation");
        dialog.setHeaderText("Consultez et gérez cette donation");
        dialog.initOwner(listDonations.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType modifyType = new ButtonType("Modifier", ButtonBar.ButtonData.OK_DONE);
        ButtonType readType = new ButtonType("Lire à voix haute", ButtonBar.ButtonData.OTHER);
        ButtonType stopReadType = new ButtonType("Arrêter la lecture", ButtonBar.ButtonData.OTHER);
        ButtonType deleteType = new ButtonType("Supprimer", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(modifyType, readType, stopReadType, deleteType, ButtonType.CLOSE);

        final String annonceTxt = donation.getAnnonceId() == null
                ? "Aucune"
                : store.findAnnonce(donation.getAnnonceId())
                        .map(Annonce::getTitreAnnonce)
                        .orElse("Annonce supprimée");

        Label typeDon = new Label("Type de don : " + safe(donation.getTypeDon()));
        typeDon.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label quantite = new Label("Quantité : " + (donation.getQuantite() == null ? "—" : donation.getQuantite()));
        Label date = new Label("Date du don : " + (donation.getDateDonation() == null ? "—" : donation.getDateDonation()));
        Label statut = new Label("Statut : " + safe(donation.getStatut()));
        Label annonce = new Label("Annonce liée : " + annonceTxt);

        VBox content = new VBox(10, typeDon, quantite, date, statut, annonce);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 14;");
        dialog.getDialogPane().setContent(content);

        Button readButton = (Button) dialog.getDialogPane().lookupButton(readType);
        readButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            speakText(
                    "Donation. Type de don " + safe(donation.getTypeDon())
                            + ". Quantite " + (donation.getQuantite() == null ? "non définie" : donation.getQuantite())
                            + ". Date du don " + (donation.getDateDonation() == null ? "non définie" : donation.getDateDonation())
                            + ". Statut " + safe(donation.getStatut())
                            + ". Annonce liee " + annonceTxt + "."
            );
        });
        Button stopReadButton = (Button) dialog.getDialogPane().lookupButton(stopReadType);
        stopReadButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            stopSpeak();
        });

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == modifyType) {
                openDonationEditor(donation);
            } else if (bt == deleteType) {
                store.deleteDonation(donation.getId());
                refreshStats();
                clearSelectionSilently();
            }
        });
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private void clearSelectionSilently() {
        suppressSelectionDialogs = true;
        listDonations.getSelectionModel().clearSelection();
        Platform.runLater(() -> suppressSelectionDialogs = false);
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
}
