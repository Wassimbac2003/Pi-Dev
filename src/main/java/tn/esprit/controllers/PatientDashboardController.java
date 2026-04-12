package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.Fiche;
import tn.esprit.entities.Medicament;
import tn.esprit.entities.Ordonnance;
import tn.esprit.entities.User;
import tn.esprit.fx.Session;
import tn.esprit.fx.UiTheme;
import tn.esprit.services.ServiceFiche;
import tn.esprit.services.ServiceLigneOrdonnance;
import tn.esprit.services.ServiceMedicament;
import tn.esprit.services.ServiceOrdonnance;
import tn.esprit.services.ServiceUser;
import tn.esprit.tools.OrdonnancePdfExporter;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PatientDashboardController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE);

    @FXML
    private Label welcomeLabel;
    @FXML
    private VBox ficheBox;
    @FXML
    private FlowPane ordoFlow;
    @FXML
    private TextField ordoSearchField;
    @FXML
    private StackPane patientStack;
    @FXML
    private ScrollPane scrollFiche;
    @FXML
    private ScrollPane scrollOrdo;
    @FXML
    private ToggleButton navFicheBtn;
    @FXML
    private ToggleButton navOrdoBtn;

    private final ServiceFiche ficheService = new ServiceFiche();
    private final ServiceOrdonnance ordonnanceService = new ServiceOrdonnance();
    private final ServiceUser userService = new ServiceUser();
    private final ServiceMedicament medicamentService = new ServiceMedicament();
    private final ServiceLigneOrdonnance ligneService = new ServiceLigneOrdonnance();

    private List<Ordonnance> ordonnanceMaster = new ArrayList<>();
    private ToggleGroup navGroup;

    @FXML
    private void initialize() {
        User u = Session.getCurrentUser();
        if (u != null) {
            welcomeLabel.setText(u.getPrenom() + " " + u.getNom());
        } else {
            welcomeLabel.setText("Espace patient");
        }

        navGroup = new ToggleGroup();
        navFicheBtn.setToggleGroup(navGroup);
        navOrdoBtn.setToggleGroup(navGroup);
        navFicheBtn.setSelected(true);
        navFicheBtn.setOnAction(e -> showFicheView());
        navOrdoBtn.setOnAction(e -> showOrdoView());

        int uid = Session.getCurrentUserId();
        ficheBox.getChildren().clear();
        Optional<Fiche> fiche = ficheService.findByUserId(uid);
        if (fiche.isPresent()) {
            ficheBox.getChildren().add(buildFicheCard(fiche.get()));
        } else {
            Label empty = new Label("Aucune fiche enregistrée pour votre compte.");
            empty.getStyleClass().add("card-sub");
            empty.setWrapText(true);
            ficheBox.getChildren().add(empty);
        }

        ordonnanceMaster = ordonnanceService.listByUserId(uid);
        ordoSearchField.textProperty().addListener((o, a, b) -> refreshOrdoCards());
        refreshOrdoCards();

        showFicheView();
    }

    private void showFicheView() {
        navFicheBtn.setSelected(true);
        scrollFiche.setVisible(true);
        scrollFiche.setManaged(true);
        scrollOrdo.setVisible(false);
        scrollOrdo.setManaged(false);
    }

    private void showOrdoView() {
        navOrdoBtn.setSelected(true);
        scrollFiche.setVisible(false);
        scrollFiche.setManaged(false);
        scrollOrdo.setVisible(true);
        scrollOrdo.setManaged(true);
    }

    private void refreshOrdoCards() {
        String q = ordoSearchField.getText() == null ? "" : ordoSearchField.getText().trim().toLowerCase(Locale.ROOT);
        ordoFlow.getChildren().clear();
        var filtered = ordonnanceMaster.stream()
                .filter(o -> matchesOrdoSearch(o, q))
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            Label l = new Label(q.isEmpty() ? "Aucune ordonnance pour le moment." : "Aucun résultat pour cette recherche.");
            l.getStyleClass().add("card-sub");
            l.setWrapText(true);
            ordoFlow.getChildren().add(l);
            return;
        }
        for (Ordonnance o : filtered) {
            ordoFlow.getChildren().add(buildOrdoCard(o));
        }
    }

    private boolean matchesOrdoSearch(Ordonnance o, String q) {
        if (q.isEmpty()) {
            return true;
        }
        String med = o.getMedecinUserId() != null
                ? userService.findById(o.getMedecinUserId()).map(UiTheme::userDisplayName).orElse("").toLowerCase(Locale.ROOT)
                : "";
        String ficheTxt = ficheService.findById(o.getIdFicheId())
                .map(f -> f.getLibelleMaladie() != null ? f.getLibelleMaladie() : "").orElse("").toLowerCase(Locale.ROOT);
        String meds = ligneService.listMedicamentIdsByOrdonnance(o.getId()).stream()
                .map(medicamentService::findById)
                .filter(Optional::isPresent)
                .map(x -> x.get().getNomMedicament().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
        String dt = o.getDateOrdonnance() != null ? o.getDateOrdonnance().toString().toLowerCase(Locale.ROOT) : "";
        return contains(o.getPosologie(), q)
                || contains(o.getFrequence(), q)
                || med.contains(q)
                || ficheTxt.contains(q)
                || meds.contains(q)
                || dt.contains(q);
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    private VBox buildFicheCard(Fiche f) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));
        card.setMaxWidth(640);

        Label badge = new Label("Dossier clinique");
        badge.getStyleClass().add("card-badge");

        Label title = new Label(nullSafe(f.getLibelleMaladie()));
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        Label meta = new Label("Consultation du " + (f.getDate() != null ? f.getDate().toString() : "—")
                + " · Gravité : " + nullSafe(f.getGravite()));
        meta.getStyleClass().add("card-meta");
        meta.setWrapText(true);

        card.getChildren().addAll(badge, title, meta, new Separator());
        card.getChildren().add(line("Poids / taille", f.getPoids() + " kg · " + f.getTaille() + " cm"));
        card.getChildren().add(line("Groupe sanguin", nullSafe(f.getGrpSanguin())));
        card.getChildren().add(line("Tension / glycémie", nullSafe(f.getTension()) + " · " + f.getGlycemie()));
        if (f.getAllergie() != null && !f.getAllergie().isBlank()) {
            card.getChildren().add(line("Allergies", f.getAllergie()));
        }
        if (f.getMaladieChronique() != null && !f.getMaladieChronique().isBlank()) {
            card.getChildren().add(line("Antécédents", f.getMaladieChronique()));
        }
        if (f.getSymptomes() != null && !f.getSymptomes().isBlank()) {
            Label s = new Label("Symptômes : " + f.getSymptomes());
            s.setWrapText(true);
            s.getStyleClass().add("card-sub");
            card.getChildren().add(s);
        }
        if (f.getRecommandation() != null && !f.getRecommandation().isBlank()) {
            Label r = new Label("Recommandations : " + f.getRecommandation());
            r.setWrapText(true);
            r.getStyleClass().add("card-sub");
            card.getChildren().add(r);
        }
        return card;
    }

    private HBox line(String k, String v) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_LEFT);
        Label a = new Label(k + " :");
        a.getStyleClass().add("card-sub");
        Label b = new Label(v);
        b.setWrapText(true);
        b.getStyleClass().add("card-meta");
        row.getChildren().addAll(a, b);
        return row;
    }

    private static String nullSafe(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private VBox buildOrdoCard(Ordonnance o) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        String dateLabel = o.getDateOrdonnance() != null ? DT.format(o.getDateOrdonnance()) : "Date non renseignée";
        Label badge = new Label("Ordonnance");
        badge.getStyleClass().add("card-badge");

        Label title = new Label("Prescription du " + dateLabel);
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        String patientLine = userService.findById(o.getIdUId()).map(UiTheme::userDisplayName).orElse("");
        String medLine = o.getMedecinUserId() != null
                ? userService.findById(o.getMedecinUserId()).map(m -> "Dr " + UiTheme.userDisplayName(m)).orElse("Médecin")
                : "Médecin";
        Label sub = new Label(medLine + (patientLine.isBlank() ? "" : " · Pour " + patientLine));
        sub.getStyleClass().add("card-sub");
        sub.setWrapText(true);

        Label p = new Label(o.getPosologie());
        p.setWrapText(true);
        p.getStyleClass().add("card-meta");

        Label fr = new Label("Fréquence : " + nullSafe(o.getFrequence()) + " · Durée : " + o.getDureeTraitement() + " jour(s)");
        fr.setWrapText(true);
        fr.getStyleClass().add("card-sub");

        Button detail = new Button("Voir le détail");
        detail.getStyleClass().add("ghost-button");
        detail.setOnAction(e -> showOrdoDetail(o));

        Button pdf = new Button("Exporter PDF");
        pdf.getStyleClass().add("primary-button");
        pdf.setOnAction(e -> exportOrdoPdf(o));

        HBox actions = new HBox(10, detail, pdf);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("card-actions");

        card.getChildren().addAll(badge, title, sub, p, fr, actions);
        return card;
    }

    private void showOrdoDetail(Ordonnance o) {
        Dialog<ButtonType> d = new Dialog<>();
        d.initOwner(ordoFlow.getScene().getWindow());
        UiTheme.applyDialog(d);
        d.setTitle("Détail de l’ordonnance");
        d.setHeaderText("Prescription du " + (o.getDateOrdonnance() != null ? DT.format(o.getDateOrdonnance()) : "—"));

        StringBuilder meds = new StringBuilder();
        for (Integer mid : ligneService.listMedicamentIdsByOrdonnance(o.getId())) {
            medicamentService.findById(mid).ifPresent(m ->
                    meds.append("• ").append(m.getNomMedicament())
                            .append(m.getDosage() != null && !m.getDosage().isBlank() ? " (" + m.getDosage() + ")" : "")
                            .append("\n"));
        }
        String ficheMotif = ficheService.findById(o.getIdFicheId())
                .map(f -> f.getLibelleMaladie() != null ? f.getLibelleMaladie() : "").orElse("—");
        String medNom = o.getMedecinUserId() != null
                ? userService.findById(o.getMedecinUserId()).map(m -> "Dr " + UiTheme.userDisplayName(m)).orElse("—")
                : "—";

        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(8, 0, 0, 0));
        int r = 0;
        g.add(new Label("Prescripteur"), 0, r);
        g.add(new Label(medNom), 1, r++);
        g.add(new Label("Dossier lié (motif)"), 0, r);
        Label fm = new Label(ficheMotif);
        fm.setWrapText(true);
        g.add(fm, 1, r++);
        g.add(new Label("Posologie"), 0, r);
        Label po = new Label(nullSafe(o.getPosologie()));
        po.setWrapText(true);
        g.add(po, 1, r++);
        g.add(new Label("Fréquence"), 0, r);
        g.add(new Label(nullSafe(o.getFrequence())), 1, r++);
        g.add(new Label("Durée"), 0, r);
        g.add(new Label(o.getDureeTraitement() + " jour(s)"), 1, r++);
        g.add(new Label("Médicaments"), 0, r);
        Label ml = new Label(meds.length() > 0 ? meds.toString().trim() : "—");
        ml.setWrapText(true);
        g.add(ml, 1, r);
        GridPane.setHgrow(ml, Priority.ALWAYS);
        UiTheme.tagFormLabels(g);

        ScrollPane sp = new ScrollPane(g);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(320);
        d.getDialogPane().setContent(sp);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.showAndWait();
    }

    private void exportOrdoPdf(Ordonnance o) {
        Stage st = (Stage) ordoFlow.getScene().getWindow();
        User patient = Session.getCurrentUser();
        User medecin = o.getMedecinUserId() != null ? userService.findById(o.getMedecinUserId()).orElse(null) : null;
        List<Medicament> meds = new ArrayList<>();
        for (Integer mid : ligneService.listMedicamentIdsByOrdonnance(o.getId())) {
            medicamentService.findById(mid).ifPresent(meds::add);
        }
        String motif = ficheService.findById(o.getIdFicheId())
                .map(f -> f.getLibelleMaladie() != null ? f.getLibelleMaladie() : "").orElse("");

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer l’ordonnance en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("Ordonnance_" + (o.getDateOrdonnance() != null
                ? o.getDateOrdonnance().toLocalDate().toString() : "export") + ".pdf");
        File dest = fc.showSaveDialog(st);
        if (dest == null) {
            return;
        }
        try {
            OrdonnancePdfExporter.export(dest, o, patient, medecin, meds, motif);
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Le fichier a été enregistré :\n" + dest.getAbsolutePath(), ButtonType.OK);
            UiTheme.applyDialog(ok);
            ok.showAndWait();
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Impossible d’écrire le PDF : " + ex.getMessage(), ButtonType.OK);
            UiTheme.applyDialog(err);
            err.showAndWait();
        }
    }


