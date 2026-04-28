package Services;

import Utils.MyDb;

import java.sql.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

/**
 * CreneauAiService — Version locale (sans API externe, 100% gratuite)
 *
 * Génère 3 créneaux optimaux en analysant :
 *  - Les RDV existants du médecin (évite les conflits)
 *  - Les horaires standards (09:00–17:00, lun–ven)
 *  - Le motif du patient (pour choisir le meilleur moment)
 */
public class CreneauAiService {

    private final Connection cnx;

    public CreneauAiService() {
        this.cnx = MyDb.getInstance().getConnection();
    }

    // ── Modèle de données ─────────────────────────────────────────

    public static class CreneauSuggere {
        public String date;
        public String hdebut;
        public String hfin;
        public String raison;
        public String score;

        @Override
        public String toString() {
            return date + " · " + hdebut + " – " + hfin + " (" + score + ")";
        }
    }

    // ── Point d'entrée principal ──────────────────────────────────

    public List<CreneauSuggere> suggererCreneaux(String nomMedecin,
                                                 String specialite,
                                                 String motif) {
        try {
            // 1. Lire les créneaux déjà pris
            Set<String> creneauxPris = lireCreneauxPris(nomMedecin);

            // 2. Déterminer les horaires préférés selon le motif
            List<LocalTime> heuresPref = heuresPreferees(motif);

            // 3. Générer les créneaux libres
            return genererCreneaux(creneauxPris, heuresPref, motif);

        } catch (Exception e) {
            System.err.println("❌ [CreneauAiService] Erreur : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── Lire les créneaux déjà réservés ──────────────────────────

    private Set<String> lireCreneauxPris(String nomMedecin) {
        Set<String> pris = new HashSet<>();
        try {
            String sql = """
                SELECT date, hdebut
                FROM rdv
                WHERE medecin = ?
                  AND date >= ?
                  AND LOWER(statut) NOT IN ('annule','annulé','expiré','expire','passe')
                """;
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, nomMedecin);
            ps.setString(2, LocalDate.now().toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String date   = rs.getString("date");
                String hdebut = rs.getString("hdebut");
                if (hdebut != null && hdebut.length() >= 5)
                    pris.add(date + "_" + hdebut.substring(0, 5));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture RDV : " + e.getMessage());
        }
        return pris;
    }

    // ── Heures préférées selon le motif ──────────────────────────

    private List<LocalTime> heuresPreferees(String motif) {
        String m = motif != null ? motif.toLowerCase() : "";

        // Urgence → le plus tôt possible
        if (m.contains("urgence")) {
            return List.of(
                    LocalTime.of(9,  0),
                    LocalTime.of(9, 30),
                    LocalTime.of(10, 0),
                    LocalTime.of(10, 30),
                    LocalTime.of(11, 0)
            );
        }
        // Suivi / contrôle → matin préféré
        if (m.contains("suivi") || m.contains("contrôle") || m.contains("controle")) {
            return List.of(
                    LocalTime.of(9,  0),
                    LocalTime.of(10, 0),
                    LocalTime.of(11, 0),
                    LocalTime.of(14, 0),
                    LocalTime.of(15, 0),
                    LocalTime.of(16, 0)
            );
        }
        // Consultation générale → varié matin + après-midi
        return List.of(
                LocalTime.of(9,  0),
                LocalTime.of(10, 30),
                LocalTime.of(14, 0),
                LocalTime.of(15, 30),
                LocalTime.of(11, 0),
                LocalTime.of(16, 0)
        );
    }

    // ── Générer 3 créneaux libres ─────────────────────────────────

    private List<CreneauSuggere> genererCreneaux(Set<String> creneauxPris,
                                                 List<LocalTime> heuresPref,
                                                 String motif) {
        List<CreneauSuggere> resultats = new ArrayList<>();
        LocalDate today = LocalDate.now();
        boolean matinFait = false;

        // Parcourir les 14 prochains jours ouvrables
        for (int jour = 1; jour <= 14 && resultats.size() < 3; jour++) {
            LocalDate date = today.plusDays(jour);

            // Ignorer week-ends
            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;

            for (LocalTime heure : heuresPref) {
                if (resultats.size() >= 3) break;

                String key = date + "_" + heure.toString();
                if (creneauxPris.contains(key)) continue;

                LocalTime hfin = heure.plusMinutes(30);
                boolean estMatin = heure.isBefore(LocalTime.NOON);

                // Pour le 2e créneau, forcer l'opposé du 1er (varier matin/après-midi)
                if (resultats.size() == 1) {
                    if (matinFait && estMatin) continue;
                    if (!matinFait && !estMatin) continue;
                }

                String score  = calculerScore(heure, resultats.size(), motif);
                String raison = construireRaison(heure, date, motif);

                CreneauSuggere cs = new CreneauSuggere();
                cs.date   = date.toString();
                cs.hdebut = String.format("%02d:%02d", heure.getHour(), heure.getMinute());
                cs.hfin   = String.format("%02d:%02d", hfin.getHour(), hfin.getMinute());
                cs.score  = score;
                cs.raison = raison;
                resultats.add(cs);

                if (estMatin) matinFait = true;
            }
        }

        return resultats;
    }

    // ── Calcul du score ───────────────────────────────────────────

    private String calculerScore(LocalTime heure, int position, String motif) {
        String m = motif != null ? motif.toLowerCase() : "";

        if (position == 0) return "Optimal";

        if (m.contains("urgence") && heure.isBefore(LocalTime.NOON))
            return "Optimal";

        if (m.contains("suivi") && heure.isBefore(LocalTime.NOON))
            return "Bon";

        if (position == 1) return "Bon";

        return "Disponible";
    }

    // ── Construction de la raison ─────────────────────────────────

    private String construireRaison(LocalTime heure, LocalDate date, String motif) {
        String m = motif != null ? motif.toLowerCase() : "";
        boolean estMatin = heure.isBefore(LocalTime.NOON);

        String jour = date.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.FRENCH);
        jour = jour.substring(0, 1).toUpperCase() + jour.substring(1);

        if (m.contains("urgence")) {
            return estMatin ? "Créneau prioritaire disponible" : "Disponibilité rapide";
        }
        if (m.contains("suivi") || m.contains("controle") || m.contains("contrôle")) {
            return estMatin ? "Idéal pour un suivi le matin" : "Suivi en début d'après-midi";
        }
        if (estMatin) {
            return "Créneau matinal — " + jour;
        }
        return "Créneau après-midi — " + jour;
    }
}