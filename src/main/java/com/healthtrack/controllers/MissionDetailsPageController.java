package com.healthtrack.controllers;

import com.healthtrack.entities.MissionVolunteer;
import com.healthtrack.services.VolunteerService;
import com.healthtrack.util.AppConstants;
import com.healthtrack.util.MissionMediaUtil;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class MissionDetailsPageController implements PageController {
    @FXML private StackPane imageCard;
    @FXML private Label locationBadge;
    @FXML private Label missionTitleLabel;
    @FXML private Label dateMetaLabel;
    @FXML private Label statusMetaLabel;
    @FXML private Label descriptionLabel;
    @FXML private Button proceedButton;

    private final VolunteerService volunteerService = new VolunteerService();
    private MainController mainController;
    private MissionVolunteer mission;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setMission(MissionVolunteer mission) {
        this.mission = mission;
        renderMission();
    }

    @FXML
    private void goBack() {
        mainController.showBenevolatPage();
    }

    @FXML
    private void openApplication() {
        if (mission != null && !hasAlreadyApplied()) {
            mainController.openMissionApplication(mission);
        }
    }

    private void renderMission() {
        if (mission == null) {
            return;
        }

        imageCard.getChildren().clear();
        imageCard.getStyleClass().remove("detail-image-card-filled");
        ImageView missionImage = MissionMediaUtil.createMissionImageView(mission.getPhoto(), 760, 300);
        if (missionImage != null) {
            imageCard.getStyleClass().add("detail-image-card-filled");
            imageCard.getChildren().add(missionImage);
        } else {
            Label fallbackTitle = new Label(MissionMediaUtil.safeText(mission.getTitre(), "Mission"));
            fallbackTitle.getStyleClass().add("detail-image-title");
            imageCard.getChildren().add(fallbackTitle);
        }

        StackPane.setAlignment(locationBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(locationBadge, new Insets(14, 14, 0, 0));
        imageCard.getChildren().add(locationBadge);

        locationBadge.setText(MissionMediaUtil.safeText(mission.getLieu(), "Tunisie"));
        missionTitleLabel.setText(MissionMediaUtil.safeText(mission.getTitre(), "Mission"));
        dateMetaLabel.setText(MissionMediaUtil.buildMissionPeriodText(mission.getDateDebut(), mission.getDateFin()));
        statusMetaLabel.setText(MissionMediaUtil.safeText(mission.getStatut(), "Places limitees"));
        descriptionLabel.setText(MissionMediaUtil.safeText(mission.getDescription(), "Description de la mission."));

        boolean alreadyApplied = hasAlreadyApplied();
        proceedButton.setDisable(alreadyApplied);
        proceedButton.setText(alreadyApplied ? "Deja postule" : "Je postule maintenant !");
    }

    private boolean hasAlreadyApplied() {
        return volunteerService.getAll().stream()
                .anyMatch(volunteer -> volunteer.getMissionId() == mission.getId()
                        && volunteer.getUserId() == AppConstants.CURRENT_USER_ID);
    }
}
