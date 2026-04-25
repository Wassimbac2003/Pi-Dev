package Services;

import Models.disponibilite;
import Models.rdv;
import Utils.MyDb;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Logique des créneaux — même logique que CalendrierController.php
 *
 * Horaires fixes : 09:00-12:00 (matin) + 14:00-17:00 (soir)
 * Samedi : 09:00-13:00
 * Dimanche : fermé sauf dispo exceptionnelle
 * Le médecin id=1 a des dispos dynamiques (midi, annulations, etc.)
 */
public class CreneauService {

    private static final int DYNAMIC_MED_ID = 1;

    private final DisponibiliteService dispoService = new DisponibiliteService();
    private final RdvService rdvService = new RdvService();

    /**
     * Résultat d'un créneau
     */
    public static class Creneau {
        public String heure;
        public boolean disponible;
        public boolean pris;

        public Creneau(String heure, boolean disponible, boolean pris) {
            this.heure = heure;
            this.disponible = disponible;
            this.pris = pris;
        }
    }

    /**
     * Résultat complet pour une date + médecin
     */
    public static class CreneauxResult {
        public List<Creneau> creneaux;
        public boolean samedi;
        public String message;

        public CreneauxResult(List<Creneau> creneaux, boolean samedi, String message) {
            this.creneaux = creneaux;
            this.samedi = samedi;
            this.message = message;
        }
    }

    /**
     * Point d'entrée principal — récupérer les créneaux pour un médecin + date
     */
    public CreneauxResult getCreneaux(int medecinId, String dateStr, String nomMedecin) throws SQLException {
        LocalDate date = LocalDate.parse(dateStr);
        DayOfWeek jour = date.getDayOfWeek();

        // Récupérer les heures déjà réservées (RDV non annulés)
        List<String> heuresReservees = getHeuresReservees(nomMedecin, dateStr);

        // Médecin dynamique (id=1) → logique spéciale
        if (medecinId == DYNAMIC_MED_ID) {
            return getCreneauxDynamic(medecinId, dateStr, jour, heuresReservees);
        }

        // Autres médecins → logique statique
        return getCreneauxStatique(jour, heuresReservees);
    }

    // ======================== CRÉNEAUX DYNAMIQUES (médecin id=1) ========================
    private CreneauxResult getCreneauxDynamic(int medecinId, String dateStr, DayOfWeek jour, List<String> heuresReservees) throws SQLException {
        List<disponibilite> toutesDispos = dispoService.findByMedecinAndDate(medecinId, dateStr);

        List<String[]> seancesAnnulees = new ArrayList<>();
        List<String[]> disposExtra = new ArrayList<>();

        for (disponibilite d : toutesDispos) {
            String hdebut = d.getHdebut().length() > 5 ? d.getHdebut().substring(0, 5) : d.getHdebut();
            String hfin = d.getH_fin().length() > 5 ? d.getH_fin().substring(0, 5) : d.getH_fin();
            String[] item = {hdebut, hfin};

            if ("non_disponible".equalsIgnoreCase(d.getStatut())) {
                seancesAnnulees.add(item);
            } else {
                disposExtra.add(item);
            }
        }

        boolean matinAnnule = isSeanceAnnulee(seancesAnnulees, "09:00", "12:00");
        boolean soirAnnule = isSeanceAnnulee(seancesAnnulees, "14:00", "17:00");
        boolean midiActif = isExtraActif(disposExtra, "12:00", "14:00");

        // Samedi
        if (jour == DayOfWeek.SATURDAY) {
            return new CreneauxResult(genererSlots("09:00", "13:00", heuresReservees), true, null);
        }

        // Dimanche
        if (jour == DayOfWeek.SUNDAY) {
            boolean dimExtra = isExtraActif(disposExtra, "10:00", "14:00");
            if (dimExtra) {
                return new CreneauxResult(genererSlots("10:00", "14:00", heuresReservees), false, "Dimanche — créneaux exceptionnels 10h00-14h00");
            }
            return new CreneauxResult(new ArrayList<>(), false, "Aucun créneau disponible le dimanche");
        }

        // Jour normal
        List<Creneau> creneaux = new ArrayList<>();
        LocalTime current = LocalTime.of(9, 0);
        LocalTime fin = LocalTime.of(17, 0);

        while (current.isBefore(fin)) {
            String h = String.format("%02d:%02d", current.getHour(), current.getMinute());
            boolean disponible = true;

            // Pause midi bloquée sauf si dispo extra
            if (current.getHour() >= 12 && current.getHour() < 14) disponible = midiActif;
            // Matin annulé
            if (matinAnnule && current.getHour() >= 9 && current.getHour() < 12) disponible = false;
            // Soir annulé
            if (soirAnnule && current.getHour() >= 14 && current.getHour() < 17) disponible = false;
            // Déjà réservé
            if (heuresReservees.contains(h)) disponible = false;

            creneaux.add(new Creneau(h, disponible, heuresReservees.contains(h)));
            current = current.plusMinutes(30);
        }

        return new CreneauxResult(creneaux, false, null);
    }

