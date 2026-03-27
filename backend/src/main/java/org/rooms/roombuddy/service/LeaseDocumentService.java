package org.rooms.roombay.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.BaseFont;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.entity.Lease;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.LeaseRepository;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaseDocumentService {

    private final LeaseRepository leaseRepository;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    
    @Transactional(readOnly = true)
    public byte[] generateLeaseDocument(UUID leaseId) {
        log.info("Generating PDF document for lease: {}", leaseId);
        
        // Use findByIdWithDetails to eagerly fetch all relationships needed for PDF
        Lease lease = leaseRepository.findByIdWithDetails(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        // Log to verify data is loaded
        log.info("Lease loaded - Landlord: {}, Student: {}, Listing: {}", 
            lease.getLandlord() != null ? lease.getLandlord().getEmail() : "NULL",
            lease.getStudent() != null ? lease.getStudent().getEmail() : "NULL",
            lease.getListing() != null ? lease.getListing().getTitle() : "NULL");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();
            
            // Add content
            addHeader(document, lease);
            addParties(document, lease);
            addPropertyDetails(document, lease);
            addLeaseTerms(document, lease);
            addFinancialTerms(document, lease);
            addTermsAndConditions(document);
            addSignatureSection(document, lease);
            
            document.close();
            writer.close();
            
            byte[] pdfBytes = baos.toByteArray();
            log.info("PDF document generated successfully for lease: {}, size: {} bytes", leaseId, pdfBytes.length);
            return pdfBytes;
            
        } catch (Exception e) {
            log.error("Failed to generate PDF for lease {}: {}", leaseId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate lease document: " + e.getMessage());
        } finally {
            try {
                baos.close();
            } catch (Exception ignored) {}
        }
    }
    
    private void addHeader(Document document, Lease lease) throws DocumentException {
        // Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(0, 51, 102));
        Paragraph title = new Paragraph("RESIDENTIAL LEASE AGREEMENT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);
        
        // Reference Code
        Font refFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY);
        String refCode = lease.getReferenceCode() != null ? lease.getReferenceCode() : "N/A";
        Paragraph ref = new Paragraph("Reference: " + refCode, refFont);
        ref.setAlignment(Element.ALIGN_CENTER);
        ref.setSpacingAfter(20);
        document.add(ref);
        
        // Date
        Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        String dateStr = lease.getCreatedAt() != null ? lease.getCreatedAt().format(DATE_FORMAT) : "N/A";
        Paragraph date = new Paragraph("Date: " + dateStr, dateFont);
        date.setAlignment(Element.ALIGN_RIGHT);
        date.setSpacingAfter(30);
        document.add(date);
    }
    
    private void addParties(Document document, Lease lease) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(0, 51, 102));
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        
        Paragraph partiesTitle = new Paragraph("PARTIES TO THIS AGREEMENT", sectionFont);
        partiesTitle.setSpacingAfter(15);
        document.add(partiesTitle);
        
        // Landlord
        Paragraph landlordLabel = new Paragraph("LANDLORD:", boldFont);
        document.add(landlordLabel);
        
        String landlordName = safeStr(lease.getLandlord().getFirstName()) + " " + safeStr(lease.getLandlord().getLastName());
        String landlordEmail = safeStr(lease.getLandlord().getEmail());
        String landlordPhone = lease.getLandlord().getPhone() != null ? lease.getLandlord().getPhone() : "N/A";
        
        Paragraph landlordInfo = new Paragraph(
            "Name: " + landlordName + "\n" +
            "Email: " + landlordEmail + "\n" +
            "Phone: " + landlordPhone,
            normalFont
        );
        landlordInfo.setIndentationLeft(20);
        landlordInfo.setSpacingAfter(15);
        document.add(landlordInfo);
        
        // Tenant
        Paragraph tenantLabel = new Paragraph("TENANT:", boldFont);
        document.add(tenantLabel);
        
        String tenantName = safeStr(lease.getStudent().getFirstName()) + " " + safeStr(lease.getStudent().getLastName());
        String tenantEmail = safeStr(lease.getStudent().getEmail());
        String tenantPhone = lease.getStudent().getPhone() != null ? lease.getStudent().getPhone() : "N/A";
        
        Paragraph tenantInfo = new Paragraph(
            "Name: " + tenantName + "\n" +
            "Email: " + tenantEmail + "\n" +
            "Phone: " + tenantPhone,
            normalFont
        );
        tenantInfo.setIndentationLeft(20);
        tenantInfo.setSpacingAfter(25);
        document.add(tenantInfo);
    }
    
    private void addPropertyDetails(Document document, Lease lease) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(0, 51, 102));
        
        Paragraph propertyTitle = new Paragraph("PROPERTY DETAILS", sectionFont);
        propertyTitle.setSpacingAfter(15);
        document.add(propertyTitle);
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});
        
        addTableRow(table, "Property:", safeStr(lease.getListing().getTitle()));
        addTableRow(table, "Address:", safeStr(lease.getListing().getAddress()));
        addTableRow(table, "City:", safeStr(lease.getListing().getCity()));
        addTableRow(table, "Property Type:", lease.getListing().getPropertyType() != null ? lease.getListing().getPropertyType().toString() : "N/A");
        
        table.setSpacingAfter(25);
        document.add(table);
    }
    
    private void addLeaseTerms(Document document, Lease lease) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(0, 51, 102));
        
        Paragraph termsTitle = new Paragraph("LEASE TERMS", sectionFont);
        termsTitle.setSpacingAfter(15);
        document.add(termsTitle);
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});
        
        addTableRow(table, "Start Date:", lease.getStartDate() != null ? lease.getStartDate().format(DATE_FORMAT) : "N/A");
        addTableRow(table, "End Date:", lease.getEndDate() != null ? lease.getEndDate().format(DATE_FORMAT) : "N/A");
        addTableRow(table, "Duration:", lease.getDurationMonths() + " months");
        addTableRow(table, "Lease Status:", lease.getStatus() != null ? lease.getStatus().toString() : "N/A");
        
        table.setSpacingAfter(25);
        document.add(table);
    }
    
    private void addFinancialTerms(Document document, Lease lease) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(0, 51, 102));
        
        Paragraph financialTitle = new Paragraph("FINANCIAL TERMS", sectionFont);
        financialTitle.setSpacingAfter(15);
        document.add(financialTitle);
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});
        
        addTableRow(table, "Monthly Rent:", formatCurrency(lease.getMonthlyRent()) + " XAF");
        addTableRow(table, "Security Deposit:", formatCurrency(lease.getDepositAmount()) + " XAF");
        addTableRow(table, "Total Initial Payment:", formatCurrency(lease.getTotalInitialPayment()) + " XAF");
        
        table.setSpacingAfter(25);
        document.add(table);
    }
    
    private void addTermsAndConditions(Document document) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(0, 51, 102));
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        
        Paragraph conditionsTitle = new Paragraph("TERMS AND CONDITIONS", sectionFont);
        conditionsTitle.setSpacingAfter(15);
        document.add(conditionsTitle);
        
        String[] conditions = {
            "1. RENT PAYMENT: Rent is due on the first day of each month. Late payments may incur additional fees.",
            "2. SECURITY DEPOSIT: The security deposit will be returned within 30 days of lease termination, minus any deductions for damages or unpaid rent.",
            "3. MAINTENANCE: The Tenant agrees to maintain the property in good condition and report any damages promptly.",
            "4. UTILITIES: Unless otherwise specified, the Tenant is responsible for all utility payments.",
            "5. SUBLETTING: The Tenant may not sublet the property without written consent from the Landlord.",
            "6. TERMINATION: Either party may terminate this lease with 30 days written notice.",
            "7. PROPERTY ACCESS: The Landlord may access the property for inspections with 24 hours notice.",
            "8. PETS: Pets are not allowed unless explicitly permitted in writing by the Landlord.",
            "9. NOISE: The Tenant agrees to maintain reasonable noise levels and respect neighbors.",
            "10. GOVERNING LAW: This agreement is governed by the laws of Cameroon."
        };
        
        for (String condition : conditions) {
            Paragraph p = new Paragraph(condition, normalFont);
            p.setSpacingAfter(8);
            p.setIndentationLeft(10);
            document.add(p);
        }
        
        document.add(new Paragraph("\n"));
    }
    
    private void addSignatureSection(Document document, Lease lease) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(0, 51, 102));
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);
        
        Paragraph sigTitle = new Paragraph("SIGNATURES", sectionFont);
        sigTitle.setSpacingAfter(20);
        document.add(sigTitle);
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        
        // Landlord signature
        PdfPCell landlordCell = new PdfPCell();
        landlordCell.setBorder(Rectangle.NO_BORDER);
        landlordCell.setPadding(10);
        
        Paragraph landlordSig = new Paragraph();
        landlordSig.add(new Chunk("LANDLORD\n\n", normalFont));
        landlordSig.add(new Chunk("_______________________________\n", normalFont));
        landlordSig.add(new Chunk(lease.getLandlord().getFirstName() + " " + lease.getLandlord().getLastName() + "\n\n", normalFont));
        
        if (Boolean.TRUE.equals(lease.getLandlordAcceptedTerms()) && lease.getLandlordAcceptedAt() != null) {
            landlordSig.add(new Chunk("[SIGNED] Digitally signed on " + lease.getLandlordAcceptedAt().format(DATE_FORMAT), smallFont));
        } else {
            landlordSig.add(new Chunk("Pending signature", smallFont));
        }
        landlordCell.addElement(landlordSig);
        table.addCell(landlordCell);
        
        // Tenant signature
        PdfPCell tenantCell = new PdfPCell();
        tenantCell.setBorder(Rectangle.NO_BORDER);
        tenantCell.setPadding(10);
        
        Paragraph tenantSig = new Paragraph();
        tenantSig.add(new Chunk("TENANT\n\n", normalFont));
        tenantSig.add(new Chunk("_______________________________\n", normalFont));
        tenantSig.add(new Chunk(lease.getStudent().getFirstName() + " " + lease.getStudent().getLastName() + "\n\n", normalFont));
        
        if (Boolean.TRUE.equals(lease.getStudentAcceptedTerms()) && lease.getStudentAcceptedAt() != null) {
            tenantSig.add(new Chunk("[SIGNED] Digitally signed on " + lease.getStudentAcceptedAt().format(DATE_FORMAT), smallFont));
        } else {
            tenantSig.add(new Chunk("Pending signature", smallFont));
        }
        tenantCell.addElement(tenantSig);
        table.addCell(tenantCell);
        
        document.add(table);
        
        // Footer
        Paragraph footer = new Paragraph(
            "\n\nThis document was generated by RoomBay - Student Housing Platform for Cameroon",
            smallFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }
    
    private String safeStr(String s) {
        return s != null ? s : "N/A";
    }
    
    private void addTableRow(PdfPTable table, String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(new Color(245, 245, 245));
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    private String formatCurrency(Integer amount) {
        if (amount == null) return "0";
        return String.format("%,d", amount);
    }
}
