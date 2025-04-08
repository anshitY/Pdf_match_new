package com.anshit.pdf_match_demo.pdfcompare.service;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.state.*;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class PdfImageSignatureComparatorService {

    public String compare(MultipartFile file1, MultipartFile file2, float tolerance) throws IOException {
        StringBuilder result = new StringBuilder();

        List<Rectangle2D.Float> images1 = extractImages(file1.getInputStream());
        List<Rectangle2D.Float> images2 = extractImages(file2.getInputStream());

        Map<String, Rectangle2D.Float> signs1 = extractSignatures(file1.getInputStream());
        Map<String, Rectangle2D.Float> signs2 = extractSignatures(file2.getInputStream());

        result.append(printRectangles("PDF 1 Images", images1));
        result.append(printRectangles("PDF 1 Signatures", signs1));
        result.append(printRectangles("PDF 2 Images", images2));
        result.append(printRectangles("PDF 2 Signatures", signs2));

        List<Rectangle2D.Float> commonImages = new ArrayList<>();
        List<Rectangle2D.Float> remainingImages1 = new ArrayList<>(images1);
        List<Rectangle2D.Float> remainingImages2 = new ArrayList<>(images2);

        for (Rectangle2D.Float r1 : images1) {
            for (Rectangle2D.Float r2 : images2) {
                if (rectsMatch(r1, r2, tolerance)) {
                    commonImages.add(r1);
                    remainingImages1.remove(r1);
                    remainingImages2.remove(r2);
                    break;
                }
            }
        }

        if (!commonImages.isEmpty()) {
            result.append("\n✅ Images match.\n");
        }

        boolean signatureMatched = false;

        signs1.values().removeIf(r -> r.width == 0 && r.height == 0);
        signs2.values().removeIf(r -> r.width == 0 && r.height == 0);

        if (images1.size() > images2.size()) {
            for (Rectangle2D.Float extraImg : remainingImages1) {
                for (Rectangle2D.Float sig : signs2.values()) {
                    if (rectsMatch(extraImg, sig, tolerance)) {
                        result.append("✅ Remaining image in PDF 1 matches signature field in PDF 2.\n");
                        signatureMatched = true;
                        break;
                    }
                }
            }
        } else {
            for (Rectangle2D.Float extraImg : remainingImages2) {
                for (Rectangle2D.Float sig : signs1.values()) {
                    if (rectsMatch(extraImg, sig, tolerance)) {
                        result.append("✅ Remaining image in PDF 2 matches signature field in PDF 1.\n");
                        signatureMatched = true;
                        break;
                    }
                }
            }
        }

        if (!signatureMatched) {
            outer:
            for (Rectangle2D.Float sig1 : signs1.values()) {
                for (Rectangle2D.Float sig2 : signs2.values()) {
                    if (rectsMatch(sig1, sig2, tolerance)) {
                        signatureMatched = true;
                        break outer;
                    }
                }
            }
        }

        if (signatureMatched) {
            result.append("✅ Signatures match.\n");
        } else {
            result.append("❌ Signature mismatch.\n");
        }

        return result.toString();
    }

    private List<Rectangle2D.Float> extractImages(InputStream input) throws IOException {
        try (PDDocument doc = PDDocument.load(input)) {
            List<Rectangle2D.Float> positions = new ArrayList<>();
            ImagePositionEngine engine = new ImagePositionEngine(positions);
            for (PDPage page : doc.getPages()) {
                engine.processPage(page);
            }
            return positions;
        }
    }

    private Map<String, Rectangle2D.Float> extractSignatures(InputStream input) throws IOException {
        Map<String, Rectangle2D.Float> signatureMap = new LinkedHashMap<>();
        try (PDDocument doc = PDDocument.load(input)) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                for (PDField field : acroForm.getFields()) {
                    if (field instanceof PDSignatureField sigField && !sigField.getWidgets().isEmpty()) {
                        PDRectangle rect = sigField.getWidgets().get(0).getRectangle();
                        Rectangle2D.Float box = new Rectangle2D.Float(
                                rect.getLowerLeftX(),
                                rect.getLowerLeftY(),
                                rect.getWidth(),
                                rect.getHeight()
                        );
                        signatureMap.put(field.getFullyQualifiedName(), box);
                    }
                }
            } else {
                signatureMap.put("NoField", new Rectangle2D.Float(0, 0, 0, 0));
            }
        }
        return signatureMap;
    }

    private boolean rectsMatch(Rectangle2D.Float r1, Rectangle2D.Float r2, float tolerance) {
        Rectangle2D.Float norm1 = normalize(r1);
        Rectangle2D.Float norm2 = normalize(r2);
        return Math.abs(norm1.x - norm2.x) <= tolerance &&
                Math.abs(norm1.y - norm2.y) <= tolerance &&
                Math.abs(norm1.width - norm2.width) <= tolerance &&
                Math.abs(norm1.height - norm2.height) <= tolerance;
    }

    private Rectangle2D.Float normalize(Rectangle2D.Float rect) {
        float x = rect.width < 0 ? rect.x + rect.width : rect.x;
        float y = rect.height < 0 ? rect.y + rect.height : rect.y;
        float width = Math.abs(rect.width);
        float height = Math.abs(rect.height);
        return new Rectangle2D.Float(x, y, width, height);
    }

    private String printRectangles(String label, List<Rectangle2D.Float> list) {
        StringBuilder sb = new StringBuilder("\n🖼️ ").append(label).append(": ").append(list).append("\n");
        for (Rectangle2D.Float rect : list) {
            if (rect.height < 0 || rect.width < 0) {
                sb.append("⚠️ Detected image with negative dimension. Might be inverted: ").append(formatRect(rect)).append("\n");
            }
        }
        return sb.toString();
    }

    private String printRectangles(String label, Map<String, Rectangle2D.Float> map) {
        StringBuilder sb = new StringBuilder("\n🖋️ ").append(label).append(":\n");
        for (Map.Entry<String, Rectangle2D.Float> entry : map.entrySet()) {
            Rectangle2D.Float rect = entry.getValue();
            sb.append("Field: ").append(entry.getKey()).append(" => ").append(formatRect(rect)).append("\n");
            if (rect.width < 0 || rect.height < 0) {
                sb.append("⚠️ Signature '").append(entry.getKey()).append("' is malformed:");
                if (rect.width < 0) sb.append(" negative width");
                if (rect.height < 0) sb.append(rect.width < 0 ? ", negative height" : " negative height");
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String formatRect(Rectangle2D.Float rect) {
        return String.format("[X=%.2f, Y=%.2f, W=%.2f, H=%.2f]", rect.x, rect.y, rect.width, rect.height);
    }

    static class ImagePositionEngine extends PDFStreamEngine {
        private final List<Rectangle2D.Float> positions;

        public ImagePositionEngine(List<Rectangle2D.Float> positions) throws IOException {
            this.positions = positions;
            addOperator(new DrawObject());
            addOperator(new Concatenate());
            addOperator(new Save());
            addOperator(new Restore());
            addOperator(new SetGraphicsStateParameters());
            addOperator(new SetMatrix());
        }

        @Override
        protected void processOperator(org.apache.pdfbox.contentstream.operator.Operator operator, List<COSBase> operands) throws IOException {
            String operation = operator.getName();
            if ("Do".equals(operation)) {
                COSName objectName = (COSName) operands.get(0);
                PDXObject xobject = getResources().getXObject(objectName);
                if (xobject instanceof PDImageXObject) {
                    float x = getGraphicsState().getCurrentTransformationMatrix().getTranslateX();
                    float y = getGraphicsState().getCurrentTransformationMatrix().getTranslateY();
                    float w = getGraphicsState().getCurrentTransformationMatrix().getScalingFactorX();
                    float h = getGraphicsState().getCurrentTransformationMatrix().getScalingFactorY();
                    Rectangle2D.Float rect = new Rectangle2D.Float(x, y, w, h);
                    positions.add(rect);
                }
            } else {
                super.processOperator(operator, operands);
            }
        }
    }

    public List<String> detectTextOverlappedByImages(MultipartFile pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            List<Rectangle2D.Float> imageRects = new ArrayList<>();
            ImagePositionEngine engine = new ImagePositionEngine(imageRects);
            for (PDPage page : document.getPages()) {
                engine.processPage(page);
            }

            List<TextPositionInfo> textPositions = new ArrayList<>();
            TextPositionExtractor textExtractor = new TextPositionExtractor(textPositions);
            for (PDPage page : document.getPages()) {
                textExtractor.processPage(page);
            }

            List<String> overlappedTexts = new ArrayList<>();
            for (TextPositionInfo text : textPositions) {
                Rectangle2D.Float tRect = text.getRect();
                for (Rectangle2D.Float img : imageRects) {
                    if (img.intersects(tRect)) {
                        overlappedTexts.add("Overlapped text: '" + text.getText() + "' at " + formatRect(tRect));
                        break;
                    }
                }
            }

            return overlappedTexts;
        }
    }

    static class TextPositionInfo {
        private final String text;
        private final Rectangle2D.Float rect;

        public TextPositionInfo(String text, Rectangle2D.Float rect) {
            this.text = text;
            this.rect = rect;
        }

        public String getText() {
            return text;
        }

        public Rectangle2D.Float getRect() {
            return rect;
        }
    }

    static class TextPositionExtractor extends PDFTextStripper {
        private final List<TextPositionInfo> positions;

        public TextPositionExtractor(List<TextPositionInfo> positions) throws IOException {
            this.positions = positions;
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            Rectangle2D.Float rect = new Rectangle2D.Float(
                    text.getX(),
                    text.getY(),
                    text.getWidth(),
                    text.getHeight()
            );
            positions.add(new TextPositionInfo(text.getUnicode(), rect));
        }
    }
}
