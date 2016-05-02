package blaze.athena.DatabaseQueries;

import blaze.athena.QuestionGeneration.Question;
import blaze.athena.config.DatabaseConnection;
import blaze.athena.dto.QuestionDTO;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wesley on 7/04/2016.
 */
public class SearchQuestionsQuery {

    public List<QuestionDTO> search(String[] tags) {
        // Declare the JDBC objects.
        Connection connection = DatabaseConnection.getInstance().getConnection();
        Statement statement = null;
        ResultSet resultSet = null;
        List<QuestionDTO> arrayList = new ArrayList<QuestionDTO>();

        try {
            // Create and execute a SELECT SQL statement.
            statement = connection.createStatement();
            String query = "SELECT q.Topic, q.Question, a.Answer, a.Correct\n " +
                    "FROM QuestionCategory qc, Categories c, Questions q, Answers a\n" +
                    "WHERE q.Id = qc.QuestionId and c.Id = qc.CategoryId and a.Question = qc.QuestionId and " +
                    "c.GroupName = '" + tags[0].replace("'", "''") + "'";
            for (int i = 1; i < tags.length; i++) {
                query += "\nINTERSECT\n" +
                        "SELECT q.Topic, q.Question, a.Answer, a.Correct\n " +
                        "FROM QuestionCategory qc, Categories c, Questions q, Answers a\n" +
                        "WHERE q.Id = qc.QuestionId and c.Id = qc.CategoryId and a.Question = qc.QuestionId and " +
                        "c.GroupName = '" + tags[i].replace("'", "''") + "'";
            }
            System.out.println(query);
            resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                QuestionDTO questionDto = new QuestionDTO(resultSet.getString(1), resultSet.getString(2));
                QuestionDTO reference;
                if ((reference = contains(arrayList, questionDto)) != null) {
                    reference.addAnswer(resultSet.getString(3));
                    if (resultSet.getInt(4) == 1) {
                        reference.setAnswer(reference.getAnswers().size()-1);
                    }
                } else {
                    questionDto.addAnswer(resultSet.getString(3));
                    if (resultSet.getInt(4) == 1) {
                        questionDto.setAnswer(0);
                    }
                    arrayList.add(questionDto);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Close the connections after the data has been handled.
            if (resultSet != null) try { resultSet.close(); } catch(Exception e) {}
            if (statement != null) try { statement.close(); } catch(Exception e) {}
            if (connection != null) try {connection.close();} catch (Exception e) {};
        }
        return arrayList;
    }

    private QuestionDTO contains(List<QuestionDTO> questionDTOs, QuestionDTO questionDTO) {
        for (QuestionDTO q : questionDTOs) {
            if (q.getQuestion().equals(questionDTO.getQuestion())) {
                return q;
            }
        }
        return null;
    }

}
