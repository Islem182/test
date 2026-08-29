package com.dgi.platform.service.impl;

import com.dgi.platform.entity.FiscalNumber;
import com.dgi.platform.entity.TaxApplication;
import com.dgi.platform.entity.Taxpayer;
import com.dgi.platform.exception.ResourceNotFoundException;
import com.dgi.platform.repository.FiscalNumberRepository;
import com.dgi.platform.repository.TaxApplicationRepository;
import com.dgi.platform.repository.TaxpayerRepository;
import com.dgi.platform.service.PdfGeneratorService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfGeneratorServiceImpl implements PdfGeneratorService {

    private final TaxpayerRepository taxpayerRepository;
    private final FiscalNumberRepository fiscalNumberRepository;
    private final TaxApplicationRepository taxApplicationRepository;

    @Override
    public byte[] generateRegistrationCertificate(Long taxpayerId) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new ResourceNotFoundException("Taxpayer", "id", taxpayerId));

        FiscalNumber fiscalNum = fiscalNumberRepository.findByTaxpayerId(taxpayerId)
                .orElse(null);

        String fiscalStr = fiscalNum != null ? fiscalNum.getFiscalNumber() : "PENDING-APPROVAL";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Header Title
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 76, 129));
            Paragraph header = new Paragraph("RÉPUBLIQUE TUNISIENNE\nMINISTÈRE DES FINANCES\nDIRECTION GÉNÉRALE DES IMPÔTS", headerFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            document.add(new Paragraph(" "));

            // Document Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Paragraph title = new Paragraph("ATTESTATION D'IMMATRICULATION FISCALE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));

            // Table with details
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(15, 76, 129));
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

            addCell(table, "Numéro Matricule Fiscal:", labelFont);
            addCell(table, fiscalStr, valueFont);

            addCell(table, "Nom & Prénom:", labelFont);
            addCell(table, taxpayer.getFirstName() + " " + taxpayer.getLastName(), valueFont);

            addCell(table, "Carte d'Identité (CIN):", labelFont);
            addCell(table, taxpayer.getCin() != null ? taxpayer.getCin() : "N/A", valueFont);

            addCell(table, "Passport:", labelFont);
            addCell(table, taxpayer.getPassport() != null ? taxpayer.getPassport() : "N/A", valueFont);

            addCell(table, "Adresse Domiciliation:", labelFont);
            addCell(table, taxpayer.getAddress() != null ? taxpayer.getAddress() : "N/A", valueFont);

            addCell(table, "Date d'Émission:", labelFont);
            addCell(table, java.time.LocalDate.now().format(DateTimeFormatter.ISO_DATE), valueFont);

            document.add(table);

            document.add(new Paragraph(" "));
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC, Color.GRAY);
            Paragraph footer = new Paragraph("Ce document officiel est délivré électroniquement et fait foi de matricule fiscal auprès des autorités financières.", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF certificate", e);
        }

        return out.toByteArray();
    }

    @Override
    public byte[] generateApplicationPdf(Long applicationId) {
        TaxApplication app = taxApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("TaxApplication", "id", applicationId));

        return generateRegistrationCertificate(app.getTaxpayer().getId());
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        cell.setBorderColor(new Color(220, 220, 220));
        table.addCell(cell);
    }
}
