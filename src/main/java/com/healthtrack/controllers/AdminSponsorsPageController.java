package com.healthtrack.controllers;

import com.healthtrack.entities.Sponsor;
import com.healthtrack.services.SponsorService;
import com.healthtrack.util.MissionMediaUtil;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdminSponsorsPageController implements AdminPageController {
    @FXML private Label countLabel;
    @FXML private TextField searchField;
    @FXML private VBox rowsContainer;

    private final SponsorService sponsorService = new SponsorService();
    private AdminMainController adminMainController;

    @FXML
    private void initialize() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshRows());
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
    private void openSponsorForm() {
        adminMainController.showAdminSponsorForm(null);
    }

    @FXML
    private void refreshRows() {
        List<Sponsor> sponsors = sponsorService.getAll().stream()
                .sorted(Comparator.comparingInt(Sponsor::getId).reversed())
                .filter(this::matchesSearch)
                .toList();

        countLabel.setText(sponsors.size() + " sponsors enregistres");
        rowsContainer.getChildren().clear();

        if (sponsors.isEmpty()) {
            VBox emptyBox = new VBox(new Label("Aucun sponsor ne correspond a votre recherche."));
            emptyBox.getStyleClass().add("admin-empty-state");
            rowsContainer.getChildren().add(emptyBox);
            return;
        }

        rowsContainer.getChildren().add(buildHeaderRow());
        for (Sponsor sponsor : sponsors) {
            rowsContainer.getChildren().add(buildSponsorRow(sponsor));
        }
    }

    private boolean matchesSearch(Sponsor sponsor) {
        String query = searchField.getText();
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        return contains(sponsor.getNomSociete(), normalized) || contains(sponsor.getContactEmail(), normalized);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private GridPane buildHeaderRow() {
        GridPane header = createGrid();
        header.getStyleClass().add("admin-table-header");
        header.add(createHeader("LOGO"), 0, 0);
        header.add(createHeader("ENTREPRISE"), 1, 0);
        header.add(createHeader("CONTACT"), 2, 0);
        header.add(createHeader("ACTIONS"), 3, 0);
        return header;
    }

    private GridPane buildSponsorRow(Sponsor sponsor) {
        GridPane row = createGrid();
        row.getStyleClass().add("admin-table-row");

        StackPane logoPane = new StackPane();
        logoPane.getStyleClass().add("admin-logo-wrapper");
        ImageView imageView = MissionMediaUtil.createMissionImageView(sponsor.getLogo(), 52, 52);
        if (imageView != null) {
            logoPane.getChildren().add(imageView);
        } else {
            Label fallback = new Label("Logo");
            fallback.getStyleClass().add("admin-avatar-fallback");
            logoPane.getChildren().add(fallback);
        }

        Label companyLabel = new Label(MissionMediaUtil.safeText(sponsor.getNomSociete(), "Sponsor"));
        companyLabel.getStyleClass().add("admin-main-value");

        Label contactLabel = new Label(MissionMediaUtil.safeText(sponsor.getContactEmail(), "Aucun email"));
        contactLabel.getStyleClass().add("admin-cell-value");

        VBox actions = new VBox();
        Button editButton = new Button("Modifier");
        editButton.getStyleClass().addAll("admin-action-button", "admin-edit-button");
        editButton.setOnAction(event -> adminMainController.showAdminSponsorForm(sponsor));
        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().addAll("admin-action-button", "admin-delete-button");
        deleteButton.setOnAction(event -> {
            sponsorService.supprimer(sponsor.getId());
            refreshRows();
        });
        GridPane actionGrid = new GridPane();
        actionGrid.setHgap(8);
        actionGrid.add(editButton, 0, 0);
        actionGrid.add(deleteButton, 1, 0);
        actions.getChildren().add(actionGrid);

        row.add(logoPane, 0, 0);
        row.add(companyLabel, 1, 0);
        row.add(contactLabel, 2, 0);
        row.add(actions, 3, 0);
        return row;
    }

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(18, 24, 18, 24));
        grid.setAlignment(Pos.CENTER_LEFT);
        grid.setHgap(24);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(100);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setMinWidth(360);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setMinWidth(260);
        ColumnConstraints c4 = new ColumnConstraints();
        c4.setMinWidth(170);
        grid.getColumnConstraints().setAll(c1, c2, c3, c4);
        return grid;
    }

    private Label createHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("admin-header-label");
        return label;
    }
}
