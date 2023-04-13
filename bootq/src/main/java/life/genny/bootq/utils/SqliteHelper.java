package life.genny.bootq.utils;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class SqliteHelper {

    public static final String SQLITE_CONNECTION_URL = "jdbc:sqlite:../../genny-main/sqlite/volume_mount/db/";

    public static final String SQLITE_EXTENSION = ".sqlite";
    public static String CREATE_TABLE_VALIDATION = "src/main/resources/sqlite/create_table_validation.sql";
    public static String CREATE_TABLE_DATATYPE = "src/main/resources/sqlite/create_table_datatype.sql";
    public static String CREATE_TABLE_ATTRIBUTE = "src/main/resources/sqlite/create_table_attribute.sql";
    public static String CREATE_TABLE_DEF_BASEENTITY = "src/main/resources/sqlite/create_table_def_baseentity.sql";
    public static String CREATE_TABLE_BASEENTITY = "src/main/resources/sqlite/create_table_baseentity.sql";
    public static String CREATE_TABLE_DEF_ENTITY_ATTRIBUTE = "src/main/resources/sqlite/create_table_def_entityattribute.sql";
    public static String CREATE_TABLE_ENTITY_ATTRIBUTE = "src/main/resources/sqlite/create_table_entityattribute.sql";
    public static String CREATE_TABLE_QUESTION = "src/main/resources/sqlite/create_table_question.sql";
    public static String CREATE_TABLE_QUESTION_QUESTION = "src/main/resources/sqlite/create_table_questionquestion.sql";

    public static String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS ";

    public static String INSERT_INTO = "INSERT OR REPLACE INTO ";

    public static String SELECT_FROM = "SELECT * FROM ";

    public static String VALUES = " VALUES ";

    public static char OPEN_PARENTHESIS = '(';

    public static char CLOSE_PARENTHESIS = ')';

    public static char QUESTION_MARK = '?';

    public static char COMMA = ',';

    @Inject
    Logger log;

    public Connection getConnectionToDatabase(String databaseName) throws SQLException {
        if (StringUtils.isBlank(databaseName)) {
            throw new UnsupportedOperationException("Database name cannot be empty!");
        }
        String url = SQLITE_CONNECTION_URL + databaseName + SQLITE_EXTENSION;
        return DriverManager.getConnection(url);
    }

    public void createTable(Connection connection, String tableName, boolean dropBeforeCreate) throws SQLException, IOException {
        if (connection == null) {
            throw new UnsupportedOperationException("Connection object is null!");
        }
        if (StringUtils.isBlank(tableName)) {
            throw new UnsupportedOperationException("Tablename cannot be empty!");
        }
        String fileName;
        switch (tableName.toLowerCase()) {
            case "validation" -> fileName = CREATE_TABLE_VALIDATION;
            case "datatype" -> fileName = CREATE_TABLE_DATATYPE;
            case "attribute" -> fileName = CREATE_TABLE_ATTRIBUTE;
            case "def_baseentity" -> fileName = CREATE_TABLE_DEF_BASEENTITY;
            case "baseentity" -> fileName = CREATE_TABLE_BASEENTITY;
            case "def_entityattribute" -> fileName = CREATE_TABLE_DEF_ENTITY_ATTRIBUTE;
            case "entityattribute" -> fileName = CREATE_TABLE_ENTITY_ATTRIBUTE;
            case "question" -> fileName = CREATE_TABLE_QUESTION;
            case "question_question" -> fileName = CREATE_TABLE_QUESTION_QUESTION;
            default -> throw new UnsupportedOperationException("Table not supported: " + tableName);
        }
        if (dropBeforeCreate) {
            dropTable(connection, tableName);
        }
        String createTableStatement = loadCreateTableStatementFromFile(fileName);
        log.infof("createTableStatement for Sqlite: " + createTableStatement);
        PreparedStatement preparedStatementForCreate = connection.prepareStatement(createTableStatement);
        preparedStatementForCreate.execute();
        log.infof("Table created in Sqlite: " + tableName);
    }

    public void dropTable(Connection connection, String tableName) throws SQLException {
        if (connection == null) {
            throw new UnsupportedOperationException("Connection object is null!");
        }
        if (StringUtils.isBlank(tableName)) {
            throw new UnsupportedOperationException("Tablename cannot be empty!");
        }
        StringBuilder dropSqlStatement = new StringBuilder(DROP_TABLE_IF_EXISTS);
        dropSqlStatement.append(tableName);
        PreparedStatement preparedStatementForDrop = connection.prepareStatement(dropSqlStatement.toString());
        preparedStatementForDrop.execute();
    }

    private String loadCreateTableStatementFromFile(String fileName) throws IOException {
        if (StringUtils.isBlank(fileName)) {
            throw new UnsupportedOperationException("FileName cannot be empty!");
        }
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(fileName);
            bufferedReader = new BufferedReader(fileReader);
            StringBuilder createTableStatementBuilder = new StringBuilder();
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null)
                    break;
                createTableStatementBuilder.append(line);
            }
            return createTableStatementBuilder.toString();
        } catch (IOException e) {
            throw e;
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileReader != null) {
                fileReader.close();
            }
        }
    }

    public void insertRecordIntoDatabase(Connection connection, String tableName, Collection<Map<String, String>> recordsMapCollection) throws SQLException {
        if (connection == null) {
            throw new UnsupportedOperationException("Connection object is null!");
        }
        if (StringUtils.isBlank(tableName)) {
            throw new UnsupportedOperationException("Tablename cannot be empty!");
        }
        if (recordsMapCollection == null || recordsMapCollection.size() == 0) {
            log.infof("Nothing to insert since recordsMapCollection is empty.");
            return;
        }
        log.infof("Found %s record(s) to be inserted into %s table", recordsMapCollection.size(), tableName);
        connection.setAutoCommit(false);
        boolean logFlag = true;
        for (Map<String, String> row : recordsMapCollection) {
            StringBuilder columnNames = new StringBuilder(" ");
            StringBuilder valuePlaceholders = new StringBuilder();
            boolean isFirst = true;
            for (Map.Entry<String, String> entry : row.entrySet()) {
                if (StringUtils.isBlank(entry.getKey()) || entry.getKey().contains(" "))
                    continue;
                if (!isFirst) {
                    columnNames.append(COMMA);
                    valuePlaceholders.append(COMMA);
                }
                isFirst = false;
                columnNames.append(entry.getKey());
                valuePlaceholders.append(QUESTION_MARK);
            }
            StringBuilder insertSqlStatement = new StringBuilder(INSERT_INTO);
            insertSqlStatement.append(tableName).
                    append(OPEN_PARENTHESIS).append(columnNames).append(CLOSE_PARENTHESIS).
                    append(VALUES).
                    append(OPEN_PARENTHESIS).append(valuePlaceholders).append(CLOSE_PARENTHESIS);
            if (logFlag) {
                log.info("Constructed insert statement -> " + insertSqlStatement);
                logFlag = false;
            }
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(insertSqlStatement.toString());
                int i = 1;
                for (Map.Entry<String, String> entry : row.entrySet()) {
                    if (StringUtils.isBlank(entry.getKey()) || entry.getKey().contains(" "))
                        continue;
                    preparedStatement.setString(i, entry.getValue());
                    i++;
                }
                preparedStatement.execute();
            } catch (Exception e) {
                log.errorf("Problematic statement -> %s", insertSqlStatement.toString());
                throw e;
            }
        }
        connection.commit();
    }

    public void executeCrudStatement(Connection connection, String crudStatement) throws SQLException {
        if (connection == null) {
            throw new UnsupportedOperationException("Connection object is null!");
        }
        if (StringUtils.isBlank(crudStatement)) {
            throw new UnsupportedOperationException("crudStatement cannot be empty!");
        }
        connection.prepareStatement(crudStatement).execute();
        connection.commit();
    }

    public Map<String, Map<String, String>> fetchRecordsFromTable(Connection connection, String tableName) throws SQLException {
        if (connection == null) {
            throw new UnsupportedOperationException("Connection object is null!");
        }
        if (StringUtils.isBlank(tableName)) {
            throw new UnsupportedOperationException("Tablename cannot be empty!");
        }
        Map<String, Map<String, String>> recordsMap = new HashMap<>();
	PreparedStatement preparedStatement;
        try {
            preparedStatement = connection.prepareStatement(SELECT_FROM + tableName);
        } catch (SQLException e) {
            log.errorf("Skipping loading records from table '%s' in SQLite because of an error while creating prepared-statement", tableName);
            log.error(e.getMessage());
            return recordsMap;
        }
        ResultSet results = preparedStatement.executeQuery();
        int rownum = 0;
        ResultSetMetaData resultsMetaData = results.getMetaData();
        int columnCount = resultsMetaData.getColumnCount();
        while(results.next()) {
            Map<String, String> record = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = resultsMetaData.getColumnName(i);
                String value = results.getString(columnName);
                if (!StringUtils.isBlank(value))
                    record.put(columnName, value);
            }
            if (record.size() > 0) {
                recordsMap.put("" + rownum, record);
                rownum++;
            }
        }
        return recordsMap;
    }

    public void closeConnection(Connection connection) throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
