package com.healthtrack.controllers;

import com.healthtrack.entities.MissionVolunteer;
import com.healthtrack.util.AppNavigator;
import com.healthtrack.util.PageType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class MainController {
    @FXML private Label pageTitleLabel;
    @FXML private StackPane contentPane;
    @FXML private Button dashboardButton;
    @FXML private Button meetingButton;
    @FXML private Button medicineButton;
    @FXML private Button statsButton;
    @FXML private Button missionsButton;
    @FXML private Button benevolatButton;
    @FXML private Button sponsorsButton;
    @FXML private Button volunteersButton;
    @FXML private Button linksButton;

    private final List<Button> navButtons = new ArrayList<>();

    @FXML
    private void initialize() {
        navButtons.addAll(List.of(
                dashboardButton,
                meetingButton,
                medicineButton,
                statsButton,
                missionsButton,
                benevolatButton,
                sponsorsButton,
                volunteersButton,
                linksButton
        ));
        showMissionsPage();
    }

    @FXML
    public void showMissionsPage() {
        activatePage(missionsButton, PageType.MISSIONS, "Gestion des missions", "/fxml/missions-page.fxml");
    }

    @FXML
    public void showRendezVousModule() {
        AppNavigator.switchRoot("/MainLayout.fxml");
    }

    @FXML
    public void showSponsorsPage() {
        activatePage(sponsorsButton, PageType.SPONSORS, "Annonces & Dons", "/fxml/sponsors-page.fxml");
    }

    @FXML
    public void showVolunteersPage() {
        activatePage(volunteersButton, PageType.VOLUNTEERS, "Mes candidatures", "/fxml/volunteers-page.fxml");
    }

    @FXML
    public void showLinksPage() {
        AppNavigator.switchRoot("/fxml/admin-main-view.fxml");
    }

    @FXML
    public void showBenevolatPage() {
        activatePage(benevolatButton, PageType.BENEVOLAT, "Missions de Benevolat", "/fxml/benevolat-page.fxml");
    }

    public void openMissionDetails(MissionVolunteer mission) {
        setActiveNav(benevolatButton);
        setPageTitle(mission.getTitre() == null || mission.getTitre().isBlank() ? "Mission" : mission.getTitre());
        MissionDetailsPageController controller = (MissionDetailsPageController) setPage("/fxml/mission-details-page.fxml");
        controller.setMission(mission);
    }

    public void openMissionApplication(MissionVolunteer mission) {
        setActiveNav(benevolatButton);
        setPageTitle("Postuler a " + (mission.getTitre() == null || mission.getTitre().isBlank() ? "la mission" : mission.getTitre()));
        MissionApplicationPageController controller =
                (MissionApplicationPageController) setPage("/fxml/mission-application-page.fxml");
        controller.setMission(mission);
    }

    public void setPageTitle(String title) {
        pageTitleLabel.setText(title);
    }

    private void activatePage(Button activeButton, PageType pageType, String title, String fxmlPath) {
        setActiveNav(activeButton);
        setPageTitle(title);
        PageController controller = setPage(fxmlPath);
        if (controller instanceof NavigablePageController navigablePageController) {
            navigablePageController.setPageType(pageType);
        }
        controller.onPageShown();
    }

    private PageController setPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();
            Object controller = loader.getController();
            if (!(controller instanceof PageController pageController)) {
                throw new IllegalStateException("Controller must implement PageController: " + fxmlPath);
            }
            pageController.setMainController(this);
            contentPane.getChildren().setAll(page);
            return pageController;
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de charger la vue " + fxmlPath, e);
        }
    }

    private void setActiveNav(Button activeButton) {
        for (Button button : navButtons) {
            button.getStyleClass().remove("active");
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }
}
