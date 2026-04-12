package tn.esprit.fx;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import tn.esprit.entities.User;

public final class UiTheme {

    private UiTheme() {
    }

    public static void applyDialog(Dialog<?> dialog) {
        applyDialogPane(dialog.getDialogPane());
    }

    public static void applyDialog(Alert alert) {
        applyDialogPane(alert.getDialogPane());
    }

    private static void applyDialogPane(DialogPane pane) {
        pane.getStyleClass().add("modern-dialog");
        pane.setMinWidth(520);
        pane.setPrefWidth(640);
        pane.setMaxWidth(900);
    }

    /** Libellés colonne 0 d’une grille de formulaire + largeur minimale des colonnes. */
    public static void tagFormLabels(GridPane grid) {
        if (grid.getColumnConstraints().isEmpty()) {
            ColumnConstraints c0 = new ColumnConstraints(160, 180, 240);
            c0.setHgrow(Priority.NEVER);
            ColumnConstraints c1 = new ColumnConstraints(260, 320, Double.MAX_VALUE);
            c1.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().addAll(c0, c1);
        }
        for (var node : grid.getChildren()) {
            Integer col = GridPane.getColumnIndex(node);
            if (node instanceof Label lab && (col == null || col == 0)) {
                if (!lab.getStyleClass().contains("form-label")) {
                    lab.getStyleClass().add("form-label");
                }
                lab.setWrapText(true);
                lab.setMaxWidth(240);
            }
        }
    }

    public static Region spacer() {
        Region r = new Region();
        r.setMinHeight(8);
        r.setMaxHeight(8);
        return r;
    }

    public static String userDisplayName(User u) {
        if (u == null) {
            return "";
        }
        if (u.getId() < 0) {
            return u.getPrenom();
        }
        String name = (nullToEmpty(u.getPrenom()) + " " + nullToEmpty(u.getNom())).trim();
        if (name.isEmpty()) {
            name = nullToEmpty(u.getEmail());
        }
        return name;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
