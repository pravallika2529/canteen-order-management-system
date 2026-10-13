-- Canteen Order Management System
-- Database: canteen_db
-- DBMS: MySQL 8.x
--
-- IMPORTANT:
-- Compare the table and column names below with the SQL queries in your Java
-- source code. If your Java code uses different names, update this script so
-- that the names match exactly before submission.
--
-- Run this script in MySQL Workbench or the MySQL command line.
-- It recreates the database, so any existing canteen_db database will be deleted.

DROP DATABASE IF EXISTS canteen_db;
CREATE DATABASE canteen_db;
USE canteen_db;

-- 1. Student details
CREATE TABLE Student (
    Student_ID INT PRIMARY KEY AUTO_INCREMENT,
    Student_Name VARCHAR(100) NOT NULL,
    Department VARCHAR(50) NOT NULL,
    Year INT NOT NULL
);

-- 2. Menu items
CREATE TABLE Menu (
    Item_ID INT PRIMARY KEY AUTO_INCREMENT,
    Item_Name VARCHAR(100) NOT NULL UNIQUE,
    Category VARCHAR(50) NOT NULL,
    Price DECIMAL(10,2) NOT NULL,
    Availability BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (Price >= 0)
);

-- 3. Orders
CREATE TABLE Orders (
    Order_ID INT PRIMARY KEY AUTO_INCREMENT,
    Student_ID INT NOT NULL,
    Order_Date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Total_Amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    Status ENUM('Placed', 'Preparing', 'Ready', 'Completed') NOT NULL DEFAULT 'Placed',
    CONSTRAINT fk_orders_student
        FOREIGN KEY (Student_ID) REFERENCES Student(Student_ID),
    CHECK (Total_Amount >= 0)
);

-- 4. Individual items belonging to each order
CREATE TABLE Order_Details (
    Order_ID INT NOT NULL,
    Item_ID INT NOT NULL,
    Quantity INT NOT NULL,
    Amount DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (Order_ID, Item_ID),
    CONSTRAINT fk_details_order
        FOREIGN KEY (Order_ID) REFERENCES Orders(Order_ID)
        ON DELETE CASCADE,
    CONSTRAINT fk_details_menu
        FOREIGN KEY (Item_ID) REFERENCES Menu(Item_ID),
    CHECK (Quantity > 0),
    CHECK (Amount >= 0)
);

-- Sample students
INSERT INTO Student (Student_Name, Department, Year) VALUES
('Ananya Rao', 'CSE', 2),
('Meera Sharma', 'IT', 3),
('Kavya Reddy', 'ECE', 1);

-- Sample menu items
INSERT INTO Menu (Item_Name, Category, Price, Availability) VALUES
('Idli', 'Breakfast', 30.00, TRUE),
('Dosa', 'Breakfast', 45.00, TRUE),
('Veg Sandwich', 'Snacks', 50.00, TRUE),
('Veg Fried Rice', 'Meals', 80.00, TRUE),
('Tea', 'Beverages', 15.00, TRUE),
('Coffee', 'Beverages', 20.00, TRUE);

-- Sample order and order details
INSERT INTO Orders (Student_ID, Total_Amount, Status) VALUES
(1, 75.00, 'Placed');

INSERT INTO Order_Details (Order_ID, Item_ID, Quantity, Amount) VALUES
(1, 1, 1, 30.00),
(1, 5, 3, 45.00);

-- Required/reporting query examples

-- View available menu items
SELECT Item_ID, Item_Name, Category, Price
FROM Menu
WHERE Availability = TRUE
ORDER BY Category, Item_Name;

-- View an order with student details
SELECT o.Order_ID, s.Student_Name, o.Order_Date, o.Total_Amount, o.Status
FROM Orders o
JOIN Student s ON s.Student_ID = o.Student_ID
ORDER BY o.Order_Date DESC;

-- View item-level details for an order
SELECT od.Order_ID, m.Item_Name, od.Quantity, m.Price, od.Amount
FROM Order_Details od
JOIN Menu m ON m.Item_ID = od.Item_ID
ORDER BY od.Order_ID, m.Item_Name;

-- Today's orders
SELECT Order_ID, Student_ID, Order_Date, Total_Amount, Status
FROM Orders
WHERE DATE(Order_Date) = CURDATE()
ORDER BY Order_Date DESC;

-- Student-wise order history
SELECT s.Student_ID, s.Student_Name, o.Order_ID, o.Order_Date,
       o.Total_Amount, o.Status
FROM Student s
JOIN Orders o ON o.Student_ID = s.Student_ID
ORDER BY s.Student_ID, o.Order_Date DESC;

-- Completed and pending order counts
SELECT Status, COUNT(*) AS Order_Count
FROM Orders
GROUP BY Status;

-- Total sales from completed orders
SELECT COALESCE(SUM(Total_Amount), 0.00) AS Total_Sales
FROM Orders
WHERE Status = 'Completed';
