package blaze.athena.DatabaseQueries;

import blaze.athena.config.DatabaseConnection;
import blaze.athena.dto.QuestionDTO;
import com.microsoft.sqlserver.jdbc.SQLServerException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wesley on 7/04/2016.
 */
public class RatingQuery {

    public void insertRating(QuestionDTO question) {
        // Declare the JDBC objects.
        Connection connection = DatabaseConnection.getInstance().getConnection();
        ResultSet resultSet = null;
        PreparedStatement prepsInsertProduct = null;

        try {
            int id = getIdOfQuestion(connection, question);
            String sql;
            List<String> rating = checkRatingExists(connection, id);
            if (rating == null) {
                // Create and execute an INSERT SQL prepared statement.
                sql = "INSERT INTO dbo.Rating (Question, VoteCount, CurrentAvgValue) VALUES "
                        + "('" + id + "', '1', '" + question.getRating() + "');";
            } else {
                double existingRating = Double.parseDouble(rating.get(3));
                int voteCount = Integer.parseInt(rating.get(2));
                int ratingId = Integer.parseInt(rating.get(0));
                double newRating = (existingRating * voteCount + question.getRating()) / (voteCount + 1.00);
                int newVoteCount = voteCount + 1;
                sql = "UPDATE dbo.Rating SET VoteCount = " + newVoteCount + ", CurrentAvgValue = " + newRating + " WHERE Id = " + ratingId + ";";
            }
            System.out.println("Rating sql: " + sql);
            prepsInsertProduct = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS);
            prepsInsertProduct.execute();

            // Retrieve the generated key from the insert.
            resultSet = prepsInsertProduct.getGeneratedKeys();

            // Print the ID of the inserted row.
            String resultId = "";
            while (resultSet.next()) {
                resultId =  resultSet.getString(1);
                System.out.println("Generated rating: " +resultId);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Close the connections after the data has been handled.
            if (resultSet != null) try { resultSet.close(); } catch(Exception e) {}
            if (prepsInsertProduct != null) try { prepsInsertProduct.close(); } catch(Exception e) {}
        }
    }

    private List<String> checkRatingExists(Connection connection, int questionId) {
        List<String> arrayList = new ArrayList<String>();
        try {
            Statement statement = connection.createStatement();
            String query = "SELECT * FROM dbo.Rating WHERE Question=" + questionId + ";";
            System.out.println(query);
            ResultSet resultSet = statement.executeQuery(query);
            // add results from select statement into list
            ResultSetMetaData metadata = resultSet.getMetaData();
            int numberOfColumns = metadata.getColumnCount();
            resultSet.next();
            int i = 1;
            while(i <= numberOfColumns) {
                arrayList.add(resultSet.getString(i++));
            }
            System.out.println(arrayList);
            return arrayList; //rating exists
        }catch (SQLException e) {
            System.out.println("rating doesn't exist yet");
            return null; //rating doesn't exist
        }
    }

    private int getIdOfQuestion(Connection connection, QuestionDTO q) throws SQLException {
        Statement statement = connection.createStatement();
        String query = "SELECT * FROM dbo.Questions WHERE Question='" + q.getQuestion() + "';";
        System.out.println(query);
        ResultSet resultSet = statement.executeQuery(query);
         // add results from select statement into list
        resultSet.next();
        String id = resultSet.getString(1); //db index start at 1 ie. the id
        return Integer.parseInt(id);
    }

}
