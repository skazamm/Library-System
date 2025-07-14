package com.example.demo.controllers.utils;

import com.example.demo.controllers.models.Book;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    // Add a new book (status auto-set based on copies)
    public static boolean addBook(Book book) {
        String sql = "INSERT INTO books (title, author, isbn, status, copies, date_added) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setString(3, book.getIsbn());
            stmt.setString(4, (book.getCopies() > 0) ? "Available" : "Unavailable");
            stmt.setInt(5, book.getCopies());
            stmt.setTimestamp(6, book.getDateAdded()); // New field for date_added
            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    updateBookStatus(conn, keys.getInt(1));
                }
            }
            return rowsInserted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get all books (never throws, always returns a list)
    public static List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getString("status"),
                        rs.getInt("copies"),
                        rs.getTimestamp("date_added") // New
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    // --- NEW FOR DASHBOARD: Get the most recently added books ---
    public static List<Book> getRecentBooks(int limit) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY date_added DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getString("status"),
                        rs.getInt("copies"),
                        rs.getTimestamp("date_added") // New
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }
    // ----------------------------------------------------------

    // Get a book by ID
    public static Book getBookById(int bookId) {
        String sql = "SELECT * FROM books WHERE book_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Book(
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getString("status"),
                        rs.getInt("copies"),
                        rs.getTimestamp("date_added") // New
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updateBook(Book book) {
        String sql = "UPDATE books SET title = ?, author = ?, isbn = ?, status = ?, copies = ?, date_added = ? WHERE book_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setString(3, book.getIsbn());
            stmt.setString(4, book.getStatus());
            stmt.setInt(5, book.getCopies());
            stmt.setTimestamp(6, book.getDateAdded()); // New
            stmt.setInt(7, book.getBookId());
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Delete a book with user-friendly business error for FK violations
    public static boolean deleteBook(int bookId) {
        String sql = "DELETE FROM books WHERE book_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            int rowsDeleted = stmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            // PostgreSQL SQLState "23503" = foreign key violation (book is referenced in another table)
            if ("23503".equals(e.getSQLState())) {
                System.err.println("Book is referenced and cannot be deleted."); // log for devs
                return false; // Controller will show a user-friendly message
            }
            e.printStackTrace();
            return false;
        }
    }

    // Search books by title, author, or isbn (never throws, always returns a list)
    public static List<Book> searchBooks(String keyword) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE LOWER(title) LIKE ? OR LOWER(author) LIKE ? OR LOWER(isbn) LIKE ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String like = "%" + keyword.toLowerCase() + "%";
            stmt.setString(1, like);
            stmt.setString(2, like);
            stmt.setString(3, like);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getString("status"),
                        rs.getInt("copies"),
                        rs.getTimestamp("date_added") // New
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    // Mark a book as returned (increment copies, status auto-updated)
    public static boolean returnBook(int bookId) {
        String sql = "UPDATE books SET copies = copies + 1 WHERE book_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                updateBookStatus(conn, bookId);
            }
            return rowsUpdated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Borrow a book (decrement copies, status auto-updated)
    public static boolean borrowBook(int bookId) {
        String sql = "UPDATE books SET copies = copies - 1 WHERE book_id = ? AND copies > 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                updateBookStatus(conn, bookId);
            }
            return rowsUpdated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Always call this after any change to the 'copies' column!
    public static void updateBookStatus(int bookId) {
        try (Connection conn = DBConnection.getConnection()) {
            updateBookStatus(conn, bookId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Overloaded for atomic update within a transaction (private)
    public static void updateBookStatus(Connection conn, int bookId) {
        String sql = "UPDATE books SET status = CASE WHEN copies > 0 THEN 'Available' ELSE 'Unavailable' END WHERE book_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
