package tn.esprit.tools;

import tn.esprit.entities.Medicament;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

public final class ValidationUtil {

    private static final Pattern TENSION = Pattern.compile("^\\d{1,3}\\s*/\\s*\\d{1,3}$");
    public static final List<String> GRAVITES = List.of("Faible", "Modérée", "Élevée");
    public static final List<String> GROUPES_SANGUINS = List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");

    public static final int MAX_LIBELLE = 100;
    public static final int MAX_ALLERGIE = 255;
    public static final int MAX_CHRONIQUE = 255;
    public static final int MAX_RECO = 2000;
    public static final int MAX_SYMPTOMES = 2000;
    public static final int MAX_SCAN_TOKEN = 120;

    /** Saisie fiche (champs bruts + options patient / médecin). */
    public record FicheFormInput(
            Integer patientUserId,
            Integer medecinUserId,
            boolean requireMedecin,
            String poidsRaw,
            String tailleRaw,
            String grpSanguin,
            String gravite,
            String allergie,
            String maladieChronique,
            String tension,
            String glycemieRaw,
            String libelle,
            String recommandation,
            String symptomes,
            LocalDate dateConsult,
            boolean consultationDateRequired
    ) {
    }

    /** Saisie ordonnance (sélections + champs). */
    public record OrdonnanceFormInput(
            Integer patientUserId,
            Integer medecinUserId,
            Integer ficheId,
            int selectedMedicamentCount,
            String posologie,
            String frequence,
            String dureeRaw,
            LocalDate date,
            String hour,
            String minute,
            String scanToken
    ) {
    }

