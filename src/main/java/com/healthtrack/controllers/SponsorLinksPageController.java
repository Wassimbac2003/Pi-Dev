package com.healthtrack.controllers;

import com.healthtrack.entities.MissionVolunteer;
import com.healthtrack.entities.Sponsor;
import com.healthtrack.services.MissionVolunteerService;
import com.healthtrack.services.SponsorMissionService;
import com.healthtrack.services.SponsorService;
import com.healthtrack.util.PageType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class SponsorLinksPageController implements PageController, NavigablePageController {
    @FXML private TableView<String> linksTable;
    @FXML private TableColumn<String, String> linkColumn;
    @FXML private ComboBox<Sponsor> sponsorBox;
    @FXML private ComboBox<MissionVolunteer> missionBox;

    private final SponsorMissionService sponsorMissionService = new SponsorMissionService();
    private final SponsorService sponsorService = new SponsorService();
    private final MissionVolunteerService missionService = new MissionVolunteerService();

    @FXML
    private void initialize() {
        linksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        linkColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
    }

    @Override
    public void setMainController(MainController mainController) {
    }

    @Override
    public void setPageType(PageType pageType) {
    }

    @Override
    public void onPageShown() {
        refreshData();
    }

    @FXML
    private void refreshData() {
        linksTable.setItems(FXCollections.observableArrayList(sponsorMissionService.getAllLinks()));
        sponsorBox.setItems(FXCollections.observableArrayList(sponsorService.getAll()));
        missionBox.setItems(FXCollections.observableArrayList(missionService.getAll()));
    }

    @FXML
    private void addLink() {
        Sponsor sponsor = sponsorBox.getSelectionModel().getSelectedItem();
        MissionVolunteer mission = missionBox.getSelectionModel().getSelectedItem();
        if (sponsor == null || mission == null) {
            return;
        }
        sponsorMissionService.ajouterLien(sponsor.getId(), mission.getId());
        refreshData();
    }

    @FXML
    private void deleteLink() {
        Sponsor sponsor = sponsorBox.getSelectionModel().getSelectedItem();
        MissionVolunteer mission = missionBox.getSelectionModel().getSelectedItem();
        if (sponsor == null || mission == null) {
            return;
        }
        sponsorMissionService.supprimerLien(sponsor.getId(), mission.getId());
        refreshData();
    }
}
