package Controllers;

import Models.rdv;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class VideoCallController {

    @FXML private Label lblTitre;
    @FXML private Button btnFermer;
    @FXML private StackPane mainContent;

    private String url;
    private String roomName;

    public void initCall(rdv r, String nomInterlocuteur) {
        roomName = "vitaltech-rdv-" + r.getId();
        url = "https://meet.jit.si/" + roomName;

        lblTitre.setText("Consultation avec " + nomInterlocuteur);
        btnFermer.setOnAction(e -> fermer());

        // Construire l'UI
        construireUI(nomInterlocuteur);

        // Ouvrir automatiquement le navigateur après 1 seconde
        new Timeline(new KeyFrame(Duration.seconds(1), e -> ouvrirNavigateur()))
                .play();
    }

    private void construireUI(String nomInterlocuteur) {
        // ── Fond principal ──
        VBox center = new VBox(28);
        center.setAlignment(Pos.CENTER);
        center.setStyle("-fx-background-color: #0f172a; -fx-padding: 50;");
        StackPane.setAlignment(center, Pos.CENTER);

        // ── Icône animée ──
        Label icon = new Label("📹");
        icon.setStyle("-fx-font-size: 56;");
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1200), icon);
        pulse.setFromX(1.0); pulse.setFromY(1.0);
        pulse.setToX(1.15); pulse.setToY(1.15);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        // ── Titre ──
        Label titre = new Label("Consultation Vidéo");
        titre.setStyle(
                "-fx-font-size: 22; -fx-font-weight: bold; " +
                        "-fx-text-fill: white;"
        );

        Label sousTitre = new Label("avec " + nomInterlocuteur);
        sousTitre.setStyle(
                "-fx-font-size: 15; -fx-text-fill: #60a5fa; -fx-font-weight: bold;"
        );

        // ── Séparateur ──
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1e3a5f;");
        sep.setMaxWidth(300);

        // ── Card Room ──
        VBox roomCard = new VBox(10);
        roomCard.setAlignment(Pos.CENTER);
        roomCard.setStyle(
                "-fx-background-color: #1e293b; " +
                        "-fx-background-radius: 14; " +
                        "-fx-padding: 20 30; " +
                        "-fx-border-color: #334155; " +
                        "-fx-border-radius: 14; " +
                        "-fx-border-width: 1;"
        );
        roomCard.setMaxWidth(380);

        Label roomTitleLabel = new Label("🔗 Lien de la consultation");
        roomTitleLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        Label urlLabel = new Label(url);
        urlLabel.setStyle(
                "-fx-font-size: 12; -fx-text-fill: #93c5fd; " +
                        "-fx-wrap-text: true; -fx-text-alignment: center;"
        );
        urlLabel.setMaxWidth(320);
        urlLabel.setWrapText(true);

        // ── Bouton Copier ──
        Button btnCopier = new Button("📋  Copier le lien");
        btnCopier.setStyle(
                "-fx-background-color: #1e40af; -fx-text-fill: white; " +
                        "-fx-font-size: 12; -fx-font-weight: bold; " +
                        "-fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;"
        );
        btnCopier.setOnMouseEntered(e -> btnCopier.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; " +
                        "-fx-font-size: 12; -fx-font-weight: bold; " +
                        "-fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;"
        ));
        btnCopier.setOnMouseExited(e -> btnCopier.setStyle(
                "-fx-background-color: #1e40af; -fx-text-fill: white; " +
                        "-fx-font-size: 12; -fx-font-weight: bold; " +
                        "-fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;"
        ));
        btnCopier.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent cc = new ClipboardContent();
            cc.putString(url);
            clipboard.setContent(cc);
            btnCopier.setText("✅  Lien copié !");
            btnCopier.setStyle(
                    "-fx-background-color: #15803d; -fx-text-fill: white; " +
                            "-fx-font-size: 12; -fx-font-weight: bold; " +
                            "-fx-background-radius: 8; -fx-padding: 8 20;"
            );
            new Timeline(new KeyFrame(Duration.seconds(2), ev -> {
                btnCopier.setText("📋  Copier le lien");
                btnCopier.setStyle(
                        "-fx-background-color: #1e40af; -fx-text-fill: white; " +
                                "-fx-font-size: 12; -fx-font-weight: bold; " +
                                "-fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;"
                );
            })).play();
        });

        roomCard.getChildren().addAll(roomTitleLabel, urlLabel, btnCopier);

        // ── Bouton principal ──
        Button btnRejoindre = new Button("🚀  Rejoindre la consultation");
        btnRejoindre.setStyle(
                "-fx-background-color: linear-gradient(to right, #16a34a, #15803d); " +
                        "-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold; " +
                        "-fx-background-radius: 12; -fx-padding: 16 36; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(22,163,74,0.4), 12, 0, 0, 4);"
        );
        btnRejoindre.setOnMouseEntered(e -> btnRejoindre.setStyle(
                "-fx-background-color: linear-gradient(to right, #15803d, #166534); " +
                        "-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold; " +
                        "-fx-background-radius: 12; -fx-padding: 16 36; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(22,163,74,0.6), 16, 0, 0, 6);"
        ));
        btnRejoindre.setOnMouseExited(e -> btnRejoindre.setStyle(
                "-fx-background-color: linear-gradient(to right, #16a34a, #15803d); " +
                        "-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold; " +
                        "-fx-background-radius: 12; -fx-padding: 16 36; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(22,163,74,0.4), 12, 0, 0, 4);"
        ));
        btnRejoindre.setOnAction(e -> ouvrirNavigateur());

        // Pulse sur le bouton
        ScaleTransition btnPulse = new ScaleTransition(Duration.millis(900), btnRejoindre);
        btnPulse.setFromX(1.0); btnPulse.setFromY(1.0);
        btnPulse.setToX(1.04); btnPulse.setToY(1.04);
        btnPulse.setCycleCount(Animation.INDEFINITE);
        btnPulse.setAutoReverse(true);
        btnPulse.play();

        // ── Note info ──
        Label note = new Label(
                "💡  Le navigateur s'ouvre automatiquement.\n" +
                        "Le médecin rejoint la même room depuis son interface."
        );
        note.setStyle(
                "-fx-font-size: 11; -fx-text-fill: #475569; " +
                        "-fx-text-alignment: center; -fx-wrap-text: true;"
        );
        note.setMaxWidth(360);
        note.setWrapText(true);

        // ── Statut connexion ──
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER);
        Label dot = new Label("●");
        dot.setStyle("-fx-font-size: 10; -fx-text-fill: #4ade80;");
        FadeTransition dotFade = new FadeTransition(Duration.millis(800), dot);
        dotFade.setFromValue(1.0); dotFade.setToValue(0.2);
        dotFade.setCycleCount(Animation.INDEFINITE); dotFade.setAutoReverse(true);
        dotFade.play();
        Label statusLabel = new Label("Connexion sécurisée — Jitsi Meet");
        statusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #4ade80;");
        statusRow.getChildren().addAll(dot, statusLabel);

        center.getChildren().addAll(
                icon, titre, sousTitre, sep,
                roomCard, btnRejoindre, note, statusRow
        );

        mainContent.getChildren().setAll(center);
    }

    private void ouvrirNavigateur() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback — essayer avec la commande système
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec("open " + url);
                } else {
                    Runtime.getRuntime().exec("xdg-open " + url);
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Lien de consultation");
                    alert.setHeaderText("Ouvrez ce lien dans votre navigateur :");
                    alert.setContentText(url);
                    alert.showAndWait();
                });
            }
        }
    }

    private void fermer() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        stage.close();
    }
}