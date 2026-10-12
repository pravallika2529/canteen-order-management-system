
import java.sql.*;
import java.util.Scanner;

public class Main {
    static final String URL = "jdbc:mysql://localhost:3306/canteen_db";
    static final String USER = "root";
    static final String PASSWORD = System.getenv("DB_PASSWORD");

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {
            while (true) {
                System.out.println("\n===== CANTEEN MANAGEMENT SYSTEM =====");
                System.out.println("1. View Menu");
                System.out.println("2. Register Student");
                System.out.println("3. Place Order");
                System.out.println("4. Exit");
                System.out.println("5. View Order History");
                System.out.println("6. Update Order Status");
                System.out.println("7. View Order Details");
                System.out.println("8. Cancel Order");
                System.out.println("9. Add Menu Item");
                System.out.println("10. Update Item Price");
                System.out.println("11. Remove Menu Item");
                System.out.println("12. View All Orders");
                System.out.println("13. View Student Details");
                System.out.println("14. Search Student by ID");
                System.out.println("15. Search Food Item");
                System.out.println("16. View Daily Orders");
                System.out.println("17. View Student-wise Orders");
                System.out.println("18. View Completed/Pending Orders");
                System.out.println("19. View Total Sales");
                System.out.print("Enter choice: ");

                if (!sc.hasNextInt()) {
                    System.out.println("Please enter a valid number.");
                    sc.nextLine();
                    continue;
                }

                int choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        viewMenu(con);
                        break;
                    case 2:
                        registerStudent(con, sc);
                        break;
                    case 3:
                        placeOrder(con, sc);
                        break;
                    case 4:
                        System.out.println("Thank you!");
                        return;
                    case 5:
                        viewOrderHistory(con, sc);
                        break;
                    case 6:
                        updateOrderStatus(con, sc);
                        break;
                    case 7:
                        viewOrderDetails(con, sc);
                        break;
                    case 8:
                        cancelOrder(con, sc);
                        break;
                    case 9:
                        addMenuItem(con, sc);
                        break;
                    case 10:
                        updateItemPrice(con, sc);
                        break;
                    case 11:
                        removeMenuItem(con, sc);
                        break;
                    case 12:
                        viewAllOrders(con);
                        break;
                    case 13:
                        viewStudentDetails(con);
                        break;
                    case 14:
                        searchStudentById(con, sc);
                        break;
                    case 15:
                        searchFoodItem(con, sc);
                        break;
                    case 16:
                        viewDailyOrders(con);
                        break;
                    case 17:
                        viewStudentWiseOrders(con, sc);
                        break;
                    case 18:
                        viewOrdersByStatus(con, sc);
                        break;
                    case 19:
                        viewTotalSales(con);
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } finally {
            sc.close();
        }
    }

