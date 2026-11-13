CREATE DATABASE IF NOT EXISTS school_library;
USE school_library;

CREATE TABLE IF NOT EXISTS books (
    book_id VARCHAR(20) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    author VARCHAR(100) NOT NULL,
    status VARCHAR(20) DEFAULT 'Available',
    category VARCHAR(50) DEFAULT 'BOOK'
);

INSERT IGNORE INTO books (book_id, title, author, status, category) VALUES
('B001', 'The Hobbit', 'J.R.R. Tolkien', 'Available', 'Fantasy'),
('B002', 'Harry Potter and the Sorcerer''s Stone', 'J.K. Rowling', 'Available', 'Fantasy'),
('B003', 'Wonder', 'R.J. Palacio', 'Borrowed', 'Fiction'),
('B004', 'The Little Prince', 'Antoine de Saint-Exupery', 'Available', 'Fiction'),
('B005', 'Noli Me Tangere', 'Jose Rizal', 'Available', 'Classic'),
('B006', 'El Filibusterismo', 'Jose Rizal', 'Available', 'Classic'),
('B007', 'Charlotte''s Web', 'E.B. White', 'Available', 'Children'),
('B008', 'The Giver', 'Lois Lowry', 'Available', 'Science Fiction'),
('B009', 'Percy Jackson and the Lightning Thief', 'Rick Riordan', 'Available', 'Fantasy'),
('B010', 'The Alchemist', 'Paulo Coelho', 'Available', 'Fiction');
