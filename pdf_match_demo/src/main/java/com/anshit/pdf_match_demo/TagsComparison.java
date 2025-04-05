package com.anshit.pdf_match_demo;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
@RestController
@RequestMapping("/compare")
public class TagsComparison {

    public static void main(String[] args) {
        SpringApplication.run(TagsComparison.class, args);
    }

    @PostMapping("/pdfs")
    public ResponseEntity<String> comparePdfs(@RequestParam("pdf1") MultipartFile pdf1,
                                              @RequestParam("pdf2") MultipartFile pdf2) {
        try {
            InputStream stream1 = new ByteArrayInputStream(pdf1.getBytes());
            InputStream stream2 = new ByteArrayInputStream(pdf2.getBytes());

            String result = comparePdfData(stream1, stream2);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error processing PDF files: " + e.getMessage());
        }
    }

    private String comparePdfData(InputStream pdfStream1, InputStream pdfStream2) throws IOException {

        String invoiceID1 = extractInvoiceID(pdfStream1);
        pdfStream1.reset();
        String orderID1 = extractOrderID(pdfStream1);
        pdfStream1.reset();
        String gstin1 = extractGSTIN(pdfStream1);
        pdfStream1.reset();
        String orderDate1 = extractOrderDate(pdfStream1);
        pdfStream1.reset();
        String invoiceDate1 = extractInvoiceDate(pdfStream1);
        pdfStream1.reset();
        String description1 = extractDescription(pdfStream1);
        pdfStream1.reset();
        String shipToAddress1 = extractShipToAddress(pdfStream1);
        pdfStream1.reset();
        String billToAddress1 = extractBillToAddress(pdfStream1);

        String invoiceID2 = extractInvoiceID(pdfStream2);
        pdfStream2.reset();
        String orderID2 = extractOrderID(pdfStream2);
        pdfStream2.reset();
        String gstin2 = extractGSTIN(pdfStream2);
        pdfStream2.reset();
        String orderDate2 = extractOrderDate(pdfStream2);
        pdfStream2.reset();
        String invoiceDate2 = extractInvoiceDate(pdfStream2);
        pdfStream2.reset();
        String description2 = extractDescription(pdfStream2);
        pdfStream2.reset();
        String shipToAddress2 = extractShipToAddress(pdfStream2);
        pdfStream2.reset();
        String billToAddress2 = extractBillToAddress(pdfStream2);

        if (invoiceID1.isEmpty() || orderID1.isEmpty() || gstin1.isEmpty() ||
                invoiceID2.isEmpty() || orderID2.isEmpty() || gstin2.isEmpty()) {
            return "Error: One or more fields could not be extracted from the PDFs.";
        }

        if (invoiceID1.equals(invoiceID2) && orderID1.equals(orderID2) && gstin1.equals(gstin2) &&
                orderDate1.equals(orderDate2) && invoiceDate1.equals(invoiceDate2) && description1.equals(description2) &&
                shipToAddress1.equals(shipToAddress2) && billToAddress1.equals(billToAddress2)) {
            return "The PDFs are same.\n\n PDF:\nInvoice ID: " + invoiceID1 + "\nOrder ID: " + orderID1 + "\nGSTIN: " + gstin1 +
                    "\nOrder Date: " + orderDate1 + "\nInvoice Date: " + invoiceDate1 + "\nDescription: " + description1 +
                    "\nShip To Address: " + shipToAddress1 + "\nBill To Address: " + billToAddress1;
        } else {
            return "The PDFs are different.\n\nFirst PDF:\nInvoice ID: " + invoiceID1 + "\nOrder ID: " + orderID1 + "\nGSTIN: " + gstin1 +
                    "\nOrder Date: " + orderDate1 + "\nInvoice Date: " + invoiceDate1 + "\nDescription: " + description1 +
                    "\nShip To Address: " + shipToAddress1 + "\nBill To Address: " + billToAddress1 +
                    "\n\nSecond PDF:\nInvoice ID: " + invoiceID2 + "\nOrder ID: " + orderID2 + "\nGSTIN: " + gstin2 +
                    "\nOrder Date: " + orderDate2 + "\nInvoice Date: " + invoiceDate2 + "\nDescription: " + description2 +
                    "\nShip To Address: " + shipToAddress2 + "\nBill To Address: " + billToAddress2;
        }
    }

    private String extractText(InputStream pdfStream) throws IOException {
        try (PDDocument document = PDDocument.load(pdfStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document).trim();
            System.out.println(text);
            return text;
        }
    }

    private String extractInvoiceID(InputStream pdfStream) throws IOException {
        return extractField(pdfStream, "Invoice Number #\\s([A-Za-z0-9]+)");
    }

    private String extractOrderID(InputStream pdfStream) throws IOException {
        return extractField(pdfStream, "Order ID:\\s([A-Za-z0-9]+)");
    }

    private String extractGSTIN(InputStream pdfStream) throws IOException {
        return extractField(pdfStream, "GSTIN\\s?-\\s?([A-Z0-9]+)");
    }

    private String extractOrderDate(InputStream pdfStream) throws IOException {
        return extractField(pdfStream, "(\\d{2}-\\d{2}-\\d{4})Order Date:");
    }

    private String extractInvoiceDate(InputStream pdfStream) throws IOException {
        return extractField(pdfStream, "(\\d{2}-\\d{2}-\\d{4})Invoice Date:");
    }

    private String extractDescription(InputStream pdfStream) throws IOException {
        return extractField(pdfStream, "Description:\\s([\\s\\S]+?)\\s(?:Qty|Total)");
    }

    private String extractShipToAddress(InputStream pdfStream) throws IOException {
        return extractField(pdfStream, "Ship To\\s*(.*?)\\s*Phone:", Pattern.DOTALL);
    }

    private String extractBillToAddress(InputStream pdfStream) throws IOException {
        return extractField(pdfStream, "Bill To\\s*(.*?)\\s*Phone:", Pattern.DOTALL);
    }

    private String extractField(InputStream pdfStream, String regex) throws IOException {
        return extractField(pdfStream, regex, 0);
    }

    private String extractField(InputStream pdfStream, String regex, int flags) throws IOException {
        String text = extractText(pdfStream);
        Pattern pattern = Pattern.compile(regex, flags);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }
}
