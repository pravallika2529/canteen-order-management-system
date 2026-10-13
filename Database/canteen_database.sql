-- Canteen Order Management System
-- Database: canteen_db
-- DBMS: MySQL
--
-- This schema matches the table and column names used in Main.java.
-- WARNING: DROP DATABASE deletes the existing canteen_db and its data.
-- Back up any data you need before running this script.

DROP DATABASE IF EXISTS canteen_db;
CREATE DATABASE canteen_db;
USE canteen_db;

-- 1. Students
CREATE TABLE students (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    roll_no VARCHAR(30) NOT NULL UNIQUE,
    department VARCHAR(50) NOT NULL,
    year INT NOT NULL,
    CONSTRAINT chk_student_year CHECK (year BETWEEN 1 AND 4)
);

-- 2. Menu
CREATE TABLE menu (
    item_id INT PRIMARY KEY AUTO_INCREMENT,
    item_name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    category VARCHAR(50) NOT NULL,
    CONSTRAINT chk_menu_price CHECK (price >= 0)
);

-- 3. Orders
CREATE TABLE orders (
    order_id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    order_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status ENUM('Placed', 'Preparing', 'Ready', 'Completed', 'Cancelled')
        NOT NULL DEFAULT 'Placed',
    CONSTRAINT fk_orders_students
        FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT chk_order_total CHECK (total_amount >= 0)
);

-- 4. Items belonging to each order
CREATE TABLE order_items (
    order_id INT NOT NULL,
    item_id INT NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (order_id, item_id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_order_items_menu
        FOREIGN KEY (item_id) REFERENCES menu(item_id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_subtotal CHECK (subtotal >= 0)
);

-- Sample student data
INSERT INTO students (name, roll_no, department, year) VALUES
('Ananya Rao', 'DEMO001', 'CSE', 2),
('Meera Sharma', 'DEMO002', 'IT', 3),
('Kavya Reddy', 'DEMO003', 'ECE', 1);

-- Sample menu data
INSERT INTO menu (item_name, price, available, category) VALUES
('Idli', 30.00, TRUE, 'Breakfast'),
('Dosa', 45.00, TRUE, 'Breakfast'),
('Veg Sandwich', 50.00, TRUE, 'Snacks'),
('Veg Fried Rice', 80.00, TRUE, 'Meals'),
('Tea', 15.00, TRUE, 'Beverages'),
('Coffee', 20.00, TRUE, 'Beverages');

-- Sample order and line items
INSERT INTO orders (student_id, total_amount, status) VALUES
(1, 75.00, 'Placed');

INSERT INTO order_items (order_id, item_id, quantity, subtotal) VALUES
(1, 1, 1, 30.00),
(1, 5, 3, 45.00);

-- Required/reporting query examples

-- View available menu items
SELECT item_id, item_name, price, category
FROM menu
WHERE available = TRUE
ORDER BY category, item_name;

-- View all orders with student names
SELECT o.order_id, s.name, o.order_date, o.total_amount, o.status
FROM orders o
JOIN students s ON s.student_id = o.student_id
ORDER BY o.order_id DESC;

-- View item-level details for orders
SELECT oi.order_id, m.item_name, m.price, oi.quantity, oi.subtotal
FROM order_items oi
JOIN menu m ON m.item_id = oi.item_id
ORDER BY oi.order_id, m.item_name;

-- View today's orders
SELECT *
FROM orders
WHERE DATE(order_date) = CURDATE()
ORDER BY order_date DESC;

-- View orders for each student
SELECT s.student_id, s.name, o.order_id, o.order_date,
       o.total_amount, o.status
FROM students s
JOIN orders o ON o.student_id = s.student_id
ORDER BY s.student_id, o.order_date DESC;

-- View completed and pending orders
SELECT *
FROM orders
WHERE status = 'Completed'
   OR status IN ('Placed', 'Preparing', 'Ready')
ORDER BY order_date DESC;

-- Total sales from completed orders
SELECT COALESCE(SUM(total_amount), 0.00) AS total_sales
FROM orders
WHERE status = 'Completed';