    private ValidationUtil() {
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    public static LinkedHashMap<String, String> validateMedicamentDetailed(Medicament m) {
        LinkedHashMap<String, String> e = new LinkedHashMap<>();
        String nom = m.getNomMedicament() == null ? "" : m.getNomMedicament().trim();
        if (nom.isEmpty()) {
            e.put("nom", "Le nom du médicament est obligatoire.");
        } else if (nom.length() > 255) {
            e.put("nom", "Le nom ne doit pas dépasser 255 caractères.");
        }
        String cat = trim(m.getCategorie());
        if (cat.length() > 50) {
            e.put("categorie", "La catégorie est trop longue (50 caractères maximum).");
        }
        String dos = trim(m.getDosage());
        if (dos.length() > 50) {
            e.put("dosage", "Le dosage est trop long (50 caractères maximum).");
        }
        String forme = trim(m.getForme());
        if (forme.length() > 50) {
            e.put("forme", "La forme est trop longue (50 caractères maximum).");
        }
        if (m.getDateExpiration() == null) {
            e.put("dateExpiration", "La date d’expiration est obligatoire.");
        } else if (m.getDateExpiration().isBefore(LocalDate.now())) {
            e.put("dateExpiration", "La date d’expiration doit être aujourd’hui ou dans le futur.");
        }
        return e;
    }

    public static String validateMedicament(Medicament m) {
        LinkedHashMap<String, String> d = validateMedicamentDetailed(m);
        return d.isEmpty() ? null : d.values().iterator().next();
    }

    public static LinkedHashMap<String, String> validateFicheDetailed(FicheFormInput in) {
        LinkedHashMap<String, String> e = new LinkedHashMap<>();
        if (in.patientUserId() == null) {
            e.put("patient", "Choisissez un patient (rôle ROLE_PATIENT).");
        }
        if (in.requireMedecin() && in.medecinUserId() == null) {
            e.put("medecin", "Choisissez le médecin qui rédige la fiche.");
        }
        if (in.consultationDateRequired() && in.dateConsult() == null) {
            e.put("date", "Choisissez une date de consultation.");
        }

        String pRaw = trim(in.poidsRaw());
        if (pRaw.isEmpty()) {
            e.put("poids", "Le poids est obligatoire.");
        } else {
            try {
                double p = Double.parseDouble(pRaw.replace(',', '.'));
                if (p <= 0 || p > 400) {
                    e.put("poids", "Le poids doit être compris entre 0 et 400 kg.");
                }
            } catch (NumberFormatException ex) {
                e.put("poids", "Le poids doit être un nombre valide (ex. 72 ou 72,5).");
            }
        }

        String tRaw = trim(in.tailleRaw());
        if (tRaw.isEmpty()) {
            e.put("taille", "La taille est obligatoire.");
        } else {
            try {
                double t = Double.parseDouble(tRaw.replace(',', '.'));
                if (t <= 0 || t > 280) {
                    e.put("taille", "La taille doit être comprise entre 0 et 280 cm.");
                }
            } catch (NumberFormatException ex) {
                e.put("taille", "La taille doit être un nombre valide (ex. 175).");
            }
        }

        String grp = in.grpSanguin();
        if (grp == null || !GROUPES_SANGUINS.contains(grp)) {
            e.put("grpSanguin", "Choisissez un groupe sanguin dans la liste.");
        }

        String grav = in.gravite();
        if (grav == null || !GRAVITES.contains(grav)) {
            e.put("gravite", "Choisissez une gravité : Faible, Modérée ou Élevée.");
        }

        String lib = trim(in.libelle());
        if (lib.isEmpty()) {
            e.put("libelle", "Le libellé / motif de consultation est obligatoire.");
        } else if (lib.length() > MAX_LIBELLE) {
            e.put("libelle", "Le libellé ne doit pas dépasser " + MAX_LIBELLE + " caractères.");
        }

        String tension = trim(in.tension());
        if (tension.isEmpty()) {
            e.put("tension", "La tension est obligatoire (format 120/80).");
        } else if (!TENSION.matcher(tension).matches()) {
            e.put("tension", "Tension invalide : utilisez le format systolique/diastolique (ex. 120/80).");
        }

        String gRaw = trim(in.glycemieRaw());
        if (gRaw.isEmpty()) {
            e.put("glycemie", "La glycémie est obligatoire.");
        } else {
            try {
                double g = Double.parseDouble(gRaw.replace(',', '.'));
                if (g < 0 || g > 60) {
                    e.put("glycemie", "La glycémie doit être entre 0 et 60 mmol/L (valeurs plausibles).");
                }
            } catch (NumberFormatException ex) {
                e.put("glycemie", "La glycémie doit être un nombre valide.");
            }
        }

        String all = in.allergie() != null ? in.allergie() : "";
        if (all.length() > MAX_ALLERGIE) {
            e.put("allergie", "Le champ allergies ne doit pas dépasser " + MAX_ALLERGIE + " caractères.");
        }
        String chr = in.maladieChronique() != null ? in.maladieChronique() : "";
        if (chr.length() > MAX_CHRONIQUE) {
            e.put("chronique", "Les maladies chroniques ne doivent pas dépasser " + MAX_CHRONIQUE + " caractères.");
        }
        String reco = in.recommandation() != null ? in.recommandation() : "";
        if (reco.length() > MAX_RECO) {
            e.put("recommandation", "Les recommandations ne doivent pas dépasser " + MAX_RECO + " caractères.");
        }
        String sym = in.symptomes() != null ? in.symptomes() : "";
        if (sym.length() > MAX_SYMPTOMES) {
            e.put("symptomes", "Les symptômes ne doivent pas dépasser " + MAX_SYMPTOMES + " caractères.");
        }
        return e;
    }

    public static String validateFiche(double poids, double taille, String tension, double glycemie,
                                       String libelle, String grpSanguin, String gravite) {
        if (libelle == null || libelle.isBlank()) {
            return "Le libellé / motif est obligatoire.";
        }
        if (libelle.length() > MAX_LIBELLE) {
            return "Le libellé ne doit pas dépasser " + MAX_LIBELLE + " caractères.";
        }
        if (poids <= 0 || poids > 400) {
            return "Le poids doit être compris entre 0 et 400 kg.";
        }
        if (taille <= 0 || taille > 280) {
            return "La taille doit être comprise entre 0 et 280 cm.";
        }
        if (tension == null || tension.isBlank()) {
            return "La tension est obligatoire (format 120/80).";
        }
        if (!TENSION.matcher(tension.trim()).matches()) {
            return "Tension invalide : utilisez le format systolique/diastolique (ex. 120/80).";
        }
        if (glycemie < 0 || glycemie > 60) {
            return "La glycémie doit être entre 0 et 60 (valeurs plausibles).";
        }
        if (grpSanguin == null || !GROUPES_SANGUINS.contains(grpSanguin)) {
            return "Choisissez un groupe sanguin valide.";
        }
        if (gravite == null || !GRAVITES.contains(gravite)) {
            return "Choisissez une gravité : Faible, Modérée ou Élevée.";
        }
        return null;
    }

    public static LinkedHashMap<String, String> validateOrdonnanceDetailed(OrdonnanceFormInput in) {
        LinkedHashMap<String, String> e = new LinkedHashMap<>();
        if (in.patientUserId() == null) {
            e.put("patient", "Choisissez un patient.");
        }
        if (in.medecinUserId() == null) {
            e.put("medecin", "Choisissez le médecin prescripteur.");
        }
        if (in.ficheId() == null) {
            e.put("fiche", "Choisissez une fiche clinique pour ce patient.");
        }
        if (in.selectedMedicamentCount() <= 0) {
            e.put("medicaments", "Sélectionnez au moins un médicament (Ctrl+clic).");
        }

        String pos = trim(in.posologie());
        if (pos.isEmpty()) {
            e.put("posologie", "La posologie est obligatoire.");
        } else if (pos.length() > 500) {
            e.put("posologie", "La posologie ne doit pas dépasser 500 caractères.");
        }

        String freq = trim(in.frequence());
        if (freq.isEmpty()) {
            e.put("frequence", "La fréquence est obligatoire.");
        } else if (freq.length() > 200) {
            e.put("frequence", "La fréquence ne doit pas dépasser 200 caractères.");
        }

        String dRaw = trim(in.dureeRaw());
        if (dRaw.isEmpty()) {
            e.put("duree", "La durée du traitement (en jours) est obligatoire.");
        } else {
            try {
                int d = Integer.parseInt(dRaw);
                if (d <= 0 || d > 3650) {
                    e.put("duree", "La durée doit être un entier entre 1 et 3650 jours.");
                }
            } catch (NumberFormatException ex) {
                e.put("duree", "La durée doit être un nombre entier de jours.");
            }
        }

        if (in.date() == null) {
            e.put("date", "Choisissez la date de l’ordonnance.");
        }

        String h = trim(in.hour());
        String mi = trim(in.minute());
        if (h.isEmpty() || mi.isEmpty()) {
            e.put("heure", "Choisissez l’heure et les minutes.");
        } else {
            try {
                int hi = Integer.parseInt(h);
                int mni = Integer.parseInt(mi);
                if (hi < 0 || hi > 23 || mni < 0 || mni > 59) {
                    e.put("heure", "Heure invalide (0–23 h, 0–59 min).");
                }
            } catch (NumberFormatException ex) {
                e.put("heure", "Heure ou minutes invalides.");
            }
        }

        String tok = in.scanToken() != null ? in.scanToken().trim() : "";
        if (tok.length() > MAX_SCAN_TOKEN) {
            e.put("token", "La référence / jeton ne doit pas dépasser " + MAX_SCAN_TOKEN + " caractères.");
        }
        return e;
    }

    public static String validateOrdonnance(String posologie, String frequence, int dureeJours) {
        if (posologie == null || posologie.isBlank()) {
            return "La posologie est obligatoire.";
        }
        if (posologie.length() > 500) {
            return "La posologie est trop longue.";
        }
        if (frequence == null || frequence.isBlank()) {
            return "La fréquence est obligatoire.";
        }
        if (frequence.length() > 200) {
            return "La fréquence est trop longue.";
        }
        if (dureeJours <= 0 || dureeJours > 3650) {
            return "La durée doit être entre 1 et 3650 jours.";
        }
        return null;
    }
}
