package Controllers;

import Models.disponibilite;
import Models.rdv;
import Services.DisponibiliteService;
import Services.RdvService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class AdminDispoController {

    @FXML private GridPane calendarGrid;
    @FXML private Label lblMoisAnnee, lblTodayCount, lblPendingCount, lblDoneCount;
    @FXML private Button btnPrev, btnNext, btnToday, btnRetourRdv;

    private YearMonth currentMonth;
    private RdvService rdvService = new RdvService();
    private DisponibiliteService dispoService = new DisponibiliteService();

    // Horaires par défaut
    private static final Map<DayOfWeek, String[][]> HORAIRES_DEFAULT = new HashMap<>();
    static {
        String[][] lunVen = {{"09:00", "12:00"}, {"14:00", "17:00"}};
        HORAIRES_DEFAULT.put(DayOfWeek.MONDAY, lunVen);
        HORAIRES_DEFAULT.put(DayOfWeek.TUESDAY, lunVen);
        HORAIRES_DEFAULT.put(DayOfWeek.WEDNESDAY, lunVen);
        HORAIRES_DEFAULT.put(DayOfWeek.THURSDAY, lunVen);
        HORAIRES_DEFAULT.put(DayOfWeek.FRIDAY, lunVen);
        HORAIRES_DEFAULT.put(DayOfWeek.SATURDAY, new String[][]{{"09:00", "13:00"}});
        HORAIRES_DEFAULT.put(DayOfWeek.SUNDAY, new String[][]{});
    }

    // Extra autorisé
    private static final Map<DayOfWeek, String[]> EXTRA_AUTORISE = new HashMap<>();
    static {
        String[] lunVenExtra = {"12:00", "14:00"};
        EXTRA_AUTORISE.put(DayOfWeek.MONDAY, lunVenExtra);
        EXTRA_AUTORISE.put(DayOfWeek.TUESDAY, lunVenExtra);
        EXTRA_AUTORISE.put(DayOfWeek.WEDNESDAY, lunVenExtra);
        EXTRA_AUTORISE.put(DayOfWeek.THURSDAY, lunVenExtra);
        EXTRA_AUTORISE.put(DayOfWeek.FRIDAY, lunVenExtra);
        EXTRA_AUTORISE.put(DayOfWeek.SATURDAY, null);
        EXTRA_AUTORISE.put(DayOfWeek.SUNDAY, new String[]{"10:00", "14:00"});
    }

    private static final int MED_ID = 1;

    @FXML
    public void initialize() {
        currentMonth = YearMonth.now();
        chargerStats();
        afficherCalendrier();

        btnPrev.setOnAction(e -> { currentMonth = currentMonth.minusMonths(1); afficherCalendrier(); });
        btnNext.setOnAction(e -> { currentMonth = currentMonth.plusMonths(1); afficherCalendrier(); });
        btnToday.setOnAction(e -> { currentMonth = YearMonth.now(); afficherCalendrier(); });
        btnRetourRdv.setOnAction(e -> retourRdv());
    }

    private void chargerStats() {
        try {
            List<rdv> rdvs = rdvService.findAll();
            String today = LocalDate.now().toString();
            long todayCount = rdvs.stream().filter(r -> r.getDate().equals(today)).count();
            long pendingCount = rdvs.stream().filter(r -> r.getStatut().toLowerCase().contains("attente")).count();
            long doneCount = rdvs.stream().filter(r -> r.getStatut().toLowerCase().contains("confirm")).count();

            lblTodayCount.setText(todayCount + " RDV");
            lblPendingCount.setText(pendingCount + " Demandes");
            lblDoneCount.setText(doneCount + " Consultations");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    private void afficherCalendrier() {
        calendarGrid.getChildren().clear();

        String mois = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblMoisAnnee.setText(mois + " " + currentMonth.getYear());

        LocalDate premierJour = currentMonth.atDay(1);
        int jourSemaine = premierJour.getDayOfWeek().getValue();
        int nbJours = currentMonth.lengthOfMonth();

        int row = 0;
        int col = jourSemaine - 1;

        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate date = currentMonth.atDay(jour);
            boolean isPasse = date.isBefore(LocalDate.now());
            boolean isToday = date.equals(LocalDate.now());

            VBox cellule = new VBox(5);
            cellule.setPrefWidth(170);
            cellule.setPrefHeight(90);
            cellule.setAlignment(Pos.TOP_RIGHT);

            String bgColor = "white";
            if (isToday) bgColor = "#e3f2fd";
            if (isPasse) bgColor = "#fafafa";

            cellule.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: #f0f0f0; -fx-border-width: 1; -fx-padding: 8; -fx-cursor: hand;");

            Label jourLabel = new Label(String.valueOf(jour));
            String textColor = isToday ? "#1a73e8" : (isPasse ? "#ccc" : "#333");
            jourLabel.setStyle("-fx-font-size: 14; -fx-text-fill: " + textColor + ";");

            cellule.getChildren().add(jourLabel);

            // Clic sur la cellule → ouvrir le popup
            final LocalDate clickedDate = date;
            cellule.setOnMouseClicked(e -> ouvrirPopupJour(clickedDate));

            calendarGrid.add(cellule, col, row);

            col++;
            if (col > 6) { col = 0; row++; }
        }
    }

    // ==================== POPUP JOUR ====================
    private void ouvrirPopupJour(LocalDate date) {
        boolean isPasse = date.isBefore(LocalDate.now());
        DayOfWeek jour = date.getDayOfWeek();
        String jourFr = jour.getDisplayName(TextStyle.FULL, Locale.FRENCH);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Planning du " + formatDateFr(date));

        VBox content = new VBox(15);
        content.setPrefWidth(650);
        content.setStyle("-fx-padding: 25;");

        // Titre + badge
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label titre = new Label("Planning du " + formatDateFr(date));
        titre.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #333;");
        titleBox.getChildren().add(titre);

        if (isPasse) {
            Label badge = new Label("🔒 Lecture seule");
            badge.setStyle("-fx-font-size: 11; -fx-text-fill: #888; -fx-background-color: #f0f0f0; -fx-background-radius: 12; -fx-padding: 3 10;");
            titleBox.getChildren().add(badge);
        }
        content.getChildren().add(titleBox);

        if (isPasse) {
            Label banniere = new Label("ℹ️ Cette date est passée. Vous pouvez consulter mais aucune modification n'est possible.");
            banniere.setWrapText(true);
            banniere.setStyle("-fx-font-size: 12; -fx-text-fill: #888; -fx-background-color: #f8f8f8; -fx-background-radius: 10; -fx-padding: 10; -fx-border-color: #eee; -fx-border-radius: 10;");
            content.getChildren().add(banniere);
        }

        // ===== RDVs du jour =====
        Label lblRdv = new Label("Rendez-vous");
        lblRdv.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");
        content.getChildren().add(lblRdv);

        try {
            List<rdv> rdvsDuJour = rdvService.findAll().stream()
                    .filter(r -> r.getDate().equals(date.toString()))
                    .collect(Collectors.toList());

            if (rdvsDuJour.isEmpty()) {
                Label noRdv = new Label("Aucun rendez-vous");
                noRdv.setStyle("-fx-font-size: 12; -fx-text-fill: #999; -fx-font-style: italic;");
                content.getChildren().add(noRdv);
            } else {
                for (rdv r : rdvsDuJour) {
                    VBox card = new VBox(4);
                    card.setStyle("-fx-background-color: #eff6ff; -fx-border-color: #bfdbfe; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12;");
                    Label heure = new Label(r.getHdebut() + " - " + r.getHfin());
                    heure.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");
                    Label motif = new Label("Motif : " + r.getMotif());
                    motif.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");
                    Label statut = new Label(r.getStatut());
                    statut.setStyle("-fx-font-size: 11; -fx-text-fill: #888;");
                    card.getChildren().addAll(heure, motif, statut);
                    content.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        // ===== Disponibilités personnalisées =====
        Label lblDispo = new Label("Disponibilités personnalisées");
        lblDispo.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
        content.getChildren().add(lblDispo);

        List<disponibilite> disposDuJour = new ArrayList<>();
        List<disponibilite> seancesAnnulees = new ArrayList<>();
        try {
            List<disponibilite> allDispos = dispoService.findAll().stream()
                    .filter(d -> d.getDate_dispo().equals(date.toString()) && d.getMed_id() == MED_ID)
                    .collect(Collectors.toList());

            for (disponibilite d : allDispos) {
                if ("non_disponible".equals(d.getStatut())) {
                    seancesAnnulees.add(d);
                } else {
                    disposDuJour.add(d);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        if (disposDuJour.isEmpty()) {
            Label noDispo = new Label("Aucune disponibilité personnalisée");
            noDispo.setStyle("-fx-font-size: 12; -fx-text-fill: #999; -fx-font-style: italic;");
            content.getChildren().add(noDispo);
        } else {
            for (disponibilite d : disposDuJour) {
                HBox card = new HBox(10);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12;");
                Label info = new Label(d.getHdebut() + " - " + d.getH_fin() + "  ·  " + d.getStatut() + "  ·  " + d.getNbr_h() + "h");
                info.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #166534;");
                card.getChildren().add(info);
                content.getChildren().add(card);
            }
        }

        // ===== Horaires par défaut =====
        VBox defaultBox = new VBox(8);
        defaultBox.setAlignment(Pos.CENTER);
        defaultBox.setStyle("-fx-background-color: #fffbeb; -fx-border-color: #fde68a; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 15;");

        Label lblDefault = new Label("Horaires par défaut de ce jour");
        lblDefault.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #b45309;");
        defaultBox.getChildren().add(lblDefault);

        String[][] horaires = HORAIRES_DEFAULT.getOrDefault(jour, new String[][]{});
        Set<String> annuleesKeys = seancesAnnulees.stream()
                .map(d -> d.getHdebut() + "-" + d.getH_fin())
                .collect(Collectors.toSet());

        if (horaires.length == 0) {
            Label noHoraire = new Label("Aucun horaire fixe ce jour");
            noHoraire.setStyle("-fx-font-size: 12; -fx-text-fill: #999; -fx-font-style: italic;");
            defaultBox.getChildren().add(noHoraire);
        } else {
            for (String[] h : horaires) {
                String key = h[0] + "-" + h[1];
                Label lbl;
                if (annuleesKeys.contains(key)) {
                    lbl = new Label("🚫 " + h[0] + " - " + h[1] + "  (annulée)");
                    lbl.setStyle("-fx-font-size: 13; -fx-text-fill: #e53935; -fx-strikethrough: true;");
                } else {
                    lbl = new Label(h[0] + " - " + h[1]);
                    lbl.setStyle("-fx-font-size: 13; -fx-text-fill: #555;");
                }
                defaultBox.getChildren().add(lbl);
            }
        }
        content.getChildren().add(defaultBox);

        // ===== Actions (seulement si pas passé) =====
        if (!isPasse) {
            // Annulation séances (pas samedi/dimanche)
            if (jour != DayOfWeek.SATURDAY && jour != DayOfWeek.SUNDAY) {
                content.getChildren().add(new Separator());

                HBox annulationBox = new HBox(10);
                annulationBox.setAlignment(Pos.CENTER);

                boolean matinAnnulee = annuleesKeys.contains("09:00-12:00");
                Button btnMatin = new Button(matinAnnulee ? "✅ Restaurer matin (09h-12h)" : "Annuler matin (09h-12h)");
                btnMatin.setStyle(matinAnnulee
                        ? "-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-font-size: 11; -fx-background-radius: 8; -fx-padding: 8 12; -fx-cursor: hand; -fx-border-color: #a5d6a7; -fx-border-radius: 8;"
                        : "-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-size: 11; -fx-background-radius: 8; -fx-padding: 8 12; -fx-cursor: hand; -fx-border-color: #ef9a9a; -fx-border-radius: 8;");
                btnMatin.setOnAction(e -> {
                    toggleCancelSession(date, "09:00", "12:00", matinAnnulee, seancesAnnulees);
                    dialog.close();
                    ouvrirPopupJour(date);
                });

                boolean soirAnnulee = annuleesKeys.contains("14:00-17:00");
                Button btnSoir = new Button(soirAnnulee ? "✅ Restaurer soir (14h-17h)" : "Annuler soir (14h-17h)");
                btnSoir.setStyle(soirAnnulee
                        ? "-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-font-size: 11; -fx-background-radius: 8; -fx-padding: 8 12; -fx-cursor: hand; -fx-border-color: #a5d6a7; -fx-border-radius: 8;"
                        : "-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-size: 11; -fx-background-radius: 8; -fx-padding: 8 12; -fx-cursor: hand; -fx-border-color: #ef9a9a; -fx-border-radius: 8;");
                btnSoir.setOnAction(e -> {
                    toggleCancelSession(date, "14:00", "17:00", soirAnnulee, seancesAnnulees);
                    dialog.close();
                    ouvrirPopupJour(date);
                });

                annulationBox.getChildren().addAll(btnMatin, btnSoir);
                content.getChildren().add(annulationBox);
            }

            // Extra
            String[] extra = EXTRA_AUTORISE.get(jour);
            if (jour == DayOfWeek.SATURDAY) {
                Label msgSamedi = new Label("🔒 Le samedi est limité à 09h00 - 13h00. Aucun ajout n'est possible.");
                msgSamedi.setWrapText(true);
                msgSamedi.setStyle("-fx-font-size: 12; -fx-text-fill: #888; -fx-background-color: #f5f5f5; -fx-background-radius: 8; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 8;");
                content.getChildren().add(msgSamedi);
            } else if (extra != null) {
                content.getChildren().add(new Separator());

                Label lblExtra = new Label("Disponibilité supplémentaire");
                lblExtra.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #333;");
                content.getChildren().add(lblExtra);

                boolean extraActif = disposDuJour.stream()
                        .anyMatch(d -> d.getHdebut().startsWith(extra[0]) && d.getH_fin().startsWith(extra[1]));

                HBox extraBox = new HBox(15);
                extraBox.setAlignment(Pos.CENTER_LEFT);
                extraBox.setStyle("-fx-background-color: #f8f8f8; -fx-background-radius: 8; -fx-padding: 12; -fx-border-color: #eee; -fx-border-radius: 8;");

                Label extraLabel = new Label("Créneau : " + extra[0] + " - " + extra[1]);
                extraLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");

                Label extraStatut = new Label(extraActif ? "✅ Activé" : "— Non activé");
                extraStatut.setStyle(extraActif
                        ? "-fx-font-size: 11; -fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                        : "-fx-font-size: 11; -fx-text-fill: #999;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button btnToggle = new Button(extraActif ? "Supprimer" : "Ajouter");
                btnToggle.setStyle(extraActif
                        ? "-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-size: 11; -fx-background-radius: 8; -fx-padding: 6 15; -fx-cursor: hand; -fx-border-color: #ef9a9a; -fx-border-radius: 8;"
                        : "-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-size: 11; -fx-background-radius: 8; -fx-padding: 6 15; -fx-cursor: hand;");

                final String[] extraFinal = extra;
                btnToggle.setOnAction(e -> {
                    toggleExtraDispo(date, extraFinal, extraActif, disposDuJour);
                    dialog.close();
                    ouvrirPopupJour(date);
                });

                extraBox.getChildren().addAll(extraLabel, extraStatut, spacer, btnToggle);
                content.getChildren().add(extraBox);
            }
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Button btnFermer = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        btnFermer.setText("Fermer");
        btnFermer.setStyle("-fx-background-color: #1a1f36; -fx-text-fill: white; -fx-font-size: 12; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");

        dialog.showAndWait();
    }

    // ==================== TOGGLE CANCEL SESSION ====================
    private void toggleCancelSession(LocalDate date, String hdebut, String hfin, boolean estAnnulee, List<disponibilite> seancesAnnulees) {
        try {
            if (estAnnulee) {
                // Restaurer : supprimer la dispo "non_disponible"
                Optional<disponibilite> toDelete = seancesAnnulees.stream()
                        .filter(d -> d.getHdebut().startsWith(hdebut) && d.getH_fin().startsWith(hfin))
                        .findFirst();
                if (toDelete.isPresent()) {
                    dispoService.delete(toDelete.get());
                }
            } else {
                // Annuler : créer une dispo "non_disponible"
                int nbrH = Integer.parseInt(hfin.split(":")[0]) - Integer.parseInt(hdebut.split(":")[0]);
                disponibilite d = new disponibilite(date.toString(), hdebut, hfin, "non_disponible", nbrH, MED_ID);
                dispoService.insert(d);
            }
            chargerStats();
        } catch (SQLException e) {
            System.err.println("Erreur toggle session : " + e.getMessage());
        }
    }

    // ==================== TOGGLE EXTRA DISPO ====================
    private void toggleExtraDispo(LocalDate date, String[] extra, boolean extraActif, List<disponibilite> disposDuJour) {
        try {
            if (extraActif) {
                // Supprimer
                Optional<disponibilite> toDelete = disposDuJour.stream()
                        .filter(d -> d.getHdebut().startsWith(extra[0]) && d.getH_fin().startsWith(extra[1]))
                        .findFirst();
                if (toDelete.isPresent()) {
                    dispoService.delete(toDelete.get());
                }
            } else {
                // Ajouter
                int nbrH = Integer.parseInt(extra[1].split(":")[0]) - Integer.parseInt(extra[0].split(":")[0]);
                disponibilite d = new disponibilite(date.toString(), extra[0], extra[1], "disponible", nbrH, MED_ID);
                dispoService.insert(d);
            }
            chargerStats();
        } catch (SQLException e) {
            System.err.println("Erreur toggle extra : " + e.getMessage());
        }
    }

    // ==================== UTILITAIRES ====================
    private String formatDateFr(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " +
                date.getDayOfMonth() + " " +
                date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " +
                date.getYear();
    }

    private void retourRdv() {
        try {
            Node content = FXMLLoader.load(getClass().getResource("/AdminRdv.fxml"));
            StackPane parent = (StackPane) calendarGrid.getScene().lookup("#contentArea");
            if (parent != null) {
                parent.getChildren().clear();
                parent.getChildren().add(content);
            }
        } catch (IOException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}