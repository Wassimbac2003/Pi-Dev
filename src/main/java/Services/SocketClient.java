package Services;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SocketClient {

    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 5555;

    private static SocketClient instance;

    private Socket         socket;
    private PrintWriter    out;
    private BufferedReader in;
    private boolean        connecte = false;

    private final String type;
    private final String userId;

    // ✅ FIX PRINCIPAL : liste de listeners au lieu d'un seul callback
    // Permet au FAB (AdminRdvController) ET à la fenêtre chat (AdminChatController)
    // d'écouter en même temps sans s'écraser mutuellement
    private final List<Consumer<String[]>> onChatRecuListeners = new ArrayList<>();

    private Consumer<String[]> onHistorique;
    private Consumer<String>   onTyping;
    private Consumer<String[]> onOnlineList;

    private SocketClient(String type, String userId) {
        this.type   = type;
        this.userId = userId;
    }

    public static synchronized SocketClient getInstance(String type, String userId) {
        if (instance == null || !instance.userId.equals(userId)) {
            instance = new SocketClient(type, userId);
        }
        return instance;
    }

    public void connecter() {
        if (connecte) return;
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out.println("REGISTER|" + userId);
            connecte = true;
            System.out.println("✅ [CLIENT] Connecté : " + userId);
            demarrerEcoute();
        } catch (IOException e) {
            System.err.println("❌ [CLIENT] Connexion échouée : " + e.getMessage());
            connecte = false;
        }
    }

    public void deconnecter() {
        if (!connecte) return;
        try { connecte = false; if (socket != null && !socket.isClosed()) socket.close(); }
        catch (IOException e) { System.err.println("❌ deconnecter : " + e.getMessage()); }
    }

    public void envoyerMessage(String destinataire, String message) {
        if (!connecte || out == null) { System.err.println("❌ Non connecté"); return; }
        out.println("CHAT|" + userId + "|" + destinataire + "|" + message);
        System.out.println("📤 " + userId + " → " + destinataire + " : " + message);
    }

    public void demanderHistorique(String autreUserId) {
        if (!connecte || out == null) return;
        out.println("HISTORY|" + userId + "|" + autreUserId);
    }

    public void demanderListeEnLigne() {
        if (!connecte || out == null) return;
        out.println("ONLINE|" + userId);
    }

    public void signalerEnTrainDEcrire(String destinataire) {
        if (!connecte || out == null) return;
        out.println("TYPING|" + userId + "|" + destinataire);
    }

    private void demarrerEcoute() {
        Thread t = new Thread(() -> {
            try {
                String msg;
                while (connecte && (msg = in.readLine()) != null) traiterMessage(msg);
            } catch (IOException e) {
                if (connecte) System.err.println("❌ Écoute : " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void traiterMessage(String message) {
        String[] parts = message.split("\\|", 4);
        switch (parts[0]) {

            case "CHAT_SENT":
                if (parts.length >= 4 && !onChatRecuListeners.isEmpty()) {
                    String[] data = {parts[1], parts[2], parts[3]};
                    // Copie de la liste pour éviter ConcurrentModificationException
                    new ArrayList<>(onChatRecuListeners).forEach(l -> l.accept(data));
                }
                break;

            case "HISTORY":
                if (parts.length >= 4 && onHistorique != null)
                    onHistorique.accept(new String[]{parts[1], parts[2], parts[3]});
                break;

            case "TYPING":
                if (parts.length >= 2 && onTyping != null) onTyping.accept(parts[1]);
                break;

            case "ONLINE_LIST":
                if (parts.length >= 2 && onOnlineList != null) onOnlineList.accept(parts[1].split(","));
                break;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  API MULTI-LISTENERS pour onChatRecu
    // ══════════════════════════════════════════════════════════════

    /**
     * Ajoute un listener pour les messages reçus.
     * Retourne le même callback pour pouvoir le retirer avec removeOnChatRecu().
     */
    public Consumer<String[]> addOnChatRecu(Consumer<String[]> callback) {
        onChatRecuListeners.add(callback);
        return callback;
    }

    /**
     * Retire un listener spécifique.
     * À appeler quand la fenêtre chat se ferme (AdminChatController.onFermeture()).
     */
    public void removeOnChatRecu(Consumer<String[]> callback) {
        onChatRecuListeners.remove(callback);
    }

    /**
     * @deprecated Utiliser addOnChatRecu() — cette méthode efface tous les listeners existants.
     * Conservée uniquement pour compatibilité avec ChatController (côté patient).
     */
    @Deprecated
    public void setOnChatRecu(Consumer<String[]> callback) {
        onChatRecuListeners.clear();
        onChatRecuListeners.add(callback);
    }

    public void setOnHistorique(Consumer<String[]> c) { this.onHistorique = c; }
    public void setOnTyping(Consumer<String> c)       { this.onTyping     = c; }
    public void setOnOnlineList(Consumer<String[]> c) { this.onOnlineList = c; }

    public boolean isConnecte() { return connecte; }
    public String  getUserId()  { return userId;   }

    public static String userIdPatient(int id)         { return "patient_" + id; }
    public static String slugMedecin(String nomComplet) {
        return "medecin_" + nomComplet.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}