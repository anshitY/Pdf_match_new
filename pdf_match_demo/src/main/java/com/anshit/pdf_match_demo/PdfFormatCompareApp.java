package com.anshit.pdf_match_demo;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@SpringBootApplication
public class PdfFormatCompareApp {
    public static void main(String[] args) {
        SpringApplication.run(PdfFormatCompareApp.class, args);
    }
}

@RestController
@RequestMapping("/format")
class PdfFormatCompareController {  // 🔁 Renamed here

    @PostMapping("/compare")
    public ResponseEntity<?> comparePdfs(@RequestParam("pdf1") MultipartFile file1,
                                         @RequestParam("pdf2") MultipartFile file2) throws IOException {
        File tempFile1 = convertToFile(file1);
        File tempFile2 = convertToFile(file2);

        boolean areSame = PdfComparator.comparePDFs(tempFile1.getAbsolutePath(), tempFile2.getAbsolutePath());

        Map<String, Object> result = new HashMap<>();
        result.put("areSame", areSame);

        return ResponseEntity.ok(result);
    }

    private File convertToFile(MultipartFile multipartFile) throws IOException {
        File convFile = File.createTempFile("upload", ".pdf");
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(multipartFile.getBytes());
        }
        return convFile;
    }
}

class PdfComparator extends PDFTextStripper {
    List<TextProperties> textData = new ArrayList<>();

    public PdfComparator() throws IOException {
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        textData.add(new TextProperties(
                getBaseFontName(text.getFont().getName()),
                text.getFontSizeInPt(),
                round(text.getXDirAdj()),
                round(text.getYDirAdj()),
                text.getUnicode()
        ));
    }

    public static String getBaseFontName(String fullFontName) {
        if (fullFontName.contains("+")) {
            return fullFontName.substring(fullFontName.indexOf("+") + 1);
        }
        return fullFontName;
    }

    public static List<TextProperties> extractTextProperties(String pdfPath) throws IOException {
        PDDocument document = PDDocument.load(new File(pdfPath));
        PdfComparator stripper = new PdfComparator();
        stripper.getText(document); // Triggers processing
        document.close();
        return stripper.textData;
    }

    public static boolean comparePDFs(String pdf1, String pdf2) throws IOException {
        List<TextProperties> pdf1Data = extractTextProperties(pdf1);
        List<TextProperties> pdf2Data = extractTextProperties(pdf2);

        return new HashSet<>(pdf1Data).equals(new HashSet<>(pdf2Data));
    }

    private static float round(float value) {
        return (float) (Math.round(value * 100.0) / 100.0);
    }
}

class TextProperties {
    String fontName;
    float fontSize;
    float xPosition;
    float yPosition;
    String character;

    public TextProperties(String fontName, float fontSize, float xPosition, float yPosition, String character) {
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.character = character;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TextProperties that = (TextProperties) obj;
        return Math.abs(that.fontSize - fontSize) < 0.5 &&
                Math.abs(that.xPosition - xPosition) < 1.0 &&
                Math.abs(that.yPosition - yPosition) < 1.0 &&
                Objects.equals(fontName, that.fontName) &&
                Objects.equals(character, that.character);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fontName, (int) fontSize, (int) xPosition, (int) yPosition, character);
    }

    @Override
    public String toString() {
        return String.format("Text: '%s', Font: %s, Size: %.2f, X: %.2f, Y: %.2f",
                character, fontName, fontSize, xPosition, yPosition);
    }
}
