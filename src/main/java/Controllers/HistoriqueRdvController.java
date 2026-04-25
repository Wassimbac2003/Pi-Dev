package Controllers;

import Models.rdv;
import Services.RdvService;
import Services.PdfExportService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javafx.stage.FileChooser;
import java.io.File;

public class HistoriqueRdvController {

    @FXML
    private VBox rootContainer;

    private final RdvService rdvService = new RdvService();
    private List<rdv> historiqueRdvs;

    @FXML
    public void initialize() {
        List<rdv> tousLesRdv;
        try {
            tousLesRdv = rdvService.findAll();
        } catch (SQLException e) {
            System.err.println("Erreur chargement historique : " + e.getMessage());
            return;
        }

        LocalDate today = LocalDate.now();

        historiqueRdvs = tousLesRdv.stream()
                .filter(r -> {
                    LocalDate dateRdv = LocalDate.parse(r.getDate());
                    return dateRdv.isBefore(today) || r.getStatut().equalsIgnoreCase("Annulé");
                })
                .sorted((a, b) -> LocalDate.parse(b.getDate()).compareTo(LocalDate.parse(a.getDate())))
                .collect(Collectors.toList());

        int total = historiqueRdvs.size();
        int confirmes = (int) historiqueRdvs.stream().filter(r -> r.getStatut().equalsIgnoreCase("Confirmé")).count();
        int annules = (int) historiqueRdvs.stream().filter(r -> r.getStatut().equalsIgnoreCase("Annulé")).count();
        int expires = total - confirmes - annules;

        rootContainer.getChildren().clear();
        rootContainer.getChildren().add(buildHeader());
        rootContainer.getChildren().add(buildStatsBar(total, confirmes, annules, expires));

        if (historiqueRdvs.isEmpty()) {
            rootContainer.getChildren().add(buildEmptyState());
        } else {
            rootContainer.getChildren().add(buildTimeline(historiqueRdvs));
        }
    }

    // ======================== HEADER ========================
    private HBox buildHeader() {
        Label icon = new Label("🕓");
        icon.setStyle("-fx-font-size: 22;");

        Label title = new Label("Historique des RDV");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#1e293b"));

        Label subtitle = new Label("Tous vos rendez-vous passés");
        subtitle.setStyle("-fx-font-size: 13; -fx-text-fill: #94a3b8;");

        VBox titleBox = new VBox(2, new HBox(8, icon, title), subtitle);
        ((HBox) titleBox.getChildren().get(0)).setAlignment(Pos.CENTER_LEFT);

        Button btnRetour = new Button("← Retour");
        btnRetour.setStyle(
                "-fx-background-color: white; -fx-text-fill: #475569; -fx-font-size: 13; " +
                        "-fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 12; " +
                        "-fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-cursor: hand;"
        );
        btnRetour.setCursor(Cursor.HAND);
        btnRetour.setOnAction(e -> retourListeRdv());

        Button btnPdf = new Button("📄 Exporter PDF");
        btnPdf.setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 13; " +
                        "-fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 12; -fx-cursor: hand;"
        );
        btnPdf.setCursor(Cursor.HAND);
        btnPdf.setOnAction(e -> exporterPdf(historiqueRdvs));

        HBox buttonsBox = new HBox(10, btnRetour, btnPdf);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(titleBox, spacer, buttonsBox);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    // ======================== STATS CARDS ========================
    private HBox buildStatsBar(int total, int confirmes, int annules, int expires) {
        HBox stats = new HBox(15,
                buildStatCard(String.valueOf(total), "Total RDV", "#2563eb"),
                buildStatCard(String.valueOf(confirmes), "Confirmés", "#16a34a"),
                buildStatCard(String.valueOf(annules), "Annulés", "#ef4444"),
                buildStatCard(String.valueOf(expires), "Expirés", "#94a3b8")
        );
        stats.setAlignment(Pos.CENTER_LEFT);
        return stats;
    }

