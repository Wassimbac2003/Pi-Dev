package Services;

import Models.rdv;

import javax.mail.*;
import javax.mail.internet.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

/**
 * Service d'envoi d'emails pour les notifications RDV
 * Utilise JavaMail API avec Gmail SMTP
 */
public class EmailService {

    // ── Configuration Gmail SMTP ──
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "vitaltech.noreply@gmail.com";
    private static final String EMAIL_PASSWORD = "yvduocsjngzajord";  // App Password sans espaces

    // ── Informations cabinet médical ──
    private static final String CABINET_NOM = "VitalTech Medical Center";
    private static final String CABINET_TEL = "+216 29 254 485";
    private static final String CABINET_ADRESSE = "15 Avenue Habib Bourguiba, Tunis 1000";

    /**
     * Envoyer email de CONFIRMATION de RDV
     */
    public boolean envoyerEmailConfirmation(rdv rdv, String emailPatient) {
        String sujet = "✅ Confirmation de votre rendez-vous - " + CABINET_NOM;
        String corps = genererTemplateConfirmation(rdv);
        return envoyerEmail(emailPatient, sujet, corps);
    }

    /**
     * Envoyer email de MODIFICATION de RDV
     */
    public boolean envoyerEmailModification(rdv rdv, String emailPatient, String ancienneDate, String ancienneHeure) {
        String sujet = "✏ Modification de votre rendez-vous - " + CABINET_NOM;
        String corps = genererTemplateModification(rdv, ancienneDate, ancienneHeure);
        return envoyerEmail(emailPatient, sujet, corps);
    }

    /**
     * Envoyer email d'ANNULATION de RDV
     */
    public boolean envoyerEmailAnnulation(rdv rdv, String emailPatient) {
        String sujet = "❌ Annulation de votre rendez-vous - " + CABINET_NOM;
        String corps = genererTemplateAnnulation(rdv);
        return envoyerEmail(emailPatient, sujet, corps);
    }

    // ═════════════════════════════════════════════════════════════
    //  MÉTHODE GÉNÉRIQUE D'ENVOI
    // ═════════════════════════════════════════════════════════════

