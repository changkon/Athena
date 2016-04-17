package blaze.athena.DatabaseQueries;

import blaze.athena.config.DatabaseConnection;
import blaze.athena.dto.GroupDTO;
import blaze.athena.dto.QuestionDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wesley on 7/04/2016.
 */
public class InsertGroupQuery {

    public String insert(GroupDTO group) {
        // Declare the JDBC objects.
        Connection connection = DatabaseConnection.getInstance().getConnection();
        ResultSet resultSet = null;
        PreparedStatement prepsInsertProduct = null;

        try {
            // Create and execute an INSERT SQL prepared statement.
            String insertSql = "INSERT INTO dbo.Groups (Name, Description, OwnerId) VALUES "
                    + "('"+ group.getName().replace("'", "''") + "', '" + group.getDescription().replace("'", "''")  + "', '6');";

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
                System.out.println("Generated group: " +resultId);
            }

            return resultId;
        }
        catch (Exception e) {
            if (e.getMessage().contains("Violation of UNIQUE KEY constraint")) {
                System.err.println("Group already exists!");
            } else {
                e.printStackTrace();
            }
        }
        finally {
            // Close the connections after the data has been handled.
            if (resultSet != null) try { resultSet.close(); } catch(Exception e) {}
            if (prepsInsertProduct != null) try { prepsInsertProduct.close(); } catch(Exception e) {}
        }
        return "-1";
    }




}
