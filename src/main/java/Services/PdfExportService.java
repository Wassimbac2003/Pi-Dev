package Services;

import Models.rdv;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExportService {

    // Couleurs
    private static final BaseColor BLUE_PRIMARY = new BaseColor(29, 78, 216);
    private static final BaseColor BLUE_LIGHT = new BaseColor(219, 234, 254);
    private static final BaseColor BLUE_ACCENT = new BaseColor(96, 165, 250);
    private static final BaseColor GREEN_BG = new BaseColor(220, 252, 231);
    private static final BaseColor GREEN_TEXT = new BaseColor(21, 128, 61);
    private static final BaseColor RED_BG = new BaseColor(254, 226, 226);
    private static final BaseColor RED_TEXT = new BaseColor(185, 28, 28);
    private static final BaseColor GRAY_BG = new BaseColor(241, 245, 249);
    private static final BaseColor GRAY_TEXT = new BaseColor(100, 116, 139);
    private static final BaseColor TEXT_DARK = new BaseColor(30, 41, 59);
    private static final BaseColor TEXT_MID = new BaseColor(51, 65, 85);
    private static final BaseColor BORDER_LIGHT = new BaseColor(226, 232, 240);
    private static final BaseColor ROW_ALT = new BaseColor(248, 250, 252);

    // Fonts
    private Font fontTitle;
    private Font fontSubtitle;
    private Font fontNormal;
    private Font fontBold;
    private Font fontSmall;
    private Font fontSmallBold;
    private Font fontWhite;
    private Font fontWhiteSmall;
    private Font fontWhiteBold;
    private Font fontStatNum;
    private Font fontStatLabel;
    private Font fontTableHeader;
    private Font fontTableCell;
    private Font fontTableCellBold;

    public PdfExportService() {
        fontTitle = new Font(Font.FontFamily.HELVETICA, 17, Font.BOLD, BaseColor.WHITE);
        fontSubtitle = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(255, 255, 255, 180));
        fontNormal = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, TEXT_MID);
        fontBold = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, TEXT_DARK);
        fontSmall = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, GRAY_TEXT);
        fontSmallBold = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BLUE_PRIMARY);
        fontWhite = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.WHITE);
        fontWhiteSmall = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(255, 255, 255, 200));
        fontWhiteBold = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, BaseColor.WHITE);
        fontStatNum = new Font(Font.FontFamily.HELVETICA, 26, Font.BOLD);
        fontStatLabel = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, GRAY_TEXT);
        fontTableHeader = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
        fontTableCell = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, TEXT_MID);
        fontTableCellBold = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, TEXT_DARK);
    }

    public String genererPdf(List<rdv> rdvs, String cheminFichier) throws Exception {
        Document document = new Document(PageSize.A4, 0, 0, 0, 0);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        document.open();

        String dateNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String ref = "BILAN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        // Stats
        int total = rdvs.size();
        int confirmes = (int) rdvs.stream().filter(r -> r.getStatut().equalsIgnoreCase("Confirmé")).count();
        int annules = (int) rdvs.stream().filter(r -> r.getStatut().equalsIgnoreCase("Annulé")).count();
        int expires = total - confirmes - annules;

        // ===== HEADER =====
        buildHeader(document, dateNow, ref);

        // ===== STATS =====
        document.add(new Paragraph(" ")); // spacing
        buildStats(document, total, confirmes, annules, expires);

        // ===== SECTION TITLE =====
        document.add(new Paragraph(" "));
        Paragraph sectionTitle = new Paragraph("DETAIL DES RENDEZ-VOUS", fontSmallBold);
        sectionTitle.setIndentationLeft(40);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        // Ligne bleue sous le titre
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(85);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setFixedHeight(2);
        lineCell.setBackgroundColor(BLUE_LIGHT);
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineTable.addCell(lineCell);
        document.add(lineTable);
        document.add(new Paragraph(" "));

        // ===== TABLE =====
        buildTable(document, rdvs);

        // ===== NOTE =====
        document.add(new Paragraph(" "));
        buildNote(document);

        // ===== FOOTER =====
        buildFooter(document, ref);

        document.close();
        return cheminFichier;
    }

    // ======================== HEADER ========================
    private void buildHeader(Document document, String dateNow, String ref) throws DocumentException {
        // Header background
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});

        // Left: Logo + Title
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setBackgroundColor(BLUE_PRIMARY);
        leftCell.setPadding(25);
        leftCell.setPaddingBottom(10);

        Paragraph logo = new Paragraph();
        logo.add(new Chunk("Vital", fontWhiteBold));
        Chunk techChunk = new Chunk("Tech", new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, BLUE_ACCENT));
        logo.add(techChunk);
        leftCell.addElement(logo);

        Paragraph tagline = new Paragraph("Plateforme de Gestion Médicale", fontWhiteSmall);
        tagline.setSpacingAfter(15);
        leftCell.addElement(tagline);

        Paragraph title = new Paragraph("Bilan de Suivi Médical", fontTitle);
        leftCell.addElement(title);

        Paragraph subtitle = new Paragraph("Récapitulatif complet de vos rendez-vous passés", fontSubtitle);
        subtitle.setSpacingAfter(5);
        leftCell.addElement(subtitle);

        headerTable.addCell(leftCell);

        // Right: Date + Ref
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setBackgroundColor(BLUE_PRIMARY);
        rightCell.setPadding(25);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setVerticalAlignment(Element.ALIGN_TOP);

        Paragraph dateP = new Paragraph("Généré le : " + dateNow, fontWhiteSmall);
        dateP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(dateP);

        Paragraph refP = new Paragraph("Réf : " + ref, fontWhiteSmall);
        refP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(refP);

        Paragraph confP = new Paragraph("Document confidentiel", fontWhiteSmall);
        confP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(confP);

        headerTable.addCell(rightCell);
        document.add(headerTable);

        // Accent line
        PdfPTable accent = new PdfPTable(2);
        accent.setWidthPercentage(100);
        accent.setWidths(new float[]{50, 50});

        PdfPCell a1 = new PdfPCell();
        a1.setFixedHeight(4);
        a1.setBackgroundColor(BLUE_ACCENT);
        a1.setBorder(Rectangle.NO_BORDER);
        accent.addCell(a1);

        PdfPCell a2 = new PdfPCell();
        a2.setFixedHeight(4);
        a2.setBackgroundColor(new BaseColor(129, 140, 248));
        a2.setBorder(Rectangle.NO_BORDER);
        accent.addCell(a2);

        document.add(accent);
    }

    // ======================== STATS ========================
    private void buildStats(Document document, int total, int confirmes, int annules, int expires) throws DocumentException {
        PdfPTable stats = new PdfPTable(4);
        stats.setWidthPercentage(85);
        stats.setSpacingBefore(10);
        stats.setSpacingAfter(10);

        addStatCell(stats, String.valueOf(total), "TOTAL RDV", BLUE_PRIMARY, new BaseColor(191, 219, 254));
        addStatCell(stats, String.valueOf(confirmes), "CONFIRMÉS", GREEN_TEXT, new BaseColor(187, 247, 208));
        addStatCell(stats, String.valueOf(annules), "ANNULÉS", RED_TEXT, new BaseColor(254, 202, 202));
        addStatCell(stats, String.valueOf(expires), "EXPIRÉS", GRAY_TEXT, new BaseColor(203, 213, 225));

        document.add(stats);
    }

    private void addStatCell(PdfPTable table, String value, String label, BaseColor numColor, BaseColor borderColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(borderColor);
        cell.setBorderWidth(1);
        cell.setBackgroundColor(new BaseColor(248, 250, 252));
        cell.setPadding(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font numFont = new Font(Font.FontFamily.HELVETICA, 26, Font.BOLD, numColor);
        Paragraph num = new Paragraph(value, numFont);
        num.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(num);

        Paragraph lbl = new Paragraph(label, fontStatLabel);
        lbl.setAlignment(Element.ALIGN_CENTER);
        lbl.setSpacingBefore(4);
        cell.addElement(lbl);

        table.addCell(cell);
    }

    // ======================== TABLE ========================
    private void buildTable(Document document, List<rdv> rdvs) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(85);
        table.setWidths(new float[]{16, 26, 18, 25, 15});

        // Header
        String[] headers = {"DATE", "MÉDECIN", "HEURE", "MOTIF", "STATUT"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontTableHeader));
            cell.setBackgroundColor(BLUE_PRIMARY);
            cell.setPadding(10);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        // Rows
        int row = 0;
        for (rdv r : rdvs) {
            BaseColor bgColor = (row % 2 == 1) ? ROW_ALT : BaseColor.WHITE;

            // Date
            String dateStr = r.getDate();
            try {
                LocalDate d = LocalDate.parse(r.getDate());
                dateStr = d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception ignored) {}
            addTableCell(table, dateStr, fontTableCellBold, bgColor);

            // Médecin
            addTableCell(table, r.getMedecin(), fontTableCellBold, bgColor);

            // Heure
            String hDebut = r.getHdebut().length() > 5 ? r.getHdebut().substring(0, 5) : r.getHdebut();
            String hFin = r.getHfin().length() > 5 ? r.getHfin().substring(0, 5) : r.getHfin();
            addTableCell(table, hDebut + " - " + hFin, fontTableCell, bgColor);

            // Motif
            String motif = (r.getMotif() != null && !r.getMotif().isEmpty()) ? r.getMotif() : "Consultation";
            addTableCell(table, motif, fontTableCell, bgColor);

            // Statut (badge)
            addStatutBadge(table, r.getStatut(), bgColor);

            row++;
        }

        document.add(table);
    }

    private void addTableCell(PdfPTable table, String text, Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(10);
        cell.setBorderColor(new BaseColor(241, 245, 249));
        cell.setBorderWidth(0.5f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addStatutBadge(PdfPTable table, String statut, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bgColor);
        cell.setPadding(10);
        cell.setBorderColor(new BaseColor(241, 245, 249));
        cell.setBorderWidth(0.5f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        String label;
        BaseColor badgeBg, badgeText;

        if (statut.equalsIgnoreCase("Confirmé")) {
            label = "Confirmé";
            badgeBg = GREEN_BG;
            badgeText = GREEN_TEXT;
        } else if (statut.equalsIgnoreCase("Annulé")) {
            label = "Annulé";
            badgeBg = RED_BG;
            badgeText = RED_TEXT;
        } else {
            label = "Expiré";
            badgeBg = GRAY_BG;
            badgeText = GRAY_TEXT;
        }

        Font badgeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, badgeText);
        Chunk chunk = new Chunk(" " + label + " ", badgeFont);
        chunk.setBackground(badgeBg, 4, 2, 4, 2);

        cell.addElement(new Phrase(chunk));
        table.addCell(cell);
    }

    // ======================== NOTE ========================
    private void buildNote(Document document) throws DocumentException {
        PdfPTable noteTable = new PdfPTable(1);
        noteTable.setWidthPercentage(85);

        PdfPCell noteCell = new PdfPCell();
        noteCell.setBackgroundColor(new BaseColor(240, 249, 255));
        noteCell.setBorderColor(new BaseColor(14, 165, 233));
        noteCell.setBorderWidth(0);
        noteCell.setBorderWidthLeft(4);
        noteCell.setBorderColorLeft(new BaseColor(14, 165, 233));
        noteCell.setPadding(12);

        Font noteFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(3, 105, 161));
        Paragraph noteText = new Paragraph(
                "Ce document est généré automatiquement par le système VitalTech. " +
                        "Il constitue un récapitulatif informatif de vos rendez-vous médicaux " +
                        "et ne remplace pas un document médical officiel.", noteFont);
        noteCell.addElement(noteText);

        noteTable.addCell(noteCell);
        document.add(noteTable);
    }

    // ======================== FOOTER ========================
    private void buildFooter(Document document, String ref) throws DocumentException {
        document.add(new Paragraph(" "));

        PdfPTable footer = new PdfPTable(3);
        footer.setWidthPercentage(85);
        footer.setWidths(new float[]{33, 34, 33});

        Font footerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(148, 163, 184));

        PdfPCell left = new PdfPCell(new Phrase("VitalTech — Gestion Médicale © 2026", footerFont));
        left.setBorder(Rectangle.TOP);
        left.setBorderColor(BORDER_LIGHT);
        left.setPaddingTop(10);
        left.setHorizontalAlignment(Element.ALIGN_LEFT);
        footer.addCell(left);

        PdfPCell mid = new PdfPCell(new Phrase("Usage personnel uniquement", footerFont));
        mid.setBorder(Rectangle.TOP);
        mid.setBorderColor(BORDER_LIGHT);
        mid.setPaddingTop(10);
        mid.setHorizontalAlignment(Element.ALIGN_CENTER);
        footer.addCell(mid);

        PdfPCell right = new PdfPCell(new Phrase("Réf : " + ref, footerFont));
        right.setBorder(Rectangle.TOP);
        right.setBorderColor(BORDER_LIGHT);
        right.setPaddingTop(10);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        footer.addCell(right);

        document.add(footer);
    }
}