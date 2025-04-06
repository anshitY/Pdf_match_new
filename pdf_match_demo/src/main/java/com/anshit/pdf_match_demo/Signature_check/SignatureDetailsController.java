package com.anshit.pdf_match_demo.Signature_check;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/signature")
public class SignatureDetailsController {

    @PostMapping("/compare")
    public ResponseEntity<String> compareSignatureDetails(@RequestParam("pdf1") MultipartFile pdf1,
                                                          @RequestParam("pdf2") MultipartFile pdf2) {
        StringBuilder result = new StringBuilder();

        try {
            result.append("📄 PDF 1: ").append(pdf1.getOriginalFilename()).append("\n");
            result.append(extractSignatureDetails(pdf1.getInputStream()));
        } catch (IOException e) {
            result.append("❌ Error reading PDF 1: ").append(e.getMessage()).append("\n");
        }

        result.append("\n==================================================\n");

        try {
            result.append("📄 PDF 2: ").append(pdf2.getOriginalFilename()).append("\n");
            result.append(extractSignatureDetails(pdf2.getInputStream()));
        } catch (IOException e) {
            result.append("❌ Error reading PDF 2: ").append(e.getMessage()).append("\n");
        }

        return ResponseEntity.ok(result.toString());
    }

    private String extractSignatureDetails(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();

        try (PDDocument document = PDDocument.load(inputStream)) {

            // Digital Signatures
            List<PDSignature> signatures = document.getSignatureDictionaries();
            if (signatures.isEmpty()) {
                output.append("❌ No digital signatures found.\n");
            } else {
                for (PDSignature sig : signatures) {
                    output.append("✅ Digital Signature Found:\n");
                    output.append("   🔐 Name: ").append(sig.getName()).append("\n");
                    output.append("   📝 Reason: ").append(sig.getReason()).append("\n");
                    output.append("   📍 Location: ").append(sig.getLocation()).append("\n");
                    output.append("   🧰 Filter: ").append(sig.getFilter()).append("\n");
                    output.append("   🔎 SubFilter: ").append(sig.getSubFilter()).append("\n");
                    output.append("   📅 Sign Date: ").append(sig.getSignDate()).append("\n");
                }
            }

            // Visible Signature Fields
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                List<PDField> fields = acroForm.getFields();
                for (PDField field : fields) {
                    if (field instanceof PDSignatureField) {
                        PDSignatureField sigField = (PDSignatureField) field;
                        output.append("🖋️ Visible Signature Field: ").append(field.getFullyQualifiedName()).append("\n");
                        for (PDAnnotationWidget widget : sigField.getWidgets()) {
                            PDRectangle rect = widget.getRectangle();
                            PDPage page = widget.getPage();
                            int pageIndex = getPageIndex(document, page);
                            output.append(String.format("   📄 Page: %d\n", pageIndex + 1));
                            output.append(String.format("   📌 Position -> X: %.2f, Y: %.2f\n", rect.getLowerLeftX(), rect.getLowerLeftY()));
                            output.append(String.format("   📐 Size -> Width: %.2f, Height: %.2f\n", rect.getWidth(), rect.getHeight()));
                        }
                    }
                }
            } else {
                output.append("ℹ️ No AcroForm found (no visible signature fields).\n");
            }
        }

        return output.toString();
    }

    private int getPageIndex(PDDocument document, PDPage page) {
        int index = 0;
        for (PDPage p : document.getPages()) {
            if (p == page) {
                return index;
            }
            index++;
        }
        return -1;
    }
}
