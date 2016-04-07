package blaze.athena.config;

import java.sql.*;

/**
 * Created by Wesley on 7/04/2016.
 */
public class DatabaseConnection {

    private static DatabaseConnection dbConnection;
    private Connection connection;
    //get this from environmental variable later
    private static final String connectionString = "jdbc:sqlserver://teamblaze.database.windows.net:1433;database=athena_db;user=blaze@teamblaze;password=Rosathena123;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";

    public static DatabaseConnection getInstance() {
        if (dbConnection == null) {
            //create connection
            dbConnection = new DatabaseConnection();
            try {
                dbConnection.connection = DriverManager.getConnection(connectionString);
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        return dbConnection;
    }

    public Connection getConnection() {
        return connection;
    }

}
