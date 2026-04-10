package com.healthtrack.controllers;

import com.healthtrack.entities.Volunteer;
import com.healthtrack.services.MissionVolunteerService;
import com.healthtrack.services.VolunteerService;
import com.healthtrack.tools.JsonUtil;
import com.healthtrack.util.PageType;
import com.healthtrack.util.UiMessageUtil;
import com.healthtrack.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class VolunteersPageController implements PageController, NavigablePageController {
    @FXML private TableView<Volunteer> volunteersTable;
    @FXML private TableColumn<Volunteer, Integer> idColumn;
    @FXML private TableColumn<Volunteer, String> motivationColumn;
    @FXML private TableColumn<Volunteer, String> telephoneColumn;
    @FXML private TableColumn<Volunteer, String> statutColumn;
    @FXML private TableColumn<Volunteer, Integer> userIdColumn;
    @FXML private TableColumn<Volunteer, Integer> missionIdColumn;
    @FXML private TableColumn<Volunteer, String> disponibilitesColumn;
    @FXML private TextField motivationField;
    @FXML private TextField telephoneField;
    @FXML private TextField statutField;
    @FXML private TextField userIdField;
    @FXML private TextField missionIdField;
    @FXML private TextField disponibilitesField;

    private final VolunteerService volunteerService = new VolunteerService();
    private final MissionVolunteerService missionService = new MissionVolunteerService();

    @FXML
    private void initialize() {
        volunteersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        motivationColumn.setCellValueFactory(new PropertyValueFactory<>("motivation"));
        telephoneColumn.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        missionIdColumn.setCellValueFactory(new PropertyValueFactory<>("missionId"));
        disponibilitesColumn.setCellValueFactory(new PropertyValueFactory<>("disponibilitesCsv"));
        volunteersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, volunteer) -> fillForm(volunteer));
    }

    @Override
    public void setMainController(MainController mainController) {
    }

    @Override
    public void setPageType(PageType pageType) {
    }

    @Override
    public void onPageShown() {
        refreshTable();
    }

    @FXML
    private void refreshTable() {
        volunteersTable.setItems(FXCollections.observableArrayList(volunteerService.getAll()));
    }

    @FXML
    private void addVolunteer() {
        ValidationUtil.clearInvalid(motivationField, telephoneField, statutField, userIdField, missionIdField, disponibilitesField);
        int missionId = parseInt(missionIdField.getText());
        var errors = ValidationUtil.validateVolunteer(
                motivationField.getText(),
                telephoneField.getText(),
                statutField.getText(),
                parseInt(userIdField.getText()),
                missionId,
                disponibilitesField.getText()
        );
        if (missionId > 0 && missionService.getOneById(missionId) == null) {
            errors.add("La mission referencee n'existe pas.");
        }
        if (!errors.isEmpty()) {
            markVolunteerInvalidFields();
            UiMessageUtil.showValidationErrors("Ajout volontaire", errors);
            return;
        }
        Volunteer volunteer = buildVolunteer(new Volunteer());
        volunteerService.ajouter(volunteer);
        refreshTable();
        clearForm();
    }

    @FXML
    private void updateVolunteer() {
        Volunteer volunteer = volunteersTable.getSelectionModel().getSelectedItem();
        if (volunteer == null) {
            UiMessageUtil.showInfo("Modification volontaire", "Selectionnez un volontaire a modifier.");
            return;
        }
        ValidationUtil.clearInvalid(motivationField, telephoneField, statutField, userIdField, missionIdField, disponibilitesField);
        int missionId = parseInt(missionIdField.getText());
        var errors = ValidationUtil.validateVolunteer(
                motivationField.getText(),
                telephoneField.getText(),
                statutField.getText(),
                parseInt(userIdField.getText()),
                missionId,
                disponibilitesField.getText()
        );
        if (missionId > 0 && missionService.getOneById(missionId) == null) {
            errors.add("La mission referencee n'existe pas.");
        }
        if (!errors.isEmpty()) {
            markVolunteerInvalidFields();
            UiMessageUtil.showValidationErrors("Modification volontaire", errors);
            return;
        }
        volunteerService.modifier(buildVolunteer(volunteer));
        refreshTable();
    }

    @FXML
    private void deleteVolunteer() {
        Volunteer volunteer = volunteersTable.getSelectionModel().getSelectedItem();
        if (volunteer == null) {
            UiMessageUtil.showInfo("Suppression volontaire", "Selectionnez un volontaire a supprimer.");
            return;
        }
        volunteerService.supprimer(volunteer.getId());
        refreshTable();
        clearForm();
    }

    private Volunteer buildVolunteer(Volunteer volunteer) {
        volunteer.setMotivation(motivationField.getText());
        volunteer.setTelephone(telephoneField.getText());
        volunteer.setStatut(statutField.getText());
        volunteer.setUserId(parseInt(userIdField.getText()));
        volunteer.setMissionId(parseInt(missionIdField.getText()));
        volunteer.setDisponibilitesJson(JsonUtil.toJsonArray(disponibilitesField.getText()));
        return volunteer;
    }

    private void fillForm(Volunteer volunteer) {
        if (volunteer == null) {
            return;
        }
        motivationField.setText(volunteer.getMotivation());
        telephoneField.setText(volunteer.getTelephone());
        statutField.setText(volunteer.getStatut());
        userIdField.setText(String.valueOf(volunteer.getUserId()));
        missionIdField.setText(String.valueOf(volunteer.getMissionId()));
        disponibilitesField.setText(volunteer.getDisponibilitesCsv());
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    private void clearForm() {
        ValidationUtil.clearInvalid(motivationField, telephoneField, statutField, userIdField, missionIdField, disponibilitesField);
        motivationField.clear();
        telephoneField.clear();
        statutField.setText("En attente");
        userIdField.clear();
        missionIdField.clear();
        disponibilitesField.clear();
        volunteersTable.getSelectionModel().clearSelection();
    }

    private void markVolunteerInvalidFields() {
        ValidationUtil.markIfInvalid(motivationField, motivationField.getText() == null || motivationField.getText().trim().length() < 10);
        ValidationUtil.markIfInvalid(telephoneField, telephoneField.getText() == null
                || !telephoneField.getText().replaceAll("\\s+", "").matches("^(\\+\\d{1,3})?\\d{8,14}$"));
        ValidationUtil.markIfInvalid(statutField, statutField.getText() == null || statutField.getText().trim().length() < 3);
        ValidationUtil.markIfInvalid(userIdField, parseInt(userIdField.getText()) <= 0);
        ValidationUtil.markIfInvalid(missionIdField, parseInt(missionIdField.getText()) <= 0 || missionService.getOneById(parseInt(missionIdField.getText())) == null);
        ValidationUtil.markIfInvalid(disponibilitesField, disponibilitesField.getText() == null || disponibilitesField.getText().isBlank());
    }
}
