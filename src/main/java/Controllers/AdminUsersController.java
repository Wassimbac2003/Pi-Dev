package Controllers;

import com.healthtrack.entities.User;
import com.healthtrack.services.UserService;
import com.healthtrack.util.UiMessageUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;

public class AdminUsersController {

    @FXML
    private TilePane usersGrid;
    @FXML
    private Label countLabel;
    @FXML
    private Label selectedUserLabel;
    @FXML
    private TextField emailField;
    @FXML
    private TextField nomField;
    @FXML
    private TextField prenomField;
    @FXML
    private TextField telephoneField;
    @FXML
    private TextField adresseField;
    @FXML
    private TextField verificationStatusField;
    @FXML
    private ComboBox<String> roleCombo;
    @FXML
    private CheckBox verifiedCheckBox;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;

    private final UserService userService = new UserService();
    private List<User> users = List.of();
    private User selectedUser;

    @FXML
    public void initialize() {
        roleCombo.getItems().setAll("ROLE_ADMIN", "ROLE_MEDECIN", "ROLE_PATIENT");
        clearSelection();
        refreshUsers();
    }

    @FXML
    public void refreshUsers() {
        users = userService.findAll();
        countLabel.setText(users.size() + " utilisateur(s)");
        renderGrid();
        if (selectedUser != null) {
            selectedUser = users.stream().filter(u -> u.getId() == selectedUser.getId()).findFirst().orElse(null);
            if (selectedUser == null) {
                clearSelection();
            } else {
                fillForm(selectedUser);
            }
        }
    }

    @FXML
    private void updateUser() {
        if (selectedUser == null) {
            UiMessageUtil.showInfo("Utilisateurs", "Selectionnez un utilisateur a modifier.");
            return;
        }
        String email = safe(emailField.getText());
        String nom = safe(nomField.getText());
        String prenom = safe(prenomField.getText());
        String role = roleCombo.getValue();

        if (email.isBlank() || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            UiMessageUtil.showInfo("Utilisateurs", "Email invalide.");
            return;
        }
        if (nom.isBlank() || prenom.isBlank()) {
            UiMessageUtil.showInfo("Utilisateurs", "Nom et prenom sont obligatoires.");
            return;
        }
        if (role == null || role.isBlank()) {
            UiMessageUtil.showInfo("Utilisateurs", "Role obligatoire.");
            return;
        }

        User emailOwner = userService.findByEmail(email);
        if (emailOwner != null && emailOwner.getId() != selectedUser.getId()) {
            UiMessageUtil.showInfo("Utilisateurs", "Cet email est deja utilise par un autre utilisateur.");
            return;
        }

        selectedUser.setEmail(email);
        selectedUser.setNom(nom);
        selectedUser.setPrenom(prenom);
        selectedUser.setTelephone(safeOrNull(telephoneField.getText()));
        selectedUser.setAdresse(safeOrNull(adresseField.getText()));
        selectedUser.setRole(role);
        selectedUser.setVerified(verifiedCheckBox.isSelected());
        selectedUser.setVerificationStatus(safeOrNull(verificationStatusField.getText()));

        userService.update(selectedUser);
        UiMessageUtil.showInfo("Utilisateurs", "Utilisateur mis a jour.");
        refreshUsers();
    }

    @FXML
    private void deleteUser() {
        if (selectedUser == null) {
            UiMessageUtil.showInfo("Utilisateurs", "Selectionnez un utilisateur a supprimer.");
            return;
        }
        userService.delete(selectedUser.getId());
        UiMessageUtil.showInfo("Utilisateurs", "Utilisateur supprime.");
        clearSelection();
        refreshUsers();
    }

    @FXML
    private void clearSelection() {
        selectedUser = null;
        selectedUserLabel.setText("Aucun utilisateur selectionne");
        emailField.clear();
        nomField.clear();
        prenomField.clear();
        telephoneField.clear();
        adresseField.clear();
        verificationStatusField.clear();
        roleCombo.getSelectionModel().clearSelection();
        verifiedCheckBox.setSelected(false);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    private void renderGrid() {
        usersGrid.getChildren().clear();
        for (User user : users) {
            VBox card = createUserCard(user);
            usersGrid.getChildren().add(card);
        }
    }



    private String cardStyle(User user) {
        boolean selected = selectedUser != null && Objects.equals(selectedUser.getId(), user.getId());
        if (selected) {
            return "-fx-background-color: #dbeafe; -fx-border-color: #2563eb; -fx-border-width: 1.5; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 12; -fx-cursor: hand;";
        }
        return "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 12; -fx-cursor: hand;";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeOrNull(String value) {
        String clean = safe(value);
        return clean.isBlank() ? null : clean;
    }
}
