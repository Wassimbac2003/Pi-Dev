package Services;

import Utils.MyDb;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de rappels — même logique que /api/rappels dans Symfony
 * Génère des bannières selon la proximité du RDV confirmé
 */
public class RappelService {

    private Connection cnx;

    public RappelService() {
        cnx = MyDb.getInstance().getConnection();
    }

    public static class Rappel {
        public int id;
        public String message;
        public String niveau;   // "urgent" (rouge), "today" (orange), "tomorrow" (bleu)
        public long minutes;
        public String medecin;

        public Rappel(int id, String message, String niveau, long minutes, String medecin) {
            this.id = id;
            this.message = message;
            this.niveau = niveau;
            this.minutes = minutes;
            this.medecin = medecin;
        }
    }

    public static class RappelResult {
        public int count;
        public List<Rappel> rappels;

        public RappelResult(int count, List<Rappel> rappels) {
            this.count = count;
            this.rappels = rappels;
        }
    }

    /**
     * Récupérer les rappels actifs (uniquement RDV confirmés)
     */
    public RappelResult getRappels() throws SQLException {
        List<Rappel> rappels = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        String query = "SELECT id, date, hdebut, statut, medecin FROM rdv WHERE LOWER(statut) LIKE '%confirm%' ORDER BY date ASC, hdebut ASC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(query);

        while (rs.next()) {
            int id = rs.getInt("id");
            String medecin = rs.getString("medecin");
            String dateStr = rs.getString("date");
            String hdebutStr = rs.getString("hdebut");

            // Parser la date et l'heure
            LocalDate rdvDate;
            LocalTime rdvTime;
            try {
                rdvDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                continue;
            }

            try {
                if (hdebutStr.length() >= 5) {
                    rdvTime = LocalTime.parse(hdebutStr.substring(0, 5));
                } else {
                    rdvTime = LocalTime.parse(hdebutStr);
                }
            } catch (Exception e) {
                rdvTime = LocalTime.of(9, 0);
            }

            LocalDateTime rdvDateTime = LocalDateTime.of(rdvDate, rdvTime);

            // RDV déjà passé → ignorer
            if (rdvDateTime.isBefore(now)) continue;

            long minutesRestantes = ChronoUnit.MINUTES.between(now, rdvDateTime);
            String heureFormatted = rdvTime.toString().substring(0, 5);

            String message;
            String niveau;

            if (minutesRestantes <= 60) {
                // Moins d'1h → URGENT (rouge)
                niveau = "urgent";
                message = "URGENT ! RDV avec " + medecin + " dans " + minutesRestantes + " minutes !";
            } else if (rdvDate.equals(today)) {
                // Aujourd'hui → orange
                niveau = "today";
                message = "Rappel : RDV avec " + medecin + " aujourd'hui a " + heureFormatted;
            } else if (rdvDate.equals(tomorrow)) {
                // Demain → bleu
                niveau = "tomorrow";
                message = "Rappel : RDV avec " + medecin + " demain a " + heureFormatted;
            } else {
                continue; // Après-demain ou plus → pas de bannière
            }

            rappels.add(new Rappel(id, message, niveau, minutesRestantes, medecin));
        }

        // Trier par proximité (le plus urgent en premier)
        rappels.sort((a, b) -> Long.compare(a.minutes, b.minutes));

        return new RappelResult(rappels.size(), rappels);
    }
}