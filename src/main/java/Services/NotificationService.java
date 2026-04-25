package Services;

import Utils.MyDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service de notifications — même logique que NotificationController.php
 * Génère des notifications basées sur le statut des RDV
 */
public class NotificationService {

    private Connection cnx;
    // IDs déjà lus (équivalent de la session Symfony)
    private static final Set<Integer> notificationsLues = new HashSet<>();

    public NotificationService() {
        cnx = MyDb.getInstance().getConnection();
    }

    // ===== MODÈLE NOTIFICATION =====
    public static class Notification {
        public int id;
        public String message;
        public String type;     // success, danger, warning
        public String heure;
        public String medecin;
        public String date;
        public String statut;

        public Notification(int id, String message, String type, String heure, String medecin, String date, String statut) {
            this.id = id;
            this.message = message;
            this.type = type;
            this.heure = heure;
            this.medecin = medecin;
            this.date = date;
            this.statut = statut;
        }
    }

    // ===== RÉSULTAT =====
    public static class NotificationResult {
        public int count;
        public List<Notification> notifications;

        public NotificationResult(int count, List<Notification> notifications) {
            this.count = count;
            this.notifications = notifications;
        }
    }

    /**
     * Récupérer toutes les notifications non lues
     */
    public NotificationResult getNotifications() throws SQLException {
        List<Notification> all = new ArrayList<>();

        String query = "SELECT id, date, hdebut, statut, medecin FROM rdv ORDER BY date DESC, hdebut DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(query);

        while (rs.next()) {
            int id = rs.getInt("id");
            String medecin = rs.getString("medecin");
            String date = rs.getString("date");
            String hdebut = rs.getString("hdebut");
            String statut = rs.getString("statut");

            // Formater la date
            String dateFormatted = date;
            try {
                java.time.LocalDate ld = java.time.LocalDate.parse(date);
                dateFormatted = ld.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception ignored) {}

            // Formater l'heure
            String heureFormatted = hdebut;
            if (hdebut != null && hdebut.length() >= 5) {
                heureFormatted = hdebut.substring(0, 5);
            }

            // Générer le message selon le statut
            String message;
            String type;

            if (statut != null && statut.toLowerCase().contains("confirm")) {
                message = "RDV avec " + medecin + " le " + dateFormatted + " est confirme";
                type = "success";
            } else if (statut != null && statut.toLowerCase().contains("annul")) {
                message = "RDV avec " + medecin + " le " + dateFormatted + " a ete annule";
                type = "danger";
            } else if (statut != null && statut.toLowerCase().contains("expir")) {
                message = "RDV avec " + medecin + " le " + dateFormatted + " a expire";
                type = "danger";
            } else {
                message = "RDV avec " + medecin + " le " + dateFormatted + " est en attente";
                type = "warning";
            }

            all.add(new Notification(id, message, type, heureFormatted, medecin, dateFormatted, statut));
        }

        // Filtrer les notifications déjà lues
        List<Notification> nonLues = new ArrayList<>();
        for (Notification n : all) {
            if (!notificationsLues.contains(n.id)) {
                nonLues.add(n);
            }
        }

        return new NotificationResult(nonLues.size(), nonLues);
    }

    /**
     * Marquer une notification comme lue
     */
    public void marquerLu(int id) {
        notificationsLues.add(id);
    }

    /**
     * Marquer toutes les notifications comme lues
     */
    public void marquerToutLu() throws SQLException {
        String query = "SELECT id FROM rdv";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) {
            notificationsLues.add(rs.getInt("id"));
        }
    }

    /**
     * Nombre de notifications non lues
     */
    public int getCountNonLues() throws SQLException {
        return getNotifications().count;
    }
}