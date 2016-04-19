package blaze.athena.config;

import java.sql.*;

/**
 * Created by Wesley on 7/04/2016.
 */
public class DatabaseConnection {

   // private static DatabaseConnection dbConnection;
    private Connection connection;
    //get this from environmental variable later
    private static final String connectionString = "jdbc:sqlserver://athenablaze.database.windows.net:1433;database=athena_db;user=blaze@athenablaze;password=Rosathena123;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";

    public static DatabaseConnection getInstance() {

        DatabaseConnection db = new DatabaseConnection();
        try {
            db.connection = DriverManager.getConnection(connectionString);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        return db;
    }

    public Connection getConnection() {
        return connection;
    }

}
