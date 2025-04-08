package com.anshit.pdf_match_demo.overshadow.controller;

import com.anshit.pdf_match_demo.overshadow.service.PdfComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
public class PdfComparisonController {

    @Autowired
    private PdfComparisonService pdfComparisonService;

    @PostMapping("/compare-overshadowed")
    public ResponseEntity<Map<String, List<String>>> compareOvershadowed(
            @RequestParam("pdf1") MultipartFile file1,
            @RequestParam("pdf2") MultipartFile file2) throws IOException {

        Map<String, List<String>> result = pdfComparisonService.compareOvershadowedText(file1, file2);
        return ResponseEntity.ok(result);
    }
}
