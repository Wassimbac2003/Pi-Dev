package com.healthtrack.controllers;

import com.healthtrack.entities.MissionVolunteer;
import com.healthtrack.entities.Sponsor;
import com.healthtrack.services.MissionVolunteerService;
import com.healthtrack.services.SponsorMissionService;
import com.healthtrack.services.SponsorService;
import com.healthtrack.util.MissionMediaUtil;
import com.healthtrack.util.UiMessageUtil;
import com.healthtrack.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AdminSponsorFormPageController implements AdminPageController {
    @FXML private Label pageTitleLabel;
    @FXML private TextField companyField;
    @FXML private TextField emailField;
    @FXML private TextField logoField;
    @FXML private TextField missionSearchField;
    @FXML private Label selectedCountLabel;
    @FXML private VBox missionSelectionContainer;
    @FXML private Button logoButton;

    private final SponsorService sponsorService = new SponsorService();
    private final MissionVolunteerService missionService = new MissionVolunteerService();
    private final SponsorMissionService sponsorMissionService = new SponsorMissionService();

    private AdminMainController adminMainController;
    private Sponsor sponsor;
    private List<MissionVolunteer> allMissions = List.of();
    private final Set<Integer> selectedMissionIds = new HashSet<>();
    private int visibleMissionCount = 8;

    @FXML
    private void initialize() {
        missionSearchField.textProperty().addListener((obs, oldValue, newValue) -> refreshMissionSelection());
    }

    @Override
    public void setAdminMainController(AdminMainController adminMainController) {
        this.adminMainController = adminMainController;
    }

    public void setSponsor(Sponsor sponsor) {
        this.sponsor = sponsor;
        this.allMissions = missionService.getAll().stream()
                .sorted(Comparator.comparingInt(MissionVolunteer::getId).reversed())
                .toList();
        selectedMissionIds.clear();

        if (sponsor != null) {
            pageTitleLabel.setText("Modifier un Sponsor");
            companyField.setText(sponsor.getNomSociete());
            emailField.setText(sponsor.getContactEmail());
            logoField.setText(sponsor.getLogo());
            selectedMissionIds.addAll(sponsorMissionService.getMissionIdsForSponsor(sponsor.getId()));
        } else {
            pageTitleLabel.setText("Ajouter un Sponsor");
        }
        refreshMissionSelection();
    }

    @FXML
    private void chooseLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un logo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File selectedFile = fileChooser.showOpenDialog(logoButton.getScene().getWindow());
        if (selectedFile != null) {
            logoField.setText(MissionMediaUtil.importMissionPhoto(selectedFile));
        }
    }

    @FXML
    private void loadMore() {
        visibleMissionCount += 6;
        refreshMissionSelection();
    }

    @FXML
    private void saveSponsor() {
        ValidationUtil.clearInvalid(companyField, emailField, logoField, missionSearchField);
        var errors = ValidationUtil.validateSponsor(
                companyField.getText(),
                emailField.getText(),
                logoField.getText(),
                true,
                selectedMissionIds.size()
        );
        if (ValidationUtil.isDuplicateSponsor(sponsorService.getAll(), sponsor == null ? 0 : sponsor.getId(),
                companyField.getText(), emailField.getText())) {
            errors.add(sponsor == null
                    ? "Un sponsor avec ce nom ou cet email existe deja."
                    : "Un autre sponsor avec ce nom ou cet email existe deja.");
        }
        if (!errors.isEmpty()) {
            markInvalidFields();
            markDuplicateFieldsIfNeeded();
            UiMessageUtil.showValidationErrors("Sponsor backOffice", errors);
            return;
        }

        Sponsor current = sponsor == null ? new Sponsor() : sponsor;
        current.setNomSociete(companyField.getText());
        current.setContactEmail(emailField.getText());
        current.setLogo(logoField.getText() == null || logoField.getText().isBlank() ? null : logoField.getText().trim());

        if (sponsor == null) {
            sponsorService.ajouter(current);
        } else {
            sponsorService.modifier(current);
            sponsorMissionService.clearLinksForSponsor(current.getId());
        }

        for (Integer missionId : selectedMissionIds) {
            sponsorMissionService.ajouterLien(current.getId(), missionId);
        }
        adminMainController.showAdminSponsorsPage();
    }

    @FXML
    private void cancel() {
        adminMainController.showAdminSponsorsPage();
    }

    private void refreshMissionSelection() {
        List<MissionVolunteer> filtered = allMissions.stream()
                .filter(this::matchesSearch)
                .limit(visibleMissionCount)
                .toList();

        missionSelectionContainer.getChildren().clear();
        for (MissionVolunteer mission : filtered) {
            missionSelectionContainer.getChildren().add(createMissionSelectionCard(mission));
        }
        selectedCountLabel.setText(selectedMissionIds.size() + " mission selectionnee");
        if (selectedMissionIds.size() > 1) {
            selectedCountLabel.setText(selectedMissionIds.size() + " missions selectionnees");
        }
    }

    private boolean matchesSearch(MissionVolunteer mission) {
        String query = missionSearchField.getText();
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        return contains(mission.getTitre(), normalized)
                || contains(mission.getLieu(), normalized)
                || contains(mission.getDescription(), normalized);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private VBox createMissionSelectionCard(MissionVolunteer mission) {
        VBox card = new VBox(4);
        card.getStyleClass().add("admin-selection-card");
        card.setPadding(new Insets(12, 14, 12, 14));

        HBox header = new HBox(10);
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(selectedMissionIds.contains(mission.getId()));
        checkBox.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (selected) {
                selectedMissionIds.add(mission.getId());
            } else {
                selectedMissionIds.remove(mission.getId());
            }
            selectedCountLabel.setText(selectedMissionIds.size()
                    + (selectedMissionIds.size() > 1 ? " missions selectionnees" : " mission selectionnee"));
        });
        Label title = new Label(MissionMediaUtil.safeText(mission.getTitre(), "Mission"));
        title.getStyleClass().add("admin-main-value");
        header.getChildren().addAll(checkBox, title);

        Label meta = new Label(MissionMediaUtil.safeText(mission.getLieu(), "Tunisie") + " - "
                + MissionMediaUtil.safeText(mission.getStatut(), "active"));
        meta.getStyleClass().add("admin-sub-value");

        card.getChildren().addAll(header, meta);
        return card;
    }

    private void markInvalidFields() {
        ValidationUtil.markIfInvalid(companyField, companyField.getText() == null || companyField.getText().trim().length() < 2);
        ValidationUtil.markIfInvalid(emailField, emailField.getText() == null
                || !emailField.getText().trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));
        ValidationUtil.markIfInvalid(logoField, logoField.getText() != null && !logoField.getText().isBlank()
                && !logoField.getText().toLowerCase().matches(".*\\.(png|jpg|jpeg|gif|webp)$"));
        ValidationUtil.markIfInvalid(missionSearchField, selectedMissionIds.isEmpty());
    }

    private void markDuplicateFieldsIfNeeded() {
        if (ValidationUtil.isDuplicateSponsor(sponsorService.getAll(), sponsor == null ? 0 : sponsor.getId(),
                companyField.getText(), emailField.getText())) {
            ValidationUtil.markInvalid(companyField);
            ValidationUtil.markInvalid(emailField);
        }
    }
}
