import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CrudOperationsApp extends Application {

    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String DB_USER = "system";
    private static final String DB_PASS = "Bhav123";

    private Connection conn;

    private TreeView<String> treeView;
    private VBox mainPane;
    private TableView<RowData> tableView = new TableView<>();
    private ObservableList<RowData> tableData = FXCollections.observableArrayList();

    private List<String> currentColumns = null;
    private String currentTable = null;

    public static class RowData {
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        private final ObservableList<StringProperty> data;

        public RowData(List<String> data) {
            this.data = FXCollections.observableArrayList();
            for (String d : data) {
                this.data.add(new SimpleStringProperty(d));
            }
        }

        public BooleanProperty selectedProperty() {
            return selected;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean val) {
            selected.set(val);
        }

        public ObservableList<StringProperty> getData() {
            return data;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        if (!connectDB()) {
            showAlert("Failed to connect to DB. Verify credentials and DB server status.");
            return;
        }

        treeView = new TreeView<>();
        TreeItem<String> root = new TreeItem<>("Operations");
        root.setExpanded(true);
        String[] ops = {"Create", "Insert", "Update", "Delete", "Drop", "Truncate", "Select"};
        for (String op : ops) root.getChildren().add(new TreeItem<>(op));
        treeView.setRoot(root);
        treeView.setPrefWidth(160);

        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) {
                    switch (item) {
                        case "Create":
                            setTextFill(Color.DARKGREEN);
                            break;
                        case "Insert":
                            setTextFill(Color.DARKBLUE);
                            break;
                        case "Update":
                            setTextFill(Color.ORANGE);
                            break;
                        case "Delete":
                            setTextFill(Color.RED);
                            break;
                        case "Drop":
                            setTextFill(Color.DARKRED);
                            break;
                        case "Truncate":
                            setTextFill(Color.PURPLE);
                            break;
                        case "Select":
                            setTextFill(Color.DARKCYAN);
                            break;
                        default:
                            setTextFill(Color.BLACK);
                    }
                } else
                    setTextFill(Color.BLACK);
            }
        });

        mainPane = new VBox(12);
        mainPane.setPadding(new Insets(12));
        mainPane.getChildren().add(new Label("Select operation from the left"));

        HBox rootLayout = new HBox(treeView, mainPane);
        rootLayout.setSpacing(10);
        rootLayout.setPadding(new Insets(10));

        Scene scene = new Scene(rootLayout, 1000, 650);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Oracle DB CRUD Operations with JavaFX");
        primaryStage.show();

        tableView.setEditable(true);

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.getValue() == null) return;

            mainPane.getChildren().clear();
            currentColumns = null;
            currentTable = null;
            tableView.getColumns().clear();
            tableView.getItems().clear();
            tableData.clear();

            switch (newVal.getValue()) {
                case "Create":
                    showCreateTableUI();
                    break;
                case "Insert":
                    showInsertUI();
                    break;
                case "Update":
                    showUpdateUI();
                    break;
                case "Delete":
                    showDeleteUI();
                    break;
                case "Drop":
                    showDropUI();
                    break;
                case "Truncate":
                    showTruncateUI();
                    break;
                case "Select":
                    showSelectUI();
                    break;
                default:
                    mainPane.getChildren().add(new Label("Select operation"));
            }
        });
    }

    private boolean connectDB() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            System.out.println("Connected to Oracle DB...");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadTablesInto(ComboBox<String> comboBox) {
        comboBox.getItems().clear();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT table_name FROM user_tables ORDER BY table_name")) {
            while (rs.next()) comboBox.getItems().add(rs.getString(1));
        } catch (SQLException ex) {
            showAlert("Error loading tables: " + ex.getMessage());
        }
    }

    private List<String> getColumnsForTable(String table) {
        List<String> colList = new ArrayList<>();
        String sql = "SELECT column_name FROM user_tab_columns WHERE table_name = ? ORDER BY column_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) colList.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            showAlert("Error getting columns: " + ex.getMessage());
            return null;
        }
        return colList;
    }

    private void showCreateTableUI() {
        Label title = new Label("Create Table");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        TextField tblNameField = new TextField();
        tblNameField.setPromptText("Table Name");

        TextArea columnsArea = new TextArea();
        columnsArea.setPromptText("One column per line, e.g.:\n id NUMBER PRIMARY KEY\n name VARCHAR2(50)");
        columnsArea.setPrefRowCount(6);

        Button createBtn = new Button("Create Table");
        createBtn.setOnAction(e -> {
            String tname = tblNameField.getText().trim();
            String cols = columnsArea.getText().trim();
            if (tname.isEmpty() || cols.isEmpty()) {
                showAlert("Table name and columns cannot be empty!");
                return;
            }
            String sql = "CREATE TABLE " + tname + " (" + cols.replace("\n", ",") + ")";
            try (Statement st = conn.createStatement()) {
                st.execute(sql);
                showAlert("Table " + tname + " created successfully.");
                tblNameField.clear();
                columnsArea.clear();
            } catch (SQLException ex) {
                showAlert("Creation failed: " + ex.getMessage());
            }
        });

        VBox vbox = new VBox(10, title, new Label("Table Name:"), tblNameField,
                new Label("Columns (one per line):"), columnsArea, createBtn);
        mainPane.getChildren().add(vbox);
    }

    private void showInsertUI() {
        Label title = new Label("Insert into Table");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        ComboBox<String> cbTables = new ComboBox<>();
        loadTablesInto(cbTables);

        VBox inputsBox = new VBox(5);
        Button btnInsert = new Button("Insert");
        btnInsert.setDisable(true);

        cbTables.setOnAction(e -> {
            String selected = cbTables.getSelectionModel().getSelectedItem();
            inputsBox.getChildren().clear();
            btnInsert.setDisable(true);
            if (selected != null) {
                currentTable = selected;
                currentColumns = getColumnsForTable(selected);
                if (currentColumns == null || currentColumns.isEmpty()) {
                    showAlert("Failed to get columns for table " + selected);
                    return;
                }
                for (String col : currentColumns) {
                    TextField tf = new TextField();
                    tf.setPromptText(col);
                    inputsBox.getChildren().add(tf);
                }
                btnInsert.setDisable(false);
            }
        });

        btnInsert.setOnAction(e -> {
            if (currentTable == null || currentColumns == null) {
                showAlert("Select table first");
                return;
            }
            List<String> values = new ArrayList<>();
            for (Node node : inputsBox.getChildren()) {
                TextField tf = (TextField) node;
                values.add(tf.getText().trim());
            }
            try {
                String placeholders = String.join(",", Collections.nCopies(values.size(), "?"));
                String sql = "INSERT INTO " + currentTable + " (" + String.join(",", currentColumns) + ") VALUES (" + placeholders + ")";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 0; i < values.size(); i++) ps.setString(i + 1, values.get(i));
                    int inserted = ps.executeUpdate();
                    if (inserted > 0) {
                        showAlert("Row inserted successfully.");
                        cbTables.getOnAction().handle(null); // reload inputs
                    }
                }
            } catch (SQLException ex) {
                showAlert("Insert error: " + ex.getMessage());
            }
        });

        VBox vbox = new VBox(10, title, new Label("Select Table:"), cbTables,
                new Label("Enter Values:"), inputsBox, btnInsert);
        mainPane.getChildren().add(vbox);
    }

    private void showSelectUI() {
        Label title = new Label("Select Data");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        ComboBox<String> tablesCombo = new ComboBox<>();
        loadTablesInto(tablesCombo);

        Button loadBtn = new Button("Load Data");
        loadBtn.setDisable(true);

        tablesCombo.setOnAction(e -> loadBtn.setDisable(tablesCombo.getSelectionModel().getSelectedItem() == null));

        loadBtn.setOnAction(e -> {
            String selected = tablesCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            currentTable = selected;
            currentColumns = getColumnsForTable(selected);
            if (currentColumns == null || currentColumns.isEmpty()) {
                showAlert("Failed to get columns.");
                return;
            }
            loadTableData(selected);
        });

        VBox vbox = new VBox(10, title, new Label("Select Table:"), tablesCombo, loadBtn, tableView);
        mainPane.getChildren().add(vbox);
    }

    private void loadTableData(String table) {
        tableView.getColumns().clear();
        tableData.clear();

        for (int i = 0; i < currentColumns.size(); i++) {
            int index = i;
            TableColumn<RowData, String> col = new TableColumn<>(currentColumns.get(i));
            col.setCellValueFactory(cd -> cd.getValue().getData().get(index));
            col.setPrefWidth(150);
            tableView.getColumns().add(col);
        }

        String sql = "SELECT * FROM " + table;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                List<String> rowVals = new ArrayList<>();
                for (String c : currentColumns) {
                    rowVals.add(rs.getString(c));
                }
                tableData.add(new RowData(rowVals));
            }
            tableView.setItems(tableData);
        } catch (SQLException ex) {
            showAlert("Failed to load data: " + ex.getMessage());
        }
    }

    private void showUpdateUI() {
        Label title = new Label("Update Rows");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        ComboBox<String> tablesCombo = new ComboBox<>();
        loadTablesInto(tablesCombo);

        Button loadBtn = new Button("Load Data");
        loadBtn.setDisable(true);

        Button updateBtn = new Button("Update Selected Row");
        updateBtn.setDisable(true);

        tablesCombo.setOnAction(e -> loadBtn.setDisable(tablesCombo.getSelectionModel().getSelectedItem() == null));

        loadBtn.setOnAction(e -> {
            String selected = tablesCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            currentTable = selected;
            currentColumns = getColumnsForTable(selected);
            if (currentColumns == null || currentColumns.isEmpty()) {
                showAlert("Could not retrieve columns.");
                return;
            }
            loadTableData(selected);
            updateBtn.setDisable(false);
        });

        updateBtn.setOnAction(e -> {
            RowData selectedRow = tableView.getSelectionModel().getSelectedItem();
            if (selectedRow == null) {
                showAlert("Select a row to update.");
                return;
            }
            showUpdateDialog(selectedRow);
        });

        VBox vbox = new VBox(10, title, new Label("Select Table:"), tablesCombo, loadBtn, tableView, updateBtn);
        mainPane.getChildren().add(vbox);
    }

    private void showUpdateDialog(RowData row) {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Update Row");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));

        List<TextField> fields = new ArrayList<>();
        for (int i = 0; i < currentColumns.size(); i++) {
            Label lbl = new Label(currentColumns.get(i));
            TextField tf = new TextField(row.getData().get(i).get());
            grid.add(lbl, 0, i);
            grid.add(tf, 1, i);
            fields.add(tf);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                List<String> vals = new ArrayList<>();
                for (TextField tf : fields) vals.add(tf.getText().trim());
                return vals;
            }
            return null;
        });

        Optional<List<String>> result = dialog.showAndWait();

        result.ifPresent(vals -> {
            try {
                String pkCol = currentColumns.get(0); // assume first column is PK
                StringBuilder sql = new StringBuilder("UPDATE " + currentTable + " SET ");
                for (int i = 1; i < currentColumns.size(); i++) {
                    sql.append(currentColumns.get(i)).append(" = ?");
                    if (i < currentColumns.size() - 1) sql.append(", ");
                }
                sql.append(" WHERE ").append(pkCol).append(" = ?");
                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    int idx = 1;
                    for (int i = 1; i < vals.size(); i++) ps.setString(idx++, vals.get(i));
                    ps.setString(idx, vals.get(0)); // PK condition
                    int updated = ps.executeUpdate();
                    if (updated > 0) {
                        showAlert("Row updated successfully.");
                        loadTableData(currentTable);
                    }
                }
            } catch (SQLException ex) {
                showAlert("Update failed: " + ex.getMessage());
            }
        });
    }

    private void showDeleteUI() {
        Label title = new Label("Delete Rows");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        ComboBox<String> tablesCombo = new ComboBox<>();
        loadTablesInto(tablesCombo);

        Button loadBtn = new Button("Load Data");
        loadBtn.setDisable(true);

        Button deleteBtn = new Button("Delete Selected Rows");
        deleteBtn.setDisable(true);

        tablesCombo.setOnAction(e -> loadBtn.setDisable(tablesCombo.getSelectionModel().getSelectedItem() == null));

        loadBtn.setOnAction(e -> {
            String selected = tablesCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            currentTable = selected;
            currentColumns = getColumnsForTable(selected);
            if (currentColumns == null || currentColumns.isEmpty()) {
                showAlert("Could not retrieve columns.");
                return;
            }
            loadTableDataWithCheckboxes(selected);
            deleteBtn.setDisable(false);
        });

        deleteBtn.setOnAction(e -> {
            List<RowData> selectedRows = new ArrayList<>();
            for (RowData row : tableData) {
                if (row.isSelected()) selectedRows.add(row);
            }
            if (selectedRows.isEmpty()) {
                showAlert("No rows selected for deletion.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Deletion");
            confirm.setHeaderText(null);
            confirm.setContentText("Delete " + selectedRows.size() + " selected row(s)?");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) return;

            try {
                String pkCol = currentColumns.get(0); // assume first column is PK
                String sql = "DELETE FROM " + currentTable + " WHERE " + pkCol + " = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (RowData rd : selectedRows) {
                        ps.setString(1, rd.getData().get(0).get());
                        ps.addBatch();
                    }
                    int[] results = ps.executeBatch();
                    showAlert(results.length + " row(s) deleted.");
                    loadTableDataWithCheckboxes(currentTable);
                }
            } catch (SQLException ex) {
                showAlert("Delete failed: " + ex.getMessage());
            }
        });

        VBox vbox = new VBox(10, title, new Label("Select Table:"), tablesCombo, loadBtn, tableView, deleteBtn);
        mainPane.getChildren().add(vbox);
    }

    private void loadTableDataWithCheckboxes(String table) {
        tableView.getColumns().clear();
        tableData.clear();

        TableColumn<RowData, Boolean> checkboxCol = new TableColumn<>();
        CheckBox selectAllCheckbox = new CheckBox();
        selectAllCheckbox.setPadding(new Insets(0, 0, 0, 5));
        checkboxCol.setGraphic(selectAllCheckbox);
        checkboxCol.setPrefWidth(50);
        checkboxCol.setEditable(true);
        checkboxCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        checkboxCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkboxCol));
        checkboxCol.setStyle("-fx-alignment: CENTER;");

        tableView.setEditable(true);
        tableView.getColumns().add(checkboxCol);

        for (int i = 0; i < currentColumns.size(); i++) {
            final int index = i;
            TableColumn<RowData, String> col = new TableColumn<>(currentColumns.get(i));
            col.setCellValueFactory(cd -> cd.getValue().getData().get(index));
            col.setPrefWidth(140);
            tableView.getColumns().add(col);
        }

        String sql = "SELECT * FROM " + table;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                List<String> rowVals = new ArrayList<>();
                for (String c : currentColumns) {
                    rowVals.add(rs.getString(c));
                }
                RowData rd = new RowData(rowVals);
                rd.selectedProperty().addListener((obs, oldV, newV) -> updateSelectAllCheckbox(selectAllCheckbox));
                tableData.add(rd);
            }
            tableView.setItems(tableData);
        } catch (SQLException ex) {
            showAlert("Failed to load data: " + ex.getMessage());
        }

        selectAllCheckbox.setOnAction(e -> {
            boolean newVal = selectAllCheckbox.isSelected();
            for (RowData row : tableData) {
                row.setSelected(newVal);
            }
            tableView.refresh();
        });

        updateSelectAllCheckbox(selectAllCheckbox);
    }

    private void updateSelectAllCheckbox(CheckBox selectAllCheckbox) {
        if (tableData.isEmpty()) {
            selectAllCheckbox.setIndeterminate(false);
            selectAllCheckbox.setSelected(false);
            return;
        }
        long selectedCount = tableData.stream().filter(RowData::isSelected).count();
        if (selectedCount == tableData.size()) {
            selectAllCheckbox.setIndeterminate(false);
            selectAllCheckbox.setSelected(true);
        } else if (selectedCount == 0) {
            selectAllCheckbox.setIndeterminate(false);
            selectAllCheckbox.setSelected(false);
        } else {
            selectAllCheckbox.setIndeterminate(true);
        }
    }

    private void showDropUI() {
        Label title = new Label("Drop Table");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        ComboBox<String> tablesCombo = new ComboBox<>();
        loadTablesInto(tablesCombo);

        Button dropBtn = new Button("Drop Table");
        dropBtn.setDisable(true);

        tablesCombo.setOnAction(e -> dropBtn.setDisable(tablesCombo.getSelectionModel().getSelectedItem() == null));

        dropBtn.setOnAction(e -> {
            String selected = tablesCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Drop");
            confirm.setHeaderText(null);
            confirm.setContentText("Drop table " + selected + "? This action cannot be undone.");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) return;

            try (Statement st = conn.createStatement()) {
                st.execute("DROP TABLE " + selected + " CASCADE CONSTRAINTS");
                showAlert("Dropped table " + selected);
                tablesCombo.getItems().remove(selected);
                dropBtn.setDisable(true);
            } catch (SQLException ex) {
                showAlert("Drop failed: " + ex.getMessage());
            }
        });

        VBox vbox = new VBox(10, title, new Label("Select Table:"), tablesCombo, dropBtn);
        mainPane.getChildren().add(vbox);
    }

    private void showTruncateUI() {
        Label title = new Label("Truncate Table");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");

        ComboBox<String> tablesCombo = new ComboBox<>();
        loadTablesInto(tablesCombo);

        Button truncateBtn = new Button("Truncate Table");
        truncateBtn.setDisable(true);

        tablesCombo.setOnAction(e -> truncateBtn.setDisable(tablesCombo.getSelectionModel().getSelectedItem() == null));

        truncateBtn.setOnAction(e -> {
            String selected = tablesCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Truncate");
            confirm.setHeaderText(null);
            confirm.setContentText("Truncate table " + selected + "? This action cannot be undone.");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) return;

            try (Statement st = conn.createStatement()) {
                st.execute("TRUNCATE TABLE " + selected);
                showAlert("Truncated table " + selected);
            } catch (SQLException ex) {
                showAlert("Truncate failed: " + ex.getMessage());
            }
        });

        VBox vbox = new VBox(10, title, new Label("Select Table:"), tablesCombo, truncateBtn);
        mainPane.getChildren().add(vbox);
    }

    public static void main(String[] args) {
        launch(args);
    }
}