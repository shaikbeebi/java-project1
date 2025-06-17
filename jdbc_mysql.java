import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class jdbcmysql {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // MySQL JDBC URL: jdbc:mysql://hostname:port/databaseName?user=username&password=password
        // Replace 'your_database_name', 'your_username', 'your_password' with your MySQL credentials
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/your_database_name?useSSL=false", "your_username", "your_password")) {
            while (true) {
                System.out.println("\nMenu:");
                System.out.println("1. Create Table");
                System.out.println("2. Insert Data (Manual)");
                System.out.println("3. Insert Data (From File)");
                System.out.println("4. Update Data (Manual)");
                System.out.println("5. Update Data (From File)");
                System.out.println("6. Delete Record (Manual)");
                System.out.println("7. Delete Record (From File)");
                System.out.println("8. Clear All Data (Truncate)");
                System.out.println("9. Delete Entire Table");
                System.out.println("10. View Records (Paginated)");
                System.out.println("11. Exit");
                System.out.println("12. Create Log Table and Triggers for a Table");
                System.out.print("Enter your choice: ");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1": createTable(conn); break;
                    case "2": insertManual(conn); break;
                    case "3": insertFromFile(conn); break;
                    case "4": updateManual(conn); break;
                    case "5": updateFromFile(conn); break;
                    case "6": deleteManual(conn); break;
                    case "7": deleteFromFile(conn); break;
                    case "8": truncateTable(conn); break;
                    case "9": deleteTable(conn); break;
                    case "10": selectRecords(conn); break;
                    case "11": System.out.println("Exiting..."); return;
                    case "12": createLogTableAndTriggers(conn); break;
                    default: System.out.println("Invalid choice.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
            e.printStackTrace(); // Added for more detailed error tracing
        }
    }

    private static void insertManual(Connection conn) throws SQLException {
        System.out.print("Enter table name: ");
        String table = scanner.nextLine();
        List<ColumnInfo> columns = getTableColumns(conn, table); // Changed method name
        if (columns.isEmpty()) {
            System.out.println("Invalid table or no columns found.");
            return;
        }

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" (");
        for (ColumnInfo col : columns) {
            sql.append(col.name).append(", ");
        }
        sql.setLength(sql.length() - 2);
        sql.append(") VALUES (").append("?, ".repeat(columns.size()));
        sql.setLength(sql.length() - 2);
        sql.append(")");

        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < columns.size(); i++) {
                ColumnInfo col = columns.get(i);
                while (true) {
                    System.out.print("Enter value for " + col.name + " (" + col.type + "): ");
                    String input = scanner.nextLine().trim();
                    if (input.equalsIgnoreCase("null") || input.isEmpty()) {
                        pstmt.setNull(i + 1, java.sql.Types.NULL);
                        break;
                    }
                    try {
                        // MySQL data type handling
                        if (col.type.toUpperCase().startsWith("INT") || col.type.toUpperCase().startsWith("BIGINT") || col.type.toUpperCase().startsWith("TINYINT")) {
                            pstmt.setInt(i + 1, Integer.parseInt(input));
                        } else if (col.type.toUpperCase().startsWith("DECIMAL") || col.type.toUpperCase().startsWith("NUMERIC") || col.type.toUpperCase().startsWith("DOUBLE") || col.type.toUpperCase().startsWith("FLOAT")) {
                            pstmt.setBigDecimal(i + 1, new BigDecimal(input));
                        } else if (col.type.toUpperCase().startsWith("VARCHAR") || col.type.equalsIgnoreCase("CHAR") || col.type.equalsIgnoreCase("TEXT")) {
                            pstmt.setString(i + 1, input);
                        } else if (col.type.equalsIgnoreCase("DATE")) {
                            pstmt.setDate(i + 1, java.sql.Date.valueOf(input));
                        } else if (col.type.equalsIgnoreCase("DATETIME") || col.type.equalsIgnoreCase("TIMESTAMP")) {
                            pstmt.setTimestamp(i + 1, java.sql.Timestamp.valueOf(input));
                        } else {
                            pstmt.setString(i + 1, input); // Default to string
                        }
                        break;
                    } catch (Exception e) {
                        System.out.println("Invalid value. Try again. Error: " + e.getMessage());
                    }
                }
            }
            pstmt.executeUpdate();
            System.out.println("Record inserted.");
        }
    }

    private static void insertFromFile(Connection conn) {
        try {
            System.out.print("Enter file path (SQL file with INSERT statements): ");
            String filePath = scanner.nextLine().replace("\"", "").trim();

            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                int count = 0;
                try (Statement stmt = conn.createStatement()) {
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("--") && !line.startsWith("#")) { // Ignore comments
                            stmt.executeUpdate(line);
                            count++;
                        }
                    }
                }
                System.out.println(count + " SQL statement(s) executed from file.");
            }
        } catch (Exception e) {
            System.out.println("Error inserting from file: " + e.getMessage());
        }
    }

    private static void updateManual(Connection conn) {
        try {
            System.out.print("Enter table name: ");
            String table = scanner.nextLine();
            System.out.print("Enter column to update: ");
            String col = scanner.nextLine();
            System.out.print("Enter new value: ");
            String newVal = scanner.nextLine();
            System.out.print("Enter condition (e.g., id=1): ");
            String cond = scanner.nextLine(); // Vulnerable to SQL Injection here

            String sql = "UPDATE " + table + " SET " + col + " = ? WHERE " + cond;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // Determine the column type for setString. For simplicity, keeping as String.
                // In a robust application, you'd fetch metadata for 'col' and use appropriate setter.
                pstmt.setString(1, newVal);
                int updated = pstmt.executeUpdate();
                System.out.println(updated + " record(s) updated.");
            }
        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
        }
    }

    private static void updateFromFile(Connection conn) {
        try {
            System.out.print("Enter table name: ");
            String table = scanner.nextLine();
            System.out.print("Enter CSV file path: ");
            String filePath = scanner.nextLine();
            System.out.print("Enter condition column name (e.g., id): ");
            String conditionCol = scanner.nextLine();

            // Fetch column info to handle data types correctly
            List<ColumnInfo> columns = getTableColumns(conn, table);
            Map<String, ColumnInfo> columnMap = new HashMap<>();
            for(ColumnInfo ci : columns) {
                columnMap.put(ci.name.toUpperCase(), ci);
            }

            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String[] headers = br.readLine().split(",");
                List<String> updateCols = new ArrayList<>(Arrays.asList(headers));
                updateCols.remove(conditionCol); // Remove condition column from update list

                StringBuilder sql = new StringBuilder("UPDATE ").append(table).append(" SET ");
                for (String col : updateCols) {
                    sql.append(col.trim()).append(" = ?, ");
                }
                sql.setLength(sql.length() - 2); // Remove trailing ", "
                sql.append(" WHERE ").append(conditionCol).append(" = ?");

                try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                    String line;
                    int batchCount = 0;
                    while ((line = br.readLine()) != null) {
                        String[] values = line.split(",");
                        // Find the index of the condition column in the headers
                        int conditionColIndex = -1;
                        for (int k = 0; k < headers.length; k++) {
                            if (headers[k].trim().equalsIgnoreCase(conditionCol)) {
                                conditionColIndex = k;
                                break;
                            }
                        }
                        if (conditionColIndex == -1) {
                            System.out.println("Error: Condition column '" + conditionCol + "' not found in CSV header.");
                            return;
                        }

                        int paramIndex = 1;
                        for (int i = 0; i < headers.length; i++) {
                            String header = headers[i].trim();
                            if (!header.equalsIgnoreCase(conditionCol)) {
                                String value = values[i].trim();
                                ColumnInfo colInfo = columnMap.get(header.toUpperCase());
                                if (colInfo == null) {
                                    System.out.println("Warning: Column " + header + " not found in table metadata. Setting as String.");
                                    pstmt.setString(paramIndex++, value);
                                } else {
                                    setPreparedStatementValue(pstmt, paramIndex++, colInfo.type, value);
                                }
                            }
                        }
                        // Set the value for the WHERE clause
                        ColumnInfo conditionColInfo = columnMap.get(conditionCol.toUpperCase());
                        if (conditionColInfo == null) {
                            System.out.println("Warning: Condition column " + conditionCol + " not found in table metadata. Setting as String.");
                            pstmt.setString(paramIndex, values[conditionColIndex].trim());
                        } else {
                            setPreparedStatementValue(pstmt, paramIndex, conditionColInfo.type, values[conditionColIndex].trim());
                        }

                        pstmt.addBatch();
                        batchCount++;
                        if (batchCount % 100 == 0) { // Execute batch every 100 records
                            pstmt.executeBatch();
                            conn.commit(); // Commit transaction for every 100 records
                            batchCount = 0;
                        }
                    }
                    pstmt.executeBatch(); // Execute remaining batch
                    conn.commit();
                    System.out.println("Records updated from file.");
                }
            }
        } catch (Exception e) {
            System.out.println("Update from file error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void deleteManual(Connection conn) {
        try {
            System.out.print("Enter table name: ");
            String table = scanner.nextLine();
            System.out.print("Enter condition (e.g., id=1): ");
            String cond = scanner.nextLine(); // Vulnerable to SQL Injection here
            String sql = "DELETE FROM " + table + " WHERE " + cond;
            try (Statement stmt = conn.createStatement()) { // Changed to Statement for direct condition
                int deleted = stmt.executeUpdate(sql);
                System.out.println(deleted + " record(s) deleted.");
            }
        } catch (SQLException e) {
            System.out.println("Delete error: " + e.getMessage());
        }
    }

    private static void deleteFromFile(Connection conn) {
        try {
            System.out.print("Enter table name: ");
            String table = scanner.nextLine();
            System.out.print("Enter file path with keys to delete: ");
            String filePath = scanner.nextLine();
            System.out.print("Enter condition column name (e.g., id): ");
            String condCol = scanner.nextLine();

            // Fetch column info for the condition column
            List<ColumnInfo> columns = getTableColumns(conn, table);
            ColumnInfo conditionColInfo = null;
            for(ColumnInfo ci : columns) {
                if (ci.name.equalsIgnoreCase(condCol)) {
                    conditionColInfo = ci;
                    break;
                }
            }
            if (conditionColInfo == null) {
                System.out.println("Error: Condition column '" + condCol + "' not found in table metadata.");
                return;
            }

            String sql = "DELETE FROM " + table + " WHERE " + condCol + " = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                int batchCount = 0;
                while ((line = br.readLine()) != null) {
                    String value = line.trim();
                    setPreparedStatementValue(pstmt, 1, conditionColInfo.type, value);
                    pstmt.addBatch();
                    batchCount++;
                    if (batchCount % 100 == 0) { // Execute batch every 100 records
                        pstmt.executeBatch();
                        conn.commit();
                        batchCount = 0;
                    }
                }
                pstmt.executeBatch(); // Execute remaining batch
                conn.commit();
                System.out.println("Records deleted from file.");
            }
        } catch (Exception e) {
            System.out.println("Delete from file error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void truncateTable(Connection conn) {
        try {
            System.out.print("Enter table name: ");
            String table = scanner.nextLine();
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("TRUNCATE TABLE " + table);
                System.out.println("Table truncated.");
            }
        } catch (SQLException e) {
            System.out.println("Truncate error: " + e.getMessage());
        }
    }

    private static void deleteTable(Connection conn) {
        try {
            System.out.print("Enter table name: ");
            String table = scanner.nextLine();
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP TABLE " + table);
                System.out.println("Table deleted.");
            }
        } catch (SQLException e) {
            System.out.println("Delete table error: " + e.getMessage());
        }
    }

    private static void selectRecords(Connection conn) {
        try {
            System.out.print("Enter table name: ");
            String table = scanner.nextLine();
            System.out.print("Enter number of rows per page: ");
            int pageSize = Integer.parseInt(scanner.nextLine());

            int offset = 0;
            while (true) {
                // MySQL pagination uses LIMIT and OFFSET
                String sql = "SELECT * FROM " + table + " LIMIT ? OFFSET ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, pageSize);
                    pstmt.setInt(2, offset);
                    ResultSet rs = pstmt.executeQuery();
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    List<String[]> rows = new ArrayList<>();
                    int[] colWidths = new int[colCount];

                    for (int i = 0; i < colCount; i++) {
                        colWidths[i] = meta.getColumnName(i + 1).length();
                    }

                    while (rs.next()) {
                        String[] row = new String[colCount];
                        for (int i = 0; i < colCount; i++) {
                            String val = rs.getString(i + 1);
                            row[i] = (val == null) ? "null" : val;
                            colWidths[i] = Math.max(colWidths[i], row[i].length());
                        }
                        rows.add(row);
                    }

                    if (rows.isEmpty()) {
                        System.out.println("No more records.");
                        break;
                    }

                    for (int i = 0; i < colCount; i++) {
                        System.out.printf("%-" + (colWidths[i] + 2) + "s", meta.getColumnName(i + 1));
                    }
                    System.out.println();
                    for (int i = 0; i < Arrays.stream(colWidths).sum() + (2 * colCount); i++) {
                        System.out.print("-");
                    }
                    System.out.println();

                    for (String[] row : rows) {
                        for (int i = 0; i < colCount; i++) {
                            System.out.printf("%-" + (colWidths[i] + 2) + "s", row[i]);
                        }
                        System.out.println();
                    }

                    System.out.print("Next page? (y/n): ");
                    if (!scanner.nextLine().equalsIgnoreCase("y")) break;

                    offset += pageSize;
                }
            }
        } catch (Exception e) {
            System.out.println("Select error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createTable(Connection conn) throws SQLException {
        System.out.print("Enter table name: ");
        String table = scanner.nextLine();

        System.out.print("Enter number of columns: ");
        int colCount = Integer.parseInt(scanner.nextLine());
        StringBuilder sb = new StringBuilder("CREATE TABLE ").append(table).append(" (");

        for (int i = 0; i < colCount; i++) {
            System.out.print("Enter column " + (i + 1) + " name: ");
            String colName = scanner.nextLine();
            System.out.print("Enter column " + (i + 1) + " type (e.g., VARCHAR(100), INT, DECIMAL(10,2), DATE, DATETIME, TIMESTAMP, TEXT): ");
            String colType = scanner.nextLine();
            sb.append(colName).append(" ").append(colType);
            if (i != colCount - 1) sb.append(", ");
        }
        sb.append(")");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sb.toString());
            System.out.println("Table created successfully.");
        }
    }

    // Changed method name and implementation for MySQL metadata
    private static List<ColumnInfo> getTableColumns(Connection conn, String table) {
        List<ColumnInfo> list = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, null)) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                String type = rs.getString("TYPE_NAME"); // Get the data type name
                list.add(new ColumnInfo(name, type));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching column metadata: " + e.getMessage());
        }
        return list;
    }

    static class ColumnInfo {
        String name, type;

        ColumnInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    // Helper method to set PreparedStatement values based on column type
    private static void setPreparedStatementValue(PreparedStatement pstmt, int index, String type, String value) throws SQLException {
        if (value.equalsIgnoreCase("null") || value.isEmpty()) {
            pstmt.setNull(index, java.sql.Types.NULL);
            return;
        }

        try {
            if (type.toUpperCase().startsWith("INT") || type.toUpperCase().startsWith("BIGINT") || type.toUpperCase().startsWith("TINYINT")) {
                pstmt.setInt(index, Integer.parseInt(value));
            } else if (type.toUpperCase().startsWith("DECIMAL") || type.toUpperCase().startsWith("NUMERIC") || type.toUpperCase().startsWith("DOUBLE") || type.toUpperCase().startsWith("FLOAT")) {
                pstmt.setBigDecimal(index, new BigDecimal(value));
            } else if (type.equalsIgnoreCase("DATE")) {
                pstmt.setDate(index, java.sql.Date.valueOf(value));
            } else if (type.equalsIgnoreCase("DATETIME") || type.equalsIgnoreCase("TIMESTAMP")) {
                pstmt.setTimestamp(index, java.sql.Timestamp.valueOf(value));
            } else {
                pstmt.setString(index, value);
            }
        } catch (Exception e) {
            // Fallback for type conversion errors, or log and rethrow if stricter
            System.err.println("Warning: Failed to convert value '" + value + "' to type '" + type + "'. Setting as String. Error: " + e.getMessage());
            pstmt.setString(index, value);
        }
    }


    private static void createLogTableAndTriggers(Connection conn) {
        try {
            System.out.print("Enter the table name to create triggers for: ");
            String tableName = scanner.nextLine().toUpperCase();

            // Create log table if not exists
            // MySQL does not have 'NUMBER GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY'
            // Use AUTO_INCREMENT for primary key in MySQL
            String createLogTableSQL = "CREATE TABLE IF NOT EXISTS audit_log (\n" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY,\n" +
                    "    action VARCHAR(10),\n" +
                    "    table_name VARCHAR(50),\n" +
                    "    record_id VARCHAR(255),\n" + // Changed to VARCHAR for MySQL
                    "    action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                    ")";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createLogTableSQL);
                System.out.println("Audit log table created or already exists.");
            }

            // Drop existing triggers (if any), safely ignoring errors
            String[] triggerNames = {
                "trg_audit_log_insert_" + tableName,
                "trg_audit_log_update_" + tableName,
                "trg_audit_log_delete_" + tableName
            };
            for (String trgName : triggerNames) {
                try (Statement stmt = conn.createStatement()) {
                    // MySQL DROP TRIGGER syntax
                    stmt.executeUpdate("DROP TRIGGER IF EXISTS " + trgName);
                    System.out.println("Dropped existing trigger " + trgName);
                } catch (SQLException e) {
                    System.out.println("Error dropping trigger " + trgName + ": " + e.getMessage());
                }
            }

            // CREATE INSERT TRIGGER (MySQL syntax)
            // MySQL does not have :NEW.ID for dynamic column access, must assume 'ID' as primary key
            String insertTriggerSQL =
                    "CREATE TRIGGER trg_audit_log_insert_" + tableName + "\n" +
                    "BEFORE INSERT ON " + tableName + "\n" +
                    "FOR EACH ROW\n" +
                    "BEGIN\n" +
                    "    DECLARE v_rec_id VARCHAR(255);\n" +
                    "    -- Attempt to get the ID, assuming 'ID' is the primary key column\n" +
                    "    -- If 'ID' column doesn't exist, this will result in an error or NULL\n" +
                    "    SET v_rec_id = NEW.ID;\n" + // MySQL uses NEW.columnName directly
                    "    INSERT INTO audit_log(action, table_name, record_id, action_timestamp)\n" +
                    "    VALUES ('INSERT', '" + tableName + "', IFNULL(v_rec_id, 'NULL'), NOW());\n" + // MySQL's IFNULL, NOW()
                    "END;";

            // CREATE UPDATE TRIGGER (MySQL syntax)
            String updateTriggerSQL =
                    "CREATE TRIGGER trg_audit_log_update_" + tableName + "\n" +
                    "BEFORE UPDATE ON " + tableName + "\n" +
                    "FOR EACH ROW\n" +
                    "BEGIN\n" +
                    "    DECLARE v_rec_id VARCHAR(255);\n" +
                    "    SET v_rec_id = NEW.ID;\n" +
                    "    INSERT INTO audit_log(action, table_name, record_id, action_timestamp)\n" +
                    "    VALUES ('UPDATE', '" + tableName + "', IFNULL(v_rec_id, 'NULL'), NOW());\n" +
                    "END;";

            // CREATE DELETE TRIGGER (MySQL syntax)
            String deleteTriggerSQL =
                    "CREATE TRIGGER trg_audit_log_delete_" + tableName + "\n" +
                    "BEFORE DELETE ON " + tableName + "\n" +
                    "FOR EACH ROW\n" +
                    "BEGIN\n" +
                    "    DECLARE v_rec_id VARCHAR(255);\n" +
                    "    SET v_rec_id = OLD.ID;\n" + // MySQL uses OLD.columnName directly
                    "    INSERT INTO audit_log(action, table_name, record_id, action_timestamp)\n" +
                    "    VALUES ('DELETE', '" + tableName + "', IFNULL(v_rec_id, 'NULL'), NOW());\n" +
                    "END;";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(insertTriggerSQL);
                stmt.execute(updateTriggerSQL);
                stmt.execute(deleteTriggerSQL);
                System.out.println("Triggers created on table " + tableName + ".");
            } catch (SQLException e) {
                System.out.println("Error creating triggers: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (SQLException e) {
            System.out.println("Error in createLogTableAndTriggers: " + e.getMessage());
            e.printStackTrace();
        }
    }
}