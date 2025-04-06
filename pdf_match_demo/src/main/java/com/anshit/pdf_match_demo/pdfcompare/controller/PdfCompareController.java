package com.anshit.pdf_match_demo.pdfcompare.controller;

import com.anshit.pdf_match_demo.pdfcompare.service.PdfImageSignatureComparatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/pdf")
public class PdfCompareController {

    @Autowired
    private PdfImageSignatureComparatorService comparatorService;

    /**
     * Compares two PDFs for image and signature similarities.
     */
    @PostMapping("/compare")
    public ResponseEntity<String> comparePdfs(@RequestParam("pdf1") MultipartFile pdf1,
                                              @RequestParam("pdf2") MultipartFile pdf2,
                                              @RequestParam(value = "tolerance", defaultValue = "20.0") float tolerance) {
        try {
            String result = comparatorService.compare(pdf1, pdf2, tolerance);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Error: " + e.getMessage());
        }
    }

    /**
     * Checks if any text in the PDF is overlapped by images.
     */
    @PostMapping("/overshadowed-text")
    public ResponseEntity<?> checkOvershadowedText(@RequestParam("pdf") MultipartFile pdf) {
        try {
            List<String> overlappedText = comparatorService.detectTextOverlappedByImages(pdf);
            if (overlappedText.isEmpty()) {
                return ResponseEntity.ok("✅ No text is overshadowed by images.");
            } else {
                return ResponseEntity.ok(overlappedText);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Error: " + e.getMessage());
        }
    }
}
