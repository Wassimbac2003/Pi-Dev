package com.healthtrack.controllers;

import com.healthtrack.entities.MissionVolunteer;
import com.healthtrack.services.MissionVolunteerService;
import com.healthtrack.services.VolunteerService;
import com.healthtrack.util.MissionMediaUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdminMissionsPageController implements AdminPageController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Label totalLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> applicationFilterBox;
    @FXML private VBox rowsContainer;

    private final MissionVolunteerService missionService = new MissionVolunteerService();
    private final VolunteerService volunteerService = new VolunteerService();
    private AdminMainController adminMainController;

    @FXML
    private void initialize() {
        applicationFilterBox.getItems().addAll("Toutes candidatures", "Avec candidatures", "Sans candidatures");
        applicationFilterBox.getSelectionModel().selectFirst();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshRows());
        applicationFilterBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshRows());
    }

    @Override
    public void setAdminMainController(AdminMainController adminMainController) {
        this.adminMainController = adminMainController;
    }

    @Override
    public void onPageShown() {
        refreshRows();
    }

    @FXML
    private void openMissionForm() {
        if (adminMainController != null) {
            adminMainController.showAdminMissionForm(null);
        } else {
            chargerDansParent("/fxml/admin-mission-form-page.fxml");
        }
    }

    @FXML
    private void openSponsors() {
        if (adminMainController != null) {
            adminMainController.showAdminSponsorsPage();
        } else {
            chargerDansParent("/fxml/admin-sponsors-page.fxml");
        }
    }

    private void chargerDansParent(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Node content = loader.load();

            Object controller = loader.getController();
            if (controller instanceof AdminPageController adminPage) {
                adminPage.setAdminMainController(null);
                adminPage.onPageShown();
            }

            StackPane parent = (StackPane) rowsContainer.getScene().lookup("#contentArea");
            if (parent != null) {
                parent.getChildren().clear();
                parent.getChildren().add(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void refreshRows() {
        List<MissionVolunteer> missions = missionService.getAll().stream()
                .sorted(Comparator.comparingInt(MissionVolunteer::getId).reversed())
                .filter(this::matchesSearch)
                .filter(this::matchesApplicationFilter)
                .toList();

        totalLabel.setText("Total : " + missions.size() + " missions");
        rowsContainer.getChildren().clear();

        if (missions.isEmpty()) {
            VBox emptyBox = new VBox(new Label("Aucune mission trouvee."));
            emptyBox.getStyleClass().add("admin-empty-state");
            rowsContainer.getChildren().add(emptyBox);
            return;
        }

        rowsContainer.getChildren().add(buildHeaderRow());
        for (MissionVolunteer mission : missions) {
            rowsContainer.getChildren().add(buildMissionRow(mission));
        }
    }

    private boolean matchesSearch(MissionVolunteer mission) {
        String query = searchField.getText();
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        return contains(mission.getTitre(), normalized)
                || contains(mission.getDescription(), normalized)
                || contains(mission.getLieu(), normalized);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesApplicationFilter(MissionVolunteer mission) {
        boolean hasApplications = volunteerService.getAll().stream()
                .anyMatch(volunteer -> volunteer.getMissionId() == mission.getId());
        String selected = applicationFilterBox.getValue();
        if ("Avec candidatures".equals(selected)) {
            return hasApplications;
        }
        if ("Sans candidatures".equals(selected)) {
            return !hasApplications;
        }
        return true;
    }

    private GridPane buildHeaderRow() {
        GridPane header = createGrid();
        header.getStyleClass().add("admin-table-header");
        header.add(createHeader("ID"), 0, 0);
        header.add(createHeader("IMAGE"), 1, 0);
        header.add(createHeader("TITRE & DESCRIPTION"), 2, 0);
        header.add(createHeader("LIEU"), 3, 0);
        header.add(createHeader("PERIODE"), 4, 0);
        header.add(createHeader("STATUT"), 5, 0);
        header.add(createHeader("ACTIONS"), 6, 0);
        return header;
    }

    private GridPane buildMissionRow(MissionVolunteer mission) {
        GridPane row = createGrid();
        row.getStyleClass().add("admin-table-row");

        Label idLabel = new Label("#" + mission.getId());
        idLabel.getStyleClass().add("admin-id-label");

        StackPane imagePane = new StackPane();
        imagePane.getStyleClass().add("admin-avatar-wrapper");
        ImageView imageView = MissionMediaUtil.createMissionImageView(mission.getPhoto(), 38, 38);
        if (imageView != null) {
            imagePane.getChildren().add(imageView);
        } else {
            Label fallback = new Label("Mission");
            fallback.getStyleClass().add("admin-avatar-fallback");
            imagePane.getChildren().add(fallback);
        }

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(MissionMediaUtil.safeText(mission.getTitre(), "Mission"));
        titleLabel.getStyleClass().add("admin-main-value");
        Label descriptionLabel = new Label(MissionMediaUtil.safeText(mission.getDescription(), "Sans description"));
        descriptionLabel.getStyleClass().add("admin-sub-value");
        descriptionLabel.setWrapText(true);
        titleBox.getChildren().addAll(titleLabel, descriptionLabel);

        HBox locationBox = new HBox(4);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("•");
        dot.getStyleClass().add("admin-location-icon");
        Label location = new Label(MissionMediaUtil.safeText(mission.getLieu(), "Tunisie"));
        location.getStyleClass().add("admin-cell-value");
        locationBox.getChildren().addAll(dot, location);

        VBox periodBox = new VBox(2);
        periodBox.getChildren().addAll(
                createPeriodLine("DU:", mission.getDateDebut() == null ? "N/A" : mission.getDateDebut().format(DATE_FORMATTER)),
                createPeriodLine("AU:", mission.getDateFin() == null ? "N/A" : mission.getDateFin().format(DATE_FORMATTER))
        );

        Label statusPill = new Label(MissionMediaUtil.safeText(mission.getStatut(), "active"));
        statusPill.getStyleClass().addAll("admin-status-pill", statusClassFor(mission.getStatut()));

        HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER);
        Button editButton = new Button("Modifier");
        editButton.getStyleClass().addAll("admin-action-button", "admin-edit-button");
        editButton.setOnAction(event -> {
            if (adminMainController != null) {
                adminMainController.showAdminMissionForm(mission);
            } else {
                chargerDansParent("/fxml/admin-mission-form-page.fxml");
            }
        });
        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().addAll("admin-action-button", "admin-delete-button");
        deleteButton.setOnAction(event -> {
            missionService.supprimer(mission.getId());
            refreshRows();
        });
        actions.getChildren().addAll(editButton, deleteButton);

        row.add(idLabel, 0, 0);
        row.add(imagePane, 1, 0);
        row.add(titleBox, 2, 0);
        row.add(locationBox, 3, 0);
        row.add(periodBox, 4, 0);
        row.add(statusPill, 5, 0);
        row.add(actions, 6, 0);
        return row;
    }

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(12, 16, 12, 16));
        grid.setHgap(12);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(54);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setMinWidth(74);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setMinWidth(250);
        c3.setHgrow(Priority.ALWAYS);
        ColumnConstraints c4 = new ColumnConstraints();
        c4.setMinWidth(135);
        ColumnConstraints c5 = new ColumnConstraints();
        c5.setMinWidth(135);
        ColumnConstraints c6 = new ColumnConstraints();
        c6.setMinWidth(95);
        ColumnConstraints c7 = new ColumnConstraints();
        c7.setMinWidth(150);
        grid.getColumnConstraints().setAll(c1, c2, c3, c4, c5, c6, c7);
        return grid;
    }

    private Label createHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("admin-header-label");
        return label;
    }

    private VBox createPeriodLine(String labelText, String valueText) {
        VBox box = new VBox(1);
        Label label = new Label(labelText);
        label.getStyleClass().add("admin-period-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("admin-period-value");
        box.getChildren().addAll(label, value);
        return box;
    }

    private String statusClassFor(String status) {
        if (status == null) {
            return "admin-status-neutral";
        }
        String normalized = status.toLowerCase(Locale.ROOT);
        if (normalized.contains("ouverte")) {
            return "admin-status-open";
        }
        if (normalized.contains("ferm")) {
            return "admin-status-closed";
        }
        return "admin-status-neutral";
    }
}