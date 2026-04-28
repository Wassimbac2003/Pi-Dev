package Controllers;

import Services.CreneauAiService;
import Services.CreneauAiService.CreneauSuggere;
import Services.RdvService;
import Models.rdv;
import Models.medecin;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.List;

/**
 * CreneauAiController
 *
 * Popup qui :
 *  1. Affiche un spinner pendant que l'IA analyse
 *  2. Présente les 3 créneaux suggérés sous forme de cartes cliquables
 *  3. Au clic → pré-remplit le formulaire d'ajout de RDV
 *
 * Utilisation depuis AfficherRdvController :
 *   CreneauAiController.afficher(parentStage, medecin, motif, () -> chargerDonnees());
 */
public class CreneauAiController {

    // ── Couleurs par score ────────────────────────────────────────
    private static final String COLOR_OPTIMAL   = "#16a34a";
    private static final String COLOR_BON       = "#2563eb";
    private static final String COLOR_DISPONIBLE= "#64748b";

    /**
     * Point d'entrée unique — ouvre le popup IA.
     *
     * @param parentStage  fenêtre parente (pour centrer et init owner)
     * @param m            médecin sélectionné
     * @param motif        motif du RDV choisi par le patient
     * @param patientId    ID du patient courant
     * @param onRdvCree    callback appelé après création du RDV
     */
    public static void afficher(Stage parentStage, medecin m,
                                String motif, int patientId,
                                Runnable onRdvCree) {

        Stage popup = new Stage();
        popup.initOwner(parentStage);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.DECORATED);
        popup.setTitle("✨ Créneaux suggérés par l'IA");
        popup.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8fafc;");

        // ── Header ───────────────────────────────────────────────
        VBox header = creerHeader(m, motif);
        root.getChildren().add(header);

        // ── Zone de contenu (spinner puis cartes) ────────────────
        StackPane contenu = new StackPane();
        contenu.setMinHeight(280);
        contenu.setPadding(new Insets(20));

        VBox spinnerBox = creerSpinner();
        contenu.getChildren().add(spinnerBox);
        root.getChildren().add(contenu);

        popup.setScene(new Scene(root, 480, 520));
        popup.show();

        // ── Appel IA en arrière-plan ──────────────────────────────
        String nomMedecin = "Dr. " + m.getPrenom() + " " + m.getNom();
        String specialite = m.getSpecialite() != null ? m.getSpecialite() : "Médecine générale";

