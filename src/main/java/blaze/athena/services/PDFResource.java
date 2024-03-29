package blaze.athena.services;

import blaze.athena.DatabaseQueries.InsertQuestionQuery;
import blaze.athena.QuestionGeneration.SentenceSimplifier;
import blaze.athena.document.PDFManager;
import blaze.athena.dto.QuestionDTO;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
//            String finalStr = joiner.toString().replaceAll("\n", "#");
            System.out.println(finalStr);
            return Response.ok(finalStr).build();
        } catch (IOException e) {
            // error occurred processing input
            return Response.serverError().build();
        }
    }

    @Override
    public ResponseEntity<List<QuestionDTO>> generateQuestions(@MultipartForm MultipartFormDataInput input) {

        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("uploadedFile");
        List<InputPart> textData = uploadForm.get("uploadedText");
        List<InputPart> categoryData = uploadForm.get("uploadedCategory");
        try {
            StringJoiner joiner = new StringJoiner("\n");
            PDFManager pdfManager = new PDFManager();
            String finalStr;
            if (inputParts != null) {
                for (InputPart inputPart : inputParts) {
                    // Convert the uploaded file to inputstream
                    InputStream inputStream = inputPart.getBody(InputStream.class, null);

                    joiner.add(pdfManager.toText(inputStream));
                }
                finalStr = pdfManager.formatText(joiner.toString())
                        .trim();//.replaceAll("^[-\\u2022\\u2023\\u25E6\\u2043\\u2219]", "");
            } else {
                for (InputPart inputPart : textData) {
                    String line = inputPart.getBodyAsString();
                    joiner.add(line);
                }
                finalStr = pdfManager.formatTextForText(joiner.toString())
                        .trim();//.replaceAll("^[-\\u2022\\u2023\\u25E6\\u2043\\u2219]", "");
            }

            // Get category
            List<String> categories = Arrays.asList(categoryData.get(0).getBodyAsString().split(","));
            SentenceSimplifier ss = new SentenceSimplifier();
            List<QuestionDTO> questions = ss.run(finalStr);

            // set category
            questions.parallelStream().forEach(q -> q.setCategoryTags(categories));

        //    saveQuestionsToDB(questions);

            return new ResponseEntity<>(questions, HttpStatus.OK);
        } catch (IOException e) {
            // error occurred processing input
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    private void saveQuestionsToDB(List<QuestionDTO> questions) {
        InsertQuestionQuery insertQuestionQuery = new InsertQuestionQuery();
        insertQuestionQuery.insert(questions.get(0));
    }
}