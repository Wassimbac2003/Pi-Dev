package com.healthtrack.controllers;

import com.healthtrack.entities.MissionVolunteer;
import com.healthtrack.entities.Volunteer;
import com.healthtrack.services.VolunteerService;
import com.healthtrack.tools.JsonUtil;
import com.healthtrack.util.AppConstants;
import com.healthtrack.util.MissionMediaUtil;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;

public class MissionApplicationPageController implements PageController {
    @FXML private Label missionNameLabel;
    @FXML private CheckBox morningWeekBox;
    @FXML private CheckBox afternoonWeekBox;
    @FXML private CheckBox eveningWeekBox;
    @FXML private CheckBox weekendMorningBox;
    @FXML private CheckBox weekendAfternoonBox;
    @FXML private TextArea motivationArea;
    @FXML private TextField phoneField;

    private final VolunteerService volunteerService = new VolunteerService();
    private MainController mainController;
    private MissionVolunteer mission;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setMission(MissionVolunteer mission) {
        this.mission = mission;
        missionNameLabel.setText("Votre profil de benevole sera transmis a l'association pour la mission : "
                + MissionMediaUtil.safeText(mission.getTitre(), "Mission") + ".");
    }

    @FXML
    private void cancelApplication() {
        if (mission != null) {
            mainController.openMissionDetails(mission);
        }
    }

    @FXML
    private void submitApplication() {
        if (mission == null || hasAlreadyApplied()) {
            return;
        }

        List<String> disponibilites = new ArrayList<>();
        if (morningWeekBox.isSelected()) {
            disponibilites.add("Semaine (Matin)");
        }
        if (afternoonWeekBox.isSelected()) {
            disponibilites.add("Semaine (Apres-midi)");
        }
        if (eveningWeekBox.isSelected()) {
            disponibilites.add("Semaine (Soir)");
        }
        if (weekendMorningBox.isSelected()) {
            disponibilites.add("Week-end (Matin)");
        }
        if (weekendAfternoonBox.isSelected()) {
            disponibilites.add("Week-end (Apres-midi)");
        }

        Volunteer volunteer = new Volunteer();
        volunteer.setMissionId(mission.getId());
        volunteer.setUserId(AppConstants.CURRENT_USER_ID);
        volunteer.setMotivation(motivationArea.getText() == null || motivationArea.getText().isBlank()
                ? "Candidature envoyee depuis l'espace benevolat."
                : motivationArea.getText().trim());
        volunteer.setTelephone(phoneField.getText() == null ? "" : phoneField.getText().trim());
        volunteer.setStatut("En attente");
        volunteer.setDisponibilitesJson(JsonUtil.toJsonArray(String.join(", ", disponibilites)));
        volunteerService.ajouter(volunteer);

        mainController.showVolunteersPage();
    }

    private boolean hasAlreadyApplied() {
        return volunteerService.getAll().stream()
                .anyMatch(volunteer -> volunteer.getMissionId() == mission.getId()
                        && volunteer.getUserId() == AppConstants.CURRENT_USER_ID);
    }
}
