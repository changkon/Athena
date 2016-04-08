package blaze.athena.DatabaseQueries;

import blaze.athena.config.DatabaseConnection;
import blaze.athena.dto.QuestionDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wesley on 7/04/2016.
 */
public class InsertQuestionQuery {

//    public void insertMultiple(List<QuestionDTO> questions) {
//        // Declare the JDBC objects.
//        Connection connection = DatabaseConnection.getInstance().getConnection();
//        Statement statement = null;
//        ResultSet resultSet = null;
//        PreparedStatement prepsInsertProduct = null;
//
//        try {
//            // Create and execute an INSERT SQL prepared statement.
//            String insertSql = "INSERT INTO dbo.Questions (Question, Category) VALUES ";
//            for (int i = 0; i < questions.size(); i++) {
//                String question = questions.get(i).getQuestion();
//                if (i != questions.size() - 1) {
//                    insertSql += "('" + question + "', '1'), ";
//                } else {
//                    insertSql += "('" + question + "', '1');";
//                }
//            }
//
//            prepsInsertProduct = connection.prepareStatement(
//                    insertSql,
//                    Statement.RETURN_GENERATED_KEYS);
//            prepsInsertProduct.execute();
//
//            // Retrieve the generated key from the insert.
//            resultSet = prepsInsertProduct.getGeneratedKeys();
//
//            // Print the ID of the inserted row.
//            while (resultSet.next()) {
//                System.out.println("Generated: " + resultSet.getString(1));
//            }
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//        finally {
//            // Close the connections after the data has been handled.
//            if (resultSet != null) try { resultSet.close(); } catch(Exception e) {}
//            if (statement != null) try { statement.close(); } catch(Exception e) {}
//            if (connection != null) try { connection.close(); } catch(Exception e) {}
//        }
//    }

    public String insert(QuestionDTO question) {
        // Declare the JDBC objects.
        Connection connection = DatabaseConnection.getInstance().getConnection();
        ResultSet resultSet = null;
        PreparedStatement prepsInsertProduct = null;

        try {
            // Create and execute an INSERT SQL prepared statement.
            String insertSql = "INSERT INTO dbo.Questions (Question, Category, Topic, PdfId) VALUES "
                    + "('"+ question.getQuestion() + "', '1', '" + question.getTopic() + "', '0');";

            prepsInsertProduct = connection.prepareStatement(
                    insertSql,
                    Statement.RETURN_GENERATED_KEYS);
            prepsInsertProduct.execute();

            // Retrieve the generated key from the insert.
            resultSet = prepsInsertProduct.getGeneratedKeys();

            // Print the ID of the inserted row.
            String resultId = "";
            while (resultSet.next()) {
                resultId =  resultSet.getString(1);
                System.out.println("Generated question: " +resultId);
            }

            //now submit the answers
            submitAnswers(question, connection, Integer.parseInt(resultId));
            return resultId;
        }
        catch (Exception e) {
            if (e.getMessage().contains("Violation of UNIQUE KEY constraint")) {
                System.err.println("Question already exists!");
            }
        }
        finally {
            // Close the connections after the data has been handled.
            if (resultSet != null) try { resultSet.close(); } catch(Exception e) {}
            if (prepsInsertProduct != null) try { prepsInsertProduct.close(); } catch(Exception e) {}
        }
        return "-1";
    }

    private void submitAnswers(QuestionDTO question, Connection connection, int questionId) {
        ResultSet answersResultSet = null;
        PreparedStatement answersPrepsInsertProduct = null;
        try {
            // Create and execute an INSERT SQL prepared statement.
            List<String> answers = question.getAnswers();
            String answersInsertSql = "INSERT INTO dbo.Answers (Answer, Question, Correct) VALUES "
                    + "('" + answers.get(0).replace("'", "''") + "', '" + questionId + "', '" + (question.getAnswer() == 0) + "'), "
                    + "('" + answers.get(1).replace("'", "''") + "', '" + questionId + "', '" + (question.getAnswer() == 1) + "'), "
                    + "('" + answers.get(2).replace("'", "''") + "', '" + questionId + "', '" + (question.getAnswer() == 2) + "'), "
                    + "('" + answers.get(3).replace("'", "''") + "', '" + questionId + "', '" + (question.getAnswer() == 3) + "');";

            answersPrepsInsertProduct = connection.prepareStatement(
                    answersInsertSql,
                    Statement.RETURN_GENERATED_KEYS);
            answersPrepsInsertProduct.execute();

            // Retrieve the generated key from the insert.
            answersResultSet = answersPrepsInsertProduct.getGeneratedKeys();

            // Print the ID of the inserted row.
            while (answersResultSet.next()) {
                System.out.println("Generated answer: " + answersResultSet.getString(1));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (answersResultSet != null) try { answersResultSet.close(); } catch(Exception e) {}
            if (answersPrepsInsertProduct != null) try { answersPrepsInsertProduct.close(); } catch(Exception e) {}
        }
    }

}