        new Thread(() -> {
            CreneauAiService service = new CreneauAiService();
            List<CreneauSuggere> suggestions = service.suggererCreneaux(
                    nomMedecin, specialite, motif);

            Platform.runLater(() -> {
                contenu.getChildren().clear();

                if (suggestions.isEmpty()) {
                    contenu.getChildren().add(creerMessageErreur());
                } else {
                    VBox cartes = creerCartesSuggestions(
                            suggestions, m, motif, patientId,
                            popup, onRdvCree);
                    contenu.getChildren().add(cartes);
                    animerEntree(cartes);
                }

                popup.sizeToScene();
            });
        }, "CreneauAI-Thread").start();
    }

    // ── Header ────────────────────────────────────────────────────

    private static VBox creerHeader(medecin m, String motif) {
        VBox header = new VBox(6);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        // Badge IA
        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label("✨ Intelligence artificielle");
        badge.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; " +
                "-fx-font-size: 11; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; -fx-padding: 3 10;");
        badgeRow.getChildren().add(badge);

        Label titre = new Label("Créneaux recommandés");
        titre.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        String nomMedecin = "Dr. " + m.getPrenom() + " " + m.getNom();
        Label sousTitre = new Label(nomMedecin + " · " + motif);
        sousTitre.setStyle("-fx-font-size: 13; -fx-text-fill: #64748b;");

        header.getChildren().addAll(badgeRow, titre, sousTitre);
        return header;
    }

    // ── Spinner de chargement ─────────────────────────────────────

    private static VBox creerSpinner() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);

        // Arc animé
        javafx.scene.shape.Arc arc = new javafx.scene.shape.Arc(
                24, 24, 20, 20, 0, 270);
        arc.setType(javafx.scene.shape.ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(Color.web("#6d28d9"));
        arc.setStrokeWidth(3);
        arc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        javafx.scene.layout.Pane arcPane = new javafx.scene.layout.Pane(arc);
        arcPane.setPrefSize(48, 48);

        RotateTransition rotate = new RotateTransition(Duration.seconds(1), arc);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.setInterpolator(Interpolator.LINEAR);
        rotate.play();

        Label msg = new Label("L'IA analyse les disponibilités...");
        msg.setStyle("-fx-font-size: 14; -fx-text-fill: #475569;");

        Label sousmsg = new Label("Cela prend environ 2–3 secondes");
        sousmsg.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8;");

        box.getChildren().addAll(arcPane, msg, sousmsg);
        return box;
    }

    // ── Cartes de suggestions ─────────────────────────────────────

    private static VBox creerCartesSuggestions(List<CreneauSuggere> suggestions,
                                               medecin m, String motif,
                                               int patientId,
                                               Stage popup, Runnable onRdvCree) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(4, 0, 0, 0));

        Label intro = new Label("Choisissez un créneau :");
        intro.setStyle("-fx-font-size: 13; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        box.getChildren().add(intro);

        for (int i = 0; i < suggestions.size(); i++) {
            CreneauSuggere cs = suggestions.get(i);
            HBox carte = creerCarte(cs, i + 1, m, motif, patientId, popup, onRdvCree);
            box.getChildren().add(carte);
        }

        // Bouton annuler
        Button btnAnnuler = new Button("Saisir manuellement");
        btnAnnuler.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; " +
                "-fx-font-size: 12; -fx-cursor: hand; -fx-border-color: transparent;");
        btnAnnuler.setOnAction(e -> popup.close());
        VBox.setMargin(btnAnnuler, new Insets(8, 0, 0, 0));
        box.getChildren().add(btnAnnuler);

        return box;
    }

    private static HBox creerCarte(CreneauSuggere cs, int index,
                                   medecin m, String motif, int patientId,
                                   Stage popup, Runnable onRdvCree) {

        HBox carte = new HBox(14);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setPadding(new Insets(14, 16, 14, 16));
        carte.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-border-radius: 12; -fx-border-color: #e2e8f0; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 2);");

        // Hover
        carte.setOnMouseEntered(e -> carte.setStyle(
                "-fx-background-color: #f8faff; -fx-background-radius: 12; " +
                        "-fx-border-radius: 12; -fx-border-color: #6d28d9; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(109,40,217,0.12), 8, 0, 0, 3);"));
        carte.setOnMouseExited(e -> carte.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; " +
                        "-fx-border-radius: 12; -fx-border-color: #e2e8f0; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 2);"));

        // Numéro
        Label num = new Label(String.valueOf(index));
        num.setMinWidth(28); num.setMinHeight(28);
        num.setMaxWidth(28); num.setMaxHeight(28);
        num.setAlignment(Pos.CENTER);
        num.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; " +
                "-fx-font-weight: bold; -fx-font-size: 13; -fx-background-radius: 14;");

        // Infos date/heure
        VBox infos = new VBox(3);
        HBox.setHgrow(infos, Priority.ALWAYS);

        // Formater la date lisiblement
        String dateFormatee = formaterDate(cs.date);
        Label lblDate = new Label(dateFormatee);
        lblDate.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label lblHeure = new Label("🕐 " + cs.hdebut + " – " + cs.hfin);
        lblHeure.setStyle("-fx-font-size: 13; -fx-text-fill: #475569;");

        Label lblRaison = new Label(cs.raison);
        lblRaison.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8; -fx-font-style: italic;");

        infos.getChildren().addAll(lblDate, lblHeure, lblRaison);

        // Badge score
        String couleur = switch (cs.score) {
            case "Optimal"    -> COLOR_OPTIMAL;
            case "Bon"        -> COLOR_BON;
            default           -> COLOR_DISPONIBLE;
        };
        Label scoreBadge = new Label(cs.score);
        scoreBadge.setStyle(
                "-fx-font-size: 11; -fx-font-weight: bold; -fx-padding: 4 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-background-color: " + couleur + "22; " +
                        "-fx-text-fill: " + couleur + ";");

        carte.getChildren().addAll(num, infos, scoreBadge);

        // Clic → créer le RDV
        carte.setOnMouseClicked(e -> creerRdvDepuisSuggestion(
                cs, m, motif, patientId, popup, onRdvCree));

        return carte;
    }

    // ── Création du RDV ──────────────────────────────────────────

    private static void creerRdvDepuisSuggestion(CreneauSuggere cs, medecin m,
                                                 String motif, int patientId,
                                                 Stage popup, Runnable onRdvCree) {
        try {
            RdvService rdvService = new RdvService();
            String nomMedecin = "Dr. " + m.getPrenom() + " " + m.getNom();

            rdv nouveau = new rdv(
                    cs.date,
                    cs.hdebut,
                    cs.hfin,
                    "en_attente",
                    motif,
                    nomMedecin,
                    "RDV suggéré par IA VitalTech",
                    patientId,
                    m.getId()
            );

            rdvService.insert(nouveau);
            popup.close();

            // Notification succès
            afficherSuccesIA(cs, nomMedecin);

            if (onRdvCree != null) onRdvCree.run();

        } catch (SQLException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Impossible de créer le RDV : " + ex.getMessage());
            alert.show();
        }
    }

    // ── Notification succès ───────────────────────────────────────

    private static void afficherSuccesIA(CreneauSuggere cs, String nomMedecin) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("RDV créé ✓");
        alert.setHeaderText("Rendez-vous confirmé !");
        alert.setContentText(
                "✅ RDV avec " + nomMedecin + "\n" +
                        "📅 Le " + formaterDate(cs.date) + "\n" +
                        "🕐 " + cs.hdebut + " – " + cs.hfin + "\n\n" +
                        "Le médecin doit encore confirmer votre rendez-vous."
        );
        alert.show();
    }

    // ── Message erreur ────────────────────────────────────────────

    private static VBox creerMessageErreur() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));

        Label icone = new Label("⚠️");
        icone.setStyle("-fx-font-size: 36;");

        Label msg = new Label("Aucun créneau suggéré");
        msg.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label detail = new Label("Vérifiez votre connexion internet\n" +
                "ou la clé API Anthropic.");
        detail.setStyle("-fx-font-size: 13; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        detail.setWrapText(true);

        box.getChildren().addAll(icone, msg, detail);
        return box;
    }

    // ── Animation d'entrée ────────────────────────────────────────

    private static void animerEntree(VBox cartes) {
        cartes.setOpacity(0);
        cartes.setTranslateY(15);
        FadeTransition fade = new FadeTransition(Duration.millis(300), cartes);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), cartes);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    // ── Helper : formater la date ─────────────────────────────────

    private static String formaterDate(String dateStr) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            String jour = date.getDayOfWeek()
                    .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH);
            String mois = date.getMonth()
                    .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH);
            return capitaliser(jour) + " " + date.getDayOfMonth() + " " + capitaliser(mois);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private static String capitaliser(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}