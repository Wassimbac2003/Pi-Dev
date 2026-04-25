package Services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Génère un reçu de paiement PDF — même design que le reçu Symfony
 */
public class RecuPdfService {

    // Couleurs
    private static final BaseColor BLUE_PRIMARY = new BaseColor(29, 78, 216);
    private static final BaseColor BLUE_ACCENT = new BaseColor(96, 165, 250);
    private static final BaseColor PURPLE = new BaseColor(79, 70, 229);
    private static final BaseColor GREEN_BG = new BaseColor(220, 252, 231);
    private static final BaseColor GREEN_TEXT = new BaseColor(21, 128, 61);
    private static final BaseColor GREEN_BORDER = new BaseColor(187, 247, 208);
    private static final BaseColor TEXT_DARK = new BaseColor(30, 41, 59);
    private static final BaseColor TEXT_MID = new BaseColor(51, 65, 85);
    private static final BaseColor GRAY_TEXT = new BaseColor(100, 116, 139);
    private static final BaseColor BORDER_LIGHT = new BaseColor(226, 232, 240);
    private static final BaseColor ROW_BG = new BaseColor(241, 245, 249);
    private static final BaseColor NOTE_BG = new BaseColor(240, 249, 255);
    private static final BaseColor NOTE_BORDER = new BaseColor(14, 165, 233);
    private static final BaseColor NOTE_TEXT = new BaseColor(3, 105, 161);

    /**
     * Générer le reçu PDF
     *
     * @param cheminFichier  Chemin du fichier PDF à créer
     * @param nomMedecin     Nom du médecin (ex: "Dr. Sarah Amrani")
     * @param dateRdv        Date du RDV (ex: "22/04/2026")
     * @param heureDebut     Heure début (ex: "09:30")
     * @param heureFin       Heure fin (ex: "10:00")
     * @param motif          Motif de consultation
     * @param stripePaymentId ID de transaction Stripe (ex: "pi_3TOP...")
     * @param rdvId          ID du RDV en base
     */
    public String genererRecu(String cheminFichier, String nomMedecin, String dateRdv,
                              String heureDebut, String heureFin, String motif,
                              String stripePaymentId, int rdvId) throws Exception {

        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        document.open();

        String dateNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy a HH:mm"));
        String ref = "RECU-" + rdvId + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        // ===== HEADER =====
        buildHeader(document, dateNow, ref);

        // ===== BADGE SUCCÈS =====
        document.add(Chunk.NEWLINE);
        buildSuccessBadge(document);

        // ===== MONTANT =====
        document.add(Chunk.NEWLINE);
        buildAmountBox(document);

        // ===== DÉTAILS DU RDV =====
        document.add(Chunk.NEWLINE);
        buildDetailsSection(document, nomMedecin, dateRdv, heureDebut, heureFin, motif);

        // ===== INFOS PAIEMENT =====
        document.add(Chunk.NEWLINE);
        buildPaymentSection(document, stripePaymentId, dateNow);

        // ===== NOTE =====
        document.add(Chunk.NEWLINE);
        buildNote(document);

        // ===== FOOTER =====
        document.add(Chunk.NEWLINE);
        buildFooter(document, ref);

        document.close();
        return cheminFichier;
    }

