package com.healthtrack.controllers;

import com.healthtrack.entities.MissionVolunteer;
import com.healthtrack.services.MissionVolunteerService;
import com.healthtrack.services.VolunteerService;
import com.healthtrack.util.AppConstants;
import com.healthtrack.util.MissionMediaUtil;
import com.healthtrack.util.PageType;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BenevolatPageController implements PageController, NavigablePageController {
    @FXML private TextField searchField;
    @FXML private HBox recommendationsRow;
    @FXML private Label countLabel;
    @FXML private TilePane missionsGrid;

    private final MissionVolunteerService missionService = new MissionVolunteerService();
    private final VolunteerService volunteerService = new VolunteerService();
    private MainController mainController;
    private List<MissionVolunteer> allMissions = List.of();

    @FXML
    private void initialize() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> render(filterMissions(newValue)));
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void setPageType(PageType pageType) {
    }

    @Override
    public void onPageShown() {
        allMissions = missionService.getAll();
        render(allMissions);
    }

    @FXML
    private void openVolunteersPage() {
        mainController.showVolunteersPage();
    }

    private void render(List<MissionVolunteer> missions) {
        updateRecommendations(missions);
        updateMissionCards(missions);
    }

    private List<MissionVolunteer> filterMissions(String query) {
        if (query == null || query.isBlank()) {
            return allMissions;
        }
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        return allMissions.stream()
                .filter(mission -> containsIgnoreCase(mission.getTitre(), normalized)
                        || containsIgnoreCase(mission.getLieu(), normalized)
                        || containsIgnoreCase(mission.getDescription(), normalized))
                .collect(Collectors.toList());
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void updateRecommendations(List<MissionVolunteer> missions) {
        recommendationsRow.getChildren().clear();
        List<MissionVolunteer> recommended = missions.stream().limit(2).collect(Collectors.toList());
        for (MissionVolunteer mission : recommended) {
            recommendationsRow.getChildren().add(createRecommendationCard(mission));
        }
        if (recommended.isEmpty()) {
            Label emptyState = new Label("Aucune recommandation disponible.");
            emptyState.getStyleClass().add("empty-hint");
            recommendationsRow.getChildren().add(emptyState);
        }
    }

    private VBox createRecommendationCard(MissionVolunteer mission) {
        VBox card = new VBox(8);
        card.getStyleClass().add("recommendation-card");
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(MissionMediaUtil.safeText(mission.getTitre(), "Mission"));
        title.getStyleClass().add("recommendation-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label score = new Label(MissionMediaUtil.buildMatchPercent(mission.getId(), mission.getTitre()) + "%");
        score.getStyleClass().add("match-score");

        Label location = new Label(MissionMediaUtil.safeText(mission.getLieu(), "Tunisie"));
        location.getStyleClass().add("recommendation-meta");
        Label subtitle = new Label("Vous avez deja participe a des missions similaires.");
        subtitle.getStyleClass().add("recommendation-meta");

        header.getChildren().addAll(title, spacer, score);
        card.getChildren().addAll(header, location, subtitle);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void updateMissionCards(List<MissionVolunteer> missions) {
        missionsGrid.getChildren().clear();
        countLabel.setText(missions.size() + " missions disponibles");
        for (MissionVolunteer mission : missions) {
            missionsGrid.getChildren().add(createMissionCard(mission));
        }
        if (missions.isEmpty()) {
            VBox emptyCard = new VBox(new Label("Aucune mission ne correspond a votre recherche."));
            emptyCard.getStyleClass().add("card-panel");
            missionsGrid.getChildren().add(emptyCard);
        }
    }

    private VBox createMissionCard(MissionVolunteer mission) {
        VBox card = new VBox();
        card.getStyleClass().add("mission-card");
        card.setPrefWidth(360);

        StackPane visual = new StackPane();
        visual.getStyleClass().add("mission-visual");
        ImageView missionImage = MissionMediaUtil.createMissionImageView(mission.getPhoto(), 360, 180);
        if (missionImage != null) {
            visual.getChildren().add(missionImage);
        }

        Label favorite = new Label("Fav");
        favorite.getStyleClass().add("favorite-badge");
        StackPane.setAlignment(favorite, Pos.TOP_LEFT);
        StackPane.setMargin(favorite, new Insets(12, 0, 0, 12));

        Label newBadge = new Label("Nouveau");
        newBadge.getStyleClass().add("new-badge");
        StackPane.setAlignment(newBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(newBadge, new Insets(12, 12, 0, 0));

        Label visualTitle = new Label(MissionMediaUtil.safeText(mission.getTitre(), "Mission"));
        visualTitle.getStyleClass().add("mission-visual-title");
        visualTitle.setVisible(missionImage == null);

        visual.getChildren().addAll(visualTitle, favorite, newBadge);

        VBox body = new VBox(12);
        body.getStyleClass().add("mission-card-body");

        HBox meta = new HBox(10);
        Label place = new Label("Lieu: " + MissionMediaUtil.safeText(mission.getLieu(), "Tunisie"));
        place.getStyleClass().add("meta-chip");
        Label date = new Label("Date: " + MissionMediaUtil.formatMissionDate(mission.getDateDebut(), mission.getDateFin()));
        date.getStyleClass().add("meta-chip");
        meta.getChildren().addAll(place, date);

        Label title = new Label(MissionMediaUtil.safeText(mission.getTitre(), "Mission"));
        title.getStyleClass().add("mission-card-title");
        title.setWrapText(true);

        Label description = new Label(MissionMediaUtil.safeText(mission.getDescription(), "Description de la mission."));
        description.getStyleClass().add("mission-card-description");
        description.setWrapText(true);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        Label status = new Label(MissionMediaUtil.safeText(mission.getStatut(), "Places limitees"));
        status.getStyleClass().add("status-pill");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button applyButton = new Button(hasAlreadyApplied(mission) ? "Deja postule" : "Postuler");
        applyButton.getStyleClass().addAll("action-button", "secondary-button", "apply-button");
        applyButton.setDisable(hasAlreadyApplied(mission));
        applyButton.setOnAction(event -> mainController.openMissionDetails(mission));
        footer.getChildren().addAll(status, spacer, applyButton);

        body.getChildren().addAll(meta, title, description, footer);
        card.getChildren().addAll(visual, body);
        return card;
    }

    private boolean hasAlreadyApplied(MissionVolunteer mission) {
        return volunteerService.getAll().stream()
                .anyMatch(volunteer -> volunteer.getMissionId() == mission.getId()
                        && volunteer.getUserId() == AppConstants.CURRENT_USER_ID);
    }
}
