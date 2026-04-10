package com.healthtrack.controllers;

import com.healthtrack.entities.Sponsor;
import com.healthtrack.services.SponsorService;
import com.healthtrack.util.PageType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class SponsorsPageController implements PageController, NavigablePageController {
    @FXML private TableView<Sponsor> sponsorsTable;
    @FXML private TableColumn<Sponsor, Integer> idColumn;
    @FXML private TableColumn<Sponsor, String> nomColumn;
    @FXML private TableColumn<Sponsor, String> emailColumn;
    @FXML private TableColumn<Sponsor, String> logoColumn;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private TextField logoField;

    private final SponsorService sponsorService = new SponsorService();

    @FXML
    private void initialize() {
        sponsorsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nomSociete"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("contactEmail"));
        logoColumn.setCellValueFactory(new PropertyValueFactory<>("logo"));
        sponsorsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, sponsor) -> fillForm(sponsor));
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
        sponsorsTable.setItems(FXCollections.observableArrayList(sponsorService.getAll()));
    }

    @FXML
    private void addSponsor() {
        Sponsor sponsor = buildSponsor(new Sponsor());
        sponsorService.ajouter(sponsor);
        refreshTable();
        clearForm();
    }

    @FXML
    private void updateSponsor() {
        Sponsor sponsor = sponsorsTable.getSelectionModel().getSelectedItem();
        if (sponsor == null) {
            return;
        }
        sponsorService.modifier(buildSponsor(sponsor));
        refreshTable();
    }

    @FXML
    private void deleteSponsor() {
        Sponsor sponsor = sponsorsTable.getSelectionModel().getSelectedItem();
        if (sponsor == null) {
            return;
        }
        sponsorService.supprimer(sponsor.getId());
        refreshTable();
        clearForm();
    }

    private Sponsor buildSponsor(Sponsor sponsor) {
        sponsor.setNomSociete(nomField.getText());
        sponsor.setContactEmail(emailField.getText());
        sponsor.setLogo(logoField.getText() == null || logoField.getText().isBlank() ? null : logoField.getText().trim());
        return sponsor;
    }

    private void fillForm(Sponsor sponsor) {
        if (sponsor == null) {
            return;
        }
        nomField.setText(sponsor.getNomSociete());
        emailField.setText(sponsor.getContactEmail());
        logoField.setText(sponsor.getLogo() == null ? "" : sponsor.getLogo());
    }

    private void clearForm() {
        nomField.clear();
        emailField.clear();
        logoField.clear();
        sponsorsTable.getSelectionModel().clearSelection();
    }
}
