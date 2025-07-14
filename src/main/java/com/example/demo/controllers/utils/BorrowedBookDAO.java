package com.example.demo.controllers.utils;

import com.example.demo.controllers.models.BorrowedBook;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BorrowedBookDAO {

    // Dashboard: Get books due in 1–2 days or already overdue, limit results
    public static List<BorrowedBook> getNearOverdueBooks(int limit) {
        List<BorrowedBook> nearOverdue = new ArrayList<>();
        String sql = "SELECT t.transaction_id, b.title AS bookTitle, u.name AS borrowerName, " +
                "t.borrow_date, t.due_date, t.status " +
                "FROM transactions t " +
                "JOIN books b ON t.book_id = b.book_id " +
                "JOIN users u ON t.user_id = u.user_id " +
                "WHERE t.status = 'Borrowed' OR t.status = 'Overdue' " +
                "ORDER BY t.due_date ASC " +
                "LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int transactionId = rs.getInt("transaction_id");
                    String dueDateStr = rs.getString("due_date");
                    String status = rs.getString("status");

                    // Check for overdue (auto-update if needed)
                    if (dueDateStr != null && status.equals("Borrowed")) {
                        LocalDate dueDate = LocalDate.parse(dueDateStr);
                        if (dueDate.isBefore(LocalDate.now())) {
                            markAsOverdue(transactionId);
                            status = "Overdue";
                        }
                    }

                    // Filter: only add if due in next 2 days or overdue
                    boolean isNearOverdue = false;
                    if (dueDateStr != null) {
                        LocalDate dueDate = LocalDate.parse(dueDateStr);
                        LocalDate now = LocalDate.now();
                        if (status.equals("Overdue") || // Already overdue
                                (!dueDate.isBefore(now) && !dueDate.isAfter(now.plusDays(2)))) {
                            isNearOverdue = true;
                        }
                    }
                    if (isNearOverdue) {
                        nearOverdue.add(new BorrowedBook(
                                transactionId,
                                rs.getString("bookTitle"),
                                rs.getString("borrowerName"),
                                rs.getString("borrow_date"),
                                dueDateStr,
                                status
                        ));
                    }
                    // Stop if we've reached the limit (since filter is after limit in SQL)
                    if (nearOverdue.size() == limit) break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nearOverdue;
    }

    // Get all currently borrowed books (includes Overdue)
    public static List<BorrowedBook> getAllBorrowedBooks() {
        List<BorrowedBook> borrowedBooks = new ArrayList<>();
        String sql = "SELECT t.transaction_id, b.title AS bookTitle, u.name AS borrowerName, " +
                "t.borrow_date, t.due_date, t.status " +
                "FROM transactions t " +
                "JOIN books b ON t.book_id = b.book_id " +
                "JOIN users u ON t.user_id = u.user_id " +
                "WHERE t.status = 'Borrowed' OR t.status = 'Overdue'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int transactionId = rs.getInt("transaction_id");
                String dueDateStr = rs.getString("due_date");
                String status = rs.getString("status");

                // Check if overdue (Borrowed status only)
                if (dueDateStr != null && status.equals("Borrowed")) {
                    LocalDate dueDate = LocalDate.parse(dueDateStr);
                    if (dueDate.isBefore(LocalDate.now())) {
                        // Update status to Overdue in DB
                        markAsOverdue(transactionId);
                        status = "Overdue";
                    }
                }
                borrowedBooks.add(new BorrowedBook(
                        transactionId,
                        rs.getString("bookTitle"),
                        rs.getString("borrowerName"),
                        rs.getString("borrow_date"),
                        dueDateStr,
                        status
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return borrowedBooks;
    }

    // Helper to mark a transaction as Overdue
    private static void markAsOverdue(int transactionId) {
        String sql = "UPDATE transactions SET status = 'Overdue' WHERE transaction_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, transactionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Get all transactions regardless of status (for full borrow history)
    public static List<BorrowedBook> getAllBorrowHistory() {
        List<BorrowedBook> borrowedBooks = new ArrayList<>();
        String sql = "SELECT t.transaction_id, b.title AS bookTitle, u.name AS borrowerName, " +
                "t.borrow_date, t.due_date, t.status " +
                "FROM transactions t " +
                "JOIN books b ON t.book_id = b.book_id " +
                "JOIN users u ON t.user_id = u.user_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                borrowedBooks.add(new BorrowedBook(
                        rs.getInt("transaction_id"),
                        rs.getString("bookTitle"),
                        rs.getString("borrowerName"),
                        rs.getString("borrow_date"),
                        rs.getString("due_date"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return borrowedBooks;
    }

    // Search currently borrowed (and overdue) books by book title or borrower name
    public static List<BorrowedBook> searchBorrowedBooks(String keyword) {
        List<BorrowedBook> borrowedBooks = new ArrayList<>();
        String sql = "SELECT t.transaction_id, b.title AS bookTitle, u.name AS borrowerName, " +
                "t.borrow_date, t.due_date, t.status " +
                "FROM transactions t " +
                "JOIN books b ON t.book_id = b.book_id " +
                "JOIN users u ON t.user_id = u.user_id " +
                "WHERE (t.status = 'Borrowed' OR t.status = 'Overdue') " +
                "AND (LOWER(b.title) LIKE ? OR LOWER(u.name) LIKE ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String pattern = "%" + keyword.toLowerCase() + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int transactionId = rs.getInt("transaction_id");
                    String dueDateStr = rs.getString("due_date");
                    String status = rs.getString("status");

                    // Check for overdue
                    if (dueDateStr != null && status.equals("Borrowed")) {
                        LocalDate dueDate = LocalDate.parse(dueDateStr);
                        if (dueDate.isBefore(LocalDate.now())) {
                            markAsOverdue(transactionId);
                            status = "Overdue";
                        }
                    }
                    borrowedBooks.add(new BorrowedBook(
                            transactionId,
                            rs.getString("bookTitle"),
                            rs.getString("borrowerName"),
                            rs.getString("borrow_date"),
                            dueDateStr,
                            status
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return borrowedBooks;
    }
}
