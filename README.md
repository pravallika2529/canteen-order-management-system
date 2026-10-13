# Canteen Order Management System

## Project Overview

The Canteen Order Management System is a console-based application developed using Java and MySQL. It helps manage student details, food menu items, canteen orders, billing, and order status.

The application uses JDBC (Java Database Connectivity) to connect Java with the MySQL database.

## Technologies Used

- Java
- MySQL 8.0
- JDBC
- SQL

## Main Features

- Display available food menu items.
- Register and manage student details.
- Place orders and calculate the total bill.
- View order history and order details.
- Update order status and cancel orders.
- Add, update, remove, and search menu items.
- View order reports and completed sales.

## Database Design

The project uses a MySQL database named `canteen_db` with the following tables:

- `students` — stores student information.
- `menu` — stores food items, prices, categories, and availability.
- `orders` — stores order details, dates, totals, and statuses.
- `order_items` — stores the food items and quantities associated with each order.

## Project Structure

```text
CanteenManagement/
├── Main.java
├── Database/
│   └── canteen_database.sql
└── README.md
```

Other project files, such as the JDBC driver, may be present depending on the local setup.

## Requirements

- Java Development Kit (JDK)
- MySQL Server
- MySQL Connector/J JDBC driver

## Setup Instructions

### 1. Set Up MySQL

Install and start MySQL Server.

### 2. Create the Database

The database recreation script is available at:

`Database/canteen_database.sql`

Review the SQL script before executing it. It contains a command that drops and recreates `canteen_db`, which will delete existing data in that database. Back up any important data first.

Execute the script using MySQL to create the required database, tables, relationships, and sample data.

### 3. Configure Database Credentials

The Java application connects to:

`jdbc:mysql://localhost:3306/canteen_db`

Configure the MySQL username and set the `DB_PASSWORD` environment variable to the password for that database account. Do not commit database passwords to GitHub.

### 4. Configure JDBC

Add the MySQL Connector/J driver to the project's classpath so Java can connect to MySQL.

### 5. Run the Application

Open the project in IntelliJ IDEA or another Java IDE. Ensure the JDK, JDBC driver, MySQL service, and database credentials are configured, then run `Main.java`.

Follow the instructions displayed in the console to use the application.

## Testing

The application can be tested by performing operations such as displaying the menu, registering a student, placing an order, viewing order details, and updating order status.

Record actual test results and verify that database changes are reflected correctly.

## Future Enhancements

- Develop a graphical user interface.
- Add separate student and administrator logins.
- Integrate online payments.
- Add inventory tracking and notifications.

## Repository

GitHub: https://github.com/pravallika2529/canteen-order-management-system
