package com.healthtrack.controllers;

import com.healthtrack.entities.User;
import com.healthtrack.services.UserService;
import com.healthtrack.util.AppNavigator;
import com.healthtrack.util.SessionContext;
import com.healthtrack.util.UiMessageUtil;
import com.healthtrack.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AccountPageController implements PageController {
    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label verificationLabel;
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
    private TextField preferredCityField;
    @FXML
    private TextField actionRadiusField;
    @FXML
    private TextField latitudeField;
    @FXML
    private TextField longitudeField;
    @FXML
    private TextArea skillsField;
    @FXML
    private TextArea interestsField;
    @FXML
    private TextArea availabilityField;
    @FXML
    private TextField recommendationWeightsField;
    @FXML
    private TextField profilePictureField;
    @FXML
    private TextField diplomaDocumentField;
    @FXML
    private TextField idCardDocumentField;
    @FXML
    private CheckBox verifiedCheckBox;

    private final UserService userService = new UserService();
    private MainController mainController;
    private User currentUser;

    @FXML
    private void initialize() {
        emailField.textProperty().addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(emailField));
        passwordField.textProperty()
                .addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(passwordField));
        confirmPasswordField.textProperty()
                .addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(confirmPasswordField));
        nomField.textProperty().addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(nomField));
        prenomField.textProperty().addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(prenomField));
        telephoneField.textProperty()
                .addListener((obs, oldValue, newValue) -> ValidationUtil.clearInvalid(telephoneField));

        telephoneField.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("[+\\d ]*") ? change : null));
        actionRadiusField.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("\\d*") ? change : null));
        latitudeField.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("-?\\d*(\\.\\d*)?") ? change : null));
        longitudeField.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("-?\\d*(\\.\\d*)?") ? change : null));
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void onPageShown() {
        currentUser = SessionContext.getCurrentUser();
        if (currentUser == null) {
            AppNavigator.switchRoot("/fxml/login-view.fxml");
            return;
        }
        pageTitleLabel.setText("Mon compte");
        verificationLabel.setText(currentUser.isVerified()
                ? "Compte verifie"
                : safe(currentUser.getVerificationStatus(), "Verification en attente"));
        fillForm(currentUser);
    }

    @FXML
    private void saveAccount() {
        if (currentUser == null) {
            UiMessageUtil.showInfo("Compte", "Aucun utilisateur connecte.");
            return;
        }

        ValidationUtil.clearInvalid(emailField, passwordField, confirmPasswordField, nomField, prenomField,
                adresseField,
                telephoneField, preferredCityField, actionRadiusField, latitudeField, longitudeField,
                skillsField, interestsField, availabilityField, recommendationWeightsField,
                profilePictureField, diplomaDocumentField, idCardDocumentField);

        List<String> errors = new ArrayList<>();
        String email = safe(emailField.getText());
        String password = safe(passwordField.getText());
        String confirmPassword = safe(confirmPasswordField.getText());
        String nom = safe(nomField.getText());
        String prenom = safe(prenomField.getText());
        String telephone = safe(telephoneField.getText());

        if (email.isBlank() || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            errors.add("L'email est invalide.");
            ValidationUtil.markInvalid(emailField);
        }
        User emailOwner = userService.findByEmail(email);
        if (emailOwner != null && emailOwner.getId() != currentUser.getId()) {
            errors.add("Cet email est deja utilise.");
            ValidationUtil.markInvalid(emailField);
        }
        if (!password.isBlank() && password.length() < 6) {
            errors.add("Le nouveau mot de passe doit contenir au moins 6 caracteres.");
            ValidationUtil.markInvalid(passwordField);
        }
        if (!password.isBlank() && !Objects.equals(password, confirmPassword)) {
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

        if (!errors.isEmpty()) {
            UiMessageUtil.showValidationErrors("Mon compte", errors);
            return;
        }

        currentUser.setEmail(email);
        if (!password.isBlank()) {
            currentUser.setPassword(password);
        }
        currentUser.setNom(nom);
        currentUser.setPrenom(prenom);
        currentUser.setAdresse(safeOrNull(adresseField.getText()));
        currentUser.setTelephone(telephone);
        currentUser.setPreferredCity(safeOrNull(preferredCityField.getText()));
        currentUser.setActionRadiusKm(parseInteger(actionRadiusField.getText()));
        currentUser.setLatitude(parseDouble(latitudeField.getText()));
        currentUser.setLongitude(parseDouble(longitudeField.getText()));
        currentUser.setSkillsProfile(safeOrNull(skillsField.getText()));
        currentUser.setInterestsProfile(safeOrNull(interestsField.getText()));
        currentUser.setAvailabilityProfile(safeOrNull(availabilityField.getText()));
        currentUser.setRecommendationWeights(safeOrNull(recommendationWeightsField.getText()));
        currentUser.setProfilePicture(safeOrNull(profilePictureField.getText()));
        currentUser.setDiplomaDocument(safeOrNull(diplomaDocumentField.getText()));
        currentUser.setIdCardDocument(safeOrNull(idCardDocumentField.getText()));

        userService.update(currentUser);
        SessionContext.setCurrentUser(userService.findById(currentUser.getId()));
        currentUser = SessionContext.getCurrentUser();
        UiMessageUtil.showInfo("Mon compte", "Vos informations ont ete mises a jour.");
        if (mainController != null) {
            mainController.refreshSessionInfo();
        }
        onPageShown();
    }

    @FXML
    private void deleteAccount() {
        if (currentUser == null) {
            return;
        }
        userService.delete(currentUser.getId());
        SessionContext.clear();
        AppNavigator.switchRoot("/fxml/login-view.fxml");
    }

    @FXML
    private void goBack() {
        if (mainController != null) {
            mainController.showMissionsPage();
        }
    }

    private void fillForm(User user) {
        emailField.setText(user.getEmail());
        passwordField.clear();
        confirmPasswordField.clear();
        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        adresseField.setText(user.getAdresse());
        telephoneField.setText(user.getTelephone());
        preferredCityField.setText(user.getPreferredCity());
        actionRadiusField.setText(user.getActionRadiusKm() == null ? "" : String.valueOf(user.getActionRadiusKm()));
        latitudeField.setText(user.getLatitude() == null ? "" : String.valueOf(user.getLatitude()));
        longitudeField.setText(user.getLongitude() == null ? "" : String.valueOf(user.getLongitude()));
        skillsField.setText(user.getSkillsProfile());
        interestsField.setText(user.getInterestsProfile());
        availabilityField.setText(user.getAvailabilityProfile());
        recommendationWeightsField.setText(user.getRecommendationWeights());
        profilePictureField.setText(user.getProfilePicture());
        diplomaDocumentField.setText(user.getDiplomaDocument());
        idCardDocumentField.setText(user.getIdCardDocument());
        verifiedCheckBox.setSelected(user.isVerified());
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? null : Double.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value, String fallback) {
        String safe = safe(value);
        return safe.isBlank() ? fallback : safe;
    }

    private String safeOrNull(String value) {
        String safe = safe(value);
        return safe.isBlank() ? null : safe;
    }
}