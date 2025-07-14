package com.example.demo.controllers.utils;

import com.example.demo.controllers.models.BorrowedBook;
import com.example.demo.controllers.models.Transaction;
import com.example.demo.controllers.models.Book;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    // Add a new transaction (rarely called directly; usually use borrowBook)
    public static boolean addTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (user_id, book_id, borrow_date, due_date, return_date, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, transaction.getUserId());
            stmt.setInt(2, transaction.getBookId());
            stmt.setDate(3, Date.valueOf(transaction.getBorrowDate()));
            stmt.setDate(4, Date.valueOf(transaction.getDueDate()));
            if (transaction.getReturnDate() != null) {
                stmt.setDate(4, Date.valueOf(transaction.getReturnDate()));
            } else {
                stmt.setNull(4, Types.DATE);
            }
            stmt.setString(5, transaction.getStatus());
            int rowsInserted = stmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Get all transactions
    public static List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                transactions.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    // Get transactions by user
    public static List<Transaction> getTransactionsByUser(int userId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    private static Transaction mapResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("transaction_id");
        int userId = rs.getInt("user_id");
        int bookId = rs.getInt("book_id");
        LocalDate borrowDate = rs.getDate("borrow_date").toLocalDate();
        LocalDate dueDate = null;
        Date sqlDueDate = rs.getDate("due_date");
        if (sqlDueDate != null) dueDate = sqlDueDate.toLocalDate();

        Date sqlReturnDate = rs.getDate("return_date");
        LocalDate returnDate = (sqlReturnDate != null) ? sqlReturnDate.toLocalDate() : null;
        String status = rs.getString("status");

        return new Transaction(id, userId, bookId, borrowDate, dueDate, returnDate, status);
    }

    // Borrow a book: atomic transaction, update status in same connection
    public static boolean borrowBook(int userId, int bookId) {
        String insertTransaction = "INSERT INTO transactions (user_id, book_id, borrow_date, due_date, status) VALUES (?, ?, ?, ?, 'Borrowed')";
        String updateBook = "UPDATE books SET copies = copies - 1 WHERE book_id = ? AND copies > 0";
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Decrement available copies
            try (PreparedStatement updateStmt = conn.prepareStatement(updateBook)) {
                updateStmt.setInt(1, bookId);
                int rows = updateStmt.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false; // No copies available
                }
            }

            // Always update status after decrement (WITH SAME CONNECTION)
            BookDAO.updateBookStatus(conn, bookId);

            // Create transaction record with due date (7 days from today)
            try (PreparedStatement insertStmt = conn.prepareStatement(insertTransaction)) {
                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, bookId);
                LocalDate today = LocalDate.now();
                LocalDate dueDate = today.plusDays(7);
                insertStmt.setDate(3, java.sql.Date.valueOf(today));
                insertStmt.setDate(4, java.sql.Date.valueOf(dueDate));
                insertStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
        return false;
    }


    // Return a book: marks as returned, increments book copies, status updated in same connection
    public static boolean returnBook(int userId, int bookId) {
        String findTransaction = "SELECT transaction_id FROM transactions WHERE user_id = ? AND book_id = ? AND status = 'Borrowed' ORDER BY borrow_date LIMIT 1";
        String updateTransaction = "UPDATE transactions SET status = 'Returned', return_date = ? WHERE transaction_id = ?";
        String updateBook = "UPDATE books SET copies = copies + 1 WHERE book_id = ?";
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Find the oldest unreturned borrow
            int transactionId = -1;
            try (PreparedStatement findStmt = conn.prepareStatement(findTransaction)) {
                findStmt.setInt(1, userId);
                findStmt.setInt(2, bookId);
                rs = findStmt.executeQuery();
                if (rs.next()) {
                    transactionId = rs.getInt("transaction_id");
                } else {
                    conn.rollback();
                    return false; // No outstanding borrow
                }
            }

            // Mark as returned
            try (PreparedStatement updateTransStmt = conn.prepareStatement(updateTransaction)) {
                updateTransStmt.setDate(1, java.sql.Date.valueOf(java.time.LocalDate.now()));
                updateTransStmt.setInt(2, transactionId);
                updateTransStmt.executeUpdate();
            }

            // Increment copies
            try (PreparedStatement updateBookStmt = conn.prepareStatement(updateBook)) {
                updateBookStmt.setInt(1, bookId);
                updateBookStmt.executeUpdate();
            }

            // Always update status after increment (WITH SAME CONNECTION)
            BookDAO.updateBookStatus(conn, bookId);

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
        return false;
    }

    // Return book by transactionId (for librarian return from Borrowed Books table)
    public static boolean returnBookTransaction(int transactionId) {
        String updateTransaction = "UPDATE transactions SET status = 'Returned', return_date = ? WHERE transaction_id = ?";
        String updateBook = "UPDATE books SET copies = copies + 1 WHERE book_id = (SELECT book_id FROM transactions WHERE transaction_id = ?)";
        String getBookId = "SELECT book_id FROM transactions WHERE transaction_id = ?";
        Connection conn = null;
        int bookId = -1;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Get bookId for status update
            try (PreparedStatement stmt = conn.prepareStatement(getBookId)) {
                stmt.setInt(1, transactionId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    bookId = rs.getInt(1);
                }
            }

            // 1. Mark transaction as returned
            try (PreparedStatement updateTransStmt = conn.prepareStatement(updateTransaction)) {
                updateTransStmt.setDate(1, java.sql.Date.valueOf(java.time.LocalDate.now()));
                updateTransStmt.setInt(2, transactionId);
                int rows = updateTransStmt.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 2. Increment copies
            try (PreparedStatement updateBookStmt = conn.prepareStatement(updateBook)) {
                updateBookStmt.setInt(1, transactionId);
                updateBookStmt.executeUpdate();
            }

            // 3. Always update status in same connection
            if (bookId != -1) {
                BookDAO.updateBookStatus(conn, bookId);
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
        return false;
    }

    // Delete a transaction
    public static boolean deleteTransaction(int transactionId) {
        String sql = "DELETE FROM transactions WHERE transaction_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, transactionId);
            int rowsDeleted = stmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    // Return list of BorrowedBook for a student (userId), only those not yet returned
    public static List<BorrowedBook> getBorrowedBooksByUser(int userId) {
        List<BorrowedBook> result = new ArrayList<>();
        String sql = "SELECT t.transaction_id, b.title, u.name, t.borrow_date, t.due_date, t.status " +
                "FROM transactions t " +
                "JOIN books b ON t.book_id = b.book_id " +
                "JOIN users u ON t.user_id = u.user_id " +
                "WHERE t.user_id = ? AND (t.status = 'Borrowed' OR t.status = 'Overdue')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(new BorrowedBook(
                        rs.getInt("transaction_id"),
                        rs.getString("title"),
                        rs.getString("name"),
                        rs.getString("borrow_date"),
                        rs.getString("due_date"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // For the action column to actually process return:
    public static boolean returnBookByTransactionId(int transactionId) {
        // You can call your existing returnBookTransaction(int transactionId)
        return returnBookTransaction(transactionId);
    }

}
