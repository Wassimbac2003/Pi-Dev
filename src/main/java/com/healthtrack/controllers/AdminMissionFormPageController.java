package com.healthtrack.controllers;

import com.healthtrack.entities.MissionVolunteer;
import com.healthtrack.services.MissionVolunteerService;
import com.healthtrack.util.MissionMediaUtil;
import com.healthtrack.util.UiMessageUtil;
import com.healthtrack.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;

public class AdminMissionFormPageController implements AdminPageController {
    @FXML private Label breadcrumbLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label formTitleLabel;
    @FXML private TextField photoField;
    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField lieuField;
    @FXML private TextField statutField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Button photoButton;

    private final MissionVolunteerService missionService = new MissionVolunteerService();
    private AdminMainController adminMainController;
    private MissionVolunteer mission;

    @Override
    public void setAdminMainController(AdminMainController adminMainController) {
        this.adminMainController = adminMainController;
    }

    public void setMission(MissionVolunteer mission) {
        this.mission = mission;
        boolean editing = mission != null;
        breadcrumbLabel.setText(editing ? "Missions > Modifier" : "Missions > Nouveau");
        pageTitleLabel.setText(editing ? "Modifier la mission" : "Creer une nouvelle mission");
        formTitleLabel.setText(editing ? "Formulaire de modification" : "Formulaire de creation");

        if (editing) {
            titreField.setText(mission.getTitre());
            descriptionArea.setText(mission.getDescription());
            lieuField.setText(mission.getLieu());
            statutField.setText(mission.getStatut());
            dateDebutPicker.setValue(mission.getDateDebut());
            dateFinPicker.setValue(mission.getDateFin());
            photoField.setText(mission.getPhoto());
        } else {
            statutField.setText("Ouverte");
        }
    }

    @FXML
    private void choosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File selectedFile = fileChooser.showOpenDialog(photoButton.getScene().getWindow());
        if (selectedFile != null) {
            photoField.setText(MissionMediaUtil.importMissionPhoto(selectedFile));
        }
    }

    @FXML
    private void saveMission() {
        ValidationUtil.clearInvalid(titreField, descriptionArea, lieuField, statutField, dateDebutPicker, dateFinPicker, photoField);
        var errors = ValidationUtil.validateMission(
                titreField.getText(),
                descriptionArea.getText(),
                lieuField.getText(),
                dateDebutPicker.getValue(),
                dateFinPicker.getValue(),
                statutField.getText(),
                photoField.getText()
        );
        if (ValidationUtil.isDuplicateMission(missionService.getAll(), mission == null ? 0 : mission.getId(),
                titreField.getText(), lieuField.getText(), dateDebutPicker.getValue(), dateFinPicker.getValue())) {
            errors.add(mission == null
                    ? "Une mission similaire existe deja avec le meme titre, lieu et periode."
                    : "Une autre mission similaire existe deja avec le meme titre, lieu et periode.");
        }
        if (!errors.isEmpty()) {
            markInvalidFields();
            markDuplicateFieldsIfNeeded();
            UiMessageUtil.showValidationErrors("Mission backOffice", errors);
            return;
        }

        MissionVolunteer current = mission == null ? new MissionVolunteer() : mission;
        current.setTitre(titreField.getText());
        current.setDescription(descriptionArea.getText());
        current.setLieu(lieuField.getText());
        current.setStatut(statutField.getText());
        current.setDateDebut(dateDebutPicker.getValue());
        current.setDateFin(dateFinPicker.getValue());
        current.setPhoto(photoField.getText() == null || photoField.getText().isBlank() ? null : photoField.getText().trim());

        if (mission == null) {
            missionService.ajouter(current);
        } else {
            missionService.modifier(current);
        }
        adminMainController.showAdminMissionsPage();
    }

    @FXML
    private void cancel() {
        adminMainController.showAdminMissionsPage();
    }

    private void markInvalidFields() {
        ValidationUtil.markIfInvalid(titreField, titreField.getText() == null || titreField.getText().trim().length() < 4);
        ValidationUtil.markIfInvalid(descriptionArea, descriptionArea.getText() == null || descriptionArea.getText().trim().length() < 10);
        ValidationUtil.markIfInvalid(lieuField, lieuField.getText() == null || lieuField.getText().trim().length() < 2);
        ValidationUtil.markIfInvalid(statutField, statutField.getText() == null || statutField.getText().trim().length() < 3);
        ValidationUtil.markIfInvalid(dateDebutPicker, dateDebutPicker.getValue() == null);
        ValidationUtil.markIfInvalid(dateFinPicker, dateFinPicker.getValue() == null
                || (dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null
                && dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())));
        ValidationUtil.markIfInvalid(photoField, photoField.getText() != null && !photoField.getText().isBlank()
                && !photoField.getText().toLowerCase().matches(".*\\.(png|jpg|jpeg|gif|webp)$"));
    }

    private void markDuplicateFieldsIfNeeded() {
        if (ValidationUtil.isDuplicateMission(missionService.getAll(), mission == null ? 0 : mission.getId(),
                titreField.getText(), lieuField.getText(), dateDebutPicker.getValue(), dateFinPicker.getValue())) {
            ValidationUtil.markInvalid(titreField);
            ValidationUtil.markInvalid(lieuField);
            ValidationUtil.markInvalid(dateDebutPicker);
            ValidationUtil.markInvalid(dateFinPicker);
        }
    }
}
