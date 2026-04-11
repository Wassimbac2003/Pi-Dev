package com.healthtrack.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.List;

public final class UiMessageUtil {
    private UiMessageUtil() {
    }

    public static void showValidationErrors(String title, List<String> errors) {
        Alert alert = new Alert(Alert.AlertType.WARNING, String.join("\n", errors), ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText("Controle de saisie");
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
