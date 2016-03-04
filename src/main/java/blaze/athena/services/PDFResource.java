package blaze.athena.services;

import blaze.athena.QuestionGeneration.SentenceSimplifier;
import blaze.athena.document.PDFManager;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

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
        File pdfFile = new File(classLoader.getResource("selection.pdf").getFile());

        try {
            FileInputStream fis = new FileInputStream(pdfFile);
            String str = pdfManager.toText(fis);
            String finalStr = pdfManager.formatText(str);
            fis.close();
            return finalStr;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Response uploadFile(@MultipartForm MultipartFormDataInput input) {

        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("uploadedFile");

        try {
            StringJoiner joiner = new StringJoiner("\n");
            PDFManager pdfManager = new PDFManager();

            for (InputPart inputPart : inputParts) {
                // Convert the uploaded file to inputstream
                InputStream inputStream = inputPart.getBody(InputStream.class, null);

                joiner.add(pdfManager.toText(inputStream));
            }

            String finalStr = pdfManager.formatText(joiner.toString());
            return Response.ok(finalStr).build();
        } catch (IOException e) {
            // error occurred processing input
            return Response.serverError().build();
        }
    }

    @Override
    public Response generateQuestions(@MultipartForm MultipartFormDataInput input) {

        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("uploadedFile");

        try {
            StringJoiner joiner = new StringJoiner("\n");
            PDFManager pdfManager = new PDFManager();

            for (InputPart inputPart : inputParts) {
                // Convert the uploaded file to inputstream
                InputStream inputStream = inputPart.getBody(InputStream.class, null);

                joiner.add(pdfManager.toText(inputStream));
            }

            String finalStr = pdfManager.formatText(joiner.toString());
            SentenceSimplifier ss = new SentenceSimplifier();
            String questions = ss.run(finalStr);
            return Response.ok(questions).build();
        } catch (IOException e) {
            // error occurred processing input
            return Response.serverError().build();
        }
    }
}