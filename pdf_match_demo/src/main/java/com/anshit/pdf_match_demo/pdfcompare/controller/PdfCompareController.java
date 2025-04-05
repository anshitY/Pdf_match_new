package com.anshit.pdf_match_demo.pdfcompare.controller;

import com.anshit.pdf_match_demo.pdfcompare.service.PdfImageSignatureComparatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
public class PdfCompareController {

    @Autowired
    private PdfImageSignatureComparatorService comparatorService;

    @PostMapping("/compare")
    public ResponseEntity<String> comparePdfs(@RequestParam("pdf1") MultipartFile pdf1,
                                              @RequestParam("pdf2") MultipartFile pdf2,
                                              @RequestParam(value = "tolerance", defaultValue = "20.0") float tolerance) {
        try {
            String result = comparatorService.compare(pdf1, pdf2, tolerance);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }


}

