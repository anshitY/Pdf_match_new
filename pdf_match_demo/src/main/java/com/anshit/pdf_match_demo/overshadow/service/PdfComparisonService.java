package com.anshit.pdf_match_demo.overshadow.service;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.state.*;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.*;

@Service
public class PdfComparisonService {

    public Map<String, List<String>> compareOvershadowedText(MultipartFile file1, MultipartFile file2) throws IOException {
        Set<TextLine> overshadowed1 = getOvershadowedText(file1);
        Set<TextLine> overshadowed2 = getOvershadowedText(file2);

        Set<String> commonTexts = new HashSet<>();
        for (TextLine t1 : overshadowed1) {
            for (TextLine t2 : overshadowed2) {
                if (t1.text.equals(t2.text)) {
                    commonTexts.add(t1.text);
                }
            }
        }

        List<String> uniqueToFirst = new ArrayList<>();
        for (TextLine line : overshadowed1) {
            if (!commonTexts.contains(line.text)) {
                uniqueToFirst.add(line.text + " 📍 " + formatRect(line.boundingBox));
            }
        }

        List<String> uniqueToSecond = new ArrayList<>();
        for (TextLine line : overshadowed2) {
            if (!commonTexts.contains(line.text)) {
                uniqueToSecond.add(line.text + " 📍 " + formatRect(line.boundingBox));
            }
        }

        Map<String, List<String>> result = new HashMap<>();
        result.put("uniqueToPdf1", uniqueToFirst);
        result.put("uniqueToPdf2", uniqueToSecond);

        return result;
    }

    private Set<TextLine> getOvershadowedText(MultipartFile file) throws IOException {
        Set<TextLine> overshadowed = new HashSet<>();

        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            List<Rectangle2D.Float> imageRects = new ArrayList<>();
            ImagePositionEngine imageEngine = new ImagePositionEngine(imageRects);

            for (PDPage page : document.getPages()) {
                imageEngine.processPage(page);
            }

            LineCapturingStripper stripper = new LineCapturingStripper();
            stripper.getText(document);

            for (TextLine line : stripper.lines) {
                for (Rectangle2D.Float img : imageRects) {
                    if (img.intersects(line.boundingBox) ||
                            img.contains(line.boundingBox.getCenterX(), line.boundingBox.getCenterY())) {
                        overshadowed.add(line);
                        break;
                    }
                }
            }
        }

        return overshadowed;
    }

    private String formatRect(Rectangle2D.Float rect) {
        return String.format("[X=%.2f, Y=%.2f, W=%.2f, H=%.2f]", rect.x, rect.y, rect.width, rect.height);
    }

    // --- Inner helper classes ---

    private static class TextLine {
        String text;
        Rectangle2D.Float boundingBox;

        TextLine(String text, Rectangle2D.Float boundingBox) {
            this.text = text;
            this.boundingBox = boundingBox;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TextLine)) return false;
            TextLine other = (TextLine) obj;
            return text.equals(other.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text);
        }
    }

    private static class LineCapturingStripper extends PDFTextStripper {
        List<TextLine> lines = new ArrayList<>();

        public LineCapturingStripper() throws IOException {
            setSortByPosition(true);
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) {
            if (string.trim().isEmpty()) return;

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = 0, maxY = 0;

            for (TextPosition tp : textPositions) {
                float x = tp.getX(), y = tp.getY(), w = tp.getWidth(), h = tp.getHeight();
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x + w);
                maxY = Math.max(maxY, y + h);
            }

            Rectangle2D.Float box = new Rectangle2D.Float(minX, minY, maxX - minX, maxY - minY);
            lines.add(new TextLine(string.trim(), box));
        }
    }

    private static class ImagePositionEngine extends PDFStreamEngine {
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
            if ("Do".equals(operator.getName())) {
                COSName objectName = (COSName) operands.get(0);
                PDXObject xobject = getResources().getXObject(objectName);
                if (xobject instanceof PDImageXObject) {
                    var ctm = getGraphicsState().getCurrentTransformationMatrix();
                    float x = ctm.getTranslateX();
                    float y = ctm.getTranslateY();
                    float w = ctm.getScalingFactorX();
                    float h = ctm.getScalingFactorY();

                    if (w < 0) { x += w; w = -w; }
                    if (h < 0) { y += h; h = -h; }

                    positions.add(new Rectangle2D.Float(x, y, w, h));
                }
            } else {
                super.processOperator(operator, operands);
            }
        }
    }
}