    static void viewMenu(Connection con) throws SQLException {
        String sql = "SELECT * FROM menu WHERE available = TRUE";
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n===== CANTEEN MENU =====");
            while (rs.next()) {
                System.out.printf("%d. %s - Rs. %.2f%n",
                        rs.getInt("item_id"),
                        rs.getString("item_name"),
                        rs.getDouble("price"));
            }
        }
    }


    static void registerStudent(Connection con, Scanner sc) {
        try {
            System.out.print("Enter Student Name: ");
            String name = sc.nextLine();

            System.out.print("Enter Roll Number: ");
            String rollNo = sc.nextLine();

            System.out.print("Enter Department: ");
            String department = sc.nextLine();

            System.out.print("Enter Year (1-4): ");
            int year = Integer.parseInt(sc.nextLine());

            if (year < 1 || year > 4) {
                System.out.println("Invalid year. Enter a year from 1 to 4.");
                return;
            }

            String sql = "INSERT INTO students "
                    + "(name, roll_no, department, year) "
                    + "VALUES (?, ?, ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, rollNo);
                ps.setString(3, department);
                ps.setInt(4, year);

                ps.executeUpdate();
                System.out.println("Student registered successfully!");
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid year. Please enter a number.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("This roll number already exists.");
        } catch (SQLException e) {
            System.out.println("Error registering student: " + e.getMessage());
        }
    }


    static void placeOrder(Connection con, Scanner sc)
            throws SQLException {
        System.out.print("Enter your student ID: ");
        int studentId = sc.nextInt();

        try (PreparedStatement check = con.prepareStatement(
                "SELECT student_id FROM students WHERE student_id = ?")) {
            check.setInt(1, studentId);
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Student not found. Register first.");
                    return;
                }
            }
        }

        double total = 0;
        int orderId;

        con.setAutoCommit(false);

        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO orders (student_id, total_amount) VALUES (?, 0)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, studentId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    orderId = keys.getInt(1);
                }
            }

            viewMenu(con);

            while (true) {
                System.out.print("Enter item ID (0 to finish): ");
                int itemId = sc.nextInt();

                if (itemId == 0) {
                    break;
                }

                System.out.print("Enter quantity: ");
                int quantity = sc.nextInt();

                if (quantity <= 0) {
                    System.out.println("Quantity must be positive.");
                    continue;
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT price FROM menu WHERE item_id = ? AND available = TRUE")) {
                    ps.setInt(1, itemId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("Invalid or unavailable item.");
                            continue;
                        }

                        double price = rs.getDouble("price");
                        double subtotal = price * quantity;

                        try (PreparedStatement insert = con.prepareStatement(
                                "INSERT INTO order_items (order_id, item_id, quantity, subtotal) VALUES (?, ?, ?, ?)")) {
                            insert.setInt(1, orderId);
                            insert.setInt(2, itemId);
                            insert.setInt(3, quantity);
                            insert.setDouble(4, subtotal);
                            insert.executeUpdate();
                        }

                        total += subtotal;
                        System.out.printf("Added. Subtotal: Rs. %.2f%n", subtotal);
                    }
                }
            }

            if (total == 0) {
                con.rollback();
                System.out.println("No items selected. Order cancelled.");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE orders SET total_amount = ? WHERE order_id = ?")) {
                ps.setDouble(1, total);
                ps.setInt(2, orderId);
                ps.executeUpdate();
            }

            con.commit();

            System.out.println("\n===== BILL =====");
            System.out.println("Order ID: " + orderId);
            System.out.printf("Total Amount: Rs. %.2f%n", total);
            System.out.println("Order Status: Placed");

        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }


    static void viewOrderHistory(Connection con, Scanner sc)
            throws SQLException {

        System.out.print("Enter your student ID: ");
        int studentId = sc.nextInt();

        String sql = "SELECT order_id, total_amount, status " +
                "FROM orders WHERE student_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n===== ORDER HISTORY =====");

                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.println("Order ID: " + rs.getInt("order_id"));
                    System.out.printf("Total: Rs. %.2f%n",
                            rs.getDouble("total_amount"));
                    System.out.println("Status: " + rs.getString("status"));
                    System.out.println("----------------------");
                }

                if (!found) {
                    System.out.println("No orders found.");
                }
            }
        }
    }


    static void updateOrderStatus(Connection con, Scanner sc)
            throws SQLException {

        System.out.print("Enter Order ID: ");
        int orderId = sc.nextInt();
        sc.nextLine();

        System.out.println("1. Placed");
        System.out.println("2. Preparing");
        System.out.println("3. Ready");
        System.out.println("4. Completed");
        System.out.print("Choose new status: ");

        int choice = sc.nextInt();

        String status;

        switch (choice) {
            case 1:
                status = "Placed";
                break;
            case 2:
                status = "Preparing";
                break;
            case 3:
                status = "Ready";
                break;
            case 4:
                status = "Completed";
                break;
            default:
                System.out.println("Invalid status choice.");
                return;
        }

        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Order status updated to " + status);
            } else {
                System.out.println("Order ID not found.");
            }
        }
    }

    static void viewOrderDetails(Connection con, Scanner sc)
            throws SQLException {

        System.out.print("Enter Order ID: ");
        int orderId = sc.nextInt();

        String sql = "SELECT m.item_name, m.price, oi.quantity, oi.subtotal " +
                "FROM order_items oi " +
                "JOIN menu m ON oi.item_id = m.item_id " +
                "WHERE oi.order_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n===== ORDER DETAILS =====");
                System.out.printf("%-20s %-10s %-10s %-10s%n",
                        "Item", "Price", "Quantity", "Subtotal");

                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.printf("%-20s Rs.%-7.2f %-10d Rs.%.2f%n",
                            rs.getString("item_name"),
                            rs.getDouble("price"),
                            rs.getInt("quantity"),
                            rs.getDouble("subtotal"));
                }

                if (!found) {
                    System.out.println("No items found for this order.");
                }
            }
        }
    }


    static void cancelOrder(Connection con, Scanner sc)
            throws SQLException {

        System.out.print("Enter Order ID to cancel: ");
        int orderId = sc.nextInt();

        String sql = "UPDATE orders SET status = 'Cancelled' " +
                "WHERE order_id = ? AND status = 'Placed'";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Order cancelled successfully.");
            } else {
                System.out.println(
                        "Order not found or cannot be cancelled."
                );
            }
        }
    }


    static void addMenuItem(Connection con, Scanner sc) throws SQLException {
        System.out.print("Enter item name: ");
        String name = sc.nextLine();

        System.out.print("Enter item price: ");
        double price = Double.parseDouble(sc.nextLine());

        System.out.print("Enter item category: ");
        String category = sc.nextLine();

        String sql = "INSERT INTO menu (item_name, price, available, category) "
                + "VALUES (?, ?, TRUE, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setString(3, category);
            ps.executeUpdate();

            System.out.println("Menu item added successfully!");
        }
    }


    static void updateItemPrice(Connection con, Scanner sc)
            throws SQLException {

        System.out.print("Enter item ID: ");
        int itemId = sc.nextInt();

        System.out.print("Enter new price: ");
        double price = sc.nextDouble();

        if (price <= 0) {
            System.out.println("Price must be greater than zero.");
            return;
        }

        String sql = "UPDATE menu SET price = ? WHERE item_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, price);
            ps.setInt(2, itemId);

            if (ps.executeUpdate() > 0) {
                System.out.println("Price updated successfully!");
            } else {
                System.out.println("Item ID not found.");
            }
        }
    }

    static void removeMenuItem(Connection con, Scanner sc)
            throws SQLException {

        System.out.print("Enter item ID to remove: ");
        int itemId = sc.nextInt();

        String sql = "UPDATE menu SET available = FALSE WHERE item_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, itemId);

            if (ps.executeUpdate() > 0) {
                System.out.println("Menu item removed successfully!");
            } else {
                System.out.println("Item ID not found.");
            }
        }
    }

    static void viewAllOrders(Connection con) throws SQLException {

        String sql = "SELECT o.order_id, s.name, o.total_amount, o.status " +
                "FROM orders o " +
                "JOIN students s ON o.student_id = s.student_id " +
                "ORDER BY o.order_id DESC";

        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n===== ALL ORDERS =====");

            System.out.printf("%-10s %-20s %-15s %-15s%n",
                    "Order ID", "Student", "Total (Rs.)", "Status");

            boolean found = false;

            while (rs.next()) {
                found = true;

                System.out.printf("%-10d %-20s %-15.2f %-15s%n",
                        rs.getInt("order_id"),
                        rs.getString("name"),
                        rs.getDouble("total_amount"),
                        rs.getString("status"));
            }

            if (!found) {
                System.out.println("No orders available.");
            }
        }
    }

    static void viewStudentDetails(Connection con) throws SQLException {
        String sql = "SELECT * FROM students";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n--- Student Details ---");

            while (rs.next()) {
                System.out.println(
                        "ID: " + rs.getInt("student_id")
                                + " | Name: " + rs.getString("name")
                                + " | Roll No: " + rs.getString("roll_no")
                                + " | Department: " + rs.getString("department")
                                + " | Year: " + rs.getInt("year")
                );
            }
        }
    }

    static void searchStudentById(Connection con, Scanner sc)
            throws SQLException {
        System.out.print("Enter Student ID: ");
        int id = Integer.parseInt(sc.nextLine());

        String sql = "SELECT * FROM students WHERE student_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Student ID: "
                            + rs.getInt("student_id"));
                    System.out.println("Name: "
                            + rs.getString("name"));
                    System.out.println("Roll No: "
                            + rs.getString("roll_no"));
                    System.out.println("Department: "
                            + rs.getString("department"));
                    System.out.println("Year: "
                            + rs.getInt("year"));
                } else {
                    System.out.println("Student not found.");
                }
            }
        }
    }

    static void searchFoodItem(Connection con, Scanner sc)
            throws SQLException {

        System.out.print("Enter item name or ID: ");
        String search = sc.nextLine();

        String sql = "SELECT * FROM menu "
                + "WHERE CAST(item_id AS CHAR) = ? "
                + "OR item_name LIKE ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, search);
            ps.setString(2, "%" + search + "%");

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.println(
                            "ID: " + rs.getInt("item_id")
                                    + " | Name: " + rs.getString("item_name")
                                    + " | Price: Rs. " + rs.getBigDecimal("price")
                                    + " | Category: " + rs.getString("category")
                                    + " | Available: " + rs.getBoolean("available")
                    );
                }

                if (!found) {
                    System.out.println("No matching food item found.");
                }
            }
        }
    }

    static void viewDailyOrders(Connection con) throws SQLException {
        String sql = "SELECT * FROM orders "
                + "WHERE DATE(order_date) = CURDATE() "
                + "ORDER BY order_date DESC";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n--- Today's Orders ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println(
                        "Order ID: " + rs.getInt("order_id")
                                + " | Student ID: " + rs.getInt("student_id")
                                + " | Amount: Rs. " + rs.getBigDecimal("total_amount")
                                + " | Status: " + rs.getString("status")
                );
            }

            if (!found) {
                System.out.println("No orders found today.");
            }
        }
    }

    static void viewStudentWiseOrders(Connection con, Scanner sc)
            throws SQLException {
        System.out.print("Enter Student ID: ");
        int studentId = Integer.parseInt(sc.nextLine());

        String sql = "SELECT * FROM orders WHERE student_id = ? "
                + "ORDER BY order_date DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;
                System.out.println("\n--- Student Orders ---");

                while (rs.next()) {
                    found = true;
                    System.out.println(
                            "Order ID: " + rs.getInt("order_id")
                                    + " | Date: " + rs.getTimestamp("order_date")
                                    + " | Amount: Rs. " + rs.getBigDecimal("total_amount")
                                    + " | Status: " + rs.getString("status")
                    );
                }

                if (!found) {
                    System.out.println("No orders found for this student.");
                }
            }
        }
    }

    static void viewOrdersByStatus(Connection con, Scanner sc)
            throws SQLException {
        System.out.print("Enter status (Completed or Pending): ");
        String status = sc.nextLine().trim();

        String sql;

        if (status.equalsIgnoreCase("Completed")) {
            sql = "SELECT * FROM orders WHERE status = 'Completed' "
                    + "ORDER BY order_date DESC";
        } else if (status.equalsIgnoreCase("Pending")) {
            sql = "SELECT * FROM orders "
                    + "WHERE status IN ('Placed', 'Preparing', 'Ready') "
                    + "ORDER BY order_date DESC";
        } else {
            System.out.println("Invalid status. Enter Completed or Pending.");
            return;
        }

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n--- " + status + " Orders ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println(
                        "Order ID: " + rs.getInt("order_id")
                                + " | Student ID: " + rs.getInt("student_id")
                                + " | Amount: Rs. " + rs.getBigDecimal("total_amount")
                                + " | Status: " + rs.getString("status")
                );
            }

            if (!found) {
                System.out.println("No " + status.toLowerCase() + " orders found.");
            }
        }
    }

    static void viewTotalSales(Connection con) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) AS total_sales "
                + "FROM orders WHERE status = 'Completed'";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                System.out.println("\n--- Total Sales Report ---");
                System.out.println(
                        "Total Sales: Rs. " + rs.getBigDecimal("total_sales")
                );
            }
        }
    }
}
