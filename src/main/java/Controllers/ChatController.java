package Controllers;

import Services.SocketClient;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * Controller du chat PATIENT → MÉDECIN
 * Interface : Chat.fxml
 */
public class ChatController {

    @FXML private VBox chatMessagesContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField messageInput;
    @FXML private Button btnEnvoyer;
    @FXML private Label lblNomMedecin;
    @FXML private Label lblAvatarMedecin;
    @FXML private Label lblStatutMedecin;

    private SocketClient client;
    private String medecinUserId;
    private String medecinNom;

    /**
     * Initialise le chat avec un médecin spécifique
     */
    public void initChat(SocketClient client, String medecinUserId, String medecinNom) {
        this.client = client;
        this.medecinUserId = medecinUserId;
        this.medecinNom = medecinNom;

        // Afficher le nom du médecin
        lblNomMedecin.setText(medecinNom);
        lblAvatarMedecin.setText(extraireInitiales(medecinNom));

        // Configurer les callbacks
        configurerCallbacks();

        // Demander l'historique
        client.demanderHistorique(medecinUserId);

        // Bouton envoyer
        btnEnvoyer.setOnAction(e -> envoyerMessage());
        messageInput.setOnAction(e -> envoyerMessage());

        // ✅ AJOUTER : Détecter quand le patient tape
        messageInput.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.isEmpty()) {
                client.signalerEnTrainDEcrire(medecinUserId);
            }
        });
    }

    /**
     * Configure les callbacks du client socket
     */
    private void configurerCallbacks() {
        // Callback pour les NOUVEAUX messages reçus
        client.setOnChatRecu(data -> {
            Platform.runLater(() -> {
                String sender = data[0];
                String dest = data[1];
                String message = data[2];

                // Si le message est pour moi, c'est le médecin qui m'écrit
                if (dest.equals(client.getUserId())) {
                    ajouterBulleMedecin(message);
                }
            });
        });

        // Callback pour l'HISTORIQUE
        client.setOnHistorique(data -> {
            Platform.runLater(() -> {
                String sender = data[0];
                String dest = data[1];
                String message = data[2];

                // Déterminer qui a envoyé le message
                if (sender.equals(client.getUserId())) {
                    // C'est moi (patient) qui ai envoyé
                    ajouterBulleMoi(message);
                } else {
                    // C'est le médecin qui a envoyé
                    ajouterBulleMedecin(message);
                }
            });
        });

        // ✅ AJOUTER : Callback "en train d'écrire"
        client.setOnTyping(typer -> {
            Platform.runLater(() -> {
                lblStatutMedecin.setText("En train d'écrire...");
                lblStatutMedecin.setStyle("-fx-font-size: 12; -fx-text-fill: #f59e0b;");

                // Retour à "En ligne" après 2 secondes
                new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                    lblStatutMedecin.setText("En ligne");
                    lblStatutMedecin.setStyle("-fx-font-size: 12; -fx-text-fill: #16a34a;");
                })).play();
            });
        });
    }

    /**
     * Envoie un message au médecin
     */
    private void envoyerMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) return;

        // Envoyer au serveur
        client.envoyerMessage(medecinUserId, message);

        // Afficher dans l'interface
        ajouterBulleMoi(message);

        // Vider le champ
        messageInput.clear();

        // Scroll en bas
        scrollEnBas();
    }

    /**
     * Ajoute une bulle de message "Moi" (patient)
     */
    private void ajouterBulleMoi(String message) {
        HBox ligne = new HBox();
        ligne.setAlignment(Pos.CENTER_RIGHT);
        ligne.setPadding(new javafx.geometry.Insets(5, 10, 5, 50));

        Label bulle = new Label(message);
        bulle.setWrapText(true);
        bulle.setMaxWidth(300);
        bulle.setStyle(
                "-fx-background-color: #1a73e8; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10 15; " +
                        "-fx-background-radius: 15 15 0 15; " +
                        "-fx-font-size: 13;"
        );

        ligne.getChildren().add(bulle);
        chatMessagesContainer.getChildren().add(ligne);
        scrollEnBas();
    }

    /**
     * Ajoute une bulle de message du médecin
     */
    private void ajouterBulleMedecin(String message) {
        HBox ligne = new HBox();
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPadding(new javafx.geometry.Insets(5, 50, 5, 10));

        Label bulle = new Label(message);
        bulle.setWrapText(true);
        bulle.setMaxWidth(300);
        bulle.setStyle(
                "-fx-background-color: #f0f0f0; " +
                        "-fx-text-fill: #333; " +
                        "-fx-padding: 10 15; " +
                        "-fx-background-radius: 15 15 15 0; " +
                        "-fx-font-size: 13;"
        );

        ligne.getChildren().add(bulle);
        chatMessagesContainer.getChildren().add(ligne);
        scrollEnBas();
    }

    /**
     * Scroll automatique en bas
     */
    private void scrollEnBas() {
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    /**
     * Extrait les initiales d'un nom
     */
    private String extraireInitiales(String nom) {
        if (nom == null || nom.isEmpty()) return "?";
        String[] parts = nom.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return nom.substring(0, Math.min(2, nom.length())).toUpperCase();
    }
}