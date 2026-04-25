package Services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Service SMS via Twilio REST API (sans SDK).
 * Envoie un rappel SMS le jour même du RDV.
 */
public class SmsService {

    // ── Clés Twilio ──
    private static final String TWILIO_SID  = "AC97a4ec855bae21ac4218e5cfa87c6afe";
    private static final String TWILIO_TOKEN = "eeeac07e5c1177fbbb832744b7b98c4c";
    private static final String TWILIO_FROM  = "+17744692162";

    // ── Numéro du patient (hardcodé pour l'instant) ──
    private static final String PATIENT_PHONE = "+21629254485";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Vérifie si le RDV est aujourd'hui.
     */
    public boolean estAujourdhui(String dateRdv) {
        try {
            LocalDate rdvDate = LocalDate.parse(dateRdv); // format YYYY-MM-DD
            return rdvDate.equals(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Envoie un SMS de rappel pour un RDV.
     *
     * @param medecin  Nom du médecin
     * @param dateRdv  Date du RDV (format YYYY-MM-DD)
     * @param heureRdv Heure du RDV (ex: "09:00")
     * @param motif    Motif du RDV
     * @return true si envoi réussi, false sinon
     */
    public boolean envoyerRappel(String medecin, String dateRdv, String heureRdv, String motif) {
        // ── MODE DEMO : envoi autorisé quelle que soit la date ──
        // TODO: remettre la vérification après la démo
        // if (!estAujourdhui(dateRdv)) {
        //     System.out.println("SMS non envoyé : le RDV n'est pas aujourd'hui (" + dateRdv + ")");
        //     return false;
        // }

        // ── Formater la date pour le message ──
        String dateFormatee;
        try {
            LocalDate d = LocalDate.parse(dateRdv);
            dateFormatee = d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            dateFormatee = dateRdv;
        }

        // ── Construire le message ──
        String message = "🏥 VitalTech - Rappel RDV\n\n"
                + "Bonjour,\n"
                + "Rappel de votre rendez-vous aujourd'hui :\n\n"
                + "👨‍⚕️ Médecin : " + medecin + "\n"
                + "📅 Date : " + dateFormatee + "\n"
                + "🕐 Heure : " + heureRdv + "\n"
                + "📋 Motif : " + (motif != null ? motif : "Consultation") + "\n\n"
                + "Merci de vous présenter 10 min en avance.\n"
                + "VitalTech - Votre santé, notre priorité.";

        return envoyerSms(PATIENT_PHONE, message);
    }

    /**
     * Envoie un SMS via l'API REST Twilio.
     */
    private boolean envoyerSms(String to, String body) {
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + TWILIO_SID + "/Messages.json";

            // ── Form URL-encoded body ──
            String formData = "To=" + URLEncoder.encode(to, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(TWILIO_FROM, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(body, StandardCharsets.UTF_8);

            // ── Basic Auth ──
            String auth = Base64.getEncoder().encodeToString(
                    (TWILIO_SID + ":" + TWILIO_TOKEN).getBytes(StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                System.out.println("✅ SMS envoyé avec succès à " + to);
                return true;
            } else {
                System.err.println("❌ Erreur Twilio (HTTP " + response.statusCode() + ") : " + response.body());
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi SMS : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}