    private VBox buildStatCard(String value, String label, String color) {
        Label valLabel = new Label(value);
        valLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        valLabel.setTextFill(Color.web(color));

        Label descLabel = new Label(label);
        descLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        VBox card = new VBox(4, valLabel, descLabel);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(180);
        card.setPrefHeight(85);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; " +
                        "-fx-border-color: #f1f5f9; -fx-border-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);"
        );
        card.setPadding(new Insets(15));
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // ======================== EMPTY STATE ========================
    private VBox buildEmptyState() {
        Label emptyIcon = new Label("🕓");
        emptyIcon.setStyle("-fx-font-size: 48; -fx-opacity: 0.3;");

        Label emptyText = new Label("Aucun historique pour le moment.");
        emptyText.setStyle("-fx-font-size: 16; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        VBox empty = new VBox(15, emptyIcon, emptyText);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(60));
        return empty;
    }

    // ======================== TIMELINE ========================
    private VBox buildTimeline(List<rdv> rdvs) {
        VBox timeline = new VBox(0);

        for (int i = 0; i < rdvs.size(); i++) {
            rdv r = rdvs.get(i);
            boolean isLast = (i == rdvs.size() - 1);
            timeline.getChildren().add(buildTimelineItem(r, isLast));
        }

        return timeline;
    }

    private HBox buildTimelineItem(rdv r, boolean isLast) {
        String statut = r.getStatut();
        String badgeLabel;
        String badgeBg, badgeText, badgeBorder;
        String dotColor;

        if (statut.equalsIgnoreCase("Confirmé")) {
            badgeLabel = "✓ Confirmé";
            badgeBg = "#dcfce7"; badgeText = "#15803d"; badgeBorder = "#bbf7d0";
            dotColor = "#16a34a";
        } else if (statut.equalsIgnoreCase("Annulé")) {
            badgeLabel = "✗ Annulé";
            badgeBg = "#fee2e2"; badgeText = "#b91c1c"; badgeBorder = "#fecaca";
            dotColor = "#ef4444";
        } else {
            badgeLabel = "⏰ Expiré";
            badgeBg = "#f1f5f9"; badgeText = "#64748b"; badgeBorder = "#cbd5e1";
            dotColor = "#94a3b8";
        }

        LocalDate dateRdv = LocalDate.parse(r.getDate());
        String mois = dateRdv.format(DateTimeFormatter.ofPattern("MMM", Locale.FRENCH)).toUpperCase();
        String jour = String.valueOf(dateRdv.getDayOfMonth());

        Label moisLabel = new Label(mois);
        moisLabel.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        Label jourLabel = new Label(jour);
        jourLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        jourLabel.setTextFill(Color.web("#334155"));

        VBox dateBox = new VBox(0, moisLabel, jourLabel);
        dateBox.setAlignment(Pos.CENTER);
        dateBox.setPrefWidth(56);
        dateBox.setPrefHeight(56);
        dateBox.setMinWidth(56);
        dateBox.setMaxWidth(56);
        dateBox.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; " +
                        "-fx-border-color: #e2e8f0; -fx-border-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 4, 0, 0, 1);"
        );

        Circle dot = new Circle(6, Color.web(dotColor));
        dot.setStroke(Color.WHITE);
        dot.setStrokeWidth(2);

        Line lineTop = new Line(0, 0, 0, 24);
        lineTop.setStroke(Color.web("#cbd5e1"));
        lineTop.setStrokeWidth(1.5);

        Line lineBottom = new Line(0, 0, 0, 24);
        lineBottom.setStroke(isLast ? Color.TRANSPARENT : Color.web("#cbd5e1"));
        lineBottom.setStrokeWidth(1.5);

        VBox connector = new VBox(0, lineTop, dot, lineBottom);
        connector.setAlignment(Pos.CENTER);
        connector.setMinWidth(20);

        Label medecinLabel = new Label(r.getMedecin());
        medecinLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        medecinLabel.setTextFill(Color.web("#1e293b"));

        String motifText = (r.getMotif() != null && !r.getMotif().isEmpty()) ? r.getMotif() : "Consultation";
        Label motifLabel = new Label(motifText);
        motifLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #64748b;");

        String heureDebut = r.getHdebut().length() > 5 ? r.getHdebut().substring(0, 5) : r.getHdebut();
        String heureFin = r.getHfin().length() > 5 ? r.getHfin().substring(0, 5) : r.getHfin();
        Label heureLabel = new Label("🕐 " + heureDebut + " - " + heureFin);
        heureLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8;");

        HBox detailsRow = new HBox(15, heureLabel);
        detailsRow.setAlignment(Pos.CENTER_LEFT);
        if (r.getMessage() != null && !r.getMessage().isEmpty()) {
            Label msgLabel = new Label("💬 " + r.getMessage());
            msgLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8;");
            detailsRow.getChildren().add(msgLabel);
        }

        VBox infoBox = new VBox(3, medecinLabel, motifLabel, detailsRow);

        Label badge = new Label(badgeLabel);
        badge.setStyle(
                "-fx-font-size: 11; -fx-font-weight: bold; -fx-padding: 4 12; " +
                        "-fx-background-radius: 20; -fx-border-radius: 20; " +
                        "-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeText + "; " +
                        "-fx-border-color: " + badgeBorder + ";"
        );

        VBox badgeBox = new VBox(4, badge);
        badgeBox.setAlignment(Pos.TOP_RIGHT);

        if (!statut.equalsIgnoreCase("Confirmé") && !statut.equalsIgnoreCase("Annulé")) {
            Label noteLabel = new Label("Le médecin n'a pas répondu");
            noteLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
            badgeBox.getChildren().add(noteLabel);
        }

        Region cardSpacer = new Region();
        HBox.setHgrow(cardSpacer, Priority.ALWAYS);

        HBox cardTop = new HBox(10, infoBox, cardSpacer, badgeBox);
        cardTop.setAlignment(Pos.TOP_LEFT);

        Button btnQr = new Button("📱 QR Code");
        btnQr.setStyle(
                "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-font-size: 12; " +
                        "-fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 10; " +
                        "-fx-border-color: transparent; -fx-cursor: hand;"
        );
        btnQr.setCursor(Cursor.HAND);

        Button btnDetails = new Button("👁 Détails");
        btnDetails.setStyle(
                "-fx-background-color: #f8fafc; -fx-text-fill: #475569; -fx-font-size: 12; " +
                        "-fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 10; " +
                        "-fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-cursor: hand;"
        );
        btnDetails.setCursor(Cursor.HAND);
        btnDetails.setOnAction(e -> ouvrirDetails(r));

        HBox buttonsRow = new HBox(8, btnQr, btnDetails);
        buttonsRow.setPadding(new Insets(12, 0, 0, 0));
        buttonsRow.setStyle("-fx-border-color: #f1f5f9; -fx-border-width: 1 0 0 0;");

        VBox cardContent = new VBox(10, cardTop, buttonsRow);
        cardContent.setPadding(new Insets(18));

        String cardOpacity = (!statut.equalsIgnoreCase("Confirmé") && !statut.equalsIgnoreCase("Annulé"))
                ? "-fx-opacity: 0.75;" : "";

        VBox card = new VBox(cardContent);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; " +
                        "-fx-border-color: #f1f5f9; -fx-border-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);" +
                        cardOpacity
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; " +
                        "-fx-border-color: #bfdbfe; -fx-border-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 3);" +
                        cardOpacity
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; " +
                        "-fx-border-color: #f1f5f9; -fx-border-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);" +
                        cardOpacity
        ));

        HBox item = new HBox(12, dateBox, connector, card);
        item.setAlignment(Pos.TOP_LEFT);
        item.setPadding(new Insets(0, 0, 15, 0));
        return item;
    }

    // ======================== EXPORT PDF ========================
    private void exporterPdf(List<rdv> rdvs) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.setInitialFileName("bilan-medical-" + LocalDate.now() + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF", "*.pdf")
        );

        File file = fileChooser.showSaveDialog(rootContainer.getScene().getWindow());
        if (file != null) {
            try {
                PdfExportService pdfService = new PdfExportService();
                pdfService.genererPdf(rdvs, file.getAbsolutePath());
                java.awt.Desktop.getDesktop().open(file);
            } catch (Exception ex) {
                System.err.println("Erreur export PDF : " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // ======================== NAVIGATION ========================
    private void retourListeRdv() {
        try {
            Node content = FXMLLoader.load(getClass().getResource("/AfficherRdv.fxml"));
            StackPane parent = (StackPane) rootContainer.getScene().lookup("#contentArea");
            if (parent != null) {
                parent.getChildren().clear();
                parent.getChildren().add(content);
            }
        } catch (Exception e) {
            System.err.println("Erreur retour : " + e.getMessage());
        }
    }

    private void ouvrirDetails(rdv r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ShowOneRdv.fxml"));
            Node content = loader.load();
            ShowOneRdvController ctrl = loader.getController();
            ctrl.initData(r);
            StackPane parent = (StackPane) rootContainer.getScene().lookup("#contentArea");
            if (parent != null) {
                parent.getChildren().clear();
                parent.getChildren().add(content);
            }
        } catch (Exception e) {
            System.err.println("Erreur détails : " + e.getMessage());
        }
    }
}