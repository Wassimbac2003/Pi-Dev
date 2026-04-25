package Controllers;

import Models.rdv;
import Services.RdvService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.animation.*;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FeedbackPopupController {

    private int selectedNote = 0;
    private final List<String> selectedTags  = new ArrayList<>();
    private final Label[] emojiLabels        = new Label[5];
    private final Label[] emojiCaptions      = new Label[5];
    private final VBox[]  emojiCells         = new VBox[5];
    private final RdvService rdvService      = new RdvService();

    private static final String[][] EMOJI_DATA = {
            {"😞", "Très déçu",   "#ef4444", "#fef2f2"},
            {"😕", "Déçu",        "#f97316", "#fff7ed"},
            {"😐", "Neutre",      "#eab308", "#fefce8"},
            {"😊", "Satisfait",   "#22c55e", "#f0fdf4"},
            {"🤩", "Excellent !", "#f97316", "#fff7ed"}
    };

    private static final String[][] TAGS_DATA = {
            {"⏰", "Ponctuel"},
            {"👂", "À l'écoute"},
            {"💼", "Professionnel"},
            {"💬", "Explications claires"},
            {"🏥", "Cabinet propre"},
            {"⚡", "Rapide"}
    };

    public void show(rdv r, Runnable onSuccess, Runnable onClose) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        // ══════════════════════════════════════════
        //  CARD PRINCIPALE
        // ══════════════════════════════════════════
        VBox card = new VBox(0);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(500);
        card.setMaxWidth(500);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 40, 0, 0, 12);"
        );

        // ── HEADER ORANGE ──
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(30, 40, 24, 40));
        header.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #f97316, #fb923c);" +
                        "-fx-background-radius: 24 24 0 0;"
        );

        Label iconLbl = new Label("💬");
        iconLbl.setStyle(
                "-fx-font-size: 38;" +
                        "-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji';"
        );

        Label titreLbl = new Label("Votre avis compte !");
        titreLbl.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: white;");

        Label sousTitreLbl = new Label("RDV avec " + r.getMedecin() + "  •  " + r.getMotif());
        sousTitreLbl.setStyle(
                "-fx-font-size: 13; -fx-text-fill: rgba(255,255,255,0.88);" +
                        "-fx-background-color: rgba(255,255,255,0.18);" +
                        "-fx-background-radius: 20; -fx-padding: 4 14;"
        );
        header.getChildren().addAll(iconLbl, titreLbl, sousTitreLbl);

        // ── BODY ──
        VBox body = new VBox(20);
        body.setPadding(new Insets(28, 36, 28, 36));

        // ── SECTION NOTE ──
        Label noteQuestion = new Label("Comment s'est passée votre consultation ?");
        noteQuestion.setStyle(
                "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1e293b;"
        );

        HBox emojiBox = new HBox(10);
        emojiBox.setAlignment(Pos.CENTER);
        emojiBox.setPadding(new Insets(8, 0, 4, 0));

        for (int i = 0; i < 5; i++) {
            final int note  = i + 1;
            String emoji    = EMOJI_DATA[i][0];
            String caption  = EMOJI_DATA[i][1];
            String color    = EMOJI_DATA[i][2];
            String bgColor  = EMOJI_DATA[i][3];

            VBox emojiCell = new VBox(6);
            emojiCell.setAlignment(Pos.CENTER);
            emojiCell.setPrefWidth(80);
            emojiCell.setPadding(new Insets(10, 6, 10, 6));
            emojiCell.setStyle("-fx-background-radius: 14; -fx-cursor: hand;");

            // ← Police forcée pour afficher les emojis en couleur
            Label emojiLbl = new Label(emoji);
            emojiLbl.setStyle(
                    "-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji';" +
                            "-fx-font-size: 34; -fx-opacity: 0.38;"
            );

            Label captionLbl = new Label(caption);
            captionLbl.setMaxWidth(76);
            captionLbl.setWrapText(true);
            captionLbl.setStyle(
                    "-fx-font-size: 10; -fx-font-weight: bold;" +
                            "-fx-text-fill: #94a3b8; -fx-text-alignment: center;"
            );

            emojiLabels[i]   = emojiLbl;
            emojiCaptions[i] = captionLbl;
            emojiCells[i]    = emojiCell;

            emojiCell.getChildren().addAll(emojiLbl, captionLbl);

            // Hover
            emojiCell.setOnMouseEntered(e -> {
                if (selectedNote == 0) highlightUpTo(note - 1);
                if (selectedNote != note) {
                    emojiCell.setStyle(
                            "-fx-background-color: " + bgColor + ";" +
                                    "-fx-background-radius: 14; -fx-cursor: hand;"
                    );
                }
            });
            emojiCell.setOnMouseExited(e -> {
                if (selectedNote == 0) resetEmojis();
                else highlightUpTo(selectedNote - 1);
                if (selectedNote != note) {
                    emojiCell.setStyle("-fx-background-radius: 14; -fx-cursor: hand;");
                }
            });

            // Clic
            emojiCell.setOnMouseClicked(e -> {
                selectedNote = note;

                // Reset toutes les cellules
                for (int j = 0; j < 5; j++) {
                    emojiCells[j].setStyle("-fx-background-radius: 14; -fx-cursor: hand;");
                }
                // Sélectionner la cellule cliquée avec bordure orange
                emojiCell.setStyle(
                        "-fx-background-color: " + bgColor + ";" +
                                "-fx-background-radius: 14;" +
                                "-fx-border-color: " + color + ";" +
                                "-fx-border-radius: 14;" +
                                "-fx-border-width: 2;" +
                                "-fx-cursor: hand;"
                );

                highlightUpTo(note - 1);

                // Bounce animation
                ScaleTransition bounce = new ScaleTransition(Duration.millis(120), emojiLbl);
                bounce.setFromX(1.0); bounce.setFromY(1.0);
                bounce.setToX(1.4);  bounce.setToY(1.4);
                bounce.setAutoReverse(true); bounce.setCycleCount(2);
                bounce.play();
            });

            emojiBox.getChildren().add(emojiCell);
        }

        // ── TAGS ──
        Label tagQuestion = new Label("Points positifs (optionnel) :");
        tagQuestion.setStyle(
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #475569;"
        );

        FlowPane tagsPane = new FlowPane(8, 8);
        tagsPane.setAlignment(Pos.CENTER_LEFT);

        for (String[] tagData : TAGS_DATA) {
            String tagFull = tagData[0] + " " + tagData[1];
            Button tagBtn  = new Button(tagFull);
            tagBtn.setStyle(styleTagNormal());

            tagBtn.setOnMouseEntered(e -> {
                if (!selectedTags.contains(tagFull))
                    tagBtn.setStyle(styleTagHover());
            });
            tagBtn.setOnMouseExited(e -> {
                if (!selectedTags.contains(tagFull))
                    tagBtn.setStyle(styleTagNormal());
            });

            tagBtn.setOnAction(e -> {
                if (selectedTags.contains(tagFull)) {
                    selectedTags.remove(tagFull);
                    tagBtn.setStyle(styleTagNormal());
                } else {
                    selectedTags.add(tagFull);
                    tagBtn.setStyle(styleTagSelected());
                    ScaleTransition st = new ScaleTransition(Duration.millis(100), tagBtn);
                    st.setFromX(0.88); st.setFromY(0.88);
                    st.setToX(1.0);   st.setToY(1.0);
                    st.play();
                }
            });
            tagsPane.getChildren().add(tagBtn);
        }

        // ── COMMENTAIRE ──
        Label commentLabel = new Label("Commentaire libre (optionnel) :");
        commentLabel.setStyle(
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #475569;"
        );

        TextArea commentaire = new TextArea();
        commentaire.setPromptText("Partagez votre expérience...");
        commentaire.setPrefRowCount(3);
        commentaire.setStyle(
                "-fx-font-size: 13;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-padding: 10;"
        );
        // Focus orange
        commentaire.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                commentaire.setStyle(
                        "-fx-font-size: 13; -fx-background-radius: 12; -fx-border-radius: 12;" +
                                "-fx-border-color: #f97316; -fx-padding: 10;" +
                                "-fx-effect: dropshadow(gaussian, rgba(249,115,22,0.20), 8, 0, 0, 0);"
                );
            } else {
                commentaire.setStyle(
                        "-fx-font-size: 13; -fx-background-radius: 12; -fx-border-radius: 12;" +
                                "-fx-border-color: #e2e8f0; -fx-padding: 10;"
                );
            }
        });

        // ── ERREUR ──
        Label lblErreur = new Label();
        lblErreur.setStyle(
                "-fx-text-fill: #ef4444; -fx-font-size: 12;" +
                        "-fx-background-color: #fef2f2;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #fecaca; -fx-border-radius: 8;" +
                        "-fx-padding: 8 14; -fx-font-weight: bold;"
        );
        lblErreur.setWrapText(true);
        lblErreur.setVisible(false);
        lblErreur.setManaged(false);

        // ── BOUTONS ──
        HBox boutons = new HBox(12);
        boutons.setAlignment(Pos.CENTER);
        boutons.setPadding(new Insets(4, 0, 0, 0));

        Button btnAnnuler = new Button("Plus tard");
        btnAnnuler.setStyle(styleBtnPlusTard());
        btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle(styleBtnPlusTardHover()));
        btnAnnuler.setOnMouseExited(e  -> btnAnnuler.setStyle(styleBtnPlusTard()));
        btnAnnuler.setOnAction(e -> {
            stage.close();
            if (onClose != null) onClose.run();
        });

        Button btnEnvoyer = new Button("✨  Envoyer mon avis");
        btnEnvoyer.setStyle(styleBtnEnvoyer());
        btnEnvoyer.setOnMouseEntered(e -> btnEnvoyer.setStyle(styleBtnEnvoyerHover()));
        btnEnvoyer.setOnMouseExited(e  -> btnEnvoyer.setStyle(styleBtnEnvoyer()));

        btnEnvoyer.setOnAction(e -> {
            if (selectedNote == 0) {
                lblErreur.setText("❌  Veuillez sélectionner une note avant d'envoyer");
                lblErreur.setVisible(true);
                lblErreur.setManaged(true);
                TranslateTransition shake = new TranslateTransition(Duration.millis(55), emojiBox);
                shake.setByX(9); shake.setAutoReverse(true); shake.setCycleCount(6);
                shake.play();
                return;
            }
            try {
                String tagsStr = String.join(",", selectedTags);
                rdvService.updateFeedback(
                        r.getId(), selectedNote, tagsStr, commentaire.getText().trim()
                );
                stage.close();
                if (onClose != null) onClose.run();
                afficherToastSucces(r.getMedecin());
                if (onSuccess != null) onSuccess.run();
            } catch (SQLException ex) {
                lblErreur.setText("❌  Erreur : " + ex.getMessage());
                lblErreur.setVisible(true);
                lblErreur.setManaged(true);
            }
        });

        boutons.getChildren().addAll(btnAnnuler, btnEnvoyer);

        Separator sep1 = new Separator();
        Separator sep2 = new Separator();

        body.getChildren().addAll(
                noteQuestion, emojiBox,
                sep1,
                tagQuestion, tagsPane,
                sep2,
                commentLabel, commentaire,
                lblErreur,
                boutons
        );

        card.getChildren().addAll(header, body);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: transparent;");
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm()
        );

        stage.setScene(scene);

        // Animation d'entrée
        card.setScaleX(0.82); card.setScaleY(0.82); card.setOpacity(0);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(280), card);
        scaleIn.setToX(1.0); scaleIn.setToY(1.0);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), card);
        fadeIn.setToValue(1.0);
        new ParallelTransition(scaleIn, fadeIn).play();

        stage.show();
    }

    // ══════════════════════════════════════════
    //  HELPERS — highlight / reset
    // ══════════════════════════════════════════
    private void highlightUpTo(int index) {
        for (int i = 0; i < 5; i++) {
            if (i <= index) {
                emojiLabels[i].setStyle(
                        "-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji';" +
                                "-fx-font-size: 38; -fx-opacity: 1.0;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 1);"
                );
                emojiCaptions[i].setStyle(
                        "-fx-font-size: 10; -fx-font-weight: bold;" +
                                "-fx-text-fill: " + EMOJI_DATA[i][2] + ";" +
                                "-fx-text-alignment: center;"
                );
            } else {
                emojiLabels[i].setStyle(
                        "-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji';" +
                                "-fx-font-size: 34; -fx-opacity: 0.38;"
                );
                emojiCaptions[i].setStyle(
                        "-fx-font-size: 10; -fx-font-weight: bold;" +
                                "-fx-text-fill: #94a3b8; -fx-text-alignment: center;"
                );
            }
        }
    }

    private void resetEmojis() {
        for (int i = 0; i < 5; i++) {
            emojiLabels[i].setStyle(
                    "-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji';" +
                            "-fx-font-size: 34; -fx-opacity: 0.38;"
            );
            emojiCaptions[i].setStyle(
                    "-fx-font-size: 10; -fx-font-weight: bold;" +
                            "-fx-text-fill: #94a3b8; -fx-text-alignment: center;"
            );
        }
    }

    // ══════════════════════════════════════════
    //  STYLES INLINE
    // ══════════════════════════════════════════
    private String styleTagNormal() {
        return "-fx-background-color: #f8fafc; -fx-text-fill: #475569;" +
                "-fx-font-size: 12; -fx-background-radius: 20;" +
                "-fx-border-radius: 20; -fx-border-color: #e2e8f0;" +
                "-fx-padding: 7 16; -fx-cursor: hand;";
    }

    private String styleTagHover() {
        return "-fx-background-color: #fff7ed; -fx-text-fill: #f97316;" +
                "-fx-font-size: 12; -fx-background-radius: 20;" +
                "-fx-border-radius: 20; -fx-border-color: #fed7aa;" +
                "-fx-padding: 7 16; -fx-cursor: hand;";
    }

    private String styleTagSelected() {
        return "-fx-background-color: #fff7ed; -fx-text-fill: #f97316;" +
                "-fx-font-size: 12; -fx-font-weight: bold;" +
                "-fx-background-radius: 20; -fx-border-radius: 20;" +
                "-fx-border-color: #f97316; -fx-padding: 7 16; -fx-cursor: hand;";
    }

    private String styleBtnEnvoyer() {
        return "-fx-background-color: linear-gradient(to right, #f97316, #fb923c);" +
                "-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;" +
                "-fx-background-radius: 12; -fx-padding: 12 32; -fx-cursor: hand;";
    }

    private String styleBtnEnvoyerHover() {
        return "-fx-background-color: linear-gradient(to right, #ea6c00, #f97316);" +
                "-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;" +
                "-fx-background-radius: 12; -fx-padding: 12 32; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(249,115,22,0.45), 14, 0, 0, 5);";
    }

    private String styleBtnPlusTard() {
        return "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b;" +
                "-fx-font-size: 13; -fx-background-radius: 12;" +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 12;" +
                "-fx-padding: 12 28; -fx-cursor: hand;";
    }

    private String styleBtnPlusTardHover() {
        return "-fx-background-color: #e2e8f0; -fx-text-fill: #475569;" +
                "-fx-font-size: 13; -fx-background-radius: 12;" +
                "-fx-border-color: #cbd5e1; -fx-border-radius: 12;" +
                "-fx-padding: 12 28; -fx-cursor: hand;";
    }

    // ── TOAST SUCCÈS ──
    private void afficherToastSucces(String medecin) {
        Stage toast = new Stage();
        toast.initStyle(StageStyle.TRANSPARENT);

        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 35, 20, 35));
        box.setStyle(
                "-fx-background-color: linear-gradient(to right, #f97316, #fb923c);" +
                        "-fx-background-radius: 18;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 6);"
        );

        Label icon = new Label("🎉");
        icon.setStyle(
                "-fx-font-size: 30;" +
                        "-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji';"
        );
        Label msg = new Label("Merci pour votre avis !");
        msg.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sub = new Label("Votre retour aide " + medecin);
        sub.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.90);");

        box.getChildren().addAll(icon, msg, sub);

        Scene s = new Scene(box);
        s.setFill(Color.TRANSPARENT);
        s.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm()
        );
        toast.setScene(s);
        toast.show();

        // Position bas droite
        javafx.geometry.Rectangle2D screen =
                javafx.stage.Screen.getPrimary().getVisualBounds();
        toast.setX(screen.getMaxX() - 340);
        toast.setY(screen.getMaxY() - 130);

        // Slide-in depuis le bas
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), box);
        slideIn.setFromY(50); slideIn.setToY(0);
        FadeTransition fi = new FadeTransition(Duration.millis(300), box);
        fi.setFromValue(0); fi.setToValue(1);
        new ParallelTransition(slideIn, fi).play();

        // Auto-fermeture après 2.8s
        PauseTransition pause = new PauseTransition(Duration.seconds(2.8));
        pause.setOnFinished(e -> {
            FadeTransition fo = new FadeTransition(Duration.millis(400), box);
            fo.setToValue(0);
            fo.setOnFinished(ev -> toast.close());
            fo.play();
        });
        pause.play();
    }
}