package com.healthtrack.controllers;

import com.healthtrack.entities.User;
import com.healthtrack.services.UserService;
import com.healthtrack.util.AppConstants;
import com.healthtrack.util.AppNavigator;
import com.healthtrack.util.SessionContext;
import com.healthtrack.util.UiMessageUtil;
import com.healthtrack.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SignupController {
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextField nomField;
    @FXML
    private TextField prenomField;
    @FXML
    private TextField adresseField;
    @FXML
    private TextField telephoneField;
    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private TextField preferredCityField;
    @FXML
    private TextField actionRadiusField;
    @FXML
    private TextArea skillsField;
    @FXML
    private TextArea availabilityField;
    @FXML
    private TextField profilePictureField;
    @FXML
    private Button chooseProfilePictureButton;
    @FXML
    private TextField diplomaDocumentField;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        roleComboBox.getItems().setAll(AppConstants.ROLE_MEDECIN, AppConstants.ROLE_PATIENT);
        roleComboBox.getSelectionModel().select(AppConstants.ROLE_PATIENT);

        emailField.textProperty().addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(emailField));
        passwordField.textProperty()
                .addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(passwordField));
        confirmPasswordField.textProperty()
                .addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(confirmPasswordField));
        nomField.textProperty().addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(nomField));
        prenomField.textProperty().addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(prenomField));
        telephoneField.textProperty()
                .addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(telephoneField));
        roleComboBox.valueProperty()
                .addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(roleComboBox));
        profilePictureField.textProperty()
                .addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(profilePictureField));

        telephoneField.setTextFormatter(digitsAndSpacesFormatter());
        actionRadiusField.setTextFormatter(digitsFormatter());
        nomField.setTextFormatter(nameFormatter());
        prenomField.setTextFormatter(nameFormatter());
        profilePictureField.setEditable(false);
    }

    @FXML
    private void chooseProfilePicture() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"));

        Window owner = chooseProfilePictureButton == null ? null : chooseProfilePictureButton.getScene().getWindow();
        File selectedFile = chooser.showOpenDialog(owner);
        if (selectedFile == null) {
            return;
        }

        try {
            String storedPath = importImage(selectedFile.toPath());
            profilePictureField.setText(storedPath);
        } catch (IOException e) {
            UiMessageUtil.showInfo("Photo de profil", "Impossible d'importer la photo selectionnee.");
        }
    }

    @FXML
    private void signup() {
        ValidationUtil.clearInvalid(emailField, passwordField, confirmPasswordField, nomField, prenomField,
                adresseField, telephoneField, roleComboBox, preferredCityField, actionRadiusField,
                skillsField, availabilityField,
                profilePictureField, diplomaDocumentField);

        List<String> errors = new ArrayList<>();
        String email = safe(emailField.getText());
        String password = safe(passwordField.getText());
        String confirmPassword = safe(confirmPasswordField.getText());
        String nom = safe(nomField.getText());
        String prenom = safe(prenomField.getText());
        String telephone = safe(telephoneField.getText());
        String role = roleComboBox.getValue();

        if (email.isBlank() || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            errors.add("L'email est invalide.");
            ValidationUtil.markInvalid(emailField);
        }
        if (userService.existsByEmail(email)) {
            errors.add("Cet email est deja utilise.");
            ValidationUtil.markInvalid(emailField);
        }
        if (password.length() < 6) {
            errors.add("Le mot de passe doit contenir au moins 6 caracteres.");
            ValidationUtil.markInvalid(passwordField);
        }
        if (!Objects.equals(password, confirmPassword)) {
            errors.add("La confirmation du mot de passe ne correspond pas.");
            ValidationUtil.markInvalid(confirmPasswordField);
        }
        if (nom.isBlank() || prenom.isBlank()) {
            errors.add("Le nom et le prenom sont obligatoires.");
            ValidationUtil.markInvalid(nomField);
            ValidationUtil.markInvalid(prenomField);
        }
        if (telephone.isBlank()) {
            errors.add("Le telephone est obligatoire.");
            ValidationUtil.markInvalid(telephoneField);
        }
        if (role == null || role.isBlank()) {
            errors.add("Choisissez un role.");
            ValidationUtil.markInvalid(roleComboBox);
        }

        if (!errors.isEmpty()) {
            UiMessageUtil.showValidationErrors("Inscription", errors);
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setAdresse(safeOrNull(adresseField.getText()));
        user.setTelephone(telephone);
        user.setRole(role);
        user.setPreferredCity(safeOrNull(preferredCityField.getText()));
        user.setActionRadiusKm(parseInteger(actionRadiusField.getText()));
        user.setSkillsProfile(safeOrNull(skillsField.getText()));
        user.setAvailabilityProfile(safeOrNull(availabilityField.getText()));
        user.setProfilePicture(safeOrNull(profilePictureField.getText()));
        user.setDiplomaDocument(safeOrNull(diplomaDocumentField.getText()));
        user.setVerified(false);
        user.setVerificationStatus("pending");
        userService.create(user);

        SessionContext.setCurrentUser(user);
        AppNavigator.switchRoot(user.isAdmin() ? "/AdminLayout.fxml" : "/fxml/main-view.fxml");
    }

    @FXML
    private void backToLogin() {
        AppNavigator.switchRoot("/fxml/login-view.fxml");
    }

    private TextFormatter<String> digitsFormatter() {
        return new TextFormatter<>(change -> change.getControlNewText().matches("\\d*") ? change : null);
    }

    private TextFormatter<String> digitsAndSpacesFormatter() {
        return new TextFormatter<>(change -> change.getControlNewText().matches("[+\\d ]*") ? change : null);
    }

    private TextFormatter<String> nameFormatter() {
        return new TextFormatter<>(change -> change.getControlNewText().matches("[\\p{L}\\s'-]*") ? change : null);
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeOrNull(String value) {
        String safe = safe(value);
        return safe.isBlank() ? null : safe;
    }

    private String importImage(Path source) throws IOException {
        String fileName = source.getFileName().toString();
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = fileName.substring(dotIndex);
        }

        Path uploadDir = Path.of("uploaded-images");
        Files.createDirectories(uploadDir);
        String generatedName = "profile-" + UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(generatedName);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return uploadDir.resolve(generatedName).toString().replace('\\', '/');
    }
}