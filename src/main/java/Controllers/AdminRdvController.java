package Controllers;

import Models.rdv;
import Services.RdvService;
import Services.EmailService;
import Services.SocketClient;
import Services.ChatMessageService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.Interpolator;

import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AdminRdvController {

    // ══════════════════════════════════════════════════════════════
    //  MÉDECIN — socket ID = "medecin_sarah_amrani"
    //  Identique à ce que le patient génère via slugMedecin()
    // ══════════════════════════════════════════════════════════════
    private static final int    MEDECIN_USER_ID   = 1;
    private static final String MEDECIN_NOM       = "Dr. Sarah Amrani";
    private static final String MEDECIN_NOM_SLUG  = "Sarah Amrani";
    private static final String MEDECIN_SOCKET_ID = SocketClient.slugMedecin(MEDECIN_NOM_SLUG);

    @FXML private VBox             rdvTableContainer;
    @FXML private Label            lblTodayCount, lblPendingCount, lblDoneCount;
    @FXML private Button           btnGererDispo, btnReset;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterStatut, filterDate, sortCombo;
    @FXML private Label            lblResultCount, lblEmptyMsg;
    @FXML private VBox             emptyState;
    @FXML private StackPane        fabContainer;

    private final RdvService         rdvService         = new RdvService();
    private final EmailService       emailService       = new EmailService();
    private final ChatMessageService chatMessageService = new ChatMessageService();

    private List<rdv> tousLesRdvs    = new ArrayList<>();
    private StackPane fabButton;
    private Label     fabBadge;
    private VBox      chatListPanel;
    private boolean   chatPanelVisible = false;
    private int       totalUnread      = 0;
    private StackPane toastContainer;
    private Consumer<String[]> fabChatListener;

    // ══════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        initFiltres();
        chargerDonnees();

        searchField.textProperty().addListener((obs, old, val) -> appliquerFiltres());
        filterStatut.valueProperty().addListener((obs, old, val) -> appliquerFiltres());
        filterDate.valueProperty().addListener((obs, old, val) -> appliquerFiltres());
        sortCombo.valueProperty().addListener((obs, old, val) -> appliquerFiltres());

        btnGererDispo.setOnAction(e -> naviguerVersDispo());
        btnReset.setOnAction(e -> resetFiltres());

        setupToastContainer();
        setupFAB();
        startSocketNotifListener();
    }

    // ══════════════════════════════════════════════════════════════
    //  🔔 TOAST — barre de notif animée en haut
    // ══════════════════════════════════════════════════════════════

    private void setupToastContainer() {
        toastContainer = new StackPane();
        toastContainer.setPickOnBounds(false);
        toastContainer.setMouseTransparent(true);
        toastContainer.setStyle("-fx-padding: 14 0 0 0;");
        StackPane.setAlignment(toastContainer, Pos.TOP_CENTER);
        fabContainer.getChildren().add(0, toastContainer);
    }

    private void afficherToast(String nomPatient, String texteMessage) {
        HBox toast = new HBox(12);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMaxWidth(400);
        toast.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 13 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 16, 0, 0, 5);"
        );

        Label icone = new Label("💬");
        icone.setStyle("-fx-font-size: 20;");

        VBox textes = new VBox(3);
        Label titre = new Label("Nouveau message — " + nomPatient);
        titre.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        String apercu = texteMessage.length() > 45 ? texteMessage.substring(0, 45) + "…" : texteMessage;
        Label preview = new Label(apercu);
        preview.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");
        textes.getChildren().addAll(titre, preview);
        HBox.setHgrow(textes, Priority.ALWAYS);

        Circle dot = new Circle(5, Color.web("#4ade80"));
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(dot.radiusProperty(), 5.0)),
                new KeyFrame(Duration.millis(600),  new KeyValue(dot.radiusProperty(), 7.5)),
                new KeyFrame(Duration.millis(1200), new KeyValue(dot.radiusProperty(), 5.0))
        );
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toast.getChildren().addAll(icone, textes, spacer, dot);

        StackPane wrapper = new StackPane(toast);
        wrapper.setPickOnBounds(false);
        StackPane.setAlignment(toast, Pos.TOP_CENTER);
        toastContainer.getChildren().add(wrapper);

        // ── Entrée ──
        toast.setTranslateY(-90);
        toast.setOpacity(0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(340), toast);
        slideIn.setFromY(-90);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(340), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition entree = new ParallelTransition(slideIn, fadeIn);

        // ── Sortie ──
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(280), toast);
        slideOut.setFromY(0);
        slideOut.setToY(-90);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(280), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        ParallelTransition sortie = new ParallelTransition(slideOut, fadeOut);
        sortie.setOnFinished(e -> {
            toastContainer.getChildren().remove(wrapper);
            pulse.stop();
        });

        SequentialTransition sequence = new SequentialTransition(
                entree,
                new PauseTransition(Duration.seconds(3)),
                sortie
        );
        sequence.play();
    }

    // ══════════════════════════════════════════════════════════════
    //  SOCKET LISTENER — badge + toast en temps réel
    // ══════════════════════════════════════════════════════════════
    private void startSocketNotifListener() {
        new Thread(() -> {
            try {
                SocketClient client = SocketClient.getInstance("medecin", MEDECIN_SOCKET_ID);
                if (!client.isConnecte()) {
                    client.connecter();
                    Thread.sleep(300);
                }

                fabChatListener = (String[] data) -> {
                    if (data.length < 3) return;
                    String senderSocketId = data[0];
                    String texte          = data[2];
                    String nomPatient     = nomDepuisSocketId(senderSocketId);

                    Platform.runLater(() -> {
                        // 1. Badge +1
                        updateBadge(totalUnread + 1);

                        // 2. Flash rouge sur la bulle
                        fabButton.getChildren().stream()
                                .filter(n -> n instanceof Circle).map(n -> (Circle) n)
                                .findFirst().ifPresent(bg -> {
                                    bg.setFill(Color.web("#e24b4a"));
                                    new Timeline(new KeyFrame(Duration.millis(700),
                                            e -> bg.setFill(Color.web("#1a73e8")))).play();
                                });

                        // 3. Toast animé en haut
                        afficherToast(nomPatient, texte);
                    });
                };

                client.addOnChatRecu(fabChatListener);
                System.out.println("✅ [NOTIF] Listener actif — " + MEDECIN_SOCKET_ID);

            } catch (Exception e) {
                System.err.println("⚠️ [NOTIF] " + e.getMessage());
            }
        }).start();
    }

    private String nomDepuisSocketId(String socketId) {
        try {
            int id = Integer.parseInt(socketId.replace("patient_", ""));
            return obtenirNomPatient(id);
        } catch (Exception e) {
            return socketId;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  FILTRES & DONNÉES
    // ══════════════════════════════════════════════════════════════
    private void initFiltres() {
        filterStatut.getItems().addAll("Tous les statuts", "⏳ En attente", "✓ Confirmé", "✕ Annulé", "⏰ Expiré", "📁 Passé");
        filterStatut.setValue("Tous les statuts");
        filterDate.getItems().addAll("Toutes les dates", "📅 Aujourd'hui", "📅 Cette semaine", "📅 Ce mois", "📅 Passés", "📅 À venir");
        filterDate.setValue("Toutes les dates");
        sortCombo.getItems().addAll("📆 Date ↓ (récent)", "📆 Date ↑ (ancien)", "👤 Patient A→Z", "👤 Patient Z→A", "📊 Statut A→Z");
        sortCombo.setValue("📆 Date ↓ (récent)");
    }

    private void resetFiltres() {
        searchField.clear();
        filterStatut.setValue("Tous les statuts");
        filterDate.setValue("Toutes les dates");
        sortCombo.setValue("📆 Date ↓ (récent)");
    }

    private void chargerDonnees() {
        try {
            tousLesRdvs = rdvService.findByMedecinUserId(MEDECIN_USER_ID);
            System.out.println("✅ [ADMIN] " + tousLesRdvs.size() + " RDV | socket : " + MEDECIN_SOCKET_ID);
            calculerStats(tousLesRdvs);
            appliquerFiltres();
        } catch (SQLException e) {
            System.err.println("❌ " + e.getMessage());
        }
    }

    private void calculerStats(List<rdv> rdvs) {
        String today = LocalDate.now().toString();
        LocalDate td  = LocalDate.now();
        long todayC   = rdvs.stream().filter(r -> r.getDate().equals(today)).count();
        long pendingC = rdvs.stream().filter(r -> {
            boolean p = false;
            try { p = LocalDate.parse(r.getDate()).isBefore(td); } catch (Exception ignored) {}
            return r.getStatut().toLowerCase().contains("attente") && !p;
        }).count();
        long doneC = rdvs.stream().filter(r -> r.getStatut().toLowerCase().contains("confirm")).count();
        lblTodayCount.setText(todayC + " RDV");
        lblPendingCount.setText(pendingC + " Demandes");
        lblDoneCount.setText(doneC + " Consultations");
    }

    private void appliquerFiltres() {
        List<rdv> res = new ArrayList<>(tousLesRdvs);

        String q = searchField.getText();
        if (q != null && !q.trim().isEmpty()) {
            String low = q.trim().toLowerCase();
            res = res.stream().filter(r ->
                    obtenirNomPatient(r.getPatient_id()).toLowerCase().contains(low)
                            || (r.getMotif()   != null && r.getMotif().toLowerCase().contains(low))
                            || (r.getMessage() != null && r.getMessage().toLowerCase().contains(low))
                            || (r.getStatut()  != null && r.getStatut().toLowerCase().contains(low))
                            || (r.getDate()    != null && r.getDate().toLowerCase().contains(low))
            ).collect(Collectors.toList());
        }

        String sf = filterStatut.getValue();
        if (sf != null && !sf.equals("Tous les statuts")) {
            res = res.stream().filter(r -> {
                String s = r.getStatut().toLowerCase();
                if (sf.contains("Expiré"))   return s.contains("expir");
                if (sf.contains("Passé"))    return s.contains("passe") || s.contains("passé");
                if (sf.contains("attente"))  return s.contains("attente");
                if (sf.contains("Confirmé")) return s.contains("confirm");
                if (sf.contains("Annulé"))   return s.contains("annul");
                return true;
            }).collect(Collectors.toList());
        }

        String df = filterDate.getValue();
        if (df != null && !df.equals("Toutes les dates")) {
            LocalDate today = LocalDate.now();
            res = res.stream().filter(r -> {
                try {
                    LocalDate d = LocalDate.parse(r.getDate());
                    if (df.contains("Aujourd'hui"))   return d.equals(today);
                    if (df.contains("Cette semaine")) {
                        LocalDate lun = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                        LocalDate dim = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                        return !d.isBefore(lun) && !d.isAfter(dim);
                    }
                    if (df.contains("Ce mois"))  return d.getMonth() == today.getMonth() && d.getYear() == today.getYear();
                    if (df.contains("Passés"))   return d.isBefore(today);
                    if (df.contains("À venir"))  return !d.isBefore(today);
                } catch (Exception ignored) {}
                return true;
            }).collect(Collectors.toList());
        }

        String tri = sortCombo.getValue();
        if (tri != null) {
            if (tri.contains("Date ↓"))
                res.sort((a, b) -> { try { return LocalDate.parse(b.getDate()).compareTo(LocalDate.parse(a.getDate())); } catch (Exception e) { return 0; } });
            else if (tri.contains("Date ↑"))
                res.sort((a, b) -> { try { return LocalDate.parse(a.getDate()).compareTo(LocalDate.parse(b.getDate())); } catch (Exception e) { return 0; } });
            else if (tri.contains("A→Z"))
                res.sort(Comparator.comparing(r -> obtenirNomPatient(r.getPatient_id()).toLowerCase()));
            else if (tri.contains("Z→A"))
                res.sort((a, b) -> obtenirNomPatient(b.getPatient_id()).compareToIgnoreCase(obtenirNomPatient(a.getPatient_id())));
            else if (tri.contains("Statut"))
                res.sort(Comparator.comparing(r -> r.getStatut() != null ? r.getStatut().toLowerCase() : ""));
        }

        afficherLignes(res);
        lblResultCount.setText(res.size() + " résultat" + (res.size() > 1 ? "s" : ""));
        emptyState.setVisible(res.isEmpty());
        emptyState.setManaged(res.isEmpty());
        if (res.isEmpty())
            lblEmptyMsg.setText(q != null && !q.trim().isEmpty()
                    ? "Aucun résultat pour \"" + q.trim() + "\""
                    : "Aucun rendez-vous trouvé");
    }

    // ══════════════════════════════════════════════════════════════
    //  AFFICHER LIGNES
    // ══════════════════════════════════════════════════════════════
    private void afficherLignes(List<rdv> rdvs) {
        rdvTableContainer.getChildren().clear();
        for (int i = 0; i < rdvs.size(); i++) {
            rdv r = rdvs.get(i);
            HBox ligne = new HBox();
            ligne.setAlignment(Pos.CENTER_LEFT);
            String bg = i % 2 == 0 ? "white" : "#f8fafc";
            ligne.setStyle("-fx-padding: 15 25; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0; -fx-background-color: " + bg + ";");
            String ns = ligne.getStyle();
            ligne.setOnMouseEntered(e -> ligne.setStyle("-fx-padding: 15 25; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0; -fx-background-color: #eff6ff;"));
            ligne.setOnMouseExited(e -> ligne.setStyle(ns));

            Label ref = new Label("#RDV-" + r.getId());
            ref.setPrefWidth(100);
            ref.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

            String nomP = obtenirNomPatient(r.getPatient_id());
            HBox patBox = new HBox(8);
            patBox.setAlignment(Pos.CENTER_LEFT); patBox.setPrefWidth(280);
            Label av = new Label(getInitiales(nomP));
            av.setMinWidth(34); av.setMinHeight(34); av.setMaxWidth(34); av.setMaxHeight(34);
            av.setAlignment(Pos.CENTER);
            av.setStyle("-fx-background-color: " + getAvatarColor(nomP) + "; -fx-background-radius: 17; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold;");
            VBox pi = new VBox(2);
            Label pn = new Label(nomP);
            pn.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            Label pm = new Label(r.getMotif());
            pm.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");
            pi.getChildren().addAll(pn, pm);
            patBox.getChildren().addAll(av, pi);

            VBox dateBox = new VBox(2);
            dateBox.setPrefWidth(200);
            Label dl = new Label(formaterDateAffichage(r.getDate()));
            dl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            String h = r.getHdebut() != null ? r.getHdebut() : "";
            if (h.length() > 5) h = h.substring(0, 5);
            Label hl = new Label("🕐 " + h);
            hl.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");
            dateBox.getChildren().addAll(dl, hl);

            Label statLabel = new Label();
            statLabel.setPrefWidth(150);
            String couleur, statutText;
            String sl = r.getStatut().toLowerCase();
            boolean ep = false;
            try { ep = LocalDate.parse(r.getDate()).isBefore(LocalDate.now()); } catch (Exception ignored) {}

            if (sl.contains("annul"))                              { couleur = "#dc2626"; statutText = "✕ Annulé"; }
            else if (sl.contains("expir"))                         { couleur = "#9ca3af"; statutText = "⏰ Expiré"; }
            else if (sl.contains("passe") || sl.contains("passé")) { couleur = "#6366f1"; statutText = "📁 Passé"; }
            else if (ep && sl.contains("attente"))                 { couleur = "#9ca3af"; statutText = "⏰ Expiré"; }
            else if (ep && sl.contains("confirm"))                 { couleur = "#6366f1"; statutText = "📁 Passé"; }
            else if (sl.contains("confirm"))                       { couleur = "#16a34a"; statutText = "✓ Confirmé"; }
            else if (sl.contains("attente"))                       { couleur = "#f59e0b"; statutText = "⏳ En attente"; }
            else                                                    { couleur = "#64748b"; statutText = r.getStatut(); }

            statLabel.setText(statutText);
            statLabel.setStyle("-fx-font-size: 11; -fx-text-fill: " + couleur + "; -fx-background-color: " + couleur + "18; -fx-background-radius: 12; -fx-padding: 5 12; -fx-font-weight: bold;");

            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER_LEFT);
            boolean noAction = sl.contains("annul") || sl.contains("expir")
                    || sl.contains("passe") || sl.contains("passé") || ep;

            Button bChat = new Button("💬");
            bChat.setTooltip(new Tooltip("Discuter avec le patient"));
            bChat.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1a73e8; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;");
            bChat.setOnMouseEntered(ev -> bChat.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
            bChat.setOnMouseExited(ev -> bChat.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1a73e8; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
            bChat.setOnAction(ev -> ouvrirChatAvecPatient(r));
            actions.getChildren().add(bChat);

            if (sl.contains("attente") && !ep) {
                Button bConf = new Button("✓");
                bConf.setTooltip(new Tooltip("Confirmer"));
                bConf.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;");
                bConf.setOnMouseEntered(ev -> bConf.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
                bConf.setOnMouseExited(ev -> bConf.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
                bConf.setOnAction(ev -> ouvrirPopupConfirmation(r));
                actions.getChildren().add(bConf);
            }

            if (!noAction) {
                Button bEdit = new Button("✏");
                bEdit.setTooltip(new Tooltip("Modifier"));
                bEdit.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;");
                bEdit.setOnMouseEntered(ev -> bEdit.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
                bEdit.setOnMouseExited(ev -> bEdit.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
                bEdit.setOnAction(ev -> ouvrirPopupModifierAdmin(r));

                Button bDel = new Button("🗑");
                bDel.setTooltip(new Tooltip("Annuler le RDV"));
                bDel.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;");
                bDel.setOnMouseEntered(ev -> bDel.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
                bDel.setOnMouseExited(ev -> bDel.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-font-size: 13; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;"));
                bDel.setOnAction(ev -> ouvrirPopupAnnulation(r));
                actions.getChildren().addAll(bEdit, bDel);
            }

            ligne.getChildren().addAll(ref, patBox, dateBox, statLabel, actions);
            rdvTableContainer.getChildren().add(ligne);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  OUVRIR CHAT
    // ══════════════════════════════════════════════════════════════
    private void ouvrirChatAvecPatient(rdv r) {
        try {
            String medecinUserId = MEDECIN_SOCKET_ID;
            String patientUserId = SocketClient.userIdPatient(r.getPatient_id());
            String patientNom    = obtenirNomPatient(r.getPatient_id());

            SocketClient client = SocketClient.getInstance("medecin", medecinUserId);
            if (!client.isConnecte()) {
                client.connecter();
                Thread.sleep(300);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminChat.fxml"));
            VBox root = loader.load();
            AdminChatController ctrl = loader.getController();
            ctrl.initChat(medecinUserId, MEDECIN_NOM, patientUserId, patientNom);

            chatMessageService.marquerCommeLu(medecinUserId, patientUserId);
            refreshUnreadBadge();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("💬 Chat — " + patientNom);
            stage.setScene(new Scene(root, 480, 580));
            stage.setResizable(true);
            stage.setMinWidth(380);
            stage.setMinHeight(450);
            stage.initOwner(rdvTableContainer.getScene().getWindow());
            stage.show();

            stage.setOnHidden(ev -> {
                ctrl.onFermeture();
                refreshUnreadBadge();
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir le chat : " + ex.getMessage());
            alert.showAndWait();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  FAB
    // ══════════════════════════════════════════════════════════════
    private void setupFAB() {
        Circle bgCircle = new Circle(27);
        bgCircle.setFill(Color.web("#1a73e8"));

        Label iconLabel = new Label("💬");
        iconLabel.setFont(Font.font(20));

        Circle badgeBg = new Circle(10);
        badgeBg.setFill(Color.web("#e24b4a"));
        badgeBg.setStroke(Color.web("#f0f4f8"));
        badgeBg.setStrokeWidth(2.5);

        fabBadge = new Label("0");
        fabBadge.setFont(Font.font("System", FontWeight.BOLD, 9));
        fabBadge.setTextFill(Color.WHITE);

        StackPane badge = new StackPane(badgeBg, fabBadge);
        badge.setMaxSize(20, 20);
        badge.setVisible(false);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);

        fabButton = new StackPane(bgCircle, iconLabel, badge);
        fabButton.setMaxSize(54, 54);
        fabButton.setStyle("-fx-cursor: hand;");
        StackPane.setAlignment(fabButton, Pos.BOTTOM_RIGHT);

        fabButton.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), fabButton);
            st.setToX(1.1); st.setToY(1.1); st.play();
            bgCircle.setFill(Color.web("#1558b0"));
        });
        fabButton.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), fabButton);
            st.setToX(1.0); st.setToY(1.0); st.play();
            bgCircle.setFill(Color.web("#1a73e8"));
        });
        fabButton.setOnMouseClicked(e -> toggleChatPanel());

        chatListPanel = buildChatListPanel();
        chatListPanel.setVisible(false);
        chatListPanel.setManaged(false);
        StackPane.setAlignment(chatListPanel, Pos.BOTTOM_RIGHT);
        chatListPanel.setTranslateY(-64);

        fabContainer.getChildren().addAll(chatListPanel, fabButton);
        refreshUnreadBadge();
    }

    private VBox buildChatListPanel() {
        VBox panel = new VBox(0);
        panel.setMaxWidth(270);
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 16, 0, 0, 4);"
        );

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #1a73e8; -fx-background-radius: 14 14 0 0; -fx-padding: 12 16;");
        Label title = new Label("Messagerie");
        title.setFont(Font.font("System", FontWeight.BOLD, 13));
        title.setTextFill(Color.WHITE);
        header.getChildren().addAll(new Circle(4, Color.web("#4ade80")), title);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(220);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        VBox liste = new VBox(2);
        liste.setStyle("-fx-padding: 8;");

        List<Integer> vus = new ArrayList<>();
        for (rdv r : tousLesRdvs) {
            int pid = r.getPatient_id();
            if (!vus.contains(pid)) vus.add(pid);
        }

        if (vus.isEmpty()) {
            Label empty = new Label("Aucune conversation");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12; -fx-padding: 20;");
            liste.getChildren().add(empty);
        } else {
            for (int pid : vus) {
                String nom   = obtenirNomPatient(pid);
                int unread   = chatMessageService.getUnreadCount(MEDECIN_SOCKET_ID, SocketClient.userIdPatient(pid));
                String motif = tousLesRdvs.stream().filter(r -> r.getPatient_id() == pid)
                        .findFirst().map(rdv::getMotif).orElse("");
                liste.getChildren().add(buildConvRow(nom, motif, unread, pid));
            }
        }

        scroll.setContent(liste);
        panel.getChildren().addAll(header, scroll);
        return panel;
    }

    private HBox buildConvRow(String nom, String motif, int unread, int pid) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 8 10; -fx-background-radius: 10; -fx-cursor: hand;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-padding: 8 10; -fx-background-radius: 10; -fx-cursor: hand; -fx-background-color: #eff6ff;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-padding: 8 10; -fx-background-radius: 10; -fx-cursor: hand;"));

        StackPane avPane = new StackPane();
        avPane.setMinSize(36, 36); avPane.setMaxSize(36, 36);
        Circle c = new Circle(18);
        c.setFill(Color.web(getAvatarColor(nom)));
        Label ini = new Label(getInitiales(nom));
        ini.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: white;");
        avPane.getChildren().addAll(c, ini);

        VBox txt = new VBox(3);
        HBox.setHgrow(txt, Priority.ALWAYS);
        txt.setMinWidth(0);
        Label nl = new Label(nom);
        nl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        nl.setMaxWidth(Double.MAX_VALUE);
        Label ml = new Label(motif != null ? motif : "");
        ml.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");
        ml.setMaxWidth(140);
        txt.getChildren().addAll(nl, ml);
        row.getChildren().addAll(avPane, txt);

        if (unread > 0) {
            StackPane bp = new StackPane();
            bp.setMinSize(20, 20); bp.setMaxSize(20, 20);
            Circle bgBadge = new Circle(9, Color.web("#e24b4a"));
            Label cnt = new Label(unread > 9 ? "9+" : String.valueOf(unread));
            cnt.setStyle("-fx-font-size: 9; -fx-font-weight: bold; -fx-text-fill: white;");
            bp.getChildren().addAll(bgBadge, cnt);
            row.getChildren().add(bp);
        }

        row.setOnMouseClicked(e -> {
            chatPanelVisible = false;
            chatListPanel.setVisible(false);
            chatListPanel.setManaged(false);
            tousLesRdvs.stream()
                    .filter(r -> r.getPatient_id() == pid)
                    .findFirst()
                    .ifPresent(r -> ouvrirChatAvecPatient(r));
        });
        return row;
    }

    private void toggleChatPanel() {
        chatPanelVisible = !chatPanelVisible;

        if (chatPanelVisible) {
            fabContainer.getChildren().remove(chatListPanel);
            chatListPanel = buildChatListPanel();
            chatListPanel.setVisible(false);
            chatListPanel.setManaged(false);
            StackPane.setAlignment(chatListPanel, Pos.BOTTOM_RIGHT);
            chatListPanel.setTranslateY(-64);
            fabContainer.getChildren().add(0, chatListPanel);
            chatListPanel.setManaged(true);
            chatListPanel.setVisible(true);

            FadeTransition ft = new FadeTransition(Duration.millis(180), chatListPanel);
            ft.setFromValue(0); ft.setToValue(1); ft.play();

            ScaleTransition st = new ScaleTransition(Duration.millis(180), chatListPanel);
            st.setFromX(0.88); st.setFromY(0.88);
            st.setToX(1.0); st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();

        } else {
            FadeTransition ft = new FadeTransition(Duration.millis(150), chatListPanel);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> {
                chatListPanel.setVisible(false);
                chatListPanel.setManaged(false);
            });
            ft.play();
        }
    }

    private void refreshUnreadBadge() {
        new Thread(() -> {
            int count = chatMessageService.getTotalUnread(MEDECIN_SOCKET_ID);
            Platform.runLater(() -> updateBadge(count));
        }).start();
    }

    private void updateBadge(int count) {
        totalUnread = count;
        fabButton.getChildren().stream()
                .filter(n -> n instanceof StackPane).map(n -> (StackPane) n)
                .findFirst().ifPresent(badge -> {
                    badge.setVisible(count > 0);
                    fabBadge.setText(count > 9 ? "9+" : String.valueOf(count));
                    if (count > 0) {
                        ScaleTransition st = new ScaleTransition(Duration.millis(200), badge);
                        st.setFromX(1.0); st.setFromY(1.0);
                        st.setToX(1.5); st.setToY(1.5);
                        st.setAutoReverse(true); st.setCycleCount(2); st.play();
                    }
                });
    }

    // ══════════════════════════════════════════════════════════════
    //  POPUPS
    // ══════════════════════════════════════════════════════════════
    private void ouvrirPopupConfirmation(rdv r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirmer le RDV");
        VBox content = new VBox(15);
        content.setPrefWidth(450); content.setAlignment(Pos.CENTER); content.setStyle("-fx-padding: 30 40;");
        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 36; -fx-background-color: #dcfce7; -fx-background-radius: 30; -fx-padding: 15;");
        Label titre = new Label("Confirmer ce rendez-vous ?");
        titre.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        VBox infoCard = new VBox(8);
        infoCard.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-padding: 15; -fx-border-color: #e2e8f0; -fx-border-radius: 10;");
        infoCard.getChildren().addAll(
                creerLigneInfo("👤 Patient", obtenirNomPatient(r.getPatient_id())),
                creerLigneInfo("📅 Date",    formaterDateAffichage(r.getDate())),
                creerLigneInfo("🕐 Heure",   r.getHdebut() != null ? r.getHdebut().substring(0, 5) : "N/A"),
                creerLigneInfo("📋 Motif",   r.getMotif()));
        String em = obtenirEmailPatient(r);
        CheckBox ck = new CheckBox("📧 Envoyer un email de confirmation à :");
        ck.setSelected(true); ck.setStyle("-fx-font-size: 13; -fx-text-fill: #334155;");
        Label le = new Label("   " + em);
        le.setStyle("-fx-font-size: 12; -fx-text-fill: #16a34a; -fx-font-weight: bold;");
        content.getChildren().addAll(icon, titre, infoCard, ck, le);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.setText("✓ Confirmer le RDV");
        btnOk.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 30; -fx-cursor: hand;");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Annuler");
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                rdvService.updateStatut(r.getId(), "confirmé");
                if (ck.isSelected()) envoyerEmailEnArrierePlan("confirmation", r, em, null, null);
                chargerDonnees();
            } catch (SQLException ex) { System.err.println(ex.getMessage()); event.consume(); }
        });
        dialog.showAndWait();
    }

    private void ouvrirPopupAnnulation(rdv r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Annuler le RDV");
        VBox content = new VBox(15);
        content.setPrefWidth(450); content.setAlignment(Pos.CENTER); content.setStyle("-fx-padding: 30 40;");
        Label icon = new Label("🗑");
        icon.setStyle("-fx-font-size: 36; -fx-background-color: #fef2f2; -fx-background-radius: 30; -fx-padding: 15;");
        Label titre = new Label("Annuler ce rendez-vous ?");
        titre.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label sousTitre = new Label("Le statut sera changé en Annulé.");
        sousTitre.setStyle("-fx-font-size: 13; -fx-text-fill: #94a3b8;");
        VBox infoCard = new VBox(8);
        infoCard.setStyle("-fx-background-color: #fef2f2; -fx-background-radius: 10; -fx-padding: 15; -fx-border-color: #fecaca; -fx-border-radius: 10;");
        infoCard.getChildren().addAll(
                creerLigneInfo("👤 Patient", obtenirNomPatient(r.getPatient_id())),
                creerLigneInfo("📅 Date",    formaterDateAffichage(r.getDate())),
                creerLigneInfo("🕐 Heure",   r.getHdebut() != null ? r.getHdebut().substring(0, 5) : "N/A"),
                creerLigneInfo("📋 Motif",   r.getMotif()));
        String em = obtenirEmailPatient(r);
        CheckBox ck = new CheckBox("📧 Envoyer un email d'annulation à :");
        ck.setSelected(true); ck.setStyle("-fx-font-size: 13; -fx-text-fill: #334155;");
        Label le = new Label("   " + em);
        le.setStyle("-fx-font-size: 12; -fx-text-fill: #dc2626; -fx-font-weight: bold;");
        content.getChildren().addAll(icon, titre, sousTitre, infoCard, ck, le);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.setText("🗑 Annuler le RDV");
        btnOk.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 30; -fx-cursor: hand;");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Retour");
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                rdvService.updateStatut(r.getId(), "annulé");
                if (ck.isSelected()) envoyerEmailEnArrierePlan("annulation", r, em, null, null);
                chargerDonnees();
            } catch (SQLException ex) { System.err.println(ex.getMessage()); event.consume(); }
        });
        dialog.showAndWait();
    }

    private void ouvrirPopupModifierAdmin(rdv r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier Date & Heure");
        VBox content = new VBox(20);
        content.setPrefWidth(500); content.setStyle("-fx-padding: 25;");
        Label titre = new Label("✏ Modifier Date & Heure");
        titre.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label lblDate = new Label("Date");
        lblDate.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #334155;");
        DatePicker datePicker = new DatePicker(LocalDate.parse(r.getDate()));
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now()) && !date.equals(LocalDate.parse(r.getDate()))) {
                    setDisable(true); setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #ccc;");
                }
            }
        });

        Label lblHeure = new Label("Heure");
        lblHeure.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #334155;");
        ComboBox<String> hc = new ComboBox<>();
        hc.setMaxWidth(Double.MAX_VALUE);
        hc.setStyle("-fx-font-size: 13; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");
        for (int h = 9; h <= 16; h++) {
            hc.getItems().add(String.format("%02d:00", h));
            hc.getItems().add(String.format("%02d:30", h));
        }
        hc.getItems().add("17:00");
        if (r.getHdebut() != null && r.getHdebut().length() >= 5) {
            String hv = r.getHdebut().substring(0, 5);
            if (!hc.getItems().contains(hv)) hc.getItems().add(hv);
            hc.setValue(hv);
        }

        HBox dh = new HBox(15);
        VBox db = new VBox(5, lblDate, datePicker);
        VBox hb = new VBox(5, lblHeure, hc);
        HBox.setHgrow(db, Priority.ALWAYS); HBox.setHgrow(hb, Priority.ALWAYS);
        dh.getChildren().addAll(db, hb);

        String em = obtenirEmailPatient(r);
        CheckBox ck = new CheckBox("📧 Envoyer un email de modification à :");
        ck.setSelected(true); ck.setStyle("-fx-font-size: 13; -fx-text-fill: #334155;");
        Label le = new Label("   " + em);
        le.setStyle("-fx-font-size: 12; -fx-text-fill: #f97316; -fx-font-weight: bold;");
        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12;");
        lblErr.setWrapText(true); lblErr.setVisible(false);

        content.getChildren().addAll(titre, dh, ck, le, lblErr);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button btnE = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnE.setText("✏ Enregistrer");
        btnE.setStyle("-fx-background-color: #f97316; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 30; -fx-cursor: hand;");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Annuler");

        btnE.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            lblErr.setVisible(false);
            if (datePicker.getValue() == null) { lblErr.setText("❌ Choisir une date"); lblErr.setVisible(true); event.consume(); return; }
            if (datePicker.getValue().isBefore(LocalDate.now()) && !datePicker.getValue().equals(LocalDate.parse(r.getDate()))) { lblErr.setText("❌ Date dans le passé"); lblErr.setVisible(true); event.consume(); return; }
            if (hc.getValue() == null) { lblErr.setText("❌ Choisir une heure"); lblErr.setVisible(true); event.consume(); return; }
            try {
                String ad = r.getDate();
                String ah = r.getHdebut() != null ? r.getHdebut().substring(0, 5) : "";
                String hd = hc.getValue();
                String[] pts = hd.split(":");
                int hh = Integer.parseInt(pts[0]), mm = Integer.parseInt(pts[1]);
                mm += 30; if (mm >= 60) { mm -= 60; hh++; }
                r.setDate(datePicker.getValue().toString()); r.setHdebut(hd); r.setHfin(String.format("%02d:%02d", hh, mm));
                try (java.sql.PreparedStatement ps = Utils.MyDb.getInstance().getConnection()
                        .prepareStatement("UPDATE rdv SET date=?, hdebut=?, hfin=? WHERE id=?")) {
                    ps.setString(1, r.getDate()); ps.setString(2, r.getHdebut()); ps.setString(3, r.getHfin()); ps.setInt(4, r.getId());
                    ps.executeUpdate();
                }
                if (ck.isSelected()) envoyerEmailEnArrierePlan("modification", r, em, ad, ah);
                chargerDonnees();
            } catch (SQLException ex) { lblErr.setText("❌ " + ex.getMessage()); lblErr.setVisible(true); event.consume(); }
        });
        dialog.showAndWait();
    }

    private void envoyerEmailEnArrierePlan(String type, rdv r, String em, String ad, String ah) {
        new Thread(() -> {
            boolean s;
            switch (type) {
                case "confirmation": s = emailService.envoyerEmailConfirmation(r, em); break;
                case "annulation":   s = emailService.envoyerEmailAnnulation(r, em); break;
                case "modification": s = emailService.envoyerEmailModification(r, em, ad, ah); break;
                default: s = false;
            }
            boolean fs = s;
            Platform.runLater(() -> {
                Alert alert;
                if (fs) {
                    alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("✅ Email envoyé"); alert.setHeaderText(null);
                    alert.setContentText("📧 Email de " + type + " envoyé à\n" + em);
                } else {
                    alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("⚠️ Email non envoyé"); alert.setHeaderText(null);
                    alert.setContentText("Action effectuée mais email non envoyé.");
                }
                alert.show();
            });
        }).start();
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════
    private HBox creerLigneInfo(String label, String valeur) {
        HBox l = new HBox(10); l.setAlignment(Pos.CENTER_LEFT);
        Label lb = new Label(label); lb.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;"); lb.setMinWidth(110);
        Label vl = new Label(valeur != null ? valeur : "N/A"); vl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        l.getChildren().addAll(lb, vl); return l;
    }

    private String formaterDateAffichage(String dateISO) {
        try {
            LocalDate d = LocalDate.parse(dateISO);
            return String.format("%02d", d.getDayOfMonth()) + " " +
                    d.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + d.getYear();
        } catch (Exception e) { return dateISO; }
    }

    private String obtenirNomPatient(int id) {
        String[] n = {"Patient inconnu", "Sarah Martin", "Ahmed Ben Ali", "Leila Trabelsi", "Mohamed Kallel", "Fatima Rezgui"};
        return (id >= 0 && id < n.length) ? n[id] : "Patient #" + id;
    }

    private String obtenirEmailPatient(rdv r) { return "hmaied.nada1@gmail.com"; }

    private String getInitiales(String nom) {
        if (nom == null || nom.isEmpty()) return "?";
        String[] p = nom.replace("Dr.", "").replace("Dr", "").trim().split("\\s+");
        if (p.length >= 2) return (p[0].substring(0, 1) + p[1].substring(0, 1)).toUpperCase();
        return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
    }

    private String getAvatarColor(String nom) {
        if (nom == null) return "#64748b";
        String[] colors = {"#1a73e8", "#e53935", "#f59e0b", "#16a34a", "#8b5cf6", "#ec4899", "#06b6d4", "#f97316"};
        return colors[Math.abs(nom.hashCode()) % colors.length];
    }

    private void naviguerVersDispo() {
        try {
            Node ct = FXMLLoader.load(getClass().getResource("/AdminDispo.fxml"));
            StackPane p = (StackPane) rdvTableContainer.getScene().lookup("#contentArea");
            if (p != null) { p.getChildren().clear(); p.getChildren().add(ct); }
        } catch (IOException e) { System.err.println("Erreur navigation : " + e.getMessage()); }
    }
}