package blaze.athena.DatabaseQueries;

import blaze.athena.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wesley on 7/04/2016.
 */
public class SelectGroupsByOwnerQuery {

    public List<List<String>> select(int ownerId) {
        String query = "SELECT * FROM dbo.Groups WHERE OwnerId=" + ownerId + ";";
        // Declare the JDBC objects.
        Connection connection = DatabaseConnection.getInstance().getConnection();
        Statement statement = null;
        ResultSet resultSet = null;
        List<List<String>> arrayList = new ArrayList<>();

        try {
            // Create and execute a SELECT SQL statement.
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);

            // add results from select statement into list
            ResultSetMetaData metadata = resultSet.getMetaData();
            int numberOfColumns = metadata.getColumnCount();
            while (resultSet.next()) {
                int i = 1;
                List<String> temp = new ArrayList<>();
                while(i <= numberOfColumns) {
                    temp.add(resultSet.getString(i++));
                }
                arrayList.add(temp);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Close the connections after the data has been handled.
            if (resultSet != null) try { resultSet.close(); } catch(Exception e) {}
            if (statement != null) try { statement.close(); } catch(Exception e) {}
        }
        return arrayList;
    }

}