    private boolean envoyerEmail(String destinataire, String sujet, String corpsHTML) {
        try {
            // ── Configuration SMTP ──
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            // ── Session avec authentification ──
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                }
            });

            // ── Construction du message ──
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, CABINET_NOM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject(sujet);
            message.setContent(corpsHTML, "text/html; charset=UTF-8");

            // ── Envoi ──
            Transport.send(message);

            System.out.println("✅ Email envoyé avec succès à " + destinataire);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi d'email : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  TEMPLATES HTML
    // ═════════════════════════════════════════════════════════════

    /**
     * Template email CONFIRMATION
     */
    private String genererTemplateConfirmation(rdv rdv) {
        String dateFormatee = formaterDate(rdv.getDate());
        String heureDebut = rdv.getHdebut() != null ? rdv.getHdebut().substring(0, 5) : "N/A";

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f0f4f8; margin: 0; padding: 20px;'>" +

                "  <div style='max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>" +

                "    <!-- Header -->" +
                "    <div style='background: linear-gradient(135deg, #16a34a 0%, #22c55e 100%); padding: 30px; text-align: center;'>" +
                "      <h1 style='color: white; margin: 0; font-size: 24px;'>✅ Rendez-vous Confirmé</h1>" +
                "    </div>" +

                "    <!-- Corps -->" +
                "    <div style='padding: 30px;'>" +
                "      <p style='font-size: 16px; color: #333; margin-bottom: 20px;'>Bonjour,</p>" +
                "      <p style='font-size: 14px; color: #555; line-height: 1.6;'>Votre rendez-vous a été <strong style='color: #16a34a;'>confirmé avec succès</strong>. Voici les détails :</p>" +

                "      <!-- Carte info RDV -->" +
                "      <div style='background: #f8fafc; border-left: 4px solid #16a34a; padding: 20px; margin: 20px 0; border-radius: 8px;'>" +
                "        <table style='width: 100%; border-collapse: collapse;'>" +
                "          <tr><td style='padding: 8px 0; color: #64748b; font-size: 13px;'>👨‍⚕️ <strong>Médecin</strong></td><td style='padding: 8px 0; color: #1e293b; font-size: 14px; font-weight: bold;'>" + rdv.getMedecin() + "</td></tr>" +
                "          <tr><td style='padding: 8px 0; color: #64748b; font-size: 13px;'>📅 <strong>Date</strong></td><td style='padding: 8px 0; color: #1e293b; font-size: 14px; font-weight: bold;'>" + dateFormatee + "</td></tr>" +
                "          <tr><td style='padding: 8px 0; color: #64748b; font-size: 13px;'>🕐 <strong>Heure</strong></td><td style='padding: 8px 0; color: #1e293b; font-size: 14px; font-weight: bold;'>" + heureDebut + "</td></tr>" +
                "          <tr><td style='padding: 8px 0; color: #64748b; font-size: 13px;'>📋 <strong>Motif</strong></td><td style='padding: 8px 0; color: #1e293b; font-size: 14px;'>" + rdv.getMotif() + "</td></tr>" +
                "        </table>" +
                "      </div>" +

                "      <!-- Instructions -->" +
                "      <div style='background: #fef3c7; border-radius: 8px; padding: 15px; margin: 20px 0;'>" +
                "        <p style='margin: 0; font-size: 13px; color: #92400e;'><strong>⚠️ Important :</strong> Merci d'arriver 10 minutes avant l'heure du rendez-vous.</p>" +
                "      </div>" +

                "      <p style='font-size: 14px; color: #555; margin-top: 25px;'>En cas d'empêchement, veuillez nous contacter au moins 24h à l'avance.</p>" +
                "      <p style='font-size: 14px; color: #555;'>Cordialement,<br><strong>" + CABINET_NOM + "</strong></p>" +
                "    </div>" +

                "    <!-- Footer -->" +
                "    <div style='background: #f8fafc; padding: 20px; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "      <p style='margin: 5px 0; font-size: 12px; color: #94a3b8;'>📍 " + CABINET_ADRESSE + "</p>" +
                "      <p style='margin: 5px 0; font-size: 12px; color: #94a3b8;'>📞 " + CABINET_TEL + "</p>" +
                "    </div>" +

                "  </div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Template email MODIFICATION
     */
    private String genererTemplateModification(rdv rdv, String ancienneDate, String ancienneHeure) {
        String nouvelleDateFormatee = formaterDate(rdv.getDate());
        String nouvelleHeure = rdv.getHdebut() != null ? rdv.getHdebut().substring(0, 5) : "N/A";
        String ancienneDateFormatee = formaterDate(ancienneDate);

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f0f4f8; margin: 0; padding: 20px;'>" +

                "  <div style='max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>" +

                "    <!-- Header -->" +
                "    <div style='background: linear-gradient(135deg, #f97316 0%, #fb923c 100%); padding: 30px; text-align: center;'>" +
                "      <h1 style='color: white; margin: 0; font-size: 24px;'>✏ Rendez-vous Modifié</h1>" +
                "    </div>" +

                "    <!-- Corps -->" +
                "    <div style='padding: 30px;'>" +
                "      <p style='font-size: 16px; color: #333; margin-bottom: 20px;'>Bonjour,</p>" +
                "      <p style='font-size: 14px; color: #555; line-height: 1.6;'>Votre rendez-vous a été <strong style='color: #f97316;'>modifié</strong>. Voici les nouvelles informations :</p>" +

                "      <!-- Ancien RDV (barré) -->" +
                "      <div style='background: #fef2f2; border-left: 4px solid #dc2626; padding: 20px; margin: 20px 0; border-radius: 8px; opacity: 0.7;'>" +
                "        <p style='margin: 0 0 10px 0; font-size: 13px; color: #991b1b; font-weight: bold; text-decoration: line-through;'>ANCIEN RENDEZ-VOUS</p>" +
                "        <table style='width: 100%; border-collapse: collapse;'>" +
                "          <tr><td style='padding: 5px 0; color: #64748b; font-size: 13px;'>📅 Date</td><td style='padding: 5px 0; color: #1e293b; font-size: 14px; text-decoration: line-through;'>" + ancienneDateFormatee + "</td></tr>" +
                "          <tr><td style='padding: 5px 0; color: #64748b; font-size: 13px;'>🕐 Heure</td><td style='padding: 5px 0; color: #1e293b; font-size: 14px; text-decoration: line-through;'>" + ancienneHeure + "</td></tr>" +
                "        </table>" +
                "      </div>" +

                "      <!-- Nouveau RDV -->" +
                "      <div style='background: #f0fdf4; border-left: 4px solid #16a34a; padding: 20px; margin: 20px 0; border-radius: 8px;'>" +
                "        <p style='margin: 0 0 10px 0; font-size: 13px; color: #166534; font-weight: bold;'>✅ NOUVEAU RENDEZ-VOUS</p>" +
                "        <table style='width: 100%; border-collapse: collapse;'>" +
                "          <tr><td style='padding: 8px 0; color: #64748b; font-size: 13px;'>👨‍⚕️ <strong>Médecin</strong></td><td style='padding: 8px 0; color: #1e293b; font-size: 14px; font-weight: bold;'>" + rdv.getMedecin() + "</td></tr>" +
                "          <tr><td style='padding: 8px 0; color: #64748b; font-size: 13px;'>📅 <strong>Date</strong></td><td style='padding: 8px 0; color: #16a34a; font-size: 14px; font-weight: bold;'>" + nouvelleDateFormatee + "</td></tr>" +
                "          <tr><td style='padding: 8px 0; color: #64748b; font-size: 13px;'>🕐 <strong>Heure</strong></td><td style='padding: 8px 0; color: #16a34a; font-size: 14px; font-weight: bold;'>" + nouvelleHeure + "</td></tr>" +
                "          <tr><td style='padding: 8px 0; color: #64748b; font-size: 13px;'>📋 <strong>Motif</strong></td><td style='padding: 8px 0; color: #1e293b; font-size: 14px;'>" + rdv.getMotif() + "</td></tr>" +
                "        </table>" +
                "      </div>" +

                "      <p style='font-size: 14px; color: #555; margin-top: 25px;'>Merci de bien noter ces nouvelles informations.</p>" +
                "      <p style='font-size: 14px; color: #555;'>Cordialement,<br><strong>" + CABINET_NOM + "</strong></p>" +
                "    </div>" +

                "    <!-- Footer -->" +
                "    <div style='background: #f8fafc; padding: 20px; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "      <p style='margin: 5px 0; font-size: 12px; color: #94a3b8;'>📍 " + CABINET_ADRESSE + "</p>" +
                "      <p style='margin: 5px 0; font-size: 12px; color: #94a3b8;'>📞 " + CABINET_TEL + "</p>" +
                "    </div>" +

                "  </div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Template email ANNULATION
     */
    private String genererTemplateAnnulation(rdv rdv) {
        String dateFormatee = formaterDate(rdv.getDate());
        String heureDebut = rdv.getHdebut() != null ? rdv.getHdebut().substring(0, 5) : "N/A";

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f0f4f8; margin: 0; padding: 20px;'>" +

                "  <div style='max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>" +

                "    <!-- Header -->" +
                "    <div style='background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%); padding: 30px; text-align: center;'>" +
                "      <h1 style='color: white; margin: 0; font-size: 24px;'>❌ Rendez-vous Annulé</h1>" +
                "    </div>" +

                "    <!-- Corps -->" +
                "    <div style='padding: 30px;'>" +
                "      <p style='font-size: 16px; color: #333; margin-bottom: 20px;'>Bonjour,</p>" +
                "      <p style='font-size: 14px; color: #555; line-height: 1.6;'>Nous vous informons que votre rendez-vous a été <strong style='color: #dc2626;'>annulé</strong>.</p>" +

                "      <!-- Info RDV annulé -->" +
                "      <div style='background: #fef2f2; border-left: 4px solid #dc2626; padding: 20px; margin: 20px 0; border-radius: 8px;'>" +
                "        <p style='margin: 0 0 10px 0; font-size: 13px; color: #991b1b; font-weight: bold;'>RENDEZ-VOUS ANNULÉ</p>" +
                "        <table style='width: 100%; border-collapse: collapse;'>" +
                "          <tr><td style='padding: 8px 0; color: #64748b; font-size: 13px;'>👨‍⚕️ <strong>Médecin</strong></td><td style='padding: 8px 0; color: #1e293b; font-size: 14px;'>" + rdv.getMedecin() + "</td></tr>" +
                "          <tr><td style='padding: 8px 0; color: #64748b; font-size: 13px;'>📅 <strong>Date</strong></td><td style='padding: 8px 0; color: #1e293b; font-size: 14px;'>" + dateFormatee + "</td></tr>" +
                "          <tr><td style='padding: 8px 0; color: #64748b; font-size: 13px;'>🕐 <strong>Heure</strong></td><td style='padding: 8px 0; color: #1e293b; font-size: 14px;'>" + heureDebut + "</td></tr>" +
                "          <tr><td style='padding: 8px 0; color: #64748b; font-size: 13px;'>📋 <strong>Motif</strong></td><td style='padding: 8px 0; color: #1e293b; font-size: 14px;'>" + rdv.getMotif() + "</td></tr>" +
                "        </table>" +
                "      </div>" +

                "      <p style='font-size: 14px; color: #555; margin-top: 25px;'>Si vous souhaitez reprendre rendez-vous, n'hésitez pas à nous contacter.</p>" +
                "      <p style='font-size: 14px; color: #555;'>Cordialement,<br><strong>" + CABINET_NOM + "</strong></p>" +
                "    </div>" +

                "    <!-- Footer -->" +
                "    <div style='background: #f8fafc; padding: 20px; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "      <p style='margin: 5px 0; font-size: 12px; color: #94a3b8;'>📍 " + CABINET_ADRESSE + "</p>" +
                "      <p style='margin: 5px 0; font-size: 12px; color: #94a3b8;'>📞 " + CABINET_TEL + "</p>" +
                "    </div>" +

                "  </div>" +
                "</body>" +
                "</html>";
    }

    // ═════════════════════════════════════════════════════════════
    //  HELPER : FORMATER DATE
    // ═════════════════════════════════════════════════════════════

    private String formaterDate(String dateISO) {
        try {
            LocalDate date = LocalDate.parse(dateISO);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH);
            return date.format(formatter);
        } catch (Exception e) {
            return dateISO;
        }
    }
}