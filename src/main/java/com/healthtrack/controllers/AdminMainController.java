package com.healthtrack.controllers;

import com.healthtrack.entities.MissionVolunteer;
import com.healthtrack.entities.Sponsor;
import com.healthtrack.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class AdminMainController {
    @FXML private StackPane adminContentPane;
    @FXML private Button benevolatAdminButton;
    @FXML private Button rendezVousAdminButton;
    @FXML private Button annoncesAdminButton;
    @FXML private Button donationsAdminButton;
    @FXML private Button forumAdminButton;

    private final List<Button> navButtons = new ArrayList<>();

    @FXML
    private void initialize() {
        navButtons.addAll(List.of(
                benevolatAdminButton,
                rendezVousAdminButton,
                annoncesAdminButton,
                donationsAdminButton,
                forumAdminButton
        ));
        showAdminMissionsPage();
    }

    @FXML
    public void showAdminMissionsPage() {
        activatePage(benevolatAdminButton, "/fxml/admin-missions-page.fxml");
    }

    @FXML
    public void showAdminSponsorsPage() {
        activatePage(annoncesAdminButton, "/fxml/admin-sponsors-page.fxml");
    }

    public void showAdminMissionForm(MissionVolunteer mission) {
        setActiveNav(benevolatAdminButton);
        AdminMissionFormPageController controller =
                (AdminMissionFormPageController) setPage("/fxml/admin-mission-form-page.fxml");
        controller.setMission(mission);
    }

    public void showAdminSponsorForm(Sponsor sponsor) {
        setActiveNav(annoncesAdminButton);
        AdminSponsorFormPageController controller =
                (AdminSponsorFormPageController) setPage("/fxml/admin-sponsor-form-page.fxml");
        controller.setSponsor(sponsor);
    }

    @FXML
    private void returnToFrontoffice() {
        AppNavigator.switchRoot("/fxml/main-view.fxml");
    }

    @FXML
    private void noop() {
        // Reserved for future admin modules.
    }

    private void activatePage(Button button, String fxmlPath) {
        setActiveNav(button);
        AdminPageController controller = setPage(fxmlPath);
        controller.onPageShown();
    }

    private AdminPageController setPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();
            Object controller = loader.getController();
            if (!(controller instanceof AdminPageController adminPageController)) {
                throw new IllegalStateException("Controller must implement AdminPageController: " + fxmlPath);
            }
            adminPageController.setAdminMainController(this);
            adminContentPane.getChildren().setAll(page);
            return adminPageController;
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de charger la vue " + fxmlPath, e);
        }
    }

    private void setActiveNav(Button activeButton) {
        for (Button button : navButtons) {
            button.getStyleClass().remove("admin-nav-button-active");
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("admin-nav-button-active")) {
            activeButton.getStyleClass().add("admin-nav-button-active");
        }
    }
}
