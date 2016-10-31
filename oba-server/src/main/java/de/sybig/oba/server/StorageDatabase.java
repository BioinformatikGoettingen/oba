package de.sybig.oba.server;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to handle the database to store the mime types.
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class StorageDatabase {

    private static final Logger logger = LoggerFactory
            .getLogger(StorageDatabase.class);
    private static StorageDatabase instance;
    private Connection connection;
    private PreparedStatement ps0;
    private PreparedStatement ps1;
    private PreparedStatement ps2;
    private PreparedStatement ps3;

    private StorageDatabase() {
        // singleton
    }

    /**
     * Get the singleton instance of the StorageHandler and inits the connection
     * if necessary.
     *
     * @return The singleton instance.
     */
    public static StorageDatabase getInstance() {
        if (instance == null) {
            instance = new StorageDatabase();
            try {
                instance.getConnection();
            } catch (ClassNotFoundException e) {
                logger.error("could not load driver for the database", e);
                throw new WebApplicationException(500);
            } catch (SQLException e) {
                logger.error("could not init connection to the database because of", e);
                throw new WebApplicationException(500);
            }
        }
        return instance;
    }

    public void logPut(String space, String name, String mimetype) {

        try {
            ps0.setString(1, space);
            ps0.setString(2, name);
            ps0.execute();
            ps1.setString(1, space);
            ps1.setString(2, name);
            ps1.setString(3, mimetype);
            ps1.execute();
        } catch (SQLException e) {
            logger.error(
                    "could not log PUT command to partition {} and name {} ",
                    space, name);
            logger.error("SQL error {}", e.getMessage());
            throw new WebApplicationException(500);
        }
    }

    public void logGet(String space, String name) {
        try {
            ps2.setString(1, space);
            ps2.setString(2, name);
            ps2.execute();
        } catch (SQLException e) {
            logger.warn(
                    "could not log GET command to partition {} and name {} ",
                    space, name);
            logger.warn("SQL error {}", e.getMessage());
        }
    }

    /**
     * Gets the mime type as it is stored in the database. During a PUT command
     * the mime type of the http header is stored in the database for the
     * partition / name combination. If no record for the partition / name
     * combination is found, <code>null</code> is returned, but should not
     * happen.
     *
     * @param partition The name of the partition the list is stored in.
     * @param name The name of the list to get the mime type of.
     * @return The mime type or <code>null</code>.
     */
    public String getMimetype(String partition, String name) {
        try {
            ps3.setString(1, partition);
            ps3.setString(2, name);
            ResultSet rs = ps3.executeQuery();
            if (rs.next()) {
                String mimetype = rs.getString("mimetype");
                return mimetype;
            } else {
                logger.info(
                        "could not get mime type for {}/{}, no entry found in the db",
                        partition, name);
                return null;
            }

        } catch (SQLException e) {
            logger.error(
                    "could not get mime type for partition {} and name {} ",
                    partition, name);
            logger.error("SQL error {}", e.getMessage());
            throw new WebApplicationException(500);
        }
    }

    /**
     * Shuts the database down and sets the connection to null.
     */
    public void shutdown() {
        try {
            if (connection != null) {
                Statement stmt = connection.createStatement();
                stmt.execute("SHUTDOWN");
                stmt.closeOnCompletion();
                connection = null;
            }
        } catch (SQLException e) {
            logger.error("Error during finalization of the storage handler", e);
        }
    }

    private Connection getConnection() throws ClassNotFoundException,
            SQLException {
        if (connection == null) {
            Properties props = RestServer.getProperties();

            String dbDir = props.getProperty("storage_root",
                    System.getProperty("java.io.tmpdir", "/tmp"))
                    + "/db";
            Class.forName("org.hsqldb.jdbcDriver");
            connection = DriverManager.getConnection("jdbc:hsqldb:file:"
                    + dbDir, "sa", "");
            DatabaseMetaData dbm = connection.getMetaData();
            ResultSet rs = dbm.getTables(null, null, "ENTRY", null);
            if (!rs.next()) {
                initDB(connection);
            }
            ps0 = connection
                    .prepareStatement("DELETE FROM entry WHERE space = ? AND name = ? ");
            ps1 = connection
                    .prepareStatement("INSERT INTO entry (space, name, mimetype, created, counter) values (?, ?, ?, NOW(), 0)");
            ps2 = connection
                    .prepareStatement("UPDATE entry set counter = counter +1, last = NOW() WHERE space = ? AND name = ?");
            ps3 = connection
                    .prepareStatement("SELECT mimetype FROM entry WHERE space = ? AND name = ?");
        }
        return connection;
    }

    private void initDB(Connection c) throws SQLException {
        String c1 = "CREATE TABLE entry (space varchar(12), name varchar(12), mimetype varchar(12), created TIMESTAMP, last TIMESTAMP, counter integer)";
        Statement stmt = c.createStatement();
        stmt.execute(c1);
        stmt.closeOnCompletion();
    }

}
