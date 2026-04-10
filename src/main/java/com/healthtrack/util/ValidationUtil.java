package com.healthtrack.util;

import com.healthtrack.entities.MissionVolunteer;
import com.healthtrack.entities.Sponsor;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextInputControl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class ValidationUtil {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\+\\d{1,3}\\s?)?\\d{8,14}$");
    private static final Set<String> IMAGE_EXTENSIONS =
            Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp");

    private ValidationUtil() {
    }

    public static List<String> validateMission(String titre, String description, String lieu,
                                               LocalDate dateDebut, LocalDate dateFin,
                                               String statut, String photo) {
        List<String> errors = new ArrayList<>();
        String cleanTitre = safeTrim(titre);
        String cleanDescription = safeTrim(description);
        String cleanLieu = safeTrim(lieu);
        String cleanStatut = safeTrim(statut);

        if (isBlank(cleanTitre) || cleanTitre.length() < 4 || cleanTitre.length() > 80) {
            errors.add("Le titre doit contenir entre 4 et 80 caracteres.");
        }
        if (!containsLetterOrDigit(cleanTitre)) {
            errors.add("Le titre doit contenir de vrais caracteres significatifs.");
        }
        if (hasLongRepeatedSequence(cleanTitre)) {
            errors.add("Le titre contient trop de caracteres repetes.");
        }
        if (isBlank(cleanDescription) || cleanDescription.length() < 10 || cleanDescription.length() > 600) {
            errors.add("La description doit contenir entre 10 et 600 caracteres.");
        }
        if (countWords(cleanDescription) < 3) {
            errors.add("La description doit contenir au moins 3 mots utiles.");
        }
        if (hasLongRepeatedSequence(cleanDescription)) {
            errors.add("La description contient trop de caracteres repetes.");
        }
        if (isBlank(cleanLieu) || cleanLieu.length() < 2 || cleanLieu.length() > 60) {
            errors.add("Le lieu doit contenir entre 2 et 60 caracteres.");
        }
        if (!containsLetterOrDigit(cleanLieu)) {
            errors.add("Le lieu doit contenir une ville ou une localisation valable.");
        }
        if (dateDebut == null || dateFin == null) {
            errors.add("Les dates de debut et de fin sont obligatoires.");
        } else {
            if (dateFin.isBefore(dateDebut)) {
                errors.add("La date de fin doit etre posterieure ou egale a la date de debut.");
            }
            if (dateDebut.isBefore(LocalDate.now().minusYears(1))) {
                errors.add("La date de debut semble incoherente.");
            }
            long durationDays = ChronoUnit.DAYS.between(dateDebut, dateFin);
            if (durationDays > 365) {
                errors.add("La mission ne doit pas depasser 365 jours.");
            }
        }
        if (isBlank(cleanStatut) || cleanStatut.length() < 3 || cleanStatut.length() > 30) {
            errors.add("Le statut doit contenir entre 3 et 30 caracteres.");
        }
        if (hasLongRepeatedSequence(cleanStatut)) {
            errors.add("Le statut n'est pas coherent.");
        }
        if (!isBlank(photo) && !hasImageExtension(photo)) {
            errors.add("La photo doit etre une image png, jpg, jpeg, gif ou webp.");
        }

        return errors;
    }

    public static List<String> validateSponsor(String company, String email, String logo,
                                               boolean requireMissionSelection, int selectedMissionCount) {
        List<String> errors = new ArrayList<>();
        String cleanCompany = safeTrim(company);
        String cleanEmail = safeTrim(email);

        if (isBlank(cleanCompany) || cleanCompany.length() < 2 || cleanCompany.length() > 80) {
            errors.add("Le nom de la societe doit contenir entre 2 et 80 caracteres.");
        }
        if (!containsLetterOrDigit(cleanCompany) || cleanCompany.matches("^\\d+$")) {
            errors.add("Le nom de la societe n'est pas valide.");
        }
        if (hasLongRepeatedSequence(cleanCompany)) {
            errors.add("Le nom de la societe contient trop de caracteres repetes.");
        }
        if (isBlank(cleanEmail) || !EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            errors.add("L'email du sponsor n'est pas valide.");
        }
        if (!isBlank(logo) && !hasImageExtension(logo)) {
            errors.add("Le logo doit etre une image png, jpg, jpeg, gif ou webp.");
        }
        if (requireMissionSelection && selectedMissionCount == 0) {
            errors.add("Selectionnez au moins une mission pour ce sponsor.");
        }

        return errors;
    }

    public static List<String> validateVolunteer(String motivation, String telephone, String statut,
                                                 int userId, int missionId, String disponibilitesCsv) {
        List<String> errors = new ArrayList<>();
        String cleanMotivation = safeTrim(motivation);
        String cleanStatut = safeTrim(statut);
        List<String> disponibilites = parseDistinctCsv(disponibilitesCsv);

        if (isBlank(cleanMotivation) || cleanMotivation.length() < 10 || cleanMotivation.length() > 400) {
            errors.add("La motivation doit contenir entre 10 et 400 caracteres.");
        }
        if (countWords(cleanMotivation) < 3) {
            errors.add("La motivation doit exprimer au moins 3 mots utiles.");
        }
        if (hasLongRepeatedSequence(cleanMotivation)) {
            errors.add("La motivation contient trop de caracteres repetes.");
        }
        if (isBlank(telephone) || !PHONE_PATTERN.matcher(normalizePhone(telephone)).matches()) {
            errors.add("Le numero de telephone n'est pas valide.");
        }
        if (isBlank(cleanStatut) || cleanStatut.length() < 3 || cleanStatut.length() > 30) {
            errors.add("Le statut doit contenir entre 3 et 30 caracteres.");
        }
        if (userId <= 0) {
            errors.add("L'identifiant utilisateur doit etre superieur a 0.");
        }
        if (missionId <= 0) {
            errors.add("L'identifiant mission doit etre superieur a 0.");
        }
        if (disponibilites.isEmpty()) {
            errors.add("Ajoutez au moins une disponibilite.");
        }
        if (disponibilites.size() > 7) {
            errors.add("Le nombre de disponibilites semble excessif.");
        }

        return errors;
    }

    public static List<String> validateApplication(String motivation, String telephone, List<String> disponibilites) {
        List<String> errors = new ArrayList<>();
        String cleanMotivation = safeTrim(motivation);

        if (disponibilites == null || disponibilites.isEmpty()) {
            errors.add("Choisissez au moins une disponibilite.");
        }
        if (isBlank(cleanMotivation) || cleanMotivation.length() < 15 || cleanMotivation.length() > 500) {
            errors.add("La motivation doit contenir entre 15 et 500 caracteres.");
        }
        if (countWords(cleanMotivation) < 4) {
            errors.add("La motivation doit contenir au moins 4 mots utiles.");
        }
        if (hasLongRepeatedSequence(cleanMotivation)) {
            errors.add("La motivation contient trop de caracteres repetes.");
        }
        if (isBlank(telephone) || !PHONE_PATTERN.matcher(normalizePhone(telephone)).matches()) {
            errors.add("Le numero de telephone n'est pas valide.");
        }

        return errors;
    }

    public static boolean isDuplicateMission(Collection<MissionVolunteer> missions, int currentId, String titre,
                                             String lieu, LocalDate dateDebut, LocalDate dateFin) {
        String normalizedTitle = normalizeForComparison(titre);
        String normalizedLieu = normalizeForComparison(lieu);
        return missions != null && missions.stream()
                .filter(mission -> mission != null && mission.getId() != currentId)
                .anyMatch(mission -> normalizeForComparison(mission.getTitre()).equals(normalizedTitle)
                        && normalizeForComparison(mission.getLieu()).equals(normalizedLieu)
                        && sameDate(mission.getDateDebut(), dateDebut)
                        && sameDate(mission.getDateFin(), dateFin));
    }

    public static boolean isDuplicateSponsor(Collection<Sponsor> sponsors, int currentId, String company, String email) {
        String normalizedCompany = normalizeForComparison(company);
        String normalizedEmail = normalizeForComparison(email);
        return sponsors != null && sponsors.stream()
                .filter(sponsor -> sponsor != null && sponsor.getId() != currentId)
                .anyMatch(sponsor -> normalizeForComparison(sponsor.getNomSociete()).equals(normalizedCompany)
                        || normalizeForComparison(sponsor.getContactEmail()).equals(normalizedEmail));
    }

    public static void clearInvalid(Control... controls) {
        for (Control control : controls) {
            if (control != null) {
                control.setStyle("");
            }
        }
    }

    public static void markInvalid(Control control) {
        if (control != null) {
            control.setStyle("-fx-border-color: #e11d48; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        }
    }

    public static void markIfInvalid(TextInputControl control, boolean invalid) {
        if (invalid) {
            markInvalid(control);
        }
    }

    public static void markIfInvalid(DatePicker control, boolean invalid) {
        if (invalid) {
            markInvalid(control);
        }
    }

    public static void markIfInvalid(CheckBox control, boolean invalid) {
        if (invalid) {
            markInvalid(control);
        }
    }

    private static boolean hasImageExtension(String path) {
        String lower = path.trim().toLowerCase();
        return IMAGE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private static boolean sameDate(LocalDate first, LocalDate second) {
        return first == null ? second == null : first.equals(second);
    }

    private static String normalizeForComparison(String value) {
        return safeTrim(value).toLowerCase().replaceAll("\\s+", " ");
    }

    private static String normalizePhone(String telephone) {
        return telephone == null ? "" : telephone.replaceAll("\\s+", "");
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean containsLetterOrDigit(String value) {
        return value != null && value.chars().anyMatch(Character::isLetterOrDigit);
    }

    private static boolean hasLongRepeatedSequence(String value) {
        return value != null && value.matches(".*(.)\\1{4,}.*");
    }

    private static int countWords(String value) {
        if (isBlank(value)) {
            return 0;
        }
        return (int) Pattern.compile("\\s+")
                .splitAsStream(value.trim())
                .filter(token -> token.chars().anyMatch(Character::isLetterOrDigit))
                .count();
    }

    private static List<String> parseDistinctCsv(String value) {
        if (isBlank(value)) {
            return List.of();
        }
        return Pattern.compile(",")
                .splitAsStream(value)
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
