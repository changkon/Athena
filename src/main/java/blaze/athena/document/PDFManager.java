package blaze.athena.document;

import blaze.athena.services.PDFResource;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 01 Mar 2016
 */
public class PDFManager implements DocumentManager {

    private PDFParser parser;
    private PDFTextStripperByArea pdfStripper;
    private PDDocument pdDoc;
    private COSDocument cosDoc;

    public PDFManager() {

    }

    @Override
    public String toText(InputStream stream) throws IOException {
        parser = new PDFParser(new RandomAccessBufferedFileInputStream(stream)); // update for PDFBox V 2.0

        parser.parse();
        cosDoc = parser.getDocument();

        int x = 0;
        int y = 0;
        int width = 1000;
        pdDoc = new PDDocument(cosDoc);
        float height = pdDoc.getPage(0).getMediaBox().getHeight() - 10;

        Rectangle2D region = new Rectangle2D.Double(x, y, width, height);
        String regionName = "region";
        PDFTextStripperByArea pdfTextStripper;

        pdfTextStripper = new PDFTextStripperByArea();
        pdfTextStripper.setDropThreshold(3);
        pdfTextStripper.setIndentThreshold(10);
        pdfTextStripper.setParagraphEnd(lineSeparator);

        pdfTextStripper.addRegion(regionName, region);
        String text = "";
        for (int i = 0; i < pdDoc.getNumberOfPages(); i++) {
            pdfTextStripper.extractRegions(pdDoc.getPage(i));
            text += pdfTextStripper.getTextForRegion(regionName);
            text += "==========.\n";
        }
        return text;
    }
}