    // ======================== CRÉNEAUX STATIQUES (autres médecins) ========================
    private CreneauxResult getCreneauxStatique(DayOfWeek jour, List<String> heuresReservees) {
        // Dimanche
        if (jour == DayOfWeek.SUNDAY) {
            return new CreneauxResult(new ArrayList<>(), false, "Aucun créneau disponible le dimanche");
        }

        // Samedi
        if (jour == DayOfWeek.SATURDAY) {
            return new CreneauxResult(genererSlots("09:00", "13:00", heuresReservees), true, null);
        }

        // Jour normal : matin + midi bloqué + soir
        List<Creneau> creneaux = new ArrayList<>();
        creneaux.addAll(genererSlots("09:00", "12:00", heuresReservees));
        creneaux.addAll(genererSlotsBloques("12:00", "14:00"));
        creneaux.addAll(genererSlots("14:00", "17:00", heuresReservees));

        return new CreneauxResult(creneaux, false, null);
    }

    // ======================== HELPERS ========================

    private List<Creneau> genererSlots(String debut, String fin, List<String> heuresReservees) {
        List<Creneau> slots = new ArrayList<>();
        LocalTime current = LocalTime.parse(debut);
        LocalTime finTime = LocalTime.parse(fin);

        while (current.isBefore(finTime)) {
            String h = String.format("%02d:%02d", current.getHour(), current.getMinute());
            boolean pris = heuresReservees.contains(h);
            slots.add(new Creneau(h, !pris, pris));
            current = current.plusMinutes(30);
        }
        return slots;
    }

    private List<Creneau> genererSlotsBloques(String debut, String fin) {
        List<Creneau> slots = new ArrayList<>();
        LocalTime current = LocalTime.parse(debut);
        LocalTime finTime = LocalTime.parse(fin);

        while (current.isBefore(finTime)) {
            String h = String.format("%02d:%02d", current.getHour(), current.getMinute());
            slots.add(new Creneau(h, false, false));
            current = current.plusMinutes(30);
        }
        return slots;
    }

    private List<String> getHeuresReservees(String nomMedecin, String dateStr) throws SQLException {
        List<String> heures = new ArrayList<>();
        List<rdv> tousRdv = rdvService.findAll();

        for (rdv r : tousRdv) {
            if (r.getDate().equals(dateStr)
                    && r.getMedecin().equals(nomMedecin)
                    && !r.getStatut().equalsIgnoreCase("Annulé")) {
                String h = r.getHdebut();
                if (h.length() > 5) h = h.substring(0, 5);
                heures.add(h);
            }
        }
        return heures;
    }

    private boolean isSeanceAnnulee(List<String[]> seances, String debut, String fin) {
        for (String[] s : seances) {
            if (s[0].equals(debut) && s[1].equals(fin)) return true;
        }
        return false;
    }

    private boolean isExtraActif(List<String[]> dispos, String debut, String fin) {
        for (String[] d : dispos) {
            if (d[0].equals(debut) && d[1].equals(fin)) return true;
        }
        return false;
    }
}