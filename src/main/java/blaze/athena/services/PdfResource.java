package blaze.athena.services;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.List;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripperByArea;

/**
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 23 Feb 2016
 */
public class PdfResource implements IPdfResource {
    public static final String lineSeparator = "\r\n";

    @Override
    public String test() {
        PDFManager pdfManager = new PDFManager();
        pdfManager.setFilePath("Lecture03_Software.pdf");
        String str = null;
        try {
            str = pdfManager.ToText();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String finalStr = formatText(str);

        return finalStr;
    }

    private static String formatText(String str) {
        String[] lines = str.split(lineSeparator);
        for (int i =0; i < lines.length-1; i++) {
            String firstLine = lines[i].trim();
            String secondLine = lines[i+1].trim();
            int next = 1;
            if (secondLine.equals("") && i < lines.length - 2) {
                secondLine = lines[i+2].trim();
                next = 2;
            }
            if (firstLine.length() == 0 || secondLine.length() == 0) {
                continue;
            }
            char secondLineFirstLetter = secondLine.charAt(0);
            char firstLineLastLetter = firstLine.charAt(firstLine.length()-1);
            if (firstLineLastLetter != '.' && Character.isLowerCase(secondLineFirstLetter)) {
                lines[i] += secondLine;
                lines[i+next] = "";
            }
        }
        List<String> list = Arrays.asList(lines);
        return String.join(lineSeparator, list);
    }
}

class PDFManager {

    private PDFParser parser;
    private PDFTextStripperByArea pdfStripper;
    private PDDocument pdDoc;
    private COSDocument cosDoc;

    private String Text;
    private String filePath;
    private File file;

    public PDFManager() {

    }

    public String ToText() throws IOException {
        this.pdfStripper = null;
        this.pdDoc = null;
        this.cosDoc = null;

        file = new File(filePath);
        parser = new PDFParser(new RandomAccessFile(file, "r")); // update for PDFBox V 2.0

        parser.parse();
        cosDoc = parser.getDocument();

        int x = 0;
        int y = 0;
        int width = 1000;
        pdDoc = new PDDocument(cosDoc);
        float height = pdDoc.getPage(0).getMediaBox().getHeight() - 50;

        Rectangle2D region = new Rectangle2D.Double(x, y, width, height);
        String regionName = "region";
        PDFTextStripperByArea pdfTextStripper;

        pdfTextStripper = new PDFTextStripperByArea();
        pdfTextStripper.setParagraphEnd(PdfResource.lineSeparator);

        pdfTextStripper.addRegion(regionName, region);
        String text = "";
        for (int i = 1; i < pdDoc.getNumberOfPages(); i++) {
            pdfTextStripper.extractRegions(pdDoc.getPage(i));
            text += pdfTextStripper.getTextForRegion(regionName);
        }
        return text;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

}