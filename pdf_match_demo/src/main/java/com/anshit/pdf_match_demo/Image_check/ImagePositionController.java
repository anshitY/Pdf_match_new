package com.anshit.pdf_match_demo.Image_check;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@RestController
@RequestMapping("/image-positions")
public class ImagePositionController extends PDFStreamEngine {

    private final List<String> imageDetails = new ArrayList<>();

    public ImagePositionController() throws IOException {
        addOperator(new DrawObject());
        addOperator(new Concatenate());
        addOperator(new Save());
        addOperator(new Restore());
        addOperator(new SetGraphicsStateParameters());
        addOperator(new SetMatrix());
    }

    public static void main(String[] args) {
        SpringApplication.run(ImagePositionController.class, args);
    }

    @PostMapping("/compare")
    public ResponseEntity<String> extractImagesFromPdfs(
            @RequestParam("pdf1") MultipartFile pdf1,
            @RequestParam("pdf2") MultipartFile pdf2) {

        StringBuilder result = new StringBuilder();

        try {
            result.append("📘 PDF 1: ").append(pdf1.getOriginalFilename()).append("\n");
            result.append(processPdf(new ByteArrayInputStream(pdf1.getBytes())));

            result.append("\n\n📘 PDF 2: ").append(pdf2.getOriginalFilename()).append("\n");
            result.append(processPdf(new ByteArrayInputStream(pdf2.getBytes())));

            return ResponseEntity.ok(result.toString());

        } catch (IOException e) {
            return ResponseEntity.status(500).body("❌ Error processing PDFs: " + e.getMessage());
        }
    }

    private String processPdf(InputStream pdfStream) throws IOException {
        imageDetails.clear();
        try (PDDocument document = PDDocument.load(pdfStream)) {
            PDPageTree pages = document.getPages();
            int pageIndex = 1;
            for (PDPage page : pages) {
                imageDetails.add("📄 Page " + pageIndex++);
                this.processPage(page);
            }
        }
        return String.join("\n", imageDetails);
    }

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String operation = operator.getName();
        if ("Do".equals(operation)) {
            COSName objectName = (COSName) operands.get(0);
            PDXObject xobject = getResources().getXObject(objectName);

            if (xobject instanceof PDImageXObject image) {
                float[][] matrix = getGraphicsState().getCurrentTransformationMatrix().getValues();

                float x = matrix[2][0];
                float y = matrix[2][1];
                float width = matrix[0][0];
                float height = matrix[1][1];

                imageDetails.add(String.format(
                        "🖼️ Image at X: %.2f, Y: %.2f, Width: %.2f, Height: %.2f", x, y, width, height));
            }
        } else {
            super.processOperator(operator, operands);
        }
    }
}
