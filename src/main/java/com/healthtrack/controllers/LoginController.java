package com.healthtrack.controllers;

import com.healthtrack.entities.User;
import com.healthtrack.services.UserService;
import com.healthtrack.util.AppNavigator;
import com.healthtrack.util.SessionContext;
import com.healthtrack.util.UiMessageUtil;
import com.healthtrack.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;

public class LoginController {
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        emailField.textProperty().addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(emailField));
        passwordField.textProperty()
                .addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(passwordField));
    }

    @FXML
    private void login() {
        ValidationUtil.clearInvalid(emailField, passwordField);

        List<String> errors = new ArrayList<>();
        String email = safe(emailField.getText());
        String password = safe(passwordField.getText());
        if (email.isBlank() || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            errors.add("L'email est invalide.");
            ValidationUtil.markInvalid(emailField);
        }
        if (password.isBlank() || password.length() < 4) {
            errors.add("Le mot de passe est obligatoire.");
            ValidationUtil.markInvalid(passwordField);
        }

        if (!errors.isEmpty()) {
            UiMessageUtil.showValidationErrors("Connexion", errors);
            return;
        }

        User user = userService.authenticate(email, password);
        if (user == null) {
            ValidationUtil.markInvalid(emailField);
            ValidationUtil.markInvalid(passwordField);
            UiMessageUtil.showInfo("Connexion", "Email ou mot de passe incorrect.");
            return;
        }

        SessionContext.setCurrentUser(user);
        AppNavigator.switchRoot(user.isAdmin() ? "/AdminLayout.fxml" : "/fxml/main-view.fxml");
    }

    @FXML
    private void goToSignup() {
        AppNavigator.switchRoot("/fxml/signup-view.fxml");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}