package com.dgi.platform.service.impl;

import com.dgi.platform.entity.FiscalNumber;
import com.dgi.platform.entity.TaxApplication;
import com.dgi.platform.entity.User;
import com.dgi.platform.exception.ResourceNotFoundException;
import com.dgi.platform.repository.FiscalNumberRepository;
import com.dgi.platform.repository.TaxApplicationRepository;
import com.dgi.platform.repository.UserRepository;
import com.dgi.platform.service.PdfGeneratorService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfGeneratorServiceImpl implements PdfGeneratorService {

    private final UserRepository userRepository;
    private final FiscalNumberRepository fiscalNumberRepository;
    private final TaxApplicationRepository taxApplicationRepository;

    @Override
    public byte[] generateRegistrationCertificate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String fiscalStr = fiscalNumberRepository.findByUserId(userId)
                .map(FiscalNumber::getFiscalNumber)
                .orElse("EN ATTENTE D'APPROBATION");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(15, 76, 129));
            Paragraph header = new Paragraph(
                    "RÉPUBLIQUE TUNISIENNE\nMINISTÈRE DES FINANCES\nDIRECTION GÉNÉRALE DES IMPÔTS", headerFont);
            header.setAlignment(Element.ALIGN_CENTER);
            doc.add(header);
            doc.add(new Paragraph(" "));

            Paragraph title = new Paragraph("ATTESTATION D'IMMATRICULATION FISCALE",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK));
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            Font lbl = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(15, 76, 129));
            Font val = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

            addRow(table, "Matricule fiscal :",      fiscalStr, lbl, val);
            addRow(table, "Nom & Prénom :",
                    nvl(user.getFirstName()) + " " + nvl(user.getLastName()), lbl, val);
            addRow(table, "CIN :",                   nvl(user.getCin()),      lbl, val);
            addRow(table, "Passeport :",             nvl(user.getPassport()), lbl, val);
            addRow(table, "Adresse :",               nvl(user.getAddress()),  lbl, val);
            addRow(table, "Date d'émission :",
                    java.time.LocalDate.now().format(DateTimeFormatter.ISO_DATE), lbl, val);
            doc.add(table);

            doc.add(new Paragraph(" "));
            Font footer = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC, Color.GRAY);
            Paragraph fp = new Paragraph(
                    "Ce document est délivré électroniquement par la DGI.", footer);
            fp.setAlignment(Element.ALIGN_CENTER);
            doc.add(fp);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du certificat PDF", e);
        }
        return out.toByteArray();
    }

    @Override
    public byte[] generateApplicationPdf(Long applicationId) {
        TaxApplication app = taxApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("TaxApplication", "id", applicationId));
        return generateRegistrationCertificate(app.getUser().getId());
    }

    private void addRow(PdfPTable t, String label, String value, Font lbl, Font val) {
        PdfPCell lc = new PdfPCell(new Phrase(label, lbl));
        lc.setPadding(8); lc.setBorderColor(new Color(220, 220, 220));
        PdfPCell vc = new PdfPCell(new Phrase(value, val));
        vc.setPadding(8); vc.setBorderColor(new Color(220, 220, 220));
        t.addCell(lc); t.addCell(vc);
    }

    private String nvl(String s) { return s != null ? s : "N/A"; }
}
