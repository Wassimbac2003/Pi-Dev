package com.healthtrack.controllers;

import com.healthtrack.entities.MissionVolunteer;
import com.healthtrack.services.MissionVolunteerService;
import com.healthtrack.util.MissionMediaUtil;
import com.healthtrack.util.PageType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class MissionsPageController implements PageController, NavigablePageController {
    @FXML private TableView<MissionVolunteer> missionsTable;
    @FXML private TableColumn<MissionVolunteer, Integer> idColumn;
    @FXML private TableColumn<MissionVolunteer, String> titreColumn;
    @FXML private TableColumn<MissionVolunteer, String> lieuColumn;
    @FXML private TableColumn<MissionVolunteer, LocalDate> dateDebutColumn;
    @FXML private TableColumn<MissionVolunteer, LocalDate> dateFinColumn;
    @FXML private TableColumn<MissionVolunteer, String> statutColumn;
    @FXML private TableColumn<MissionVolunteer, String> photoColumn;
    @FXML private TextField titreField;
    @FXML private TextField descriptionField;
    @FXML private TextField lieuField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField statutField;
    @FXML private TextField photoField;
    @FXML private Label countLabel;
    @FXML private Button browsePhotoButton;

    private final MissionVolunteerService missionService = new MissionVolunteerService();

    @FXML
    private void initialize() {
        missionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        lieuColumn.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        dateDebutColumn.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        dateFinColumn.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        photoColumn.setCellValueFactory(new PropertyValueFactory<>("photo"));
        missionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, mission) -> fillForm(mission));
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
    private void browsePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selectionner une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File selectedFile = fileChooser.showOpenDialog(browsePhotoButton.getScene().getWindow());
        if (selectedFile != null) {
            photoField.setText(MissionMediaUtil.importMissionPhoto(selectedFile));
        }
    }

    @FXML
    private void refreshTable() {
        List<MissionVolunteer> missions = missionService.getAll();
        missionsTable.setItems(FXCollections.observableArrayList(missions));
        countLabel.setText(missions.size() + " missions disponibles");
    }

    @FXML
    private void addMission() {
        MissionVolunteer mission = buildMissionFromForm(new MissionVolunteer());
        missionService.ajouter(mission);
        refreshTable();
        clearForm();
    }

    @FXML
    private void updateMission() {
        MissionVolunteer mission = missionsTable.getSelectionModel().getSelectedItem();
        if (mission == null) {
            return;
        }
        missionService.modifier(buildMissionFromForm(mission));
        refreshTable();
    }

    @FXML
    private void deleteMission() {
        MissionVolunteer mission = missionsTable.getSelectionModel().getSelectedItem();
        if (mission == null) {
            return;
        }
        missionService.supprimer(mission.getId());
        refreshTable();
        clearForm();
    }

    private MissionVolunteer buildMissionFromForm(MissionVolunteer mission) {
        mission.setTitre(titreField.getText());
        mission.setDescription(descriptionField.getText());
        mission.setLieu(lieuField.getText());
        mission.setDateDebut(dateDebutPicker.getValue());
        mission.setDateFin(dateFinPicker.getValue());
        mission.setStatut(statutField.getText());
        mission.setPhoto(photoField.getText() == null || photoField.getText().isBlank() ? null : photoField.getText().trim());
        return mission;
    }

    private void fillForm(MissionVolunteer mission) {
        if (mission == null) {
            return;
        }
        titreField.setText(mission.getTitre());
        descriptionField.setText(mission.getDescription());
        lieuField.setText(mission.getLieu());
        dateDebutPicker.setValue(mission.getDateDebut());
        dateFinPicker.setValue(mission.getDateFin());
        statutField.setText(mission.getStatut());
        photoField.setText(mission.getPhoto() == null ? "" : mission.getPhoto());
    }

    private void clearForm() {
        titreField.clear();
        descriptionField.clear();
        lieuField.clear();
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        statutField.setText("Ouverte");
        photoField.clear();
        missionsTable.getSelectionModel().clearSelection();
    }
}
