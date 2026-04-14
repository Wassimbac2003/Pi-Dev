package com.mrigl.donationapp.service;

import com.mrigl.donationapp.model.Annonce;
import com.mrigl.donationapp.model.Donation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contrôle de saisie aligné sur les contraintes Symfony (Assert) des entités.
 */
public final class ValidationService {

    private static final Set<String> STATUTS = Set.of("en attente", "accepté", "refusé");
    private static final Set<String> URGENCES = Set.of("faible", "moyenne", "élevée");
    private static final Set<String> ETATS = Set.of("active", "clôturée");

    private ValidationService() {
    }

    public static List<String> validateAnnonce(Annonce a) {
        List<String> errors = new ArrayList<>();
        String titre = trim(a.getTitreAnnonce());
        if (titre.isEmpty()) {
            errors.add("Le titre est obligatoire.");
        } else if (titre.length() < 5) {
            errors.add("Le titre doit contenir au moins 5 caractères.");
        } else if (titre.length() > 150) {
            errors.add("Le titre ne peut pas dépasser 150 caractères.");
        }

        String desc = trim(a.getDescription());
        if (desc.isEmpty()) {
            errors.add("La description est obligatoire.");
        } else if (desc.length() < 10) {
            errors.add("La description doit contenir au moins 10 caractères.");
        }

        if (a.getDatePublication() == null) {
            errors.add("La date de publication est obligatoire.");
        } else if (a.getId() == null && a.getDatePublication().isBefore(LocalDate.now())) {
            errors.add("La date de publication ne peut pas être dans le passé.");
        }

        String urgence = trim(a.getUrgence());
        if (urgence.isEmpty()) {
            errors.add("Le niveau d'urgence est obligatoire.");
        } else if (!URGENCES.contains(urgence)) {
            errors.add("Le niveau d'urgence doit être faible, moyenne ou élevée.");
        }

        String etat = trim(a.getEtatAnnonce());
        if (etat.isEmpty()) {
            errors.add("L'état de l'annonce est obligatoire.");
        } else if (!ETATS.contains(etat)) {
            errors.add("L'état de l'annonce doit être active ou clôturée.");
        }

        return errors;
    }

    public static List<String> validateDonation(Donation d) {
        List<String> errors = new ArrayList<>();
        String type = trim(d.getTypeDon());
        if (type.isEmpty()) {
            errors.add("Le type de don est obligatoire.");
        } else if (type.length() > 50) {
            errors.add("Le type de don ne peut pas dépasser 50 caractères.");
        }

        if (d.getQuantite() == null) {
            errors.add("La quantité est obligatoire.");
        } else if (d.getQuantite() <= 0) {
            errors.add("La quantité doit être un nombre positif.");
        }

        if (d.getDateDonation() == null) {
            errors.add("La date du don est obligatoire.");
        }

        String statut = trim(d.getStatut());
        if (statut.isEmpty()) {
            errors.add("Le statut est obligatoire.");
        } else if (!STATUTS.contains(statut)) {
            errors.add("Le statut doit être en attente, accepté ou refusé.");
        }

        return errors;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
