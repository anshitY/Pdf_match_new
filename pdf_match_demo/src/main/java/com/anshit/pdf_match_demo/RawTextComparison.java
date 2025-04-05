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

@SpringBootApplication
@RestController
@RequestMapping("/compare_text")
public class RawTextComparison {

    public static void main(String[] args) {
        SpringApplication.run(RawTextComparison.class, args);
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
        String text1 = extractText(pdfStream1);
        String text2 = extractText(pdfStream2);

        if (text1.equals(text2)) {
            return "The PDFs are identical.\n\n" + text1;
        } else {
            return "The PDFs are different.\n\nFirst PDF Text:\n" + text1 + "\n\nSecond PDF Text:\n" + text2;
        }
    }

    private String extractText(InputStream pdfStream) throws IOException {
        try (PDDocument document = PDDocument.load(pdfStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        }
    }
}