    // ======================== HEADER ========================
    private void buildHeader(Document document, String dateNow, String ref) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});

        // Gauche : Logo + Titre
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setBackgroundColor(BLUE_PRIMARY);
        leftCell.setPadding(25);
        leftCell.setPaddingBottom(15);

        Font logoFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, BaseColor.WHITE);
        Font logoAccent = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, BLUE_ACCENT);
        Paragraph logo = new Paragraph();
        logo.add(new Chunk("Vital", logoFont));
        logo.add(new Chunk("Tech", logoAccent));
        leftCell.addElement(logo);

        Font taglineFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(255, 255, 255, 170));
        Paragraph tagline = new Paragraph("Plateforme de Gestion Medicale", taglineFont);
        tagline.setSpacingAfter(15);
        leftCell.addElement(tagline);

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.WHITE);
        leftCell.addElement(new Paragraph("Recu de Paiement — Consultation Medicale", titleFont));

        headerTable.addCell(leftCell);

        // Droite : Date + Réf
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setBackgroundColor(BLUE_PRIMARY);
        rightCell.setPadding(25);
        rightCell.setVerticalAlignment(Element.ALIGN_TOP);

        Font infoFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(255, 255, 255, 200));
        Paragraph dateP = new Paragraph("Emis le : " + dateNow, infoFont);
        dateP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(dateP);

        Paragraph refP = new Paragraph("Ref : " + ref, infoFont);
        refP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(refP);

        Paragraph confP = new Paragraph("Document officiel", infoFont);
        confP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(confP);

        headerTable.addCell(rightCell);
        document.add(headerTable);

        // Accent line
        PdfPTable accent = new PdfPTable(2);
        accent.setWidthPercentage(100);
        accent.setWidths(new float[]{50, 50});
        PdfPCell a1 = new PdfPCell(); a1.setFixedHeight(4); a1.setBackgroundColor(BLUE_ACCENT); a1.setBorder(Rectangle.NO_BORDER);
        PdfPCell a2 = new PdfPCell(); a2.setFixedHeight(4); a2.setBackgroundColor(new BaseColor(129, 140, 248)); a2.setBorder(Rectangle.NO_BORDER);
        accent.addCell(a1); accent.addCell(a2);
        document.add(accent);
    }

    // ======================== BADGE SUCCÈS ========================
    private void buildSuccessBadge(Document document) throws DocumentException {
        Paragraph badge = new Paragraph();
        badge.setAlignment(Element.ALIGN_CENTER);

        Font badgeFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, GREEN_TEXT);
        Chunk chunk = new Chunk("  Paiement confirme avec succes  ", badgeFont);
        chunk.setBackground(GREEN_BG, 8, 4, 8, 4);
        badge.add(chunk);
        badge.setSpacingAfter(5);

        document.add(badge);
    }

    // ======================== MONTANT ========================
    private void buildAmountBox(Document document) throws DocumentException {
        PdfPTable amountTable = new PdfPTable(1);
        amountTable.setWidthPercentage(70);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(BLUE_PRIMARY);
        cell.setPadding(20);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        // Label
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(255, 255, 255, 200));
        Paragraph label = new Paragraph("MONTANT PAYE", labelFont);
        label.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(label);

        // Montant
        Font amountFont = new Font(Font.FontFamily.HELVETICA, 36, Font.BOLD, BaseColor.WHITE);
        Paragraph amount = new Paragraph("70 DT", amountFont);
        amount.setAlignment(Element.ALIGN_CENTER);
        amount.setSpacingBefore(4);
        cell.addElement(amount);

        // Méthode
        Font methodFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(255, 255, 255, 190));
        Paragraph method = new Paragraph("Carte bancaire en ligne — Stripe", methodFont);
        method.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(method);

        // Rounded corners simulation
        cell.setBorderColor(BLUE_PRIMARY);
        cell.setBorderWidth(0);
        cell.setUseAscender(true);

        amountTable.addCell(cell);
        document.add(amountTable);
    }

    // ======================== DÉTAILS DU RDV ========================
    private void buildDetailsSection(Document document, String nomMedecin, String dateRdv,
                                     String heureDebut, String heureFin, String motif) throws DocumentException {
        // Section title
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BLUE_PRIMARY);
        Paragraph sectionTitle = new Paragraph("DETAILS DU RENDEZ-VOUS", sectionFont);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        // Ligne séparatrice
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell(); lineCell.setFixedHeight(1); lineCell.setBackgroundColor(new BaseColor(219, 234, 254)); lineCell.setBorder(Rectangle.NO_BORDER);
        line.addCell(lineCell);
        document.add(line);

        // Tableau détails
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{40, 60});
        table.setSpacingBefore(8);

        addDetailRow(table, "Medecin", nomMedecin, true);
        addDetailRow(table, "Date", dateRdv, false);
        addDetailRow(table, "Horaire", heureDebut + " — " + heureFin, true);
        addDetailRow(table, "Motif", motif != null && !motif.isEmpty() ? motif : "Consultation", false);
        addDetailRow(table, "Statut RDV", "Confirme", true);

        document.add(table);
    }

    // ======================== INFOS PAIEMENT ========================
    private void buildPaymentSection(Document document, String stripePaymentId, String dateNow) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BLUE_PRIMARY);
        Paragraph sectionTitle = new Paragraph("INFORMATIONS DE PAIEMENT", sectionFont);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell(); lineCell.setFixedHeight(1); lineCell.setBackgroundColor(new BaseColor(219, 234, 254)); lineCell.setBorder(Rectangle.NO_BORDER);
        line.addCell(lineCell);
        document.add(line);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{40, 60});
        table.setSpacingBefore(8);

        addDetailRow(table, "ID Transaction Stripe", stripePaymentId != null ? stripePaymentId : "—", true);
        addDetailRow(table, "Methode", "Carte bancaire (Stripe)", false);
        addDetailRow(table, "Montant", "70 DT", true);
        addDetailRow(table, "Date de paiement", dateNow, false);

        document.add(table);
    }

    // ======================== NOTE ========================
    private void buildNote(Document document) throws DocumentException {
        PdfPTable noteTable = new PdfPTable(1);
        noteTable.setWidthPercentage(100);

        PdfPCell noteCell = new PdfPCell();
        noteCell.setBackgroundColor(NOTE_BG);
        noteCell.setBorderWidth(0);
        noteCell.setBorderWidthLeft(4);
        noteCell.setBorderColorLeft(NOTE_BORDER);
        noteCell.setPadding(12);

        Font noteFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, NOTE_TEXT);
        noteCell.addElement(new Paragraph(
                "Ce recu confirme le paiement de votre consultation medicale. " +
                        "Conservez ce document comme justificatif. Pour toute question, " +
                        "contactez VitalTech a support@vitaltech.tn", noteFont));

        noteTable.addCell(noteCell);
        document.add(noteTable);
    }

    // ======================== FOOTER ========================
    private void buildFooter(Document document, String ref) throws DocumentException {
        PdfPTable footer = new PdfPTable(3);
        footer.setWidthPercentage(100);
        footer.setWidths(new float[]{33, 34, 33});

        Font footerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(148, 163, 184));

        PdfPCell left = new PdfPCell(new Phrase("VitalTech — Gestion Medicale 2026", footerFont));
        left.setBorder(Rectangle.TOP); left.setBorderColor(BORDER_LIGHT); left.setPaddingTop(10);
        left.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell mid = new PdfPCell(new Phrase("Document genere automatiquement", footerFont));
        mid.setBorder(Rectangle.TOP); mid.setBorderColor(BORDER_LIGHT); mid.setPaddingTop(10);
        mid.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell right = new PdfPCell(new Phrase("Ref : " + ref, footerFont));
        right.setBorder(Rectangle.TOP); right.setBorderColor(BORDER_LIGHT); right.setPaddingTop(10);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);

        footer.addCell(left); footer.addCell(mid); footer.addCell(right);
        document.add(footer);
    }

    // ======================== HELPER ========================
    private void addDetailRow(PdfPTable table, String label, String value, boolean altBg) {
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, GRAY_TEXT);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, TEXT_DARK);

        // Statut spécial en vert
        if ("Confirme".equals(value)) {
            valueFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, GREEN_TEXT);
            value = "Confirme";
        }

        // ID Stripe en monospace
        if (label.contains("Transaction")) {
            valueFont = new Font(Font.FontFamily.COURIER, 10, Font.NORMAL, GRAY_TEXT);
        }

        BaseColor bg = altBg ? ROW_BG : BaseColor.WHITE;

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(bg);
        labelCell.setPadding(10);
        labelCell.setBorderColor(ROW_BG);
        labelCell.setBorderWidth(0.5f);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBackgroundColor(bg);
        valueCell.setPadding(10);
        valueCell.setBorderColor(ROW_BG);
        valueCell.setBorderWidth(0.5f);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}