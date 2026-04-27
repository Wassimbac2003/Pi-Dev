package Services;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import Models.ChatMessage;
import java.util.List;

public class SocketServer {

    private static SocketServer instance;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean running = false;
    private final int PORT = 5555;

    private final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();
    private final Map<String, List<String>> historique = new ConcurrentHashMap<>();
    private ChatMessageService chatMessageService = new ChatMessageService();

    private SocketServer() {}

    public static synchronized SocketServer getInstance() {
        if (instance == null) {
            instance = new SocketServer();
        }
        return instance;
    }

    public synchronized void demarrer() {
        if (running) {
            System.out.println("⚠️ [SERVEUR] Déjà démarré");
            return;
        }

        if (serverThread != null && serverThread.isAlive()) {
            System.out.println("⚠️ [SERVEUR] Thread déjà actif");
            return;
        }

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                running = true;  // ✅ METTRE À TRUE AVANT LE PRINTLN
                System.out.println("✅ [SERVEUR] Démarré sur le port " + PORT);

                while (running && !serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (BindException e) {
                System.out.println("⚠️ [SERVEUR] Port " + PORT + " déjà utilisé - Un autre serveur tourne probablement");
                // ✅ NE PAS mettre running = false ici !
                // Le serveur existant fonctionne, donc on considère qu'il y a un serveur actif
            } catch (IOException e) {
                if (running) {
                    System.err.println("❌ [SERVEUR] Erreur : " + e.getMessage());
                }
                running = false;
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();

        // ✅ Attendre que le thread démarre
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void arreter() {
        if (!running) return;

        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            clients.clear();
            System.out.println("🛑 [SERVEUR] Arrêté");
        } catch (IOException e) {
            System.err.println("❌ [SERVEUR] Erreur arrêt : " + e.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        String userId = null;
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
        ) {
            String message;

            while ((message = in.readLine()) != null) {
                String[] parts = message.split("\\|", 4);
                String protocole = parts[0];

                switch (protocole) {
                    case "REGISTER":
                        userId = parts[1];
                        clients.put(userId, out);
                        System.out.println("📋 [SERVEUR] Enregistré: " + userId);
                        break;

                    case "CHAT":
                        if (parts.length >= 4) {
                            String sender = parts[1];
                            String dest = parts[2];
                            String msg = parts[3];

                            // ✅ SAUVEGARDER EN BASE DE DONNÉES
                            chatMessageService.sauvegarderMessage(sender, dest, msg);

                            // Sauvegarder dans l'historique mémoire
                            sauvegarderMessage(sender, dest, msg);

                            // Envoyer au destinataire
                            PrintWriter destWriter = clients.get(dest);
                            if (destWriter != null) {
                                destWriter.println("CHAT_SENT|" + sender + "|" + dest + "|" + msg);
                            }

                            System.out.println("💬 [CHAT] " + sender + " → " + dest + " : " + msg);
                        }
                        break;

                    case "HISTORY":
                        if (parts.length >= 3) {
                            String user1 = parts[1];
                            String user2 = parts[2];
                            envoyerHistorique(user1, user2, out);
                        }
                        break;

                    case "ONLINE":
                        out.println("ONLINE_LIST|" + String.join(",", clients.keySet()));
                        break;

                    case "TYPING":
                        if (parts.length >= 3) {
                            String typer = parts[1];
                            String typingDest = parts[2];
                            PrintWriter typingWriter = clients.get(typingDest);
                            if (typingWriter != null) {
                                typingWriter.println("TYPING|" + typer);
                            }
                        }
                        break;
                }
            }

        } catch (IOException e) {
            System.err.println("❌ [CLIENT] Déconnecté : " + e.getMessage());
        } finally {
            if (userId != null) {
                clients.remove(userId);
                System.out.println("👋 [SERVEUR] Déconnecté: " + userId);
            }
        }
    }

    private void sauvegarderMessage(String sender, String dest, String message) {
        String key = creerCleHistorique(sender, dest);
        historique.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>())
                .add(System.currentTimeMillis() + "|" + sender + "|" + message);
    }

    private void envoyerHistorique(String user1, String user2, PrintWriter out) {
        // ✅ CHARGER DEPUIS LA BASE DE DONNÉES
        List<ChatMessage> messages = chatMessageService.getHistorique(user1, user2);

        if (messages != null && !messages.isEmpty()) {
            for (ChatMessage msg : messages) {
                out.println("HISTORY|" + msg.getSenderId() + "|" + user2 + "|" + msg.getMessage());
            }
        }
    }

    private String creerCleHistorique(String user1, String user2) {
        return user1.compareTo(user2) < 0
                ? user1 + "::" + user2
                : user2 + "::" + user1;
    }

    public boolean isRunning() {
        return running;
    }
}