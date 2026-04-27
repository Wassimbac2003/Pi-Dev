package Controllers;

import Services.ChatMessageService;
import Services.SocketClient;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.function.Consumer;

public class AdminChatController {

    @FXML private Label      lblNomPatient;
    @FXML private Label      lblStatutPatient;
    @FXML private Label      lblAvatarPatient;
    @FXML private VBox       messagesContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField  messageInput;
    @FXML private Button     btnEnvoyer;

    private SocketClient       socketClient;
    private String             patientUserId;
    private String             medecinUserId;
    private ChatMessageService chatMessageService = new ChatMessageService();

    // ✅ Référence gardée pour retirer le listener à la fermeture
    private Consumer<String[]> chatListener;

    public void initChat(String medecinUserId, String medecinNom,
                         String patientUserId, String patientNom) {
        this.medecinUserId = medecinUserId;
        this.patientUserId = patientUserId;

        if (lblNomPatient    != null) lblNomPatient.setText(patientNom);
        if (lblStatutPatient != null) lblStatutPatient.setText("Patient");
        if (lblAvatarPatient != null) lblAvatarPatient.setText(extraireInitiales(patientNom));

        chatMessageService.marquerCommeLu(medecinUserId, patientUserId);

        socketClient = SocketClient.getInstance("medecin", medecinUserId);
        if (!socketClient.isConnecte()) {
            try { socketClient.connecter(); Thread.sleep(200); }
            catch (Exception e) { System.err.println("Erreur connexion: " + e.getMessage()); return; }
        }

        final String finalPatientId = this.patientUserId;
        final String finalMedecinId = this.medecinUserId;

        // ✅ addOnChatRecu → s'ajoute aux listeners existants
        // Ne supprime PAS le listener du FAB (AdminRdvController)
        chatListener = (String[] data) -> {
            if (data.length < 3) return;
            String sender = data[0];
            String texte  = data[2];
            Platform.runLater(() -> {
                if (sender.equals(finalPatientId)) {
                    ajouterBullePatient(texte);
                    chatMessageService.marquerCommeLu(finalMedecinId, finalPatientId);
                } else if (sender.equals(finalMedecinId)) {
                    ajouterBulleMoi(texte);
                }
                scrollEnBas();
            });
        };
        socketClient.addOnChatRecu(chatListener);

        socketClient.setOnHistorique((String[] data) -> Platform.runLater(() -> {
            if (data.length < 3) return;
            if (data[0].equals(finalPatientId))      ajouterBullePatient(data[2]);
            else if (data[0].equals(finalMedecinId)) ajouterBulleMoi(data[2]);
            scrollEnBas();
        }));

        socketClient.setOnTyping((String typer) -> Platform.runLater(() -> {
            if (lblStatutPatient != null) {
                lblStatutPatient.setText("En train d'écrire...");
                lblStatutPatient.setStyle("-fx-font-size: 12; -fx-text-fill: #f59e0b;");
                new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                    lblStatutPatient.setText("Patient");
                    lblStatutPatient.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8;");
                })).play();
            }
        }));

        socketClient.demanderHistorique(patientUserId);

        if (btnEnvoyer   != null) btnEnvoyer.setOnAction(e -> envoyerMessage());
        if (messageInput != null) {
            messageInput.setOnAction(e -> envoyerMessage());
            messageInput.textProperty().addListener((obs, old, val) -> {
                if (val != null && !val.isEmpty()) socketClient.signalerEnTrainDEcrire(finalPatientId);
            });
        }
    }

    /**
     * ✅ Appeler quand la fenêtre se ferme (chatStage.setOnHidden)
     * Retire le listener de cette fenêtre sans toucher aux autres.
     */
    public void onFermeture() {
        if (socketClient != null && chatListener != null) {
            socketClient.removeOnChatRecu(chatListener);
            System.out.println("✅ [Chat] Listener retiré proprement");
        }
    }

    private void envoyerMessage() {
        if (messageInput == null || socketClient == null) return;
        String texte = messageInput.getText().trim();
        if (texte.isEmpty()) return;
        socketClient.envoyerMessage(patientUserId, texte);
        ajouterBulleMoi(texte);
        messageInput.clear();
        scrollEnBas();
    }

    private void ajouterBulleMoi(String texte) {
        if (messagesContainer == null) return;
        HBox ligne = new HBox(); ligne.setAlignment(Pos.CENTER_RIGHT); ligne.setStyle("-fx-padding: 5 0;");
        Label bulle = new Label(texte); bulle.setWrapText(true); bulle.setMaxWidth(350);
        bulle.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 13; -fx-padding: 10 14; -fx-background-radius: 15 15 0 15;");
        ligne.getChildren().add(bulle);
        messagesContainer.getChildren().add(ligne);
    }

    private void ajouterBullePatient(String texte) {
        if (messagesContainer == null) return;
        HBox ligne = new HBox(); ligne.setAlignment(Pos.CENTER_LEFT); ligne.setStyle("-fx-padding: 5 0;");
        Label bulle = new Label(texte); bulle.setWrapText(true); bulle.setMaxWidth(350);
        bulle.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333; -fx-font-size: 13; -fx-padding: 10 14; -fx-background-radius: 15 15 15 0;");
        ligne.getChildren().add(bulle);
        messagesContainer.getChildren().add(ligne);
    }

    private void scrollEnBas() {
        if (scrollPane == null) return;
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private String extraireInitiales(String nom) {
        if (nom == null || nom.isEmpty()) return "?";
        String[] parts = nom.split("\\s+");
        if (parts.length >= 2) return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        return nom.substring(0, Math.min(2, nom.length())).toUpperCase();
    }
}