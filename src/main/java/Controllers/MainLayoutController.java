package Controllers;

import Services.NotificationService;
import Services.NotificationService.Notification;
import Services.NotificationService.NotificationResult;
import Services.RappelService;
import Services.RappelService.Rappel;
import Services.RappelService.RappelResult;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;

public class MainLayoutController {

    @FXML private StackPane contentArea;
    @FXML private Label pageTitle;
    @FXML private Button btnRendezVous;
    @FXML private Button btnTableauBord;
    @FXML private StackPane notificationBell;
    @FXML private Label notifBadge;

    private final NotificationService notificationService = new NotificationService();
    private final RappelService rappelService = new RappelService();
    private Popup notifPopup;
    private boolean popupVisible = false;
    private Timeline pollingTimeline;

    private HBox rappelBanner;
    private VBox centerVBox;

    @FXML
    public void initialize() {
        chargerContenu("/AfficherRdv.fxml");

        btnRendezVous.setOnAction(e -> chargerContenu("/AfficherRdv.fxml"));

        notificationBell.setOnMouseClicked(e -> toggleNotifPopup());

        // Attendre que la scene soit prête pour accéder au VBox parent
        contentArea.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                centerVBox = (VBox) contentArea.getParent();
                rafraichirBadge();
                rafraichirRappel();
            }
        });

        // Polling
        pollingTimeline = new Timeline(
                new KeyFrame(Duration.seconds(15), e -> {
                    rafraichirBadge();
                    rafraichirRappel();
                })
        );
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    public void chargerContenu(String fxml) {
        try {
            Node content = FXMLLoader.load(getClass().getResource(fxml));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            System.err.println("Erreur chargement : " + e.getMessage());
        }
    }

    // ===== BADGE NOTIFICATION =====
    private void rafraichirBadge() {
        try {
            int count = notificationService.getCountNonLues();
            if (count > 0) {
                notifBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                notifBadge.setVisible(true);
            } else {
                notifBadge.setVisible(false);
            }
        } catch (SQLException e) {
            System.err.println("Erreur notif : " + e.getMessage());
        }
    }
    // ===== Rappel =====

    private void rafraichirRappel() {
        try {
            RappelResult result = rappelService.getRappels();

            if (rappelBanner != null && centerVBox != null) {
                centerVBox.getChildren().remove(rappelBanner);
                rappelBanner = null;
            }

            if (result.count == 0 || centerVBox == null) return;

            Rappel top = result.rappels.get(0);

            String bannerClass, iconText;
            switch (top.niveau) {
                case "urgent": bannerClass = "rappel-banner-urgent"; iconText = "!!"; break;
                case "today":  bannerClass = "rappel-banner-today";  iconText = "!";  break;
                default:       bannerClass = "rappel-banner-tomorrow"; iconText = "i"; break;
            }

            rappelBanner = new HBox(14);
            rappelBanner.setAlignment(Pos.CENTER_LEFT);
            rappelBanner.getStyleClass().addAll("rappel-banner", bannerClass);

            Label icon = new Label(iconText);
            icon.getStyleClass().add("rappel-icon");

            Label message = new Label(top.message);
            message.getStyleClass().add("rappel-message");
            HBox.setHgrow(message, Priority.ALWAYS);

            Button btnFermer = new Button("Fermer");
            btnFermer.getStyleClass().add("rappel-close");
            btnFermer.setCursor(Cursor.HAND);
            btnFermer.setOnAction(e -> {
                centerVBox.getChildren().remove(rappelBanner);
                rappelBanner = null;
            });

            rappelBanner.getChildren().addAll(icon, message, btnFermer);

            if (!centerVBox.getChildren().contains(rappelBanner)) {
                centerVBox.getChildren().add(1, rappelBanner);
            }

        } catch (SQLException e) {
            System.err.println("Erreur rappel : " + e.getMessage());
        }
    }

    // ===== POPUP NOTIFICATIONS =====
    private void toggleNotifPopup() {
        if (popupVisible && notifPopup != null) {
            notifPopup.hide();
            popupVisible = false;
            return;
        }
        showNotifPopup();
    }

    private void showNotifPopup() {
        if (notifPopup != null) notifPopup.hide();

        notifPopup = new Popup();
        notifPopup.setAutoHide(true);
        notifPopup.setOnHidden(e -> popupVisible = false);

        VBox container = new VBox(0);
        container.setPrefWidth(380);
        container.setMaxHeight(450);
        container.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 5);");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setStyle("-fx-background-color: white; -fx-background-radius: 16 16 0 0; -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");

        Label titre = new Label("Notifications");
        titre.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(titre, Priority.ALWAYS);

        Button btnToutLu = new Button("Tout marquer lu");
        btnToutLu.setStyle("-fx-background-color: transparent; -fx-text-fill: #1a73e8; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 8;");
        btnToutLu.setCursor(Cursor.HAND);
        btnToutLu.setOnAction(e -> {
            try {
                notificationService.marquerToutLu();
                rafraichirBadge();
                notifPopup.hide();
                popupVisible = false;
            } catch (SQLException ex) {
                System.err.println("Erreur : " + ex.getMessage());
            }
        });

        header.getChildren().addAll(titre, btnToutLu);

        // Liste
        VBox listeBox = new VBox(0);
        listeBox.setPadding(new Insets(5, 0, 5, 0));

        try {
            NotificationResult result = notificationService.getNotifications();

            if (result.notifications.isEmpty()) {
                VBox emptyBox = new VBox(8);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(30));
                Label emptyText = new Label("Aucune notification");
                emptyText.setStyle("-fx-font-size: 13; -fx-text-fill: #94a3b8;");
                emptyBox.getChildren().add(emptyText);
                listeBox.getChildren().add(emptyBox);
            } else {
                for (Notification n : result.notifications) {
                    listeBox.getChildren().add(buildNotifItem(n));
                }
            }
        } catch (SQLException e) {
            Label err = new Label("Erreur chargement");
            err.setStyle("-fx-text-fill: #e53935; -fx-padding: 20;");
            listeBox.getChildren().add(err);
        }

        ScrollPane scroll = new ScrollPane(listeBox);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(350);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        container.getChildren().addAll(header, scroll);
        notifPopup.getContent().add(container);

        var bounds = notificationBell.localToScreen(notificationBell.getBoundsInLocal());
        if (bounds != null) {
            notifPopup.show(notificationBell.getScene().getWindow(),
                    bounds.getMaxX() - 380,
                    bounds.getMaxY() + 8);
        }
        popupVisible = true;
    }

    // ===== ITEM NOTIFICATION =====
    private HBox buildNotifItem(Notification n) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 20, 12, 20));
        item.setCursor(Cursor.HAND);
        item.setStyle("-fx-background-color: white;");

        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #f8fafc;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: white;"));

        String iconText, iconBg, iconColor;
        switch (n.type) {
            case "success": iconText = "OK"; iconBg = "#dcfce7"; iconColor = "#15803d"; break;
            case "danger":  iconText = "X";  iconBg = "#fee2e2"; iconColor = "#b91c1c"; break;
            default:        iconText = "..."; iconBg = "#fff7ed"; iconColor = "#c2410c"; break;
        }

        Label icon = new Label(iconText);
        icon.setPrefWidth(36); icon.setPrefHeight(36); icon.setMinWidth(36);
        icon.setAlignment(Pos.CENTER);
        icon.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 10; " +
                "-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: " + iconColor + ";");

        VBox textBox = new VBox(3);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label msgLabel = new Label(n.message);
        msgLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #334155;");
        msgLabel.setWrapText(true);

        Label heureLabel = new Label(n.heure + " - " + n.date);
        heureLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #94a3b8;");

        textBox.getChildren().addAll(msgLabel, heureLabel);

        Button btnLu = new Button("x");
        btnLu.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 2 6;");
        btnLu.setCursor(Cursor.HAND);
        btnLu.setOnAction(e -> {
            notificationService.marquerLu(n.id);
            rafraichirBadge();
            VBox parent = (VBox) item.getParent();
            parent.getChildren().remove(item);
            if (parent.getChildren().isEmpty()) {
                VBox emptyBox = new VBox(8);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(30));
                Label emptyText = new Label("Aucune notification");
                emptyText.setStyle("-fx-font-size: 13; -fx-text-fill: #94a3b8;");
                emptyBox.getChildren().add(emptyText);
                parent.getChildren().add(emptyBox);
            }
        });

        item.getChildren().addAll(icon, textBox, btnLu);
        return item;
    }
}