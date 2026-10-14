import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.sql.PreparedStatement;
import javafx.scene.control.TextInputDialog;

public class GUI extends Application {
    private Connection getConnection() throws Exception {
        String url = "jdbc:mysql://localhost:3306/canteen_db";
        String user = "root";
        String password = System.getenv("DB_PASSWORD");
        System.out.println("DB_PASSWORD loaded: " + (password != null && !password.isEmpty()));

        return DriverManager.getConnection(url, user, password);
    }
    @Override
    public void start(Stage stage) {

        Label heading = new Label("Canteen Order Management");
        heading.setStyle(
                "-fx-font-size: 26px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #142D4E;"
        );

        Label subtitle = new Label(
                "Manage your canteen orders in one place"
        );

        Button menuButton = new Button("View Menu");
        Button orderButton = new Button("Place Order");
        Button historyButton = new Button("Order History");
        Button adminButton = new Button("Admin Dashboard");
        Button updateStatusButton = new Button("Update Order Status");
        Button cancelOrderButton = new Button("Cancel Order");
        Button manageMenuButton = new Button("Manage Menu");
        Button editMenuButton = new Button("Edit Menu Item");
        Button deleteMenuButton = new Button("Delete Menu Item");
        Button orderDetailsButton = new Button("View Order Details");
        Button searchMenuButton = new Button("Search Menu Item");
        Button availabilityButton = new Button("Change Item Availability");
        Button salesButton = new Button("View Total Sales");
        Button studentsButton = new Button("View All Students");
        Button registerStudentButton = new Button("Register New Student");
        Button searchStudentButton = new Button("Search Student");
        Button editStudentButton = new Button("Edit Student Details");
        Button deleteStudentButton = new Button("Delete Student");

        menuButton.setOnAction(e -> {
            try (Connection con = getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT item_id, item_name, price, category, available FROM menu")) {

                StringBuilder menu = new StringBuilder();

                while (rs.next()) {
                    menu.append("ID: ").append(rs.getInt("item_id"))
                            .append("\nItem: ").append(rs.getString("item_name"))
                            .append("\nPrice: ₹").append(rs.getDouble("price"))
                            .append("\nCategory: ").append(rs.getString("category"))
                            .append("\nAvailable: ").append(rs.getBoolean("available"))
                            .append("\n-------------------------\n");
                }
                javafx.scene.control.TextArea textArea =
                        new javafx.scene.control.TextArea(menu.toString());

                textArea.setEditable(false);
                textArea.setWrapText(false);
                textArea.setPrefSize(600, 450);

                javafx.scene.control.Dialog<Void> dialog =
                        new javafx.scene.control.Dialog<>();

                dialog.setTitle("Canteen Menu");
                dialog.setHeaderText("All Menu Items");
                dialog.getDialogPane().setContent(textArea);
                dialog.getDialogPane().getButtonTypes().add(
                        javafx.scene.control.ButtonType.CLOSE
                );
                dialog.showAndWait();

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Could not load menu: " + ex.getMessage()).showAndWait();
            }
        });

        orderButton.setOnAction(e -> {
            TextInputDialog studentDialog = new TextInputDialog();
            studentDialog.setTitle("Place Order");
            studentDialog.setHeaderText("Enter Student ID");
            studentDialog.setContentText("Student ID:");

            studentDialog.showAndWait().ifPresent(studentIdText -> {
                try {
                    int studentId = Integer.parseInt(studentIdText);

                    TextInputDialog itemDialog = new TextInputDialog();
                    itemDialog.setTitle("Place Order");
                    itemDialog.setHeaderText("Enter Food Item ID");
                    itemDialog.setContentText("Item ID:");

                    itemDialog.showAndWait().ifPresent(itemIdText -> {
                        try {
                            int itemId = Integer.parseInt(itemIdText);

                            TextInputDialog quantityDialog = new TextInputDialog("1");
                            quantityDialog.setTitle("Place Order");
                            quantityDialog.setHeaderText("Enter Quantity");
                            quantityDialog.setContentText("Quantity:");

                            quantityDialog.showAndWait().ifPresent(quantityText -> {
                                try {
                                    int quantity = Integer.parseInt(quantityText);

                                    if (quantity <= 0) {
                                        throw new IllegalArgumentException(
                                                "Quantity must be greater than zero.");
                                    }

                                    try (Connection con = getConnection()) {
                                        con.setAutoCommit(false);

                                        try {
                                            double price;

                                            try (PreparedStatement ps = con.prepareStatement(
                                                    "SELECT price FROM menu " +
                                                            "WHERE item_id = ? AND available = 1")) {
                                                ps.setInt(1, itemId);

                                                try (ResultSet rs = ps.executeQuery()) {
                                                    if (!rs.next()) {
                                                        throw new Exception(
                                                                "Food item not found or unavailable.");
                                                    }
                                                    price = rs.getDouble("price");
                                                }
                                            }

                                            try (PreparedStatement ps = con.prepareStatement(
                                                    "SELECT student_id FROM students " +
                                                            "WHERE student_id = ?")) {
                                                ps.setInt(1, studentId);

                                                try (ResultSet rs = ps.executeQuery()) {
                                                    if (!rs.next()) {
                                                        throw new Exception(
                                                                "Student ID not found.");
                                                    }
                                                }
                                            }

                                            double total = price * quantity;
                                            int orderId;

                                            try (PreparedStatement ps = con.prepareStatement(
                                                    "INSERT INTO orders " +
                                                            "(student_id, order_date, total_amount, status) " +
                                                            "VALUES (?, NOW(), ?, 'Placed')",
                                                    Statement.RETURN_GENERATED_KEYS)) {
                                                ps.setInt(1, studentId);
                                                ps.setDouble(2, total);
                                                ps.executeUpdate();

                                                try (ResultSet keys = ps.getGeneratedKeys()) {
                                                    if (!keys.next()) {
                                                        throw new Exception(
                                                                "Could not create order.");
                                                    }
                                                    orderId = keys.getInt(1);
                                                }
                                            }

                                            try (PreparedStatement ps = con.prepareStatement(
                                                    "INSERT INTO order_items " +
                                                            "(order_id, item_id, quantity, subtotal) " +
                                                            "VALUES (?, ?, ?, ?)")) {
                                                ps.setInt(1, orderId);
                                                ps.setInt(2, itemId);
                                                ps.setInt(3, quantity);
                                                ps.setDouble(4, total);
                                                ps.executeUpdate();
                                            }

                                            con.commit();

                                            new Alert(Alert.AlertType.INFORMATION,
                                                    "Order placed successfully!\n" +
                                                            "Order ID: " + orderId +
                                                            "\nTotal: ₹" + total).showAndWait();

                                        } catch (Exception ex) {
                                            con.rollback();
                                            throw ex;
                                        } finally {
                                            con.setAutoCommit(true);
                                        }
                                    }

                                } catch (Exception ex) {
                                    new Alert(Alert.AlertType.ERROR,
                                            "Could not place order: " +
                                                    ex.getMessage()).showAndWait();
                                }
                            });

                        } catch (Exception ex) {
                            new Alert(Alert.AlertType.ERROR,
                                    "Invalid Item ID.").showAndWait();
                        }
                    });

                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Invalid Student ID.").showAndWait();
                }
            });
        });

        historyButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Order History");
            dialog.setHeaderText("View Student Order History");
            dialog.setContentText("Enter Student ID:");

            dialog.showAndWait().ifPresent(studentIdText -> {
                try {
                    int studentId = Integer.parseInt(studentIdText);

                    try (Connection con = getConnection();
                         PreparedStatement ps = con.prepareStatement(
                                 "SELECT order_id, order_date, total_amount, status " +
                                         "FROM orders WHERE student_id = ? " +
                                         "ORDER BY order_date DESC")) {

                        ps.setInt(1, studentId);

                        try (ResultSet rs = ps.executeQuery()) {
                            StringBuilder history = new StringBuilder();

                            while (rs.next()) {
                                history.append("Order ID: ")
                                        .append(rs.getInt("order_id"))
                                        .append("\nDate: ")
                                        .append(rs.getTimestamp("order_date"))
                                        .append("\nTotal: ₹")
                                        .append(rs.getDouble("total_amount"))
                                        .append("\nStatus: ")
                                        .append(rs.getString("status"))
                                        .append("\n-------------------------\n");
                            }

                            javafx.scene.control.TextArea area =
                                    new javafx.scene.control.TextArea(
                                            history.length() == 0
                                                    ? "No orders found."
                                                    : history.toString());

                            area.setEditable(false);
                            area.setPrefSize(600, 400);

                            javafx.scene.control.Alert alert =
                                    new javafx.scene.control.Alert(
                                            javafx.scene.control.Alert.AlertType.INFORMATION);

                            alert.setTitle("Order History");
                            alert.setHeaderText("Previous Orders");
                            alert.getDialogPane().setContent(area);
                            alert.getDialogPane().setPrefWidth(650);
                            alert.showAndWait();
                        }
                    }
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Could not load order history: " +
                                    ex.getMessage()).showAndWait();
                }
            });
        });
        adminButton.setOnAction(e -> {
            try (Connection con = getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT order_id, student_id, order_date, " +
                                 "total_amount, status FROM orders " +
                                 "ORDER BY order_date DESC")) {

                StringBuilder orders = new StringBuilder();

                while (rs.next()) {
                    orders.append("Order ID: ")
                            .append(rs.getInt("order_id"))
                            .append("\nStudent ID: ")
                            .append(rs.getInt("student_id"))
                            .append("\nDate: ")
                            .append(rs.getTimestamp("order_date"))
                            .append("\nTotal: ₹")
                            .append(rs.getDouble("total_amount"))
                            .append("\nStatus: ")
                            .append(rs.getString("status"))
                            .append("\n-------------------------\n");
                }

                javafx.scene.control.TextArea area =
                        new javafx.scene.control.TextArea(
                                orders.length() == 0
                                        ? "No orders found."
                                        : orders.toString());

                area.setEditable(false);
                area.setPrefSize(650, 450);

                javafx.scene.control.Alert alert =
                        new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION);

                alert.setTitle("Admin Dashboard");
                alert.setHeaderText("All Canteen Orders");
                alert.getDialogPane().setContent(area);
                alert.getDialogPane().setPrefWidth(700);
                alert.showAndWait();

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Could not load orders: " + ex.getMessage())
                        .showAndWait();
            }
        });

        updateStatusButton.setPrefSize(200, 50);
        updateStatusButton.setStyle(
                "-fx-background-color: #168C85;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;"
        );

        updateStatusButton.setOnAction(e -> {
            TextInputDialog idDialog = new TextInputDialog();
            idDialog.setTitle("Update Order Status");
            idDialog.setHeaderText("Enter Order ID");
            idDialog.setContentText("Order ID:");

            idDialog.showAndWait().ifPresent(idText -> {
                try {
                    int orderId = Integer.parseInt(idText);

                    javafx.scene.control.ChoiceDialog<String> statusDialog =
                            new javafx.scene.control.ChoiceDialog<>(
                                    "Preparing",
                                    "Placed", "Preparing", "Ready",
                                    "Completed", "Cancelled");

                    statusDialog.setTitle("Update Status");
                    statusDialog.setHeaderText("Choose the new status");
                    statusDialog.setContentText("Status:");

                    statusDialog.showAndWait().ifPresent(status -> {
                        try (Connection con = getConnection();
                             PreparedStatement ps = con.prepareStatement(
                                     "UPDATE orders SET status = ? WHERE order_id = ?")) {

                            ps.setString(1, status);
                            ps.setInt(2, orderId);

                            int updated = ps.executeUpdate();

                            if (updated > 0) {
                                new Alert(Alert.AlertType.INFORMATION,
                                        "Order status updated to " + status)
                                        .showAndWait();
                            } else {
                                new Alert(Alert.AlertType.WARNING,
                                        "Order ID not found.")
                                        .showAndWait();
                            }

                        } catch (Exception ex) {
                            new Alert(Alert.AlertType.ERROR,
                                    "Error: " + ex.getMessage()).showAndWait();
                        }
                    });

                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Please enter a valid Order ID.").showAndWait();
                }
            });
        });
        cancelOrderButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Cancel Order");
            dialog.setHeaderText("Cancel an Existing Order");
            dialog.setContentText("Enter Order ID:");

            dialog.showAndWait().ifPresent(idText -> {
                try {
                    int orderId = Integer.parseInt(idText);

                    try (Connection con = getConnection();
                         PreparedStatement ps = con.prepareStatement(
                                 "UPDATE orders SET status = 'Cancelled' " +
                                         "WHERE order_id = ? AND status NOT IN " +
                                         "('Completed', 'Cancelled')")) {

                        ps.setInt(1, orderId);
                        int updated = ps.executeUpdate();

                        if (updated > 0) {
                            new Alert(Alert.AlertType.INFORMATION,
                                    "Order cancelled successfully!")
                                    .showAndWait();
                        } else {
                            new Alert(Alert.AlertType.WARNING,
                                    "Order not found or cannot be cancelled.")
                                    .showAndWait();
                        }

                    }
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Could not cancel order: " + ex.getMessage())
                            .showAndWait();
                }
            });
        });

        manageMenuButton.setOnAction(e -> {
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("Manage Menu");
            nameDialog.setHeaderText("Add a New Food Item");
            nameDialog.setContentText("Food Name:");

            nameDialog.showAndWait().ifPresent(name -> {
                TextInputDialog priceDialog = new TextInputDialog();
                priceDialog.setTitle("Manage Menu");
                priceDialog.setHeaderText("Enter Food Price");
                priceDialog.setContentText("Price:");

                priceDialog.showAndWait().ifPresent(priceText -> {
                    TextInputDialog categoryDialog = new TextInputDialog();
                    categoryDialog.setTitle("Manage Menu");
                    categoryDialog.setHeaderText("Enter Food Category");
                    categoryDialog.setContentText("Category:");

                    categoryDialog.showAndWait().ifPresent(category -> {
                        try {
                            double price = Double.parseDouble(priceText);

                            if (name.trim().isEmpty()
                                    || category.trim().isEmpty()
                                    || price <= 0) {
                                throw new IllegalArgumentException(
                                        "Enter a valid name, category, and price.");
                            }

                            try (Connection con = getConnection();
                                 PreparedStatement ps = con.prepareStatement(
                                         "INSERT INTO menu " +
                                                 "(item_name, price, available, category) " +
                                                 "VALUES (?, ?, 1, ?)")) {

                                ps.setString(1, name.trim());
                                ps.setDouble(2, price);
                                ps.setString(3, category.trim());
                                ps.executeUpdate();

                                new Alert(Alert.AlertType.INFORMATION,
                                        "Food item added successfully!")
                                        .showAndWait();

                            }
                        } catch (Exception ex) {
                            new Alert(Alert.AlertType.ERROR,
                                    "Could not add item: " + ex.getMessage())
                                    .showAndWait();
                        }
                    });
                });
            });
        });
        editMenuButton.setOnAction(e -> {
            TextInputDialog idDialog = new TextInputDialog();
            idDialog.setTitle("Edit Menu Item");
            idDialog.setHeaderText("Enter the Food Item ID");
            idDialog.setContentText("Item ID:");

            idDialog.showAndWait().ifPresent(idText -> {
                try {
                    int itemId = Integer.parseInt(idText);

                    TextInputDialog priceDialog = new TextInputDialog();
                    priceDialog.setTitle("Edit Menu Item");
                    priceDialog.setHeaderText("Enter the New Price");
                    priceDialog.setContentText("Price:");

                    priceDialog.showAndWait().ifPresent(priceText -> {
                        try {
                            double price = Double.parseDouble(priceText);

                            if (price <= 0) {
                                throw new IllegalArgumentException(
                                        "Price must be greater than zero.");
                            }

                            try (Connection con = getConnection();
                                 PreparedStatement ps = con.prepareStatement(
                                         "UPDATE menu SET price = ? WHERE item_id = ?")) {

                                ps.setDouble(1, price);
                                ps.setInt(2, itemId);

                                int updated = ps.executeUpdate();

                                if (updated > 0) {
                                    new Alert(Alert.AlertType.INFORMATION,
                                            "Food item price updated successfully!")
                                            .showAndWait();
                                } else {
                                    new Alert(Alert.AlertType.WARNING,
                                            "Food item ID not found.")
                                            .showAndWait();
                                }
                            }
                        } catch (Exception ex) {
                            new Alert(Alert.AlertType.ERROR,
                                    "Could not update price: " + ex.getMessage())
                                    .showAndWait();
                        }
                    });
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Please enter a valid Item ID.")
                            .showAndWait();
                }
            });
        });

        deleteMenuButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Delete Menu Item");
            dialog.setHeaderText("Remove a Food Item");
            dialog.setContentText("Enter Item ID:");

            dialog.showAndWait().ifPresent(idText -> {
                try {
                    int itemId = Integer.parseInt(idText);

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Deletion");
                    confirm.setHeaderText("Delete Menu Item?");
                    confirm.setContentText("Are you sure you want to delete Item ID: "
                            + itemId + "?");

                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            try (Connection con = getConnection();
                                 PreparedStatement ps = con.prepareStatement(
                                         "DELETE FROM menu WHERE item_id = ?")) {

                                ps.setInt(1, itemId);
                                int deleted = ps.executeUpdate();

                                if (deleted > 0) {
                                    new Alert(Alert.AlertType.INFORMATION,
                                            "Menu item deleted successfully!")
                                            .showAndWait();
                                } else {
                                    new Alert(Alert.AlertType.WARNING,
                                            "Item ID not found.")
                                            .showAndWait();
                                }

                            } catch (Exception ex) {
                                new Alert(Alert.AlertType.ERROR,
                                        "Could not delete item: "
                                                + ex.getMessage())
                                        .showAndWait();
                            }
                        }
                    });

                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Please enter a valid Item ID.")
                            .showAndWait();
                }
            });
        });
        orderDetailsButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("View Order Details");
            dialog.setHeaderText("View Food Items in an Order");
            dialog.setContentText("Enter Order ID:");

            dialog.showAndWait().ifPresent(idText -> {
                try {
                    int orderId = Integer.parseInt(idText);

                    try (Connection con = getConnection();
                         PreparedStatement ps = con.prepareStatement(
                                 "SELECT m.item_name, oi.quantity, " +
                                         "oi.subtotal FROM order_items oi " +
                                         "JOIN menu m ON oi.item_id = m.item_id " +
                                         "WHERE oi.order_id = ?")) {

                        ps.setInt(1, orderId);

                        try (ResultSet rs = ps.executeQuery()) {
                            StringBuilder details = new StringBuilder();

                            while (rs.next()) {
                                details.append("Food: ")
                                        .append(rs.getString("item_name"))
                                        .append("\nQuantity: ")
                                        .append(rs.getInt("quantity"))
                                        .append("\nSubtotal: ₹")
                                        .append(rs.getDouble("subtotal"))
                                        .append("\n-------------------------\n");
                            }

                            javafx.scene.control.TextArea area =
                                    new javafx.scene.control.TextArea(
                                            details.length() == 0
                                                    ? "No items found for this order."
                                                    : details.toString());

                            area.setEditable(false);
                            area.setPrefSize(450, 300);

                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Order Details");
                            alert.setHeaderText("Order ID: " + orderId);
                            alert.getDialogPane().setContent(area);
                            alert.showAndWait();
                        }
                    }
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Could not load order details: " + ex.getMessage())
                            .showAndWait();
                }
            });
        });
        searchMenuButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Search Menu");
            dialog.setHeaderText("Find a Food Item");
            dialog.setContentText("Enter Food Name:");

            dialog.showAndWait().ifPresent(search -> {
                try (Connection con = getConnection();
                     PreparedStatement ps = con.prepareStatement(
                             "SELECT item_id, item_name, price, category, available " +
                                     "FROM menu WHERE item_name LIKE ?")) {

                    ps.setString(1, "%" + search.trim() + "%");

                    try (ResultSet rs = ps.executeQuery()) {
                        StringBuilder results = new StringBuilder();

                        while (rs.next()) {
                            results.append("ID: ")
                                    .append(rs.getInt("item_id"))
                                    .append("\nName: ")
                                    .append(rs.getString("item_name"))
                                    .append("\nPrice: ₹")
                                    .append(rs.getDouble("price"))
                                    .append("\nCategory: ")
                                    .append(rs.getString("category"))
                                    .append("\nAvailable: ")
                                    .append(rs.getBoolean("available"))
                                    .append("\n-------------------------\n");
                        }

                        javafx.scene.control.TextArea area =
                                new javafx.scene.control.TextArea(
                                        results.length() == 0
                                                ? "No matching food items found."
                                                : results.toString());

                        area.setEditable(false);
                        area.setPrefSize(500, 350);

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Search Results");
                        alert.setHeaderText("Matching Menu Items");
                        alert.getDialogPane().setContent(area);
                        alert.showAndWait();
                    }

                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Search failed: " + ex.getMessage())
                            .showAndWait();
                }
            });
        });
        availabilityButton.setOnAction(e -> {
            TextInputDialog idDialog = new TextInputDialog();
            idDialog.setTitle("Change Item Availability");
            idDialog.setHeaderText("Enter Food Item ID");
            idDialog.setContentText("Item ID:");

            idDialog.showAndWait().ifPresent(idText -> {
                try {
                    int itemId = Integer.parseInt(idText);

                    javafx.scene.control.ChoiceDialog<String> statusDialog =
                            new javafx.scene.control.ChoiceDialog<>(
                                    "Available", "Available", "Unavailable");

                    statusDialog.setTitle("Change Availability");
                    statusDialog.setHeaderText("Choose Item Availability");
                    statusDialog.setContentText("Availability:");

                    statusDialog.showAndWait().ifPresent(status -> {
                        try (Connection con = getConnection();
                             PreparedStatement ps = con.prepareStatement(
                                     "UPDATE menu SET available = ? WHERE item_id = ?")) {

                            ps.setBoolean(1, status.equals("Available"));
                            ps.setInt(2, itemId);

                            int updated = ps.executeUpdate();

                            if (updated > 0) {
                                new Alert(Alert.AlertType.INFORMATION,
                                        "Item availability changed to " + status + "!")
                                        .showAndWait();
                            } else {
                                new Alert(Alert.AlertType.WARNING,
                                        "Item ID not found.")
                                        .showAndWait();
                            }

                        } catch (Exception ex) {
                            new Alert(Alert.AlertType.ERROR,
                                    "Could not update availability: "
                                            + ex.getMessage())
                                    .showAndWait();
                        }
                    });

                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Please enter a valid Item ID.")
                            .showAndWait();
                }
            });
        });
        salesButton.setOnAction(e -> {
            try (Connection con = getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COALESCE(SUM(total_amount), 0) AS total_sales " +
                                 "FROM orders WHERE status = 'Completed'")) {

                if (rs.next()) {
                    double totalSales = rs.getDouble("total_sales");

                    new Alert(Alert.AlertType.INFORMATION,
                            "Total Sales: ₹" + totalSales)
                            .showAndWait();
                }

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Could not calculate sales: " + ex.getMessage())
                        .showAndWait();
            }
        });

        studentsButton.setOnAction(e -> {
            try (Connection con = getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT student_id, name, roll_no, department, year FROM students")) {

                StringBuilder result = new StringBuilder();
                result.append("STUDENT DETAILS\n\n");

                while (rs.next()) {
                    result.append("Student ID: ")
                            .append(rs.getInt("student_id"))
                            .append("\nName: ")
                            .append(rs.getString("name"))
                            .append("\nRoll No: ")
                            .append(rs.getString("roll_no"))
                            .append("\nDepartment: ")
                            .append(rs.getString("department"))
                            .append("\nYear: ")
                            .append(rs.getInt("year"))
                            .append("\n----------------------\n");
                }

                if (result.toString().equals("STUDENT DETAILS\n\n")) {
                    result.append("No students found.");
                }

                javafx.scene.control.TextArea area =
                        new javafx.scene.control.TextArea(result.toString());
                area.setEditable(false);
                area.setWrapText(true);

                javafx.scene.control.Alert alert =
                        new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("All Students");
                alert.setHeaderText("Registered Students");
                alert.getDialogPane().setContent(area);
                alert.showAndWait();

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Could not load students: " + ex.getMessage())
                        .showAndWait();
            }
        });

        registerStudentButton.setOnAction(e -> {
            javafx.scene.control.TextInputDialog nameDialog =
                    new javafx.scene.control.TextInputDialog();
            nameDialog.setTitle("Register Student");
            nameDialog.setHeaderText("Enter student name");

            nameDialog.showAndWait().ifPresent(name -> {
                javafx.scene.control.TextInputDialog rollDialog =
                        new javafx.scene.control.TextInputDialog();
                rollDialog.setTitle("Register Student");
                rollDialog.setHeaderText("Enter roll number");

                rollDialog.showAndWait().ifPresent(roll -> {
                    javafx.scene.control.TextInputDialog deptDialog =
                            new javafx.scene.control.TextInputDialog();
                    deptDialog.setTitle("Register Student");
                    deptDialog.setHeaderText("Enter department");

                    deptDialog.showAndWait().ifPresent(dept -> {
                        javafx.scene.control.TextInputDialog yearDialog =
                                new javafx.scene.control.TextInputDialog();
                        yearDialog.setTitle("Register Student");
                        yearDialog.setHeaderText("Enter year");

                        yearDialog.showAndWait().ifPresent(yearText -> {
                            try {
                                int year = Integer.parseInt(yearText);

                                try (Connection con = getConnection();
                                     PreparedStatement ps = con.prepareStatement(
                                             "INSERT INTO students (name, roll_no, department, year) VALUES (?, ?, ?, ?)")) {

                                    ps.setString(1, name);
                                    ps.setString(2, roll);
                                    ps.setString(3, dept);
                                    ps.setInt(4, year);
                                    ps.executeUpdate();

                                    new Alert(Alert.AlertType.INFORMATION,
                                            "Student registered successfully!")
                                            .showAndWait();

                                }
                            } catch (NumberFormatException ex) {
                                new Alert(Alert.AlertType.ERROR,
                                        "Year must be a number.")
                                        .showAndWait();
                            } catch (Exception ex) {
                                new Alert(Alert.AlertType.ERROR,
                                        "Registration failed: " + ex.getMessage())
                                        .showAndWait();
                            }
                        });
                    });
                });
            });
        });

        searchStudentButton.setOnAction(e -> {
            javafx.scene.control.TextInputDialog dialog =
                    new javafx.scene.control.TextInputDialog();

            dialog.setTitle("Search Student");
            dialog.setHeaderText("Enter Student ID");

            dialog.showAndWait().ifPresent(idText -> {
                try {
                    int studentId = Integer.parseInt(idText);

                    try (Connection con = getConnection();
                         PreparedStatement ps = con.prepareStatement(
                                 "SELECT * FROM students WHERE student_id = ?")) {

                        ps.setInt(1, studentId);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String details =
                                        "Student ID: " + rs.getInt("student_id") +
                                                "\nName: " + rs.getString("name") +
                                                "\nRoll No: " + rs.getString("roll_no") +
                                                "\nDepartment: " + rs.getString("department") +
                                                "\nYear: " + rs.getInt("year");

                                new Alert(Alert.AlertType.INFORMATION,
                                        details).showAndWait();
                            } else {
                                new Alert(Alert.AlertType.INFORMATION,
                                        "Student not found.").showAndWait();
                            }
                        }
                    }
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Please enter a valid numeric Student ID.")
                            .showAndWait();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Search failed: " + ex.getMessage())
                            .showAndWait();
                }
            });
        });

        editStudentButton.setOnAction(e -> {
            javafx.scene.control.TextInputDialog idDialog =
                    new javafx.scene.control.TextInputDialog();
            idDialog.setTitle("Edit Student");
            idDialog.setHeaderText("Enter Student ID");

            idDialog.showAndWait().ifPresent(idText -> {
                try {
                    int id = Integer.parseInt(idText);

                    try (Connection con = getConnection();
                         PreparedStatement ps = con.prepareStatement(
                                 "SELECT * FROM students WHERE student_id = ?")) {

                        ps.setInt(1, id);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                new Alert(Alert.AlertType.ERROR,
                                        "Student not found.").showAndWait();
                                return;
                            }
                        }

                        javafx.scene.control.TextInputDialog nameDialog =
                                new javafx.scene.control.TextInputDialog();
                        nameDialog.setHeaderText("Enter new name");

                        nameDialog.showAndWait().ifPresent(name -> {
                            javafx.scene.control.TextInputDialog rollDialog =
                                    new javafx.scene.control.TextInputDialog();
                            rollDialog.setHeaderText("Enter new roll number");

                            rollDialog.showAndWait().ifPresent(roll -> {
                                javafx.scene.control.TextInputDialog deptDialog =
                                        new javafx.scene.control.TextInputDialog();
                                deptDialog.setHeaderText("Enter new department");

                                deptDialog.showAndWait().ifPresent(dept -> {
                                    javafx.scene.control.TextInputDialog yearDialog =
                                            new javafx.scene.control.TextInputDialog();
                                    yearDialog.setHeaderText("Enter new year");

                                    yearDialog.showAndWait().ifPresent(yearText -> {
                                        try {
                                            int year = Integer.parseInt(yearText);

                                            try (PreparedStatement update =
                                                         con.prepareStatement(
                                                                 "UPDATE students SET name=?, roll_no=?, department=?, year=? WHERE student_id=?")) {
                                                update.setString(1, name);
                                                update.setString(2, roll);
                                                update.setString(3, dept);
                                                update.setInt(4, year);
                                                update.setInt(5, id);
                                                update.executeUpdate();

                                                new Alert(Alert.AlertType.INFORMATION,
                                                        "Student details updated!")
                                                        .showAndWait();
                                            }
                                        } catch (Exception ex) {
                                            new Alert(Alert.AlertType.ERROR,
                                                    "Update failed: " + ex.getMessage())
                                                    .showAndWait();
                                        }
                                    });
                                });
                            });
                        });
                    }
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Enter a valid numeric Student ID.").showAndWait();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Error: " + ex.getMessage()).showAndWait();
                }
            });
        });

        deleteStudentButton.setOnAction(e -> {
            javafx.scene.control.TextInputDialog dialog =
                    new javafx.scene.control.TextInputDialog();

            dialog.setTitle("Delete Student");
            dialog.setHeaderText("Enter Student ID to delete");

            dialog.showAndWait().ifPresent(idText -> {
                try {
                    int id = Integer.parseInt(idText);

                    javafx.scene.control.Alert confirm =
                            new javafx.scene.control.Alert(
                                    javafx.scene.control.Alert.AlertType.CONFIRMATION,
                                    "Are you sure you want to delete Student ID " + id + "?",
                                    javafx.scene.control.ButtonType.YES,
                                    javafx.scene.control.ButtonType.NO);

                    confirm.showAndWait().ifPresent(answer -> {
                        if (answer == javafx.scene.control.ButtonType.YES) {
                            try (Connection con = getConnection();
                                 PreparedStatement ps = con.prepareStatement(
                                         "DELETE FROM students WHERE student_id = ?")) {

                                ps.setInt(1, id);
                                int rows = ps.executeUpdate();

                                new Alert(Alert.AlertType.INFORMATION,
                                        rows > 0 ? "Student deleted successfully."
                                                : "Student not found.")
                                        .showAndWait();

                            } catch (Exception ex) {
                                new Alert(Alert.AlertType.ERROR,
                                        "Deletion failed. The student may have existing orders.\n"
                                                + ex.getMessage())
                                        .showAndWait();
                            }
                        }
                    });

                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Enter a valid numeric Student ID.")
                            .showAndWait();
                }
            });
        });








        Button[] buttons = {
                menuButton, orderButton,
                historyButton, adminButton,
                updateStatusButton, cancelOrderButton,
                manageMenuButton, editMenuButton,
                deleteMenuButton, orderDetailsButton,
                searchMenuButton, availabilityButton,
                salesButton, studentsButton, registerStudentButton, searchStudentButton,
                editStudentButton, deleteStudentButton
        };

        for (Button button : buttons) {
            button.setPrefSize(200, 70);
            button.setStyle(
                    "-fx-background-color: #142D4E;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 15px;" +
                            "-fx-background-radius: 8px;"
            );
        }

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(20);
        grid.setVgap(20);

        grid.add(menuButton, 0, 0);
        grid.add(orderButton, 1, 0);
        grid.add(historyButton, 0, 1);
        grid.add(adminButton, 1, 1);
        grid.add(updateStatusButton, 0, 2);
        grid.add(cancelOrderButton, 1, 2);
        grid.add(manageMenuButton, 0, 3);
        grid.add(editMenuButton, 1, 3);
        grid.add(deleteMenuButton, 0, 4);
        grid.add(orderDetailsButton, 1, 4);
        grid.add(searchMenuButton, 0, 5);
        grid.add(availabilityButton, 1, 5);
        grid.add(salesButton, 0, 6);
        grid.add(studentsButton, 1, 6);
        grid.add(registerStudentButton, 0, 7);
        grid.add(searchStudentButton, 1, 7);
        grid.add(editStudentButton, 0, 8);
        grid.add(deleteStudentButton, 1, 8);

        javafx.scene.control.ScrollPane scrollPane =
                new javafx.scene.control.ScrollPane(grid);

        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);

        VBox top = new VBox(10, heading, subtitle);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(35));

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(scrollPane);
        root.setStyle("-fx-background-color: #F4F7FB;");

        Scene scene = new Scene(root, 800, 500);

        stage.setTitle("Canteen Management System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}