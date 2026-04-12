package tn.esprit.tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import tn.esprit.entities.Medicament;
import tn.esprit.entities.Ordonnance;
import tn.esprit.entities.User;
import tn.esprit.fx.UiTheme;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Génère un PDF structuré façon ordonnance médicale (A4).
 */
public final class OrdonnancePdfExporter {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRANCE);
    private static final float M = 56;
    private static final float LINE = 14;

    private OrdonnancePdfExporter() {
    }

    public static void export(java.io.File dest, Ordonnance ord, User patient, User medecin,
                              List<Medicament> medicaments, String dossierMotif) throws IOException {
        String patientLine = patient != null ? UiTheme.userDisplayName(patient) : "Patient";
        String medLine = medecin != null ? ("Dr " + UiTheme.userDisplayName(medecin)) : "Médecin prescripteur";
        String dateStr = ord.getDateOrdonnance() != null ? DT.format(ord.getDateOrdonnance()) : "—";
        List<String> medNames = new ArrayList<>();
        if (medicaments != null) {
            for (Medicament m : medicaments) {
                if (m != null && m.getNomMedicament() != null) {
                    medNames.add("• " + m.getNomMedicament()
                            + (m.getDosage() != null && !m.getDosage().isBlank() ? " — " + m.getDosage() : ""));
                }
            }
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float w = page.getMediaBox().getWidth();
            float h = page.getMediaBox().getHeight();
            float y = h - M;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setNonStrokingColor(0.06f, 0.09f, 0.16f);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(M, y);
                cs.showText("ORDONNANCE MEDICALE");
                cs.endText();
                y -= 28;

                drawRule(cs, M, y, w - 2 * M);
                y -= 20;

                y = drawLabelValue(cs, M, y, w - 2 * M, "Date de prescription", dateStr);
                y -= 4;
                y = drawLabelValue(cs, M, y, w - 2 * M, "Patient", patientLine);
                y -= 4;
                if (dossierMotif != null && !dossierMotif.isBlank()) {
                    y = drawLabelValue(cs, M, y, w - 2 * M, "Dossier / motif fiche", dossierMotif);
                    y -= 4;
                }
                y = drawLabelValue(cs, M, y, w - 2 * M, "Prescripteur", medLine);
                y -= 24;

                cs.setNonStrokingColor(0.12f, 0.21f, 0.38f);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.newLineAtOffset(M, y);
                cs.showText("Prescription");
                cs.endText();
                y -= LINE + 6;

                cs.setNonStrokingColor(0.15f, 0.2f, 0.25f);
                y = drawParagraph(cs, M, y, w - 2 * M, "Posologie : " + nullSafe(ord.getPosologie()));
                y -= 6;
                y = drawParagraph(cs, M, y, w - 2 * M,
                        "Frequence : " + nullSafe(ord.getFrequence()) + "   |   Duree du traitement : "
                                + ord.getDureeTraitement() + " jour(s)");
                y -= 16;

                if (!medNames.isEmpty()) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                    cs.newLineAtOffset(M, y);
                    cs.showText("Medicaments prescrits");
                    cs.endText();
                    y -= LINE + 4;
                    for (String line : medNames) {
                        y = drawParagraph(cs, M, y, w - 2 * M, line);
                        y -= 2;
                    }
                }

                y = Math.min(y, 200);
                y -= 40;
                drawRule(cs, M, y, w - 2 * M);
                y -= 36;
                cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                cs.newLineAtOffset(M, y);
                cs.showText("Signature et cachet du medecin");
                cs.endText();
                y -= 48;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(M, y);
                cs.showText(medLine);
                cs.endText();

                cs.setNonStrokingColor(0.45f, 0.5f, 0.55f);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                cs.newLineAtOffset(M, M);
                cs.showText("Document genere a partir du dossier patient — usage informatif.");
                cs.endText();
            }
            doc.save(dest);
        }
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s.replace('\r', ' ').replace('\n', ' ');
    }

    private static void drawRule(PDPageContentStream cs, float x, float y, float len) throws IOException {
        cs.setStrokingColor(0.75f, 0.78f, 0.82f);
        cs.setLineWidth(0.8f);
        cs.moveTo(x, y);
        cs.lineTo(x + len, y);
        cs.stroke();
    }

    private static float drawLabelValue(PDPageContentStream cs, float x, float y, float maxW, String label, String value)
            throws IOException {
        cs.setNonStrokingColor(0.35f, 0.4f, 0.45f);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitizePdf(label));
        cs.endText();
        y -= LINE;
        cs.setNonStrokingColor(0.1f, 0.12f, 0.16f);
        return drawParagraph(cs, x, y, maxW, sanitizePdf(value));
    }

    private static float drawParagraph(PDPageContentStream cs, float x, float y, float maxW, String text)
            throws IOException {
        String t = text == null ? "" : text;
        List<String> lines = wrap(t, (int) ((maxW - 4) / 5.2f));
        cs.setFont(PDType1Font.HELVETICA, 10);
        for (String line : lines) {
            if (y < M + 40) {
                break;
            }
            cs.beginText();
            cs.newLineAtOffset(x, y);
            cs.showText(line);
            cs.endText();
            y -= LINE;
        }
        return y;
    }

    /** PDFDocEncoding / WinAnsi : éviter caractères hors plage pour Helvetica. */
    private static String sanitizePdf(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 32 && c <= 126) {
                b.append(c);
            } else {
                switch (c) {
                    case 'à':
                    case 'â':
                    case 'ä':
                        b.append('a');
                        break;
                    case 'é':
                    case 'è':
                    case 'ê':
                    case 'ë':
                        b.append('e');
                        break;
                    case 'î':
                    case 'ï':
                        b.append('i');
                        break;
                    case 'ô':
                    case 'ö':
                        b.append('o');
                        break;
                    case 'ù':
                    case 'û':
                    case 'ü':
                        b.append('u');
                        break;
                    case 'ç':
                        b.append('c');
                        break;
                    case 'œ':
                        b.append("oe");
                        break;
                    case 'æ':
                        b.append("ae");
                        break;
                    case '\n':
                    case '\r':
                        b.append(' ');
                        break;
                    default:
                        if (c > 127) {
                            b.append('?');
                        } else {
                            b.append(c);
                        }
                }
            }
        }
        return b.toString();
    }

    private static List<String> wrap(String text, int maxChars) {
        List<String> out = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (line.length() + word.length() + 1 > maxChars) {
                if (!line.isEmpty()) {
                    out.add(line.toString());
                    line = new StringBuilder();
                }
                while (word.length() > maxChars) {
                    out.add(word.substring(0, maxChars));
                    word = word.substring(maxChars);
                }
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        if (out.isEmpty()) {
            out.add("");
        }
        return out;
    }
}
