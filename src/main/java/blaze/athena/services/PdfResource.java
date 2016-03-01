package blaze.athena.services;

import blaze.athena.document.PDFManager;

import java.io.File;
import java.io.IOException;

/**
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 23 Feb 2016
 */
public class PDFResource implements IPDFResource {
    @Override
    public String test() {
        PDFManager pdfManager = new PDFManager();
        ClassLoader classLoader = getClass().getClassLoader();
        File pdfFile = new File(classLoader.getResource("Lecture03_Software.pdf").getFile());

        try {
            String str = pdfManager.toText(pdfFile);
            String finalStr = pdfManager.formatText(str);
            return finalStr;
        } catch (IOException e) {
            return null;
        }
    }